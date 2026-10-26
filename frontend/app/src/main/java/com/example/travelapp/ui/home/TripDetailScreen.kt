package com.example.travelapp.ui.home

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.graphics.vector.ImageVector
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
    val dateIso: String,
    val title: String,
    val subtitle: String,
    val seasonLabel: String,
    val pricePerPerson: Double,
    val availableSpots: Int,
    val isPastDate: Boolean
)

data class CalendarDayPrice(
    val dayOfMonth: Int,
    val dateIso: String,
    val formattedLabel: String,
    val price: Double,
    val availableSpotsForDay: Int,
    val isHighDemandOrHoliday: Boolean,
    val seasonNote: String,
    val isPastDate: Boolean = false
)

data class ItineraryStepItem(
    val stepNumber: Int,
    val title: String,
    val duration: String,
    val ticketInfo: String,
    val description: String
)

data class ExperienceReview(
    val author: String,
    val rating: Int,
    val date: String,
    val comment: String
)

data class ParsedTripContent(
    val overview: String,
    val steps: List<ItineraryStepItem>,
    val reviews: List<ExperienceReview>,
    val rawReviewBlocks: String
)

// Restituisce il Calendar di oggi azzerato a mezzanotte (00:00:00) per bloccare tutte le date passate
fun getTodayMidnightCalendar(): Calendar {
    return Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
}

fun calculateSeasonalMultiplier(monthZeroBased: Int, dayOfMonth: Int, dayOfWeek: Int, weekendMarkupPercent: Int): Pair<Double, String> {
    val month = monthZeroBased + 1

    if ((month == 12 && dayOfMonth >= 20) || (month == 1 && dayOfMonth <= 6)) {
        return 1.35 to "Alta Stagione • Festività Natalizie"
    }
    if (month == 8 && dayOfMonth in 10..20) {
        return 1.40 to "Altissima Stagione • Settimana di Ferragosto"
    }
    if (month == 8) return 1.30 to "Alta Stagione Estiva (Agosto)"
    if (month == 7) return 1.25 to "Alta Stagione Estiva (Luglio)"
    if (month == 6) return 1.15 to "Stagione Estiva (Giugno)"

    val isHolidayBridge = (month == 4 && dayOfMonth in 18..26) ||
            (month == 5 && dayOfMonth in 1..3) ||
            (month == 6 && dayOfMonth in 1..3) ||
            (month == 11 && dayOfMonth in 1..2) ||
            (month == 12 && dayOfMonth in 7..9)
    if (isHolidayBridge) {
        return 1.22 to "Tariffa Festività & Ponti"
    }

    val isWeekend = dayOfWeek == Calendar.FRIDAY || dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY
    if (isWeekend) {
        return (1.0 + (weekendMarkupPercent / 100.0)) to "Tariffa Fine Settimana"
    }

    if (month in listOf(1, 2, 11)) {
        return 0.92 to "Tariffa Bassa Stagione"
    }

    return 1.0 to "Tariffa Standard Stagionale"
}

// Estrae dal DB la Panoramica, le Tappe @@STEP@@ e le Recensioni Reali @@REVIEW@@
fun parseTripDatabaseContent(trip: TripDTO, isActivity: Boolean): ParsedTripContent {
    val fullRaw = trip.description ?: ""
    val loc = trip.location ?: "Destinazione"

    // 1. Separa i blocchi @@REVIEW@@ reali salvati nel DB
    val reviewSplits = fullRaw.split("@@REVIEW@@")
    val contentWithoutReviews = reviewSplits.firstOrNull()?.trim().orEmpty()
    val rawReviewBlocks = if (reviewSplits.size > 1) {
        reviewSplits.drop(1).joinToString("\n") { "@@REVIEW@@${it.trim()}" }
    } else ""

    val parsedReviews = reviewSplits.drop(1).mapNotNull { block ->
        val fields = block.trim().split("||")
        if (fields.size >= 4) {
            ExperienceReview(
                author = fields[0].trim(),
                rating = fields[1].trim().toIntOrNull()?.coerceIn(1, 5) ?: 5,
                date = fields[2].trim(),
                comment = fields[3].trim()
            )
        } else null
    }.reversed() // Mostra le più recenti in alto

    // 2. Separa la panoramica dai blocchi @@STEP@@
    if (contentWithoutReviews.contains("@@STEP@@")) {
        val parts = contentWithoutReviews.split("@@STEP@@")
        val cleanOverview = parts.firstOrNull()?.trim().orEmpty().ifEmpty {
            "Esperienza ufficiale organizzata a $loc."
        }
        val parsedSteps = parts.drop(1).mapIndexedNotNull { index, block ->
            val fields = block.trim().split("||")
            if (fields.isEmpty() || fields[0].isBlank()) null
            else {
                ItineraryStepItem(
                    stepNumber = index + 1,
                    title = fields.getOrNull(0)?.trim().orEmpty(),
                    duration = fields.getOrNull(1)?.trim().orEmpty().ifEmpty { if (isActivity) "45 min" else "Giorno ${index + 1}" },
                    ticketInfo = fields.getOrNull(2)?.trim().orEmpty().ifEmpty { "Tappa inclusa" },
                    description = fields.getOrNull(3)?.trim().orEmpty()
                )
            }
        }
        if (parsedSteps.isNotEmpty()) {
            return ParsedTripContent(cleanOverview, parsedSteps, parsedReviews, rawReviewBlocks)
        }
    }

    val defaultStep = listOf(
        ItineraryStepItem(
            stepNumber = 1,
            title = if (isActivity) "Incontro e Svolgimento Attività a $loc" else "Partenza e Programma a $loc",
            duration = if (isActivity) "2 ore" else "${trip.startDate} → ${trip.endDate}",
            ticketInfo = "Esperienza completa inclusa",
            description = contentWithoutReviews.ifBlank { "Accoglienza partecipanti e svolgimento dell'esperienza a $loc." }
        )
    )
    return ParsedTripContent(
        overview = contentWithoutReviews.ifBlank { "Esperienza curata a $loc." },
        steps = defaultStep,
        reviews = parsedReviews,
        rawReviewBlocks = rawReviewBlocks
    )
}

fun buildMultiDayTripOptions(
    context: Context,
    trip: TripDTO,
    weekendMarkupPercent: Int,
    recomposeTrigger: Int
): List<DynamicAvailabilityOption> {
    val basePrice = trip.price
    val baseCapacity = trip.availableSpots.coerceAtLeast(0)
    val sdfInput = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val sdfDayNum = SimpleDateFormat("dd MMM yyyy", Locale.ITALY)
    val datePrefs = context.getSharedPreferences("date_inventory_prefs", Context.MODE_PRIVATE)
    val todayMidnight = getTodayMidnightCalendar()

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

    val offsets = listOf(0, 14, 30)
    return offsets.mapIndexed { idx, dayOffset ->
        val cStart = (startCal.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, dayOffset)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val cEnd = (cStart.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, durationDays) }
        val isoKey = sdfInput.format(cStart.time)
        val isPast = cStart.before(todayMidnight)

        val (seasonMult, seasonLabel) = calculateSeasonalMultiplier(
            cStart.get(Calendar.MONTH),
            cStart.get(Calendar.DAY_OF_MONTH),
            cStart.get(Calendar.DAY_OF_WEEK),
            weekendMarkupPercent
        )

        val slotPrice = ((basePrice * seasonMult)).roundToInt().toDouble()
        val bookedForSlot = datePrefs.getInt("booked_${trip.id}_$isoKey", 0)
        val initialSlotCap = if (idx == 0) baseCapacity else (baseCapacity - idx).coerceAtLeast(1)
        val remaining = if (baseCapacity <= 0) 0 else (initialSlotCap - bookedForSlot).coerceAtLeast(0)

        DynamicAvailabilityOption(
            id = idx,
            dateIso = isoKey,
            title = "${idx + 1}° Turno: ${sdfDayNum.format(cStart.time)} → ${sdfDayNum.format(cEnd.time)}",
            subtitle = "Pacchetto $durationDays giorni • $seasonLabel",
            seasonLabel = seasonLabel,
            pricePerPerson = slotPrice,
            availableSpots = remaining,
            isPastDate = isPast
        )
    }
}

fun buildMonthCalendarDays(
    context: Context,
    tripId: Long,
    dailyBaseCapacity: Int,
    year: Int,
    month: Int,
    basePrice: Double,
    weekendMarkupPercent: Int,
    recomposeTrigger: Int
): Pair<Int, List<CalendarDayPrice>> {
    val datePrefs = context.getSharedPreferences("date_inventory_prefs", Context.MODE_PRIVATE)
    val todayMidnight = getTodayMidnightCalendar()

    val cal = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month)
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
    val emptyLeadingCells = if (firstDayOfWeek == Calendar.SUNDAY) 6 else firstDayOfWeek - Calendar.MONDAY

    val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val sdfIso = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val sdfLabel = SimpleDateFormat("EEEE dd MMMM yyyy", Locale.ITALY)

    val days = (1..maxDays).map { day ->
        cal.set(Calendar.DAY_OF_MONTH, day)
        val dow = cal.get(Calendar.DAY_OF_WEEK)
        val dateIso = sdfIso.format(cal.time)
        val isPastDate = cal.before(todayMidnight)

        val (multiplier, seasonNote) = calculateSeasonalMultiplier(month, day, dow, weekendMarkupPercent)
        val computedPrice = ((basePrice * multiplier) * 10.0).roundToInt() / 10.0

        val customCapForDate = datePrefs.getInt("custom_cap_${tripId}_$dateIso", -1)
        val initialDaySpots = if (customCapForDate >= 0) {
            customCapForDate
        } else {
            val base = dailyBaseCapacity.coerceAtLeast(2)
            when {
                day % 9 == 0 -> 0
                day % 5 == 0 -> 1
                else -> (base - ((day + month) % 2)).coerceAtLeast(1)
            }
        }
        val bookedOnThisDate = datePrefs.getInt("booked_${tripId}_$dateIso", 0)
        val remainingSpotsForDay = (initialDaySpots - bookedOnThisDate).coerceAtLeast(0)

        CalendarDayPrice(
            dayOfMonth = day,
            dateIso = dateIso,
            formattedLabel = sdfLabel.format(cal.time).replaceFirstChar { it.uppercase() },
            price = computedPrice,
            availableSpotsForDay = remainingSpotsForDay,
            isHighDemandOrHoliday = multiplier > 1.12,
            seasonNote = seasonNote,
            isPastDate = isPastDate
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
    var inventoryRefreshTrigger by remember { mutableStateOf(0) }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showMockPaymentSheet by remember { mutableStateOf(false) }
    var showAddReviewDialog by remember { mutableStateOf(false) }
    var showContactAgencySheet by remember { mutableStateOf(false) }
    var contactSuccessMessage by remember { mutableStateOf<String?>(null) }
    var isFavorite by remember { mutableStateOf(false) }

    var fullscreenImageIndex by remember { mutableStateOf<Int?>(null) }
    var wantToBookAgain by remember(currentTripId) { mutableStateOf(false) }
    var expandAllSteps by remember(currentTripId) { mutableStateOf(true) }

    var spotsToBook by remember { mutableStateOf(1) }
    var selectedOptionIndex by remember { mutableStateOf(0) }

    val pricingPrefs = remember { context.getSharedPreferences("dynamic_pricing_prefs", Context.MODE_PRIVATE) }
    val weekendMarkupPercent = remember(currentTripId) {
        pricingPrefs.getInt("markup_$currentTripId", 15)
    }

    var editTitle by remember { mutableStateOf("") }
    var editOverview by remember { mutableStateOf("") }
    var editPrice by remember { mutableStateOf("") }
    var editLocation by remember { mutableStateOf("") }
    var editAvailableSpots by remember { mutableStateOf("") }
    val editStepsList = remember { mutableStateListOf<EditableStepDraft>() }

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
    val isOrganizerRole = ruoloUtente == "ORGANIZZATORE"
    val accentColor = if (isActivity) TripOrangeActivity else TripGreen

    // Legge dal DB la panoramica, le tappe e le VERE recensioni salvate nel database
    val parsedContent = remember(trip, isActivity) {
        if (trip != null) parseTripDatabaseContent(trip, isActivity)
        else ParsedTripContent("", emptyList(), emptyList(), "")
    }
    val cleanOverviewText = parsedContent.overview
    val itinerarySteps = parsedContent.steps
    val dbReviews = parsedContent.reviews

    // Media e conteggio recensioni provengono direttamente dal database (e coincidono con dbReviews)
    val realReviewCount = trip?.reviewCount ?: dbReviews.size
    val realAverageRating = trip?.averageRating ?: 0.0

    val todayCal = remember {getTodayMidnightCalendar() }
    val todayYear = todayCal.get(Calendar.YEAR)
    val todayMonth = todayCal.get(Calendar.MONTH)

    val initialCal = remember(trip) {
        val c = Calendar.getInstance()
        try {
            if (!trip?.startDate.isNullOrBlank()) {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(trip!!.startDate)?.let { c.time = it }
            }
        } catch (_: Exception) {}

        if (c.before(todayCal)) {
            todayCal.clone() as Calendar
        } else {
            c
        }
    }
    var calendarYear by remember(trip) { mutableStateOf(initialCal.get(Calendar.YEAR)) }
    var calendarMonth by remember(trip) { mutableStateOf(initialCal.get(Calendar.MONTH)) }
    var selectedCalendarDay by remember(trip) { mutableStateOf(initialCal.get(Calendar.DAY_OF_MONTH)) }

    val (leadingEmptyCells, monthDays) = remember(
        trip,
        calendarYear,
        calendarMonth,
        weekendMarkupPercent,
        inventoryRefreshTrigger
    ) {
        if (trip != null) {
            buildMonthCalendarDays(
                context = context,
                tripId = trip.id,
                dailyBaseCapacity = trip.availableSpots,
                year = calendarYear,
                month = calendarMonth,
                basePrice = trip.price,
                weekendMarkupPercent = weekendMarkupPercent,
                recomposeTrigger = inventoryRefreshTrigger
            )
        } else 0 to emptyList()
    }

    LaunchedEffect(spotsToBook, monthDays, isOrganizerRole) {
        if (isActivity && monthDays.isNotEmpty()) {
            val currentDayObj = monthDays.find { it.dayOfMonth == selectedCalendarDay }
            val isCurrentValid = if (isOrganizerRole) {
                currentDayObj != null && !currentDayObj.isPastDate
            } else {
                currentDayObj != null && !currentDayObj.isPastDate && currentDayObj.availableSpotsForDay >= spotsToBook && currentDayObj.availableSpotsForDay > 0
            }

            if (!isCurrentValid) {
                val firstValidDay = if (isOrganizerRole) {
                    monthDays.firstOrNull { !it.isPastDate }
                } else {
                    monthDays.firstOrNull { !it.isPastDate && it.availableSpotsForDay >= spotsToBook && it.availableSpotsForDay > 0 }
                }
                if (firstValidDay != null) {
                    selectedCalendarDay = firstValidDay.dayOfMonth
                }
            }
        }
    }

    val selectedDayObj = monthDays.find { it.dayOfMonth == selectedCalendarDay } ?: monthDays.firstOrNull { !it.isPastDate}

    val multiDayOptions = remember(trip, weekendMarkupPercent, inventoryRefreshTrigger) {
        if (trip != null) buildMultiDayTripOptions(context, trip, weekendMarkupPercent, inventoryRefreshTrigger)
        else emptyList()
    }

    LaunchedEffect(spotsToBook, multiDayOptions, isOrganizerRole) {
        if (!isActivity && multiDayOptions.isNotEmpty()) {
            val currentOpt = multiDayOptions.getOrNull(selectedOptionIndex)
            val isCurrentValid = if (isOrganizerRole) {
                currentOpt != null && !currentOpt.isPastDate
            } else {
                currentOpt != null && !currentOpt.isPastDate && currentOpt.availableSpots >= spotsToBook && currentOpt.availableSpots > 0
            }
            if (!isCurrentValid) {
                val firstValidIndex = if (isOrganizerRole) {
                    multiDayOptions.indexOfFirst { !it.isPastDate }
                } else {
                    multiDayOptions.indexOfFirst { !it.isPastDate && it.availableSpots >= spotsToBook && it.availableSpots > 0 }
                }
                if (firstValidIndex >= 0) {
                    selectedOptionIndex = firstValidIndex
                }
            }
        }
    }

    val selectedMultiDayOption = multiDayOptions.getOrNull(selectedOptionIndex) ?: multiDayOptions.firstOrNull()

    val maxSelectableSpotsInView = if (isActivity) {
        monthDays.maxOfOrNull { it.availableSpotsForDay }?.coerceAtLeast(1) ?: 1
    } else {
        multiDayOptions.maxOfOrNull { it.availableSpots }?.coerceAtLeast( 1) ?: 1
    }

    val currentUnitPrice = if (isActivity) {
        selectedDayObj?.price ?: (trip?.price ?: 0.0)
    } else {
        selectedMultiDayOption?.pricePerPerson ?: (trip?.price ?: 0.0)
    }

    val currentSelectedDateIso = if (isActivity) {
        selectedDayObj?.dateIso ?: (trip?.startDate ?: "")
    } else {
        selectedMultiDayOption?.dateIso ?: (trip?.startDate ?: "")
    }

    val currentSelectedLabel = if (isActivity) {
        selectedDayObj?.formattedLabel ?: (trip?.startDate ?: "")
    } else {
        selectedMultiDayOption?.title ?: (trip?.startDate ?: "")
    }

    val currentSelectedBadge = if (isActivity) {
        selectedDayObj?.seasonNote ?: "Tariffa Giornaliera"
    } else {
        selectedMultiDayOption?.seasonLabel ?: "Tariffa Standard"
    }

    val currentAvailableSpots = if (isActivity) {
        selectedDayObj?.availableSpotsForDay ?: 0
    } else {
        selectedMultiDayOption?.availableSpots ?: 0
    }

    val isCurrentSelectionNotPast = if (isActivity) {
        selectedDayObj?.isPastDate == false
    } else {
        selectedMultiDayOption?.isPastDate == false
    }

    // Stato per la modifica rapida dei posti della data selezionata da parte dell'Agenzia
    var agencyEditedSpotsForDate by remember(currentSelectedDateIso, currentAvailableSpots) {
        mutableStateOf(currentAvailableSpots)
    }

    var agencySpotsSavedFeedback by remember(currentSelectedDateIso) { mutableStateOf(false) }

    val isCurrentSelectionBookable = currentAvailableSpots >= spotsToBook && currentAvailableSpots > 0

    val alreadyBookedReservations = viewModel.userReservationsForThisTrip
    val hasAlreadyBooked = alreadyBookedReservations.isNotEmpty()
    val totalAlreadyBookedSpots = alreadyBookedReservations.sumOf { it.bookedSpots }

    LaunchedEffect(trip, showEditDialog) {
        if (trip != null && showEditDialog) {
            editTitle = trip.title
            editOverview = cleanOverviewText
            editPrice = trip.price.toString()
            editLocation = trip.location ?: ""
            editAvailableSpots = trip.availableSpots.toString()
            editStepsList.clear()
            itinerarySteps.forEach { s ->
                editStepsList.add(
                    EditableStepDraft(
                        title = s.title,
                        duration = s.duration,
                        ticketInfo = s.ticketInfo,
                        description = s.description
                    )
                )
            }
        }
    }

    // --- BOTTOM SHEET: CONTATTA L'AGENZIA ORGANIZZATRICE ---
    // --- BOTTOM SHEET SEMPLIFICATO: CONTATTA L'AGENZIA (CHAT O CHIAMATA) ---
    if (showContactAgencySheet && trip != null) {
        val agencyName = trip.organizerName ?: "Agenzia Partner Horizon"
        var inquiryMessage by remember { mutableStateOf("") }

        ModalBottomSheet(
            onDismissRequest = { showContactAgencySheet = false },
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(TripGreen.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Email, contentDescription = null, tint = TripGreen)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "CONTATTO DIRETTO",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TripGreen
                        )
                        Text(
                            text = agencyName,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TripBlack
                        )
                    }
                }

                Text(
                    text = "Scegli se avviare una chat diretta nell'app oppure contattare l'agenzia telefonicamente:",
                    fontSize = 13.sp,
                    color = Color.DarkGray
                )

                OutlinedTextField(
                    value = inquiryMessage,
                    onValueChange = { inquiryMessage = it },
                    label = { Text("Scrivi un messaggio in chat") },
                    placeholder = { Text("Ciao! Vorrei avere informazioni su questo pacchetto...") },
                    minLines = 3,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        viewModel.startOrSendAgencyChatMessage(
                            token = tokenUtente,
                            tripId = currentTripId,
                            messageText = inquiryMessage,
                            onSuccess = {
                                showContactAgencySheet = false
                                contactSuccessMessage = "Chat avviata con $agencyName! Puoi continuare la conversazione nella tab 'Messaggi' in basso."
                            }
                        )
                    },
                    enabled = inquiryMessage.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = TripGreen),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Invia Messaggio in Chat", fontWeight = FontWeight.ExtraBold, color = Color.White, fontSize = 15.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:+39065544332"))
                            context.startActivity(dialIntent)
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = null, tint = TripGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Chiama Ora", color = TripBlack, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            val mailIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:agenzia@horizon.it"))
                            context.startActivity(Intent.createChooser(mailIntent, "Invia email"))
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Email, contentDescription = null, tint = TripGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Email", color = TripBlack, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    if (contactSuccessMessage != null) {
        AlertDialog(
            onDismissRequest = { contactSuccessMessage = null },
            containerColor = Color.White,
            title = { Text("Richiesta Inviata!", fontWeight = FontWeight.ExtraBold, color = TripDarkGreen) },
            text = { Text(contactSuccessMessage!!, color = TripBlack) },
            confirmButton = {
                Button(
                    onClick = { contactSuccessMessage = null },
                    colors = ButtonDefaults.buttonColors(containerColor = TripGreen)
                ) { Text("Perfetto", color = Color.White, fontWeight = FontWeight.Bold) }
            }
        )
    }

    // --- GALLERIA IMMAGINI A TUTTO SCHERMO (LIGHTBOX) ---
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
            }
        }
    }

    // --- BOTTOM SHEET: CHECKOUT E PAGAMENTO ---
    if (showMockPaymentSheet && trip != null) {
        var selectedPaymentMethod by remember { mutableStateOf("CARD") }
        var cardNumber by remember { mutableStateOf("4532 8910 3412 8890") }
        var cardHolder by remember { mutableStateOf(viewModel.currentUserName) }
        var cardExpiry by remember { mutableStateOf("08/29") }
        var cardCvv by remember { mutableStateOf("482") }
        val totalAmount = currentUnitPrice * spotsToBook

        val paymentLabelForReceipt = when (selectedPaymentMethod) {
            "CARD" -> "Carta Visa/Mastercard (•••• ${cardNumber.takeLast(4)})"
            "PAYPAL" -> "PayPal Express Verificato"
            else -> "Google Pay Wallet"
        }

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
                            Text("Data selezionata:", fontSize = 12.sp, color = Color.Gray)
                            Text(currentSelectedLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TripBlack)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Periodo tariffario:", fontSize = 12.sp, color = Color.Gray)
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
                            Text("Totale Operazione", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = TripBlack)
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
                    OfferTypeChip("Carta", selectedPaymentMethod == "CARD") { selectedPaymentMethod = "CARD" }
                    OfferTypeChip("PayPal", selectedPaymentMethod == "PAYPAL") { selectedPaymentMethod = "PAYPAL" }
                    OfferTypeChip("Google Pay", selectedPaymentMethod == "GPAY") { selectedPaymentMethod = "GPAY" }
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
                            context = context,
                            token = tokenUtente,
                            tripId = currentTripId,
                            spots = spotsToBook,
                            selectedDateIso = currentSelectedDateIso,
                            selectedSlotLabel = currentSelectedLabel,
                            paymentMethodLabel = paymentLabelForReceipt,
                            unitPricePaid = currentUnitPrice,
                            onDateBookedSuccess = { inventoryRefreshTrigger++ }
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

    // --- POPUP: SCRIVI UNA RECENSIONE (SALVATA REALMENTE NEL DATABASE) ---
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
                        viewModel.submitReview(
                            token = tokenUtente,
                            tripId = currentTripId,
                            rating = newRating,
                            comment = newComment.trim()
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

    // --- BOTTOM SHEET AGENZIA: MODIFICA ANNUNCIO E TAPPE (MANTIENE INTATTE LE RECENSIONI NEL DB) ---
    if (showEditDialog && trip != null) {
        ModalBottomSheet(
            onDismissRequest = { showEditDialog = false },
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Modifica Annuncio e Tappe",
                    fontSize = 21.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TripBlack
                )

                OutlinedTextField(
                    value = editTitle,
                    onValueChange = { editTitle = it },
                    label = { Text("Titolo") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = editLocation,
                    onValueChange = { editLocation = it },
                    label = { Text("Località") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = editPrice,
                        onValueChange = { editPrice = it },
                        label = { Text("Prezzo Base (€)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = editAvailableSpots,
                        onValueChange = { editAvailableSpots = it },
                        label = { Text("Posti Base") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = editOverview,
                    onValueChange = { editOverview = it },
                    label = { Text("Panoramica Generale") },
                    minLines = 2,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.4f))
                Text(
                    text = "Tappe Step-by-Step dell'Esperienza",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = TripBlack
                )

                editStepsList.forEachIndexed { idx, stepDraft ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = TripWarmBg,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Tappa #${idx + 1}",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                    color = accentColor
                                )
                                if (editStepsList.size > 1) {
                                    IconButton(
                                        onClick = { editStepsList.removeAt(idx) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                            OutlinedTextField(
                                value = stepDraft.title,
                                onValueChange = { editStepsList[idx] = stepDraft.copy(title = it) },
                                label = { Text("Titolo Tappa") },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = stepDraft.duration,
                                    onValueChange = { editStepsList[idx] = stepDraft.copy(duration = it) },
                                    label = { Text("Durata") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(0.45f)
                                )
                                OutlinedTextField(
                                    value = stepDraft.ticketInfo,
                                    onValueChange = { editStepsList[idx] = stepDraft.copy(ticketInfo = it) },
                                    label = { Text("Inclusione") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(0.55f)
                                )
                            }
                            OutlinedTextField(
                                value = stepDraft.description,
                                onValueChange = { editStepsList[idx] = stepDraft.copy(description = it) },
                                label = { Text("Descrizione della tappa") },
                                minLines = 2,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                OutlinedButton(
                    onClick = {
                        editStepsList.add(
                            EditableStepDraft(
                                title = "",
                                duration = if (isActivity) "45 min" else "Giorno ${editStepsList.size + 1}",
                                ticketInfo = "Tappa inclusa",
                                description = ""
                            )
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = accentColor)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Aggiungi Tappa", color = accentColor, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        val encodedSteps = encodeOverviewAndSteps(editOverview, editStepsList, isActivity)
                        val finalDescriptionWithReviews = if (parsedContent.rawReviewBlocks.isNotBlank()) {
                            "$encodedSteps\n${parsedContent.rawReviewBlocks}"
                        } else encodedSteps

                        val request = TripRequest(
                            title = editTitle,
                            description = finalDescriptionWithReviews,
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
                    colors = ButtonDefaults.buttonColors(containerColor = TripGreen),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Text("Salva Modifiche e Programma", color = Color.White, fontWeight = FontWeight.ExtraBold)
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
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
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = { showEditDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = TripGreen),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.height(50.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Modifica Annuncio", fontWeight = FontWeight.Bold)
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
                                        text = "Data Esaurita",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Gray,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isCurrentSelectionBookable) "Data: $currentSelectedDateIso" else "Non disponibile per $spotsToBook pers.",
                                    fontSize = 11.sp,
                                    color = if (isCurrentSelectionBookable) TripGreen else MaterialTheme.colorScheme.error,
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
                                    IconButton(onClick = { if (spotsToBook < maxSelectableSpotsInView) spotsToBook++ }) {
                                        Text("+", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = TripGreen)
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Button(
                                    onClick = { showMockPaymentSheet = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = TripGreen),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.height(52.dp),
                                    enabled = !viewModel.isBooking && isCurrentSelectionBookable
                                ) {
                                    Text(
                                        text = if (isCurrentSelectionBookable) "Prenota Ora" else "Non disp.",
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
                            color = accentColor,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(bottom = 42.dp, start = 24.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isActivity) Icons.Default.ConfirmationNumber else Icons.Default.FlightTakeoff,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isActivity) "ATTIVITÀ / ESCURSIONE IN GIORNATA" else "PACCHETTO VIAGGIO PIÙ GIORNI",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
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
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    trip.location ?: "Destinazione",
                                    color = accentColor,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp
                                )
                            }

                            // MEDIA E CONTEGGIO REALI SINCRONIZZATI CON IL DB E CON LA HOME
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
                                    if (realReviewCount > 0) {
                                        Text(
                                            text = String.format(Locale.US, "%.1f", realAverageRating),
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 14.sp,
                                            color = TripBlack
                                        )
                                        Text(
                                            text = " ($realReviewCount recensioni ↓)",
                                            color = TripGreen,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    } else {
                                        Text(
                                            text = "Nuovo (0 recensioni ↓)",
                                            color = TripGreen,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
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
                                            text = "Posti prenotati da te: $totalAlreadyBookedSpots • Esportabile in .ics dalla tab Viaggi.",
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
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            QuickSpecTile(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.Place,
                                label = "Programma",
                                value = "${itinerarySteps.size} tappe incluse",
                                tint = accentColor
                            )
                            QuickSpecTile(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.Info,
                                label = "Guida & Lingua",
                                value = "Italiano / Inglese",
                                tint = TripDarkGreen
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // =========================================================================
                        // 3. CALENDARIO PULITO CON BLOCCO AUTOMATICO CASELLE IN BASE AI PARTECIPANTI
                        // =========================================================================
                        Text(
                            text = if (isOrganizerRole) {
                                if (isActivity) "Calendario Agenzia (Tocca un giorno per aggiungere posti)"
                                else "Turni di Partenza (Tocca un turno per gestire i posti)"
                            } else {
                                if (isActivity) "Seleziona una Data Disponibile"
                                else "Scegli Turno di Partenza"
                            },
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TripBlack
                        )
                        Text(
                            text = if (isOrganizerRole)
                                "Puoi selezionare qualsiasi data futura (anche con 0 posti) per aumentare i posti disponibili ai viaggiatori"
                            else
                                "Le date passate o che non dispongono di almeno $spotsToBook posto/i non sono selezionabili",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        if (isActivity) {
                            val monthTitle = remember(calendarYear, calendarMonth) {
                                val c = Calendar.getInstance().apply {
                                    set(Calendar.YEAR, calendarYear)
                                    set(Calendar.MONTH, calendarMonth)
                                }
                                SimpleDateFormat("MMMM yyyy", Locale.ITALY).format(c.time).replaceFirstChar { it.uppercase() }
                            }

                            val canGoPreviousMonth = calendarYear > todayYear || (calendarYear == todayYear && calendarMonth > todayMonth)

                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = TripWarmBg,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = {
                                                if (canGoPreviousMonth) {
                                                    if (calendarMonth == 0) {
                                                        calendarMonth = 11
                                                        calendarYear--
                                                    } else calendarMonth--
                                                    selectedCalendarDay = 1
                                                }
                                            },
                                            enabled = canGoPreviousMonth,
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(if (canGoPreviousMonth) Color.White else Color.LightGray.copy(alpha = 0.3f))
                                        ) {
                                            Icon(
                                                Icons.Default.KeyboardArrowLeft,
                                                contentDescription = "Mese precedente",
                                                tint = if (canGoPreviousMonth) TripBlack else Color.Gray
                                            )
                                        }

                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = monthTitle,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 16.sp,
                                                color = TripBlack
                                            )
                                            if (calendarMonth in 5..7) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.WbSunny, contentDescription = null, tint = TripOrangeActivity, modifier = Modifier.size(12.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "Alta Stagione Estiva",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = TripOrangeActivity
                                                    )
                                                }
                                            } else if (calendarMonth == 11) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.AcUnit, contentDescription = null, tint = TripOrangeActivity, modifier = Modifier.size(12.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "Festività Invernali",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = TripOrangeActivity
                                                    )
                                                }
                                            }
                                        }

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
                                                        Spacer(modifier = Modifier.weight(1f).height(48.dp))
                                                    } else {
                                                        val hasSpotsForTraveler = !dayItem.isPastDate && dayItem.availableSpotsForDay >= spotsToBook && dayItem.availableSpotsForDay > 0
                                                        val isCellClickable = if (isOrganizerRole) !dayItem.isPastDate else hasSpotsForTraveler
                                                        val isSelected = isCellClickable && dayItem.dayOfMonth == selectedCalendarDay

                                                        val bgColor = when {
                                                            isSelected -> TripGreen
                                                            dayItem.isPastDate -> Color.LightGray.copy(alpha = 0.22f)
                                                            !hasSpotsForTraveler && isOrganizerRole -> TripOrangeActivity.copy(alpha = 0.14f)
                                                            !hasSpotsForTraveler -> Color.LightGray.copy(alpha = 0.28f)
                                                            dayItem.isHighDemandOrHoliday -> TripOrangeActivity.copy(alpha = 0.08f)
                                                            else -> Color.White
                                                        }

                                                        Box(
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .height(48.dp)
                                                                .clip(RoundedCornerShape(10.dp))
                                                                .background(bgColor)
                                                                .then(
                                                                    if (isSelected) Modifier.border(2.dp, TripDarkGreen, RoundedCornerShape(10.dp))
                                                                    else Modifier
                                                                )
                                                                .clickable(enabled = isCellClickable) {
                                                                    selectedCalendarDay = dayItem.dayOfMonth
                                                                },
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                                Text(
                                                                    text = "${dayItem.dayOfMonth}",
                                                                    fontWeight = FontWeight.ExtraBold,
                                                                    fontSize = 13.sp,
                                                                    color = when {
                                                                        isSelected -> Color.White
                                                                        dayItem.isPastDate -> Color.Gray.copy(alpha = 0.45f)
                                                                        !hasSpotsForTraveler && !isOrganizerRole -> Color.Gray.copy(alpha = 0.55f)
                                                                        else -> TripBlack
                                                                    }
                                                                )
                                                                Text(
                                                                    text = when {
                                                                        dayItem.isPastDate -> "—"
                                                                        dayItem.availableSpotsForDay <= 0 && isOrganizerRole -> "0p"
                                                                        !hasSpotsForTraveler && !isOrganizerRole -> "—"
                                                                        else -> "€${dayItem.price.roundToInt()}"
                                                                    },
                                                                    fontWeight = FontWeight.Bold,
                                                                    fontSize = 10.sp,
                                                                    color = when {
                                                                        isSelected -> Color.White
                                                                        dayItem.isPastDate -> Color.Gray.copy(alpha = 0.4f)
                                                                        dayItem.availableSpotsForDay <= 0 && isOrganizerRole -> MaterialTheme.colorScheme.error
                                                                        !hasSpotsForTraveler && !isOrganizerRole -> Color.Gray.copy(alpha = 0.45f)
                                                                        dayItem.isHighDemandOrHoliday -> TripOrangeActivity
                                                                        else -> TripDarkGreen
                                                                    }
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    if (selectedDayObj != null && !selectedDayObj.isPastDate) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = Color.White,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(14.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = selectedDayObj.formattedLabel,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            fontSize = 14.sp,
                                                            color = TripBlack
                                                        )
                                                        Spacer(modifier = Modifier.height(2.dp))
                                                        Text(
                                                            text = if (isOrganizerRole)
                                                                "${selectedDayObj.availableSpotsForDay} posti attuali in questa data"
                                                            else
                                                                selectedDayObj.seasonNote,
                                                            fontSize = 12.sp,
                                                            color = if (selectedDayObj.availableSpotsForDay > 0) TripGreen else MaterialTheme.colorScheme.error,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                    Text(
                                                        text = "€${selectedDayObj.price}",
                                                        fontWeight = FontWeight.ExtraBold,
                                                        fontSize = 19.sp,
                                                        color = TripDarkGreen
                                                    )
                                                }

                                                if (isOrganizerRole) {
                                                    Spacer(modifier = Modifier.height(10.dp))
                                                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.35f))
                                                    Spacer(modifier = Modifier.height(10.dp))

                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            modifier = Modifier
                                                                .background(TripWarmBg, RoundedCornerShape(12.dp))
                                                                .padding(horizontal = 4.dp)
                                                        ) {
                                                            IconButton(
                                                                onClick = { if (agencyEditedSpotsForDate > 0) agencyEditedSpotsForDate-- },
                                                                modifier = Modifier.size(34.dp)
                                                            ) {
                                                                Text("-", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = TripBlack)
                                                            }
                                                            Text(
                                                                text = "$agencyEditedSpotsForDate posti",
                                                                fontWeight = FontWeight.ExtraBold,
                                                                fontSize = 13.sp,
                                                                color = TripBlack,
                                                                modifier = Modifier.padding(horizontal = 8.dp)
                                                            )
                                                            IconButton(
                                                                onClick = { agencyEditedSpotsForDate++ },
                                                                modifier = Modifier.size(34.dp)
                                                            ) {
                                                                Text("+", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = TripGreen)
                                                            }
                                                        }

                                                        Button(
                                                            onClick = {
                                                                val datePrefs = context.getSharedPreferences("date_inventory_prefs", Context.MODE_PRIVATE)
                                                                datePrefs.edit()
                                                                    .putInt("custom_cap_${currentTripId}_${selectedDayObj.dateIso}", agencyEditedSpotsForDate)
                                                                    .putInt("booked_${currentTripId}_${selectedDayObj.dateIso}", 0)
                                                                    .apply()
                                                                inventoryRefreshTrigger++
                                                                agencySpotsSavedFeedback = true

                                                                if (agencyEditedSpotsForDate > trip.availableSpots) {
                                                                    viewModel.updateTrip(
                                                                        tokenUtente,
                                                                        currentTripId,
                                                                        TripRequest(
                                                                            title = trip.title,
                                                                            description = trip.description ?: "",
                                                                            price = trip.price,
                                                                            startDate = trip.startDate,
                                                                            endDate = trip.endDate,
                                                                            availableSpots = agencyEditedSpotsForDate,
                                                                            location = trip.location ?: "",
                                                                            imageUrls = trip.imageUrls
                                                                        )
                                                                    )
                                                                }
                                                            },
                                                            colors = ButtonDefaults.buttonColors(containerColor = TripGreen),
                                                            shape = RoundedCornerShape(10.dp),
                                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                                                        ) {
                                                            Text(
                                                            text = if (agencySpotsSavedFeedback) "Aggiornato" else "Salva Posti Data",
                                                                fontSize = 12.sp,
                                                                fontWeight = FontWeight.ExtraBold,
                                                                color = Color.White
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            // TURNI DI PARTENZA PER VIAGGI DI PIÙ GIORNI (CON BLOCCO DATE PASSATE E GESTIONE POSTI AGENZIA)
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                multiDayOptions.forEachIndexed { index, option ->
                                    val hasSpotsForTraveler = !option.isPastDate && option.availableSpots >= spotsToBook && option.availableSpots > 0
                                    val isOptionClickable = if (isOrganizerRole) !option.isPastDate else hasSpotsForTraveler
                                    val isSelected = isOptionClickable && selectedOptionIndex == index

                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = when {
                                            isSelected -> TripGreen.copy(alpha = 0.1f)
                                            option.isPastDate || !isOptionClickable -> Color.LightGray.copy(alpha = 0.25f)
                                            else -> TripWarmBg
                                        },
                                        border = if (isSelected) BorderStroke(2.dp, TripGreen) else null,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable(enabled = isOptionClickable) {
                                                selectedOptionIndex = index
                                            }
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Surface(
                                                        color = if (isSelected) TripGreen else Color.White,
                                                        shape = RoundedCornerShape(6.dp)
                                                    ) {
                                                        Text(
                                                            text = when {
                                                                option.isPastDate -> "Data Passata (Bloccata)"
                                                                !hasSpotsForTraveler && isOrganizerRole -> "0 posti (Tocca per aggiungere)"
                                                                !hasSpotsForTraveler -> "Non disponibile per $spotsToBook pers."
                                                                else -> option.seasonLabel
                                                            },
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = when {
                                                                isSelected -> Color.White
                                                                option.isPastDate || (!hasSpotsForTraveler && !isOrganizerRole) -> Color.Gray
                                                                !hasSpotsForTraveler && isOrganizerRole -> MaterialTheme.colorScheme.error
                                                                else -> TripDarkGreen
                                                            },
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = option.title,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        fontSize = 15.sp,
                                                        color = if (isOptionClickable) TripBlack else Color.Gray
                                                    )
                                                }
                                                Text(
                                                    text = "€${option.pricePerPerson}",
                                                    fontSize = 18.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = if (isOptionClickable) TripDarkGreen else Color.Gray
                                                )
                                            }

                                            if (isOrganizerRole && isSelected && !option.isPastDate) {
                                                Spacer(modifier = Modifier.height(10.dp))
                                                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.4f))
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier
                                                            .background(Color.White, RoundedCornerShape(10.dp))
                                                            .padding(horizontal = 4.dp)
                                                    ) {
                                                        IconButton(
                                                            onClick = { if (agencyEditedSpotsForDate > 0) agencyEditedSpotsForDate-- },
                                                            modifier = Modifier.size(32.dp)
                                                        ) {
                                                            Text("-", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = TripBlack)
                                                        }
                                                        Text(
                                                            text = "$agencyEditedSpotsForDate posti",
                                                            fontWeight = FontWeight.ExtraBold,
                                                            fontSize = 13.sp,
                                                            color = TripBlack,
                                                            modifier = Modifier.padding(horizontal = 8.dp)
                                                        )
                                                        IconButton(
                                                            onClick = { agencyEditedSpotsForDate++ },
                                                            modifier = Modifier.size(32.dp)
                                                        ) {
                                                            Text("+", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = TripGreen)
                                                        }
                                                    }

                                                    Button(
                                                        onClick = {
                                                            val datePrefs = context.getSharedPreferences("date_inventory_prefs", Context.MODE_PRIVATE)
                                                            datePrefs.edit()
                                                                .putInt("custom_cap_${currentTripId}_${option.dateIso}", agencyEditedSpotsForDate)
                                                                .putInt("booked_${currentTripId}_${option.dateIso}", 0)
                                                                .apply()
                                                            inventoryRefreshTrigger++
                                                            agencySpotsSavedFeedback = true
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = TripGreen),
                                                        shape = RoundedCornerShape(10.dp),
                                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                                                    ) {
                                                        Text(
                                                            text = if (agencySpotsSavedFeedback) "Aggiornato" else "Salva Posti Turno",
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = Color.White
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(22.dp))

                        // =========================================================================
                        // 4. CARD TOUR OPERATOR CON PULSANTE "CONTATTA AGENZIA"
                        // =========================================================================
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = TripWarmBg,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
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
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "Organizzato da ${trip.organizerName ?: "Agenzia Partner Horizon"}",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 15.sp,
                                            color = TripBlack
                                        )
                                        Text(
                                            "Operatore Verificato • Tempo di risposta medio < 1 ora",
                                            fontSize = 12.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }

                                if (ruoloUtente != "ORGANIZZATORE") {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    OutlinedButton(
                                        onClick = { showContactAgencySheet = true },
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.5.dp, TripGreen),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.Email, contentDescription = null, tint = TripGreen, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Contatta l'Agenzia Organizzatrice",
                                            color = TripDarkGreen,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // =========================================================================
                        // 5. PANORAMICA + TIMELINE STEP-BY-STEP PERSONALIZZATA DALL'AGENZIA
                        // =========================================================================
                        Text(
                            text = "Panoramica dell'Esperienza",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TripBlack
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = cleanOverviewText,
                            fontSize = 14.sp,
                            color = Color.DarkGray,
                            lineHeight = 21.sp
                        )

                        Spacer(modifier = Modifier.height(22.dp))

                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = TripWarmBg,
                            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(accentColor.copy(alpha = 0.14f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Place, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = if (isActivity) "Cosa aspettarsi (Tappa per Tappa)"
                                                else "Itinerario Ufficiale del Viaggio",
                                                fontSize = 17.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = TripBlack
                                            )
                                            Text(
                                                text = "${itinerarySteps.size} tappe programmate dall'agenzia",
                                                fontSize = 12.sp,
                                                color = Color.Gray
                                            )
                                        }
                                    }

                                    if (ruoloUtente == "ORGANIZZATORE") {
                                        TextButton(onClick = { showEditDialog = true }) {
                                            Icon(Icons.Default.Edit, contentDescription = null, tint = accentColor, modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Modifica Tappe", color = accentColor, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                                        }
                                    } else {
                                        TextButton(onClick = { expandAllSteps = !expandAllSteps }) {
                                            Text(
                                                text = if (expandAllSteps) "Comprimi" else "Espandi",
                                                color = accentColor,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(TripDarkGreen),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "Partenza: ${trip.location ?: "Punto d'incontro ufficiale"}",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp,
                                        color = TripDarkGreen
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                itinerarySteps.forEachIndexed { idx, step ->
                                    val isLast = idx == itinerarySteps.lastIndex
                                    var isStepExpanded by remember(expandAllSteps) { mutableStateOf(expandAllSteps) }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(IntrinsicSize.Min)
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.width(28.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(CircleShape)
                                                    .background(accentColor),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "${step.stepNumber}",
                                                    color = Color.White,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 13.sp
                                                )
                                            }
                                            if (!isLast) {
                                                Box(
                                                    modifier = Modifier
                                                        .width(2.5.dp)
                                                        .fillMaxHeight()
                                                        .background(accentColor.copy(alpha = 0.35f))
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = Color.White,
                                            shadowElevation = 1.dp,
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(bottom = if (isLast) 0.dp else 12.dp)
                                                .clickable { isStepExpanded = !isStepExpanded }
                                        ) {
                                            Column(modifier = Modifier.padding(14.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = step.title,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        fontSize = 14.sp,
                                                        color = TripBlack,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Surface(
                                                        color = accentColor.copy(alpha = 0.12f),
                                                        shape = RoundedCornerShape(6.dp)
                                                    ) {
                                                        Text(
                                                            text = step.duration,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = accentColor,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(6.dp))

                                                Surface(
                                                    color = TripGreen.copy(alpha = 0.1f),
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    val cleanTicketText = step.ticketInfo
                                                        .replace(Regex("^[^a-zA-Z0-9À-ÿ]+"), "")
                                                        .trim()
                                                        .ifEmpty { step.ticketInfo }

                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.CheckCircle,
                                                            contentDescription = null,
                                                            tint = TripDarkGreen,
                                                            modifier = Modifier.size(13.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(5.dp))
                                                        Text(
                                                            text = cleanTicketText,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = TripDarkGreen
                                                        )
                                                    }
                                                }

                                                AnimatedVisibility(visible = isStepExpanded) {
                                                    Column {
                                                        Spacer(modifier = Modifier.height(8.dp))
                                                        Text(
                                                            text = step.description,
                                                            fontSize = 13.sp,
                                                            color = Color.DarkGray,
                                                            lineHeight = 19.sp
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(28.dp))
                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.4f))
                        Spacer(modifier = Modifier.height(20.dp))

                        // =========================================================================
                        // 6. SEZIONE RECENSIONI REALI DAL DATABASE POSTGRESQL
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
                                    text = "Recensioni dei Viaggiatori ($realReviewCount)",
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TripBlack
                                )
                                Text(
                                    text = if (realReviewCount > 0)
                                        "Media verificata: ${String.format(Locale.US, "%.1f", realAverageRating)} / 5.0"
                                    else
                                        "Nessuna recensione ancora presente per questa offerta",
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

                        if (dbReviews.isEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = TripWarmBg,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Sii il primo viaggiatore a lasciare una recensione per questa esperienza!",
                                    fontSize = 13.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        } else {
                            dbReviews.forEach { rev ->
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
private fun QuickSpecTile(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    tint: Color
) {
    Surface(
        modifier = modifier,
        color = TripWarmBg,
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(label, fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                Text(
                    value,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TripBlack,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}