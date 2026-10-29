package com.example.travelapp.ui.home

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.travelapp.ui.theme.*
import com.example.travelapp.viewmodel.CreateTripViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Modello per ogni tappa creata dall'agenzia
data class EditableStepDraft(
    val title: String = "",
    val duration: String = "",
    val ticketInfo: String = " Biglietto incluso",
    val description: String = ""
)

// Codifica la panoramica e le tappe personalizzate in una stringa strutturata salvata su DB
fun encodeOverviewAndSteps(overview: String, steps: List<EditableStepDraft>, isActivity: Boolean): String {
    val cleanOverview = overview.trim()
    val validSteps = steps.filter { it.title.isNotBlank() || it.description.isNotBlank() }
    if (validSteps.isEmpty()) return cleanOverview

    val serializedSteps = validSteps.mapIndexed { idx, step ->
        val safeTitle = step.title.trim().ifEmpty {
            if (isActivity) "Tappa ${idx + 1}" else "Giorno ${idx + 1}"
        }.replace("||", " - ")
        val safeDuration = step.duration.trim().ifEmpty {
            if (isActivity) "45 min" else "Giorno ${idx + 1}"
        }.replace("||", " ")
        val safeTicket = step.ticketInfo.trim().ifEmpty {
            " Tappa inclusa"
        }.replace("||", " ")
        val safeDesc = step.description.trim().ifEmpty {
            "Visita guidata e tempo dedicato all'esplorazione della tappa."
        }.replace("||", " ").replace("\n", " ")

        "@@STEP@@$safeTitle||$safeDuration||$safeTicket||$safeDesc"
    }.joinToString("\n")

    return "$cleanOverview\n$serializedSteps"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTripScreen(
    tokenUtente: String,
    viewModel: CreateTripViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onNavigateBack: () -> Unit
) {
    var offerType by remember { mutableStateOf("VIAGGIO") }

    var title by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var overview by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var spots by remember { mutableStateOf("") }

    var selectedTransportType by remember { mutableStateOf("Treno Alta Velocità A/R") }
    var transportDetails by remember { mutableStateOf("Transfer stazione/aeroporto - hotel incluso") }
    var hotelName by remember { mutableStateOf("Boutique Hotel 4★ Centrale") }
    var selectedBoardType by remember { mutableStateOf("Colazione inclusa (B&B)") }

    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }

    // Lista dinamica di tappe Step-by-Step impostate dall'agenzia durante la creazione
    val itinerarySteps = remember {
        mutableStateListOf(
            EditableStepDraft(
                title = "",
                duration = "1 ora",
                ticketInfo = " Biglietto salta-fila incluso",
                description = ""
            ),
            EditableStepDraft(
                title = "",
                duration = "45 min",
                ticketInfo = " Visita guidata inclusa",
                description = ""
            )
        )
    }

    var imageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    val context = LocalContext.current

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 5),
        onResult = { uris ->
            if (uris.isNotEmpty()) {
                uris.forEach { uri ->
                    try {
                        context.contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                imageUris = uris
            }
        }
    )

    LaunchedEffect(viewModel.isSuccess) {
        if (viewModel.isSuccess) onNavigateBack()
    }

    if (showDatePicker) {
        val formatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

        if (offerType == "VIAGGIO") {
            val dateRangePickerState = rememberDateRangePickerState()
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val startMillis = dateRangePickerState.selectedStartDateMillis
                            val endMillis = dateRangePickerState.selectedEndDateMillis
                            if (startMillis != null && endMillis != null) {
                                startDate = formatter.format(Date(startMillis))
                                endDate = formatter.format(Date(endMillis))
                            }
                            showDatePicker = false
                        }
                    ) { Text("Conferma", fontWeight = FontWeight.Bold, color = TripGreen) }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) { Text("Annulla") }
                }
            ) {
                DateRangePicker(
                    state = dateRangePickerState,
                    title = { Text(text = "Seleziona le date del viaggio", modifier = Modifier.padding(16.dp)) },
                    headline = { Text(text = "Partenza - Ritorno", modifier = Modifier.padding(start = 16.dp, bottom = 16.dp)) },
                    showModeToggle = false,
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            val singleDatePickerState = rememberDatePickerState()
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val selectedMillis = singleDatePickerState.selectedDateMillis
                            if (selectedMillis != null) {
                                val formatted = formatter.format(Date(selectedMillis))
                                startDate = formatted
                                endDate = formatted
                            }
                            showDatePicker = false
                        }
                    ) { Text("Conferma", fontWeight = FontWeight.Bold, color = TripOrangeActivity) }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) { Text("Annulla") }
                }
            ) {
                DatePicker(
                    state = singleDatePickerState,
                    title = { Text(text = "Data di inizio disponibilità dell'Attività", modifier = Modifier.padding(16.dp)) }
                )
            }
        }
    }

    val isActivity = offerType == "ATTIVITA"
    val accentColor = if (isActivity) TripOrangeActivity else TripGreen

    Box(modifier = Modifier.fillMaxSize().background(TripWarmBg)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(290.dp)
                .clickable { photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
        ) {
            if (imageUris.isNotEmpty()) {
                val pagerState = rememberPagerState(pageCount = { imageUris.size })

                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                    AsyncImage(
                        model = imageUris[page],
                        contentDescription = "Immagine $page",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 48.dp, end = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("${pagerState.currentPage + 1}/${imageUris.size}", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Add, contentDescription = "Add Photo", modifier = Modifier.size(48.dp), tint = accentColor)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Tocca per caricare fino a 5 foto dalla galleria", color = accentColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)), startY = 320f))
            )
        }

        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier
                .padding(top = 44.dp, start = 16.dp)
                .size(42.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.9f))
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Indietro", tint = TripBlack)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 250.dp)
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .background(Color.White)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                text = if (!isActivity) "Nuovo Pacchetto Viaggio" else "Nuova Attività / Escursione",
                style = MaterialTheme.typography.headlineLarge,
                color = TripBlack
            )

            // Selettore Viaggio vs Attività
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (!isActivity) TripGreen else TripWarmBg,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { offerType = "VIAGGIO" }
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlightTakeoff,
                            contentDescription = null,
                            tint = if (!isActivity) Color.White else TripGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Viaggio Multi-Giorno",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            color = if (!isActivity) Color.White else TripBlack
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isActivity) TripOrangeActivity else TripWarmBg,
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            offerType = "ATTIVITA"
                            if (startDate.isNotEmpty()) endDate = startDate
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ConfirmationNumber,
                            contentDescription = null,
                            tint = if (isActivity) Color.White else TripOrangeActivity,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Attività in Giornata",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            color = if (isActivity) Color.White else TripBlack
                        )
                    }
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = {
                    Text(
                        if (!isActivity) "Titolo del viaggio (es. Weekend a Losanna)"
                        else "Nome attività (es. Tour guidato Colosseo)"
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )

            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Città / Punto d'incontro (es. Roma, Losanna)") },
                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = accentColor) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )

            if (!isActivity) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(modifier = Modifier.weight(1f).clickable { showDatePicker = true }) {
                        OutlinedTextField(
                            value = startDate.ifEmpty { "Seleziona" },
                            onValueChange = {},
                            readOnly = true,
                            enabled = false,
                            label = { Text("Data Inizio") },
                            leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null, tint = TripGreen) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = TripBlack,
                                disabledBorderColor = Color.Gray,
                                disabledLabelColor = Color.DarkGray,
                                disabledLeadingIconColor = TripGreen
                            )
                        )
                    }
                    Box(modifier = Modifier.weight(1f).clickable { showDatePicker = true }) {
                        OutlinedTextField(
                            value = endDate.ifEmpty { "Seleziona" },
                            onValueChange = {},
                            readOnly = true,
                            enabled = false,
                            label = { Text("Data Fine") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = TripBlack,
                                disabledBorderColor = Color.Gray,
                                disabledLabelColor = Color.DarkGray
                            )
                        )
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true }) {
                    OutlinedTextField(
                        value = startDate.ifEmpty { "Tocca per scegliere la data di apertura calendario" },
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        label = { Text("Data Apertura Calendario Attività") },
                        leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null, tint = TripOrangeActivity) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = TripBlack,
                            disabledBorderColor = Color.Gray,
                            disabledLabelColor = Color.DarkGray,
                            disabledLeadingIconColor = TripOrangeActivity
                        )
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Prezzo Base (€)") },
                    leadingIcon = { Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = accentColor) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp)
                )
                OutlinedTextField(
                    value = spots,
                    onValueChange = { spots = it },
                    label = { Text(if (isActivity) "Posti per Data" else "Posti per Turno") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = accentColor) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp)
                )
            }

            if (!isActivity) {
                val transportPresets = listOf(
                    "Volo A/R Incluso",
                    "Treno Alta Velocità A/R",
                    "Bus Gran Turismo",
                    "Nave / Traghetto A/R"
                )
                val boardPresets = listOf(
                    "Colazione inclusa (B&B)",
                    "Mezza Pensione",
                    "Pensione Completa",
                    "All Inclusive"
                )

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = TripWarmBg,
                    border = BorderStroke(1.dp, TripGreen.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FlightTakeoff, contentDescription = null, tint = TripGreen, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Logistica, Trasporto & Ospitalità",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TripBlack
                                )
                                Text(
                                    text = "Specifica i servizi compresi nel pacchetto multi-giorno",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                        }

                        Text("Mezzo di Trasporto principale:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TripBlack)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(transportPresets) { item ->
                                OfferTypeChip(
                                    label = item,
                                    selected = selectedTransportType == item,
                                    icon = Icons.Default.FlightTakeoff
                                ) {
                                    selectedTransportType = item
                                }
                            }
                        }

                        OutlinedTextField(
                            value = transportDetails,
                            onValueChange = { transportDetails = it },
                            label = { Text("Dettagli Operativi Trasporto (Transfer / Bagaglio)") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.4f))

                        Text("Struttura Ricettiva e Trattamento:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TripBlack)
                        OutlinedTextField(
                            value = hotelName,
                            onValueChange = { hotelName = it },
                            label = { Text("Struttura Alberghiera (Nome e Categoria)") },
                            leadingIcon = { Icon(Icons.Default.Hotel, contentDescription = null, tint = TripGreen) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(boardPresets) { board ->
                                OfferTypeChip(
                                    label = board,
                                    selected = selectedBoardType == board,
                                    icon = Icons.Default.Restaurant
                                ) {
                                    selectedBoardType = board
                                }
                            }
                        }
                    }
                }
            }

            OutlinedTextField(
                value = overview,
                onValueChange = { overview = it },
                label = {
                    Text("Panoramica generale dell'esperienza (Introduzione)")
                },
                placeholder = {
                    Text("Descrivi in breve cosa rende unica questa esperienza e cosa comprende...")
                },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                shape = RoundedCornerShape(16.dp)
            )

            // =========================================================================
            // COSTRUTTORE DINAMICO ITINERARIO STEP-BY-STEP (STILE TRIPADVISOR)
            // =========================================================================
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.4f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Place, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isActivity) "Costruttore Tappe dell'Attività"
                            else "Costruttore Programma Giorno per Giorno",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TripBlack
                        )
                    }
                    Text(
                        text = "Definisci gli step che il viaggiatore vedrà nella timeline della pagina dettagli",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            val inclusionPresets = remember(isActivity) {
                if (isActivity) {
                    listOf(
                        "Biglietto salta-fila incluso",
                        "Sosta con guida ufficiale",
                        "Degustazione inclusa",
                        "Sosta panoramica",
                        "Trasporto / Barca incluso"
                    )
                } else {
                    listOf(
                        "Hotel + Colazione inclusi",
                        "Ingressi e guida inclusi",
                        "Cena tipica inclusa",
                        "Trasferimenti inclusi",
                        "Assistenza coordinatore"
                    )
                }
            }



            itinerarySteps.forEachIndexed { index, step ->
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = TripWarmBg,
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(accentColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = if (isActivity) "Tappa #${index + 1} dell'escursione"
                                    else "Tappa / Giorno #${index + 1} del viaggio",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = TripBlack
                                )
                            }

                            if (itinerarySteps.size > 1) {
                                IconButton(
                                    onClick = { itinerarySteps.removeAt(index) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Rimuovi tappa",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = step.title,
                            onValueChange = { itinerarySteps[index] = step.copy(title = it) },
                            label = {
                                Text(
                                    if (isActivity) "Nome luogo / tappa (es. Arena dei Gladiatori)"
                                    else "Titolo della giornata (es. Arrivo e Centro Storico)"
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = step.duration,
                            onValueChange = { itinerarySteps[index] = step.copy(duration = it) },
                            label = { Text(if (isActivity) "Durata della sosta (es. 45 min)" else "Periodo (es. Giorno 1)") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )



                        OutlinedTextField(
                            value = step.description,
                            onValueChange = { itinerarySteps[index] = step.copy(description = it) },
                            label = { Text("Cosa si fa in questa tappa") },
                            placeholder = { Text("Spiega passo dopo passo cosa vedranno o faranno i partecipanti...") },
                            minLines = 2,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            OutlinedButton(
                onClick = {
                    val nextNum = itinerarySteps.size + 1
                    itinerarySteps.add(
                        EditableStepDraft(
                            title = "",
                            duration = if (isActivity) "45 min" else "Giorno $nextNum",
                            ticketInfo = if (isActivity) "Sosta con guida ufficiale" else "Ingressi e guida inclusi",
                            description = ""
                        )
                    )
                },
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.5.dp, accentColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = accentColor)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isActivity) "Aggiungi un'altra Tappa all'Attività"
                    else "Aggiungi Tappa / Giornata al Viaggio",
                    color = accentColor,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            viewModel.errorMessage?.let { error ->
                Text(text = error, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
            }

            Button(
                onClick = {
                    val finalEndDate = if (isActivity) startDate else endDate
                    val baseOverview = overview.ifBlank { "Esperienza ufficiale organizzata a $location." }
                    val overviewWithLogistics = if (!isActivity) {
                        "@@META@@${hotelName.trim()}||$selectedBoardType||$selectedTransportType • ${transportDetails.trim()}\n$baseOverview"
                    } else {
                        baseOverview
                    }
                    val combinedDescriptionWithSteps = encodeOverviewAndSteps(
                        overview = overviewWithLogistics,
                        steps = itinerarySteps,
                        isActivity = isActivity
                    )
                    viewModel.createTrip(
                        context,
                        tokenUtente,
                        title,
                        combinedDescriptionWithSteps,
                        price,
                        startDate,
                        finalEndDate,
                        spots,
                        location,
                        imageUris
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                modifier = Modifier.fillMaxWidth().height(58.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = !viewModel.isLoading
            ) {
                if (viewModel.isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text(
                        text = if (!isActivity) "Pubblica Viaggio e Programma" else "Pubblica Attività e Tappe",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}