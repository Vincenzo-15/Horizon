package com.example.travelapp.ui.home

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
    var description by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var spots by remember { mutableStateOf("") }

    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }

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
                    title = { Text(text = "Data dell'Attività / Escursione", modifier = Modifier.padding(16.dp)) }
                )
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(TripWarmBg)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
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
                        .background(TripGreen.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Add, contentDescription = "Add Photo", modifier = Modifier.size(48.dp), tint = TripGreen)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Tocca per aggiungere fino a 5 foto", color = TripGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)), startY = 350f))
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
                .padding(top = 260.dp)
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .background(Color.White)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                text = if (offerType == "VIAGGIO") "Nuovo Pacchetto Viaggio" else "Nuova Attività / Escursione",
                style = MaterialTheme.typography.headlineLarge,
                color = TripBlack
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (offerType == "VIAGGIO") TripGreen else TripWarmBg,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { offerType = "VIAGGIO" }
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "✈️ Viaggio",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = if (offerType == "VIAGGIO") Color.White else TripBlack
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (offerType == "ATTIVITA") TripOrangeActivity else TripWarmBg,
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
                        Text(
                            text = "🎟️ Attività / Tour",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = if (offerType == "ATTIVITA") Color.White else TripBlack
                        )
                    }
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = {
                    Text(
                        if (offerType == "VIAGGIO") "Titolo del viaggio (es. Weekend a Losanna)"
                        else "Nome attività (es. Tour guidato Colosseo)"
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )

            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Città / Località (es. Roma, Losanna)") },
                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = TripGreen) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )

            if (offerType == "VIAGGIO") {
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
                        value = startDate.ifEmpty { "Tocca per scegliere il giorno dell'attività" },
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        label = { Text("Data dell'Escursione / Attività") },
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

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = {
                    Text(
                        if (offerType == "VIAGGIO") "Programma e descrizione del viaggio"
                        else "Dettagli dell'attività, durata in ore e punto d'incontro"
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                shape = RoundedCornerShape(16.dp)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Prezzo Base (€)") },
                    leadingIcon = { Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = TripGreen) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp)
                )
                OutlinedTextField(
                    value = spots,
                    onValueChange = { spots = it },
                    label = { Text("Posti totali") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = TripGreen) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp)
                )
            }

            viewModel.errorMessage?.let { error ->
                Text(text = error, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
            }

            Button(
                onClick = {
                    val finalEndDate = if (offerType == "ATTIVITA") startDate else endDate
                    viewModel.createTrip(
                        context,
                        tokenUtente,
                        title,
                        description,
                        price,
                        startDate,
                        finalEndDate,
                        spots,
                        location,
                        imageUris
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (offerType == "VIAGGIO") TripGreen else TripOrangeActivity
                ),
                modifier = Modifier.fillMaxWidth().height(58.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = !viewModel.isLoading
            ) {
                if (viewModel.isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text(
                        text = if (offerType == "VIAGGIO") "Pubblica Viaggio" else "Pubblica Attività",
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