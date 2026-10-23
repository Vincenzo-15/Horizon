package com.example.travelapp.ui.home

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.travelapp.network.TripDTO
import com.example.travelapp.network.TripRequest
import com.example.travelapp.ui.theme.*
import com.example.travelapp.viewmodel.TripDetailViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

data class DynamicAvailabilityOption(
    val id: Int,
    val title: String,
    val subtitle: String,
    val badge: String,
    val pricePerPerson: Double,
    val availableSpots: Int
)

data class CalendarDayPrice(
    val dayOfMonth: Int,
    val dateIso: String,
    val formattedLabel: String,
    val price: Double,
    val tier: String, // "WEEKEND", "PROMO", "BASE"
    val badgeText: String
)

data class ExperienceReview(
    val author: String,
    val rating: Int,
    val date: String,
    val comment: String
)

// Genera i turni per i Viaggi Multi-Giorno
fun buildMultiDayTripOptions(
    trip: TripDTO,
    weekendMarkupPercent: Int,
    midweekDiscountPercent: Int
): List<DynamicAvailabilityOption> {
    val basePrice = trip.price
    val realSpots = trip.availableSpots.coerceAtLeast(0)
    val sdfInput = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val sdfDayNum = SimpleDateFormat("dd MMM", Locale.ITALY)

    val startCal = Calendar.getInstance()
    try {
        if (!trip.startDate.isNullOrBlank()) {
            sdfInput.parse(trip.startDate)?.let { startCal.time = it }
        }
    } catch (_: Exception) {}

    val endCal = (startCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 4) }
    try {
        if (!trip.endDate.isNullOrBlank()) {
            sdfInput.parse(trip.endDate)?.let { endCal.time = it }
        }
    } catch (_: Exception) {}

    val durationDays = (((endCal.timeInMillis - startCal.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()).coerceAtLeast(2)

    val slot1Start = sdfDayNum.format(startCal.time)
    val slot1End = sdfDayNum.format(endCal.time)

    val cal2Start = (startCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 7) }
    val cal2End = (cal2Start.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, durationDays) }

    val cal3Start = (startCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 14) }
    val cal3End = (cal3Start.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, durationDays) }

    val highSeasonPrice = ((basePrice * (1.0 + weekendMarkupPercent / 100.0))).roundToInt().toDouble()
    val earlyBirdPrice = ((basePrice * (1.0 - midweekDiscountPercent / 100.0))).roundToInt().toDouble()

    return listOf(
        DynamicAvailabilityOption(
            id = 0,
            title = "1° Turno: $slot1Start → $slot1End",
            subtitle = "Partenza Principale • Pacchetto Standard ($durationDays giorni)",
            badge = "Tariffa Ufficiale",
            pricePerPerson = basePrice,
            availableSpots = realSpots
        ),
        DynamicAvailabilityOption(
            id = 1,
            title = "2° Turno: ${sdfDayNum.format(cal2Start.time)} → ${sdfDayNum.format(cal2End.time)}",
            subtitle = "Partenza Alta Richiesta + Polizza Flex inclusa",
            badge = "Alta Richiesta (+$weekendMarkupPercent%)",
            pricePerPerson = highSeasonPrice,
            availableSpots = realSpots
        ),
        DynamicAvailabilityOption(
            id = 2,
            title = "3° Turno: ${sdfDayNum.format(cal3Start.time)} → ${sdfDayNum.format(cal3End.time)}",
            subtitle = "Partenza Smart • Tariffa Early Bird prenotazione anticipata",
            badge = if (midweekDiscountPercent > 0) "Early Bird (-$midweekDiscountPercent%)" else "Disponibile",
            pricePerPerson = earlyBirdPrice,
            availableSpots = realSpots
        )
    )
}

// Genera la griglia del mese per il vero Calendario delle Attività con prezzi dinamici
fun buildMonthCalendarDays(
    year: Int,
    month: Int,
    basePrice: Double,
    weekendMarkupPercent: Int,
    midweekDiscountPercent: Int
): Pair<Int, List<CalendarDayPrice>> {
    val cal = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month)
        set(Calendar.DAY_OF_MONTH, 1)
    }

    // Calcola l'offset del primo giorno della settimana (Lunedì = 0 ... Domenica = 6)
    val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
    val emptyLeadingCells = if (firstDayOfWeek == Calendar.SUNDAY) 6 else firstDayOfWeek - Calendar.MONDAY

    val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val sdfIso = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val sdfLabel = SimpleDateFormat("EEEE dd MMMM yyyy", Locale.ITALY)

    val days = (1..maxDays).map { day ->
        cal.set(Calendar.DAY_OF_MONTH, day)
        val dow = cal.get(Calendar.DAY_OF_WEEK)
        val isWeekend = dow == Calendar.FRIDAY || dow == Calendar.SATURDAY || dow == Calendar.SUNDAY
        val isPromo = dow == Calendar.TUESDAY || dow == Calendar.WEDNESDAY

        val multiplier = when {
            isWeekend -> 1.0 + (weekendMarkupPercent / 100.0)
            isPromo -> 1.0 - (midweekDiscountPercent / 100.0)
            else -> 1.0
        }
        val computedPrice = ((basePrice * multiplier) * 10.0).roundToInt() / 10.0

        CalendarDayPrice(
            dayOfMonth = day,
            dateIso = sdfIso.format(cal.time),
            formattedLabel = sdfLabel.format(cal.time).replaceFirstChar { it.uppercase() },
            price = computedPrice,
            tier = when {
                isWeekend -> "WEEKEND"
                isPromo -> "PROMO"
                else -> "BASE"
            },
            badgeText = when {
                isWeekend -> "Tariffa Weekend (+$weekendMarkupPercent%)"
                isPromo && midweekDiscountPercent > 0 -> "Tariffa Infrasettimanale (-$midweekDiscountPercent%)"
                else -> "Tariffa Standard"
            }
        )
    }
    return emptyLeadingCells to days
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDetailScreen(
    tripId: Long,
    tokenUtente: String,
    ruoloUtente: String?,
    viewModel: TripDetailViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    var reviewsSectionY by remember { mutableStateOf(0) }

    var currentTripId by remember { mutableStateOf(tripId) }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDynamicPricingDialog by remember { mutableStateOf(false) }
    var showMockPaymentSheet by remember { mutableStateOf(false) }
    var showAddReviewDialog by remember { mutableStateOf(false) }
    var isFavorite by remember { mutableStateOf(false) }

    // Stato per visualizzazione foto ingrandite a tutto schermo (Lightbox)
    var fullscreenImageIndex by remember { mutableStateOf<Int?>(null) }

    // Se l'utente ha già prenotato, nascondiamo il tasto Prenota Ora a meno che non clicchi su "Prenota di nuovo"
    var wantToBookAgain by remember(currentTripId) { mutableStateOf(false) }

    var spotsToBook by remember { mutableStateOf(1) }
    var selectedOptionIndex by remember { mutableStateOf(0) }

    val pricingPrefs = remember { context.getSharedPreferences("dynamic_pricing_prefs", Context.MODE_PRIVATE) }
    var weekendMarkupPercent by remember(currentTripId) {
        mutableStateOf(pricingPrefs.getInt("markup_$currentTripId", 20))
    }
    var midweekDiscountPercent by remember(currentTripId) {
        mutableStateOf(pricingPrefs.getInt("discount_$currentTripId", 15))
    }

    var editTitle by remember { mutableStateOf("") }
    var editDescription by remember { mutableStateOf("") }
    var editPrice by remember { mutableStateOf("") }
    var editLocation by remember { mutableStateOf("") }
    var editAvailableSpots by remember { mutableStateOf("") }

    val reviews = remember(currentTripId) {
        mutableStateListOf(
            ExperienceReview(
                author = "Giulia M.",
                rating = 5,
                date = "Ottobre 2026",
                comment = "Esperienza impeccabile! Organizzazione puntuale, guida preparatissima e ottimo rapporto qualità/prezzo. Consigliatissimo!"
            ),
            ExperienceReview(
                author = "Alessandro R.",
                rating = 5,
                date = "Settembre 2026",
                comment = "Prenotato insieme ai miei amici tramite una lista condivisa su Horizon. Tutto corrispondeva alla descrizione."
            ),
            ExperienceReview(
                author = "Elena V.",
                rating = 4,
                date = "Agosto 2026",
                comment = "Molto bello e ben strutturato, comodissima anche l'esportazione delle date nel calendario del telefono."
            )
        )
    }

    LaunchedEffect(currentTripId) {
        selectedOptionIndex = 0
        spotsToBook = 1
        wantToBookAgain = false
        viewModel.fetchTripDetail(tokenUtente, currentTripId)
    }

    LaunchedEffect(viewModel.isDeleted) {
        if (viewModel.isDeleted) onNavigateBack()
    }

    val trip = viewModel.trip
    val isActivity = trip?.let { isActivityOrExcursion(it) } ?: false

    // Stato del Calendario Mensile per le Attività
    val initialCal = remember(trip) {
        val c = Calendar.getInstance()
        try {
            if (!trip?.startDate.isNullOrBlank()) {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(trip!!.startDate)?.let { c.time = it }
            }
        } catch (_: Exception) {}
        c
    }
    var calendarYear by remember(trip) { mutableStateOf(initialCal.get(Calendar.YEAR)) }
    var calendarMonth by remember(trip) { mutableStateOf(initialCal.get(Calendar.MONTH)) }
    var selectedCalendarDay by remember(trip) { mutableStateOf(initialCal.get(Calendar.DAY_OF_MONTH)) }

    val (leadingEmptyCells, monthDays) = remember(trip, calendarYear, calendarMonth, weekendMarkupPercent, midweekDiscountPercent) {
        if (trip != null) {
            buildMonthCalendarDays(calendarYear, calendarMonth, trip.price, weekendMarkupPercent, midweekDiscountPercent)
        } else 0 to emptyList()
    }

    val selectedDayObj = monthDays.find { it.dayOfMonth == selectedCalendarDay } ?: monthDays.firstOrNull()

    // Opzioni Turno per i Viaggi di più giorni
    val multiDayOptions = remember(trip, weekendMarkupPercent, midweekDiscountPercent) {
        if (trip != null) buildMultiDayTripOptions(trip, weekendMarkupPercent, midweekDiscountPercent)
        else emptyList()
    }
    val selectedMultiDayOption = multiDayOptions.getOrNull(selectedOptionIndex) ?: multiDayOptions.firstOrNull()

    // Prezzo unitario, etichetta e posti reali attualmente selezionati
    val currentUnitPrice = if (isActivity) {
        selectedDayObj?.price ?: (trip?.price ?: 0.0)
    } else {
        selectedMultiDayOption?.pricePerPerson ?: (trip?.price ?: 0.0)
    }

    val currentSelectedLabel = if (isActivity) {
        selectedDayObj?.formattedLabel ?: (trip?.startDate ?: "")
    } else {
        selectedMultiDayOption?.title ?: (trip?.startDate ?: "")
    }

    val currentSelectedBadge = if (isActivity) {
        selectedDayObj?.badgeText ?: "Tariffa Giornaliera"
    } else {
        selectedMultiDayOption?.badge ?: "Tariffa Standard"
    }

    val currentAvailableSpots = trip?.availableSpots?.coerceAtLeast(0) ?: 0
    val alreadyBookedReservations = viewModel.userReservationsForThisTrip
    val hasAlreadyBooked = alreadyBookedReservations.isNotEmpty()
    val totalAlreadyBookedSpots = alreadyBookedReservations.sumOf { it.bookedSpots }

    LaunchedEffect(trip, showEditDialog) {
        if (trip != null && showEditDialog) {
            editTitle = trip.title
            editDescription = trip.description ?: ""
            editPrice = trip.price.toString()
            editLocation = trip.location ?: ""
            editAvailableSpots = trip.availableSpots.toString()
        }
    }

    // --- GALLERIA IMMAGINI A TUTTO SCHERMO (LIGHTBOX ZOOMABILE) ---
    if (fullscreenImageIndex != null && trip != null) {
        val rawImages = trip.imageUrls?.map { it.replace("localhost", "10.0.2.2") } ?: emptyList()
        val images = if (rawImages.isNotEmpty()) rawImages else listOf(resolveTripImageUrl(trip))

        Dialog(
            onDismissRequest = { fullscreenImageIndex = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                val fullPagerState = rememberPagerState(
                    initialPage = fullscreenImageIndex!!.coerceIn(0, (images.size - 1).coerceAtLeast(0)),
                    pageCount = { images.size }
                )

                HorizontalPager(
                    state = fullPagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = images[page],
                            contentDescription = "${trip.title} - foto ingrandita ${page + 1}",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Header del Lightbox con contatore e tasto Chiudi
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color.White.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = "Foto ${fullPagerState.currentPage + 1} di ${images.size}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }

                    IconButton(
                        onClick = { fullscreenImageIndex = null },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Chiudi", tint = TripBlack)
                    }
                }

                Text(
                    text = "Scorri a destra o sinistra per sfogliare tutte le foto",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp)
                )
            }
        }
    }

    // --- POPUP AGENZIA: CONFIGURA PREZZI DINAMICI PER GIORNO / TURNO ---
    if (showDynamicPricingDialog && trip != null) {
        var tempBasePrice by remember(trip) { mutableStateOf(trip.price.toString()) }
        var tempSpots by remember(trip) { mutableStateOf(trip.availableSpots.toString()) }
        var tempMarkup by remember { mutableStateOf(weekendMarkupPercent.toFloat()) }
        var tempDiscount by remember { mutableStateOf(midweekDiscountPercent.toFloat()) }

        AlertDialog(
            onDismissRequest = { showDynamicPricingDialog = false },
            containerColor = Color.White,
            title = {
                Text(
                    text = if (isActivity) "Calendario & Prezzi Giornalieri" else "Gestione Turni & Tariffe Viaggio",
                    fontWeight = FontWeight.ExtraBold,
                    color = TripBlack
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = if (isActivity)
                            "Imposta il prezzo base giornaliero e le variazioni automatiche sul calendario per Weekend (Ven-Dom) e giorni Infrasettimanali (Mar-Mer):"
                        else
                            "Configura la tariffa base del pacchetto e le variazioni per i turni di Alta Richiesta o Early Bird:",
                        fontSize = 13.sp,
                        color = Color.DarkGray
                    )

                    OutlinedTextField(
                        value = tempBasePrice,
                        onValueChange = { tempBasePrice = it },
                        label = { Text("Prezzo Base (€ / persona)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = tempSpots,
                        onValueChange = { tempSpots = it },
                        label = { Text("Posti Totali Disponibili") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = if (isActivity) "Maggiorazione Weekend (Ven-Sab-Dom): +${tempMarkup.roundToInt()}%"
                        else "Supplemento Turno Alta Richiesta: +${tempMarkup.roundToInt()}%",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = TripOrangeActivity
                    )
                    Slider(
                        value = tempMarkup,
                        onValueChange = { tempMarkup = it },
                        valueRange = 0f..50f,
                        colors = SliderDefaults.colors(thumbColor = TripOrangeActivity, activeTrackColor = TripOrangeActivity)
                    )

                    Text(
                        text = if (isActivity) "Sconto Infrasettimanale (Mar-Mer): -${tempDiscount.roundToInt()}%"
                        else "Sconto Turno Early Bird: -${tempDiscount.roundToInt()}%",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = TripGreen
                    )
                    Slider(
                        value = tempDiscount,
                        onValueChange = { tempDiscount = it },
                        valueRange = 0f..40f,
                        colors = SliderDefaults.colors(thumbColor = TripGreen, activeTrackColor = TripGreen)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        weekendMarkupPercent = tempMarkup.roundToInt()
                        midweekDiscountPercent = tempDiscount.roundToInt()
                        pricingPrefs.edit()
                            .putInt("markup_$currentTripId", weekendMarkupPercent)
                            .putInt("discount_$currentTripId", midweekDiscountPercent)
                            .apply()

                        val request = TripRequest(
                            title = trip.title,
                            description = trip.description ?: "",
                            price = tempBasePrice.toDoubleOrNull() ?: trip.price,
                            startDate = trip.startDate,
                            endDate = trip.endDate,
                            availableSpots = tempSpots.toIntOrNull() ?: trip.availableSpots,
                            location = trip.location ?: "",
                            imageUrls = trip.imageUrls
                        )
                        viewModel.updateTrip(tokenUtente, currentTripId, request)
                        showDynamicPricingDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TripGreen)
                ) {
                    Text("Salva Listino", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDynamicPricingDialog = false }) { Text("Annulla") }
            }
        )
    }

    // --- BOTTOM SHEET: SIMULAZIONE METODO DI PAGAMENTO APPROFONDITA ---
    if (showMockPaymentSheet && trip != null) {
        var selectedPaymentMethod by remember { mutableStateOf("CARD") }
        var cardNumber by remember { mutableStateOf("4532 8910 3412 8890") }
        var cardHolder by remember { mutableStateOf("Mario Rossi") }
        var cardExpiry by remember { mutableStateOf("08/29") }
        var cardCvv by remember { mutableStateOf("482") }
        val totalAmount = currentUnitPrice * spotsToBook

        ModalBottomSheet(
            onDismissRequest = { showMockPaymentSheet = false },
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "CHECKOUT PROTETTO SSL",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TripGreen
                        )
                        Text(
                            text = "Riepilogo e Pagamento",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TripBlack
                        )
                    }
                    Icon(Icons.Default.Lock, contentDescription = null, tint = TripGreen, modifier = Modifier.size(26.dp))
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Riepilogo dettagliato dell'ordine
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = TripWarmBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(trip.title, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = TripBlack)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Data / Turno selezionato:", fontSize = 12.sp, color = Color.Gray)
                            Text(currentSelectedLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TripBlack)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Fascia tariffaria:", fontSize = 12.sp, color = Color.Gray)
                            Text(currentSelectedBadge, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TripGreen)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Partecipanti:", fontSize = 12.sp, color = Color.Gray)
                            Text("$spotsToBook × €${String.format(Locale.ITALY, "%.2f", currentUnitPrice)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TripBlack)
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Color.LightGray.copy(alpha = 0.5f))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Totale da autorizzare", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = TripBlack)
                            Text(
                                text = "€${String.format(Locale.ITALY, "%.2f", totalAmount)}",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 20.sp,
                                color = TripDarkGreen
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
                Text("Seleziona metodo di pagamento", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TripBlack)
                Spacer(modifier = Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OfferTypeChip("💳 Carta", selectedPaymentMethod == "CARD") { selectedPaymentMethod = "CARD" }
                    OfferTypeChip("🅿️ PayPal", selectedPaymentMethod == "PAYPAL") { selectedPaymentMethod = "PAYPAL" }
                    OfferTypeChip("📱 Google Pay", selectedPaymentMethod == "GPAY") { selectedPaymentMethod = "GPAY" }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (selectedPaymentMethod == "CARD") {
                    OutlinedTextField(
                        value = cardHolder,
                        onValueChange = { cardHolder = it },
                        label = { Text("Titolare della Carta") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = TripGreen) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = cardNumber,
                        onValueChange = { cardNumber = it },
                        label = { Text("Numero Carta (Visa / Mastercard)") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = TripGreen) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = cardExpiry,
                            onValueChange = { cardExpiry = it },
                            label = { Text("Scadenza (MM/AA)") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = cardCvv,
                            onValueChange = { cardCvv = it },
                            label = { Text("CVV") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = TripGreen.copy(alpha = 0.1f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = TripGreen)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (selectedPaymentMethod == "PAYPAL")
                                    "Account PayPal collegato e verificato. L'importo verrà autorizzato istantaneamente."
                                else
                                    "Wallet Google Pay pronto per l'autorizzazione biometrica rapida.",
                                fontSize = 13.sp,
                                color = TripBlack
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = {
                        showMockPaymentSheet = false
                        wantToBookAgain = false
                        viewModel.bookTrip(
                            token = tokenUtente,
                            tripId = currentTripId,
                            spots = spotsToBook,
                            selectedSlotLabel = currentSelectedLabel
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TripGreen),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Autorizza e Paga €${String.format(Locale.ITALY, "%.2f", totalAmount)}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }

    // --- POPUP: SCRIVI UNA RECENSIONE ---
    if (showAddReviewDialog) {
        var newRating by remember { mutableStateOf(5) }
        var newComment by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddReviewDialog = false },
            containerColor = Color.White,
            title = { Text("Valuta questa esperienza", fontWeight = FontWeight.ExtraBold, color = TripBlack) },
            text = {
                Column {
                    Text("Punteggio complessivo", fontSize = 13.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row {
                        (1..5).forEach { star ->
                            IconButton(onClick = { newRating = star }) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "$star stelle",
                                    tint = if (star <= newRating) TripAmber else Color.LightGray,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newComment,
                        onValueChange = { newComment = it },
                        label = { Text("Racconta la tua esperienza ai prossimi viaggiatori") },
                        minLines = 3,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        reviews.add(
                            0,
                            ExperienceReview(
                                author = "Tu (Viaggiatore Verificato)",
                                rating = newRating,
                                date = "Oggi",
                                comment = newComment.trim()
                            )
                        )
                        showAddReviewDialog = false
                    },
                    enabled = newComment.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = TripGreen)
                ) {
                    Text("Pubblica Recensione", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddReviewDialog = false }) { Text("Annulla") }
            }
        )
    }

    // --- POPUP: ELIMINA PACCHETTO ---
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Elimina Pacchetto", fontWeight = FontWeight.Bold) },
            text = { Text("Sei sicuro di voler rimuovere definitivamente \"${trip?.title}\" dal catalogo?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteTrip(tokenUtente, currentTripId)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Elimina", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Annulla") }
            }
        )
    }

    // --- POPUP: MODIFICA PACCHETTO ---
    if (showEditDialog && trip != null) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Modifica Annuncio", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("Titolo") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editLocation,
                        onValueChange = { editLocation = it },
                        label = { Text("Località") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editPrice,
                        onValueChange = { editPrice = it },
                        label = { Text("Prezzo Base (€)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editAvailableSpots,
                        onValueChange = { editAvailableSpots = it },
                        label = { Text("Posti Disponibili") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editDescription,
                        onValueChange = { editDescription = it },
                        label = { Text("Descrizione") },
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        maxLines = 4
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val request = TripRequest(
                            title = editTitle,
                            description = editDescription,
                            price = editPrice.toDoubleOrNull() ?: trip.price,
                            startDate = trip.startDate,
                            endDate = trip.endDate,
                            availableSpots = editAvailableSpots.toIntOrNull() ?: trip.availableSpots,
                            location = editLocation,
                            imageUrls = trip.imageUrls
                        )
                        viewModel.updateTrip(tokenUtente, currentTripId, request)
                        showEditDialog = false
                    },
                    enabled = !viewModel.isUpdating,
                    colors = ButtonDefaults.buttonColors(containerColor = TripGreen)
                ) {
                    Text("Salva Modifiche", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text("Annulla") }
            }
        )
    }

    if (viewModel.updateMessage != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearUpdateMessage() },
            title = { Text("Aggiornamento Completato", fontWeight = FontWeight.Bold) },
            text = { Text(viewModel.updateMessage!!) },
            confirmButton = {
                Button(
                    onClick = { viewModel.clearUpdateMessage() },
                    colors = ButtonDefaults.buttonColors(containerColor = TripGreen)
                ) { Text("OK", color = Color.White) }
            }
        )
    }

    if (viewModel.bookingMessage != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearBookingMessage() },
            title = {
                Text(
                    text = if (viewModel.bookingSuccess) "Prenotazione Confermata!" else "Attenzione",
                    fontWeight = FontWeight.ExtraBold,
                    color = if (viewModel.bookingSuccess) TripGreen else MaterialTheme.colorScheme.error
                )
            },
            text = { Text(viewModel.bookingMessage!!) },
            confirmButton = {
                Button(
                    onClick = { viewModel.clearBookingMessage() },
                    colors = ButtonDefaults.buttonColors(containerColor = TripGreen)
                ) { Text("Perfetto", color = Color.White) }
            }
        )
    }

    // --- POPUP: SALVA NEI PREFERITI ---
    if (viewModel.showFavoriteDialog) {
        var isCreatingNew by remember { mutableStateOf(false) }
        var newListName by remember { mutableStateOf("") }
        var newListVisibility by remember { mutableStateOf("PRIVATE") }

        AlertDialog(
            onDismissRequest = { viewModel.showFavoriteDialog = false },
            containerColor = Color.White,
            title = { Text(if (isCreatingNew) "Nuova Lista" else "Salva in una lista", fontWeight = FontWeight.ExtraBold) },
            text = {
                if (!isCreatingNew) {
                    Column {
                        if (viewModel.myLists.isEmpty()) {
                            Text("Non hai ancora nessuna lista.", color = Color.Gray, modifier = Modifier.padding(bottom = 16.dp))
                        } else {
                            viewModel.myLists.forEach { list ->
                                TextButton(
                                    onClick = {
                                        isFavorite = true
                                        viewModel.addTripToList(tokenUtente, list.id, currentTripId)
                                    },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(list.name, fontWeight = FontWeight.Bold, color = TripBlack)
                                        Icon(
                                            imageVector = when (list.visibility) {
                                                "PRIVATE" -> Icons.Default.Lock
                                                "SHARED" -> Icons.Default.Person
                                                else -> Icons.Default.Share
                                            },
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = TripGreen
                                        )
                                    }
                                }
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        TextButton(onClick = { isCreatingNew = true }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = TripGreen)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Crea una nuova lista", fontWeight = FontWeight.Bold, color = TripGreen)
                        }
                    }
                } else {
                    Column {
                        OutlinedTextField(
                            value = newListName,
                            onValueChange = { newListName = it },
                            label = { Text("Nome (es. Vacanze Estive)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Privacy", fontSize = 12.sp, color = Color.Gray)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = newListVisibility == "PRIVATE", onClick = { newListVisibility = "PRIVATE" })
                            Text("Privata (Solo tu)")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = newListVisibility == "SHARED", onClick = { newListVisibility = "SHARED" })
                            Text("Condivisa (Con Amici)")
                        }
                    }
                }
            },
            confirmButton = {
                if (isCreatingNew) {
                    Button(
                        onClick = {
                            isFavorite = true
                            viewModel.createListAndAddTrip(tokenUtente, newListName, newListVisibility, currentTripId)
                        },
                        enabled = newListName.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = TripGreen)
                    ) { Text("Crea e Salva", color = Color.White) }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        if (isCreatingNew) isCreatingNew = false
                        else viewModel.showFavoriteDialog = false
                    }
                ) { Text(if (isCreatingNew) "Indietro" else "Annulla") }
            }
        )
    }

    if (viewModel.favoriteMessage != null) {
        AlertDialog(
            onDismissRequest = { viewModel.favoriteMessage = null },
            confirmButton = {
                Button(
                    onClick = { viewModel.favoriteMessage = null },
                    colors = ButtonDefaults.buttonColors(containerColor = TripGreen)
                ) { Text("OK", color = Color.White) }
            },
            text = { Text(viewModel.favoriteMessage!!) }
        )
    }

    Scaffold(
        containerColor = TripWarmBg,
        bottomBar = {
            if (trip != null) {
                Surface(
                    color = Color.White,
                    tonalElevation = 12.dp,
                    shadowElevation = 16.dp
                ) {
                    if (ruoloUtente == "ORGANIZZATORE") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Prezzo Base", fontSize = 11.sp, color = Color.Gray)
                                Text(
                                    text = "€${trip.price}",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TripBlack
                                )
                                Text("${trip.availableSpots} posti residui", fontSize = 11.sp, color = TripGreen, fontWeight = FontWeight.Bold)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { showDynamicPricingDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = TripGreen),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.height(50.dp)
                                ) {
                                    Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Listino & Posti", fontWeight = FontWeight.Bold)
                                }

                                IconButton(
                                    onClick = { showEditDialog = true },
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(TripWarmBg)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Modifica", tint = TripBlack)
                                }

                                IconButton(
                                    onClick = { showDeleteDialog = true },
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f))
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Elimina", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    } else if (hasAlreadyBooked && !wantToBookAgain) {
                        // STATO GIÀ PRENOTATO: Non mostra "Prenota Ora", ma segnala la prenotazione attiva e permette di prenotare di nuovo se voluto
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(TripGreen.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = TripGreen)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Esperienza già prenotata",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp,
                                        color = TripDarkGreen
                                    )
                                    Text(
                                        text = "Hai $totalAlreadyBookedSpots posto/i confermati",
                                        fontSize = 12.sp,
                                        color = Color.DarkGray
                                    )
                                }
                            }

                            if (currentAvailableSpots > 0) {
                                OutlinedButton(
                                    onClick = { wantToBookAgain = true },
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.5.dp, TripGreen),
                                    modifier = Modifier.height(48.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = TripGreen, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Prenota di nuovo",
                                        color = TripGreen,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp
                                    )
                                }
                            } else {
                                Surface(
                                    color = Color.LightGray.copy(alpha = 0.35f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = "Posti Esauriti",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Gray,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        // STATO NON ANCORA PRENOTATO (OPPURE L'UTENTE HA PREMUTO "PRENOTA DI NUOVO")
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = currentSelectedBadge,
                                    fontSize = 11.sp,
                                    color = TripGreen,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                val total = currentUnitPrice * spotsToBook
                                Text(
                                    text = "€${String.format(Locale.ITALY, "%.2f", total)}",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TripBlack
                                )
                                Text(
                                    text = "€$currentUnitPrice × $spotsToBook pers.",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .background(TripWarmBg, RoundedCornerShape(12.dp))
                                        .padding(horizontal = 2.dp)
                                ) {
                                    IconButton(onClick = { if (spotsToBook > 1) spotsToBook-- }) {
                                        Text("-", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = TripBlack)
                                    }
                                    Text("$spotsToBook", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = TripBlack)
                                    IconButton(onClick = { if (spotsToBook < currentAvailableSpots.coerceAtLeast(1)) spotsToBook++ }) {
                                        Text("+", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = TripGreen)
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Button(
                                    onClick = { showMockPaymentSheet = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = TripGreen),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.height(52.dp),
                                    enabled = !viewModel.isBooking && currentAvailableSpots > 0
                                ) {
                                    Text(
                                        text = if (currentAvailableSpots > 0) "Prenota Ora" else "Esaurito",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        if (viewModel.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = TripGreen)
            }
        } else if (viewModel.errorMessage != null) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text(text = viewModel.errorMessage!!, color = MaterialTheme.colorScheme.error)
            }
        } else if (trip != null) {
            val rawImages = trip.imageUrls?.map { it.replace("localhost", "10.0.2.2") } ?: emptyList()
            val images = if (rawImages.isNotEmpty()) rawImages else listOf(resolveTripImageUrl(trip))

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(TripWarmBg)
                    .padding(paddingValues)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                ) {
                    // 1. GALLERIA FOTOGRAFICA CLICCABILE CON INGRANDIMENTO A TUTTO SCHERMO
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp)
                            .background(Color.LightGray)
                    ) {
                        val pagerState = rememberPagerState(pageCount = { images.size })

                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize()
                        ) { page ->
                            AsyncImage(
                                model = images[page],
                                contentDescription = "${trip.title} - foto ${page + 1}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable { fullscreenImageIndex = page }
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.35f), Color.Transparent, Color.Black.copy(alpha = 0.7f))))
                        )

                        Surface(
                            color = if (isActivity) TripOrangeActivity else TripGreen,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(bottom = 42.dp, start = 24.dp)
                        ) {
                            Text(
                                text = if (isActivity) "🎟️ ATTIVITÀ / ESCURSIONE IN GIORNATA" else "✈️ PACCHETTO VIAGGIO PIÙ GIORNI",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                            )
                        }

                        Surface(
                            color = Color.Black.copy(alpha = 0.65f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(bottom = 40.dp, end = 24.dp)
                                .clickable { fullscreenImageIndex = pagerState.currentPage }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Search, contentDescription = "Ingrandisci", tint = Color.White, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "${pagerState.currentPage + 1}/${images.size} • Ingrandisci",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // 2. CORPO PRINCIPALE DELL'ESPERIENZA
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(y = (-26).dp)
                            .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                            .background(Color.White)
                            .padding(24.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = TripGreen, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    trip.location ?: "Destinazione",
                                    color = TripGreen,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp
                                )
                            }

                            // NUMERO DI RECENSIONI CLICCABILE -> SCORRE ALLA SEZIONE RECENSIONI
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = TripWarmBg,
                                modifier = Modifier.clickable {
                                    coroutineScope.launch {
                                        scrollState.animateScrollTo(reviewsSectionY)
                                    }
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = TripAmber, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = "${trip.averageRating}", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = TripBlack)
                                    Text(
                                        text = " (${reviews.size + trip.reviewCount} recensioni ↓)",
                                        color = TripGreen,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = trip.title,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TripBlack,
                            lineHeight = 32.sp
                        )

                        // Banner informativo se l'utente ha già prenotato questo viaggio
                        if (hasAlreadyBooked && ruoloUtente != "ORGANIZZATORE") {
                            Spacer(modifier = Modifier.height(14.dp))
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = TripGreen.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, TripGreen.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = TripGreen)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Hai già una prenotazione attiva per questa esperienza",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 13.sp,
                                            color = TripDarkGreen
                                        )
                                        Text(
                                            text = "Posti totali prenotati da te: $totalAlreadyBookedSpots • Esportabile in .ics dalla tab Viaggi.",
                                            fontSize = 12.sp,
                                            color = Color.DarkGray
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            HighlightPill(icon = Icons.Default.Person, text = "${trip.availableSpots} posti disponibili")
                            HighlightPill(icon = Icons.Default.DateRange, text = "Calendario .ics")
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // =========================================================================
                        // 3. VERO CALENDARIO MENSILE CON PREZZI VARIABILI (PER LE ATTIVITÀ)
                        //    OPPURE SELETTORE TURNI DI PARTENZA (PER I VIAGGI MULTI-GIORNO)
                        // =========================================================================
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isActivity) "Calendario Disponibilità & Tariffe"
                                    else "Scegli Turno di Partenza & Formula",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TripBlack
                                )
                                Text(
                                    text = if (isActivity) "Tocca un giorno sul calendario: i prezzi variano tra feriali e weekend"
                                    else "Seleziona il turno di partenza multi-giorno più adatto a te",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                            if (ruoloUtente == "ORGANIZZATORE") {
                                TextButton(onClick = { showDynamicPricingDialog = true }) {
                                    Text("Configura", color = TripGreen, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (isActivity) {
                            val monthTitle = remember(calendarYear, calendarMonth) {
                                val c = Calendar.getInstance().apply {
                                    set(Calendar.YEAR, calendarYear)
                                    set(Calendar.MONTH, calendarMonth)
                                }
                                SimpleDateFormat("MMMM yyyy", Locale.ITALY).format(c.time).replaceFirstChar { it.uppercase() }
                            }

                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = TripWarmBg,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    // Barra di navigazione Mese Precedente / Successivo
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = {
                                                if (calendarMonth == 0) {
                                                    calendarMonth = 11
                                                    calendarYear--
                                                } else calendarMonth--
                                                selectedCalendarDay = 1
                                            },
                                            modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.White)
                                        ) {
                                            Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Mese precedente", tint = TripBlack)
                                        }

                                        Text(
                                            text = monthTitle,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 16.sp,
                                            color = TripBlack
                                        )

                                        IconButton(
                                            onClick = {
                                                if (calendarMonth == 11) {
                                                    calendarMonth = 0
                                                    calendarYear++
                                                } else calendarMonth++
                                                selectedCalendarDay = 1
                                            },
                                            modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.White)
                                        ) {
                                            Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Mese successivo", tint = TripBlack)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Intestazione giorni della settimana (Lun - Dom)
                                    val weekDays = listOf("Lun", "Mar", "Mer", "Gio", "Ven", "Sab", "Dom")
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        weekDays.forEachIndexed { idx, dName ->
                                            Text(
                                                text = dName,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (idx >= 4) TripOrangeActivity else Color.Gray,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Griglia del Calendario con Prezzi Giornalieri
                                    val totalCells = leadingEmptyCells + monthDays.size
                                    val rows = (totalCells + 6) / 7

                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        for (r in 0 until rows) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                for (c in 0..6) {
                                                    val cellIndex = r * 7 + c
                                                    val dayIndex = cellIndex - leadingEmptyCells
                                                    val dayItem = monthDays.getOrNull(dayIndex)

                                                    if (dayItem == null) {
                                                        Spacer(modifier = Modifier.weight(1f).height(52.dp))
                                                    } else {
                                                        val isSelected = dayItem.dayOfMonth == selectedCalendarDay
                                                        val bgColor = when {
                                                            isSelected -> TripGreen
                                                            dayItem.tier == "WEEKEND" -> TripOrangeActivity.copy(alpha = 0.1f)
                                                            dayItem.tier == "PROMO" -> TripGreen.copy(alpha = 0.12f)
                                                            else -> Color.White
                                                        }
                                                        val priceColor = when {
                                                            isSelected -> Color.White
                                                            dayItem.tier == "WEEKEND" -> TripOrangeActivity
                                                            dayItem.tier == "PROMO" -> TripDarkGreen
                                                            else -> Color.DarkGray
                                                        }

                                                        Box(
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .height(52.dp)
                                                                .clip(RoundedCornerShape(10.dp))
                                                                .background(bgColor)
                                                                .then(
                                                                    if (isSelected) Modifier.border(2.dp, TripDarkGreen, RoundedCornerShape(10.dp))
                                                                    else Modifier
                                                                )
                                                                .clickable { selectedCalendarDay = dayItem.dayOfMonth },
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                                Text(
                                                                    text = "${dayItem.dayOfMonth}",
                                                                    fontWeight = FontWeight.ExtraBold,
                                                                    fontSize = 13.sp,
                                                                    color = if (isSelected) Color.White else TripBlack
                                                                )
                                                                Text(
                                                                    text = "€${dayItem.price.roundToInt()}",
                                                                    fontWeight = FontWeight.Bold,
                                                                    fontSize = 10.sp,
                                                                    color = priceColor
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Legenda Colori Prezzi del Calendario
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("🟢 Mar-Mer: Promo (-$midweekDiscountPercent%)", fontSize = 11.sp, color = TripDarkGreen, fontWeight = FontWeight.Bold)
                                        Text("⚪ Lun/Gio: Base", fontSize = 11.sp, color = Color.DarkGray)
                                        Text("🟠 Ven-Dom: Weekend (+$weekendMarkupPercent%)", fontSize = 11.sp, color = TripOrangeActivity, fontWeight = FontWeight.Bold)
                                    }

                                    if (selectedDayObj != null) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = Color.White,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = selectedDayObj.formattedLabel,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        fontSize = 13.sp,
                                                        color = TripBlack
                                                    )
                                                    Text(
                                                        text = "${selectedDayObj.badgeText} • $currentAvailableSpots posti disponibili",
                                                        fontSize = 11.sp,
                                                        color = TripGreen,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                                Text(
                                                    text = "€${selectedDayObj.price}",
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 18.sp,
                                                    color = TripDarkGreen
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            // FINESTRE DI PARTENZA PER I VIAGGI DI PIÙ GIORNI
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                multiDayOptions.forEachIndexed { index, option ->
                                    val isSelected = selectedOptionIndex == index
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = if (isSelected) TripGreen.copy(alpha = 0.1f) else TripWarmBg,
                                        border = if (isSelected) BorderStroke(2.dp, TripGreen) else null,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedOptionIndex = index }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Surface(
                                                    color = if (isSelected) TripGreen else Color.White,
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text(
                                                        text = option.badge,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = if (isSelected) Color.White else TripDarkGreen,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = option.title,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 15.sp,
                                                    color = TripBlack
                                                )
                                                Text(
                                                    text = "${option.subtitle} • ${option.availableSpots} posti liberi",
                                                    fontSize = 12.sp,
                                                    color = Color.DarkGray
                                                )
                                            }
                                            Text(
                                                text = "€${option.pricePerPerson}",
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = TripDarkGreen
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(22.dp))

                        // 4. CARD TOUR OPERATOR
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = TripWarmBg,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(TripGreen),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White)
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        "Organizzato da ${trip.organizerName ?: "Agenzia Partner Horizon"}",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp,
                                        color = TripBlack
                                    )
                                    Text(
                                        "Operatore Verificato • Assistenza dedicata inclusa",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(22.dp))

                        // 5. DESCRIZIONE E PROGRAMMA
                        Text("Programma e Dettagli dell'Esperienza", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = TripBlack)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = trip.description ?: "Nessuna descrizione fornita per questo pacchetto.",
                            fontSize = 14.sp,
                            color = Color.DarkGray,
                            lineHeight = 21.sp
                        )

                        Spacer(modifier = Modifier.height(28.dp))
                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.4f))
                        Spacer(modifier = Modifier.height(20.dp))

                        // =========================================================================
                        // 6. SEZIONE RECENSIONI (CON COORDINATA Y PER LO SCROLL AUTOMATICO)
                        // =========================================================================
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { coordinates ->
                                    reviewsSectionY = coordinates.positionInParent().y.roundToInt()
                                },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Recensioni dei Viaggiatori",
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TripBlack
                                )
                                Text(
                                    text = "Valutazioni verificate della community Horizon",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                            if (ruoloUtente != "ORGANIZZATORE") {
                                OutlinedButton(
                                    onClick = { showAddReviewDialog = true },
                                    shape = RoundedCornerShape(50),
                                    border = BorderStroke(1.dp, TripGreen)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, tint = TripGreen, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Scrivi", color = TripGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        reviews.forEach { rev ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = TripWarmBg,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(rev.author, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = TripBlack)
                                        Text(rev.date, fontSize = 11.sp, color = Color.Gray)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row {
                                        repeat(rev.rating) {
                                            Icon(Icons.Default.Star, contentDescription = null, tint = TripAmber, modifier = Modifier.size(15.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(rev.comment, fontSize = 13.sp, color = Color.DarkGray, lineHeight = 18.sp)
                                }
                            }
                        }

                        // =========================================================================
                        // 7. CAROSELLO ORIZZONTALE: ESPERIENZE SIMILI CONSIGLIATE
                        // =========================================================================
                        if (viewModel.similarTrips.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(24.dp))
                            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.4f))
                            Spacer(modifier = Modifier.height(20.dp))

                            Text(
                                text = "Esperienze Simili Consigliate",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TripBlack
                            )
                            Text(
                                text = "Altri viaggi e attività disponibili che potrebbero piacerti",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                items(viewModel.similarTrips) { simTrip ->
                                    RecentlyViewedTripCard(
                                        trip = simTrip,
                                        onClick = { currentTripId = simTrip.id }
                                    )
                                }
                            }
                        }
                    }
                }

                // BARRA SUPERIORE FLOTTANTE (INDIETRO + SALVA NEI PREFERITI)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, start = 16.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.9f))
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Indietro", tint = TripBlack)
                    }

                    if (ruoloUtente != "ORGANIZZATORE") {
                        IconButton(
                            onClick = {
                                viewModel.fetchMyListsForDialog(tokenUtente)
                                viewModel.showFavoriteDialog = true
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.9f))
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Preferiti",
                                tint = if (isFavorite) Color(0xFFE53935) else TripBlack
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HighlightPill(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Surface(
        color = TripWarmBg,
        shape = RoundedCornerShape(50)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = TripGreen, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TripBlack)
        }
    }
}