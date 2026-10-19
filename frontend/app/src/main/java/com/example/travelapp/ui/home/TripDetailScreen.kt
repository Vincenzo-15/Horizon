package com.example.travelapp.ui.home

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.travelapp.network.TripDTO
import com.example.travelapp.network.TripRequest
import com.example.travelapp.viewmodel.TripDetailViewModel
import com.example.travelapp.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

// Modello per Giorno / Fascia di partenza con prezzo e posti dinamici
data class DynamicAvailabilityOption(
    val id: Int,
    val title: String,
    val subtitle: String,
    val badge: String,
    val pricePerPerson: Double,
    val availableSpots: Int
)

// Modello per le Recensioni
data class ExperienceReview(
    val author: String,
    val rating: Int,
    val date: String,
    val comment: String
)

// Genera le opzioni dinamiche:
// - Per le ATTIVITÀ: i prossimi 7 giorni con variazione di prezzo (Feriale, Weekend, Promo Infrasettimanale)
// - Per i VIAGGI: 3 Finestre/Formule di partenza multi-giorno con prezzi differenziati
fun buildDynamicOptions(
    trip: TripDTO,
    isActivity: Boolean,
    weekendMarkupPercent: Int,
    midweekDiscountPercent: Int
): List<DynamicAvailabilityOption> {
    val basePrice = trip.price
    val baseSpots = trip.availableSpots.coerceAtLeast(0)
    val sdfInput = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val sdfDayName = SimpleDateFormat("EEE", Locale.ITALY)
    val sdfDayNum = SimpleDateFormat("dd MMM", Locale.ITALY)

    val startCal = Calendar.getInstance()
    try {
        if (!trip.startDate.isNullOrBlank()) {
            sdfInput.parse(trip.startDate)?.let { startCal.time = it }
        }
    } catch (_: Exception) {}

    if (isActivity) {
        return (0..6).map { offset ->
            val cal = (startCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, offset) }
            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
            val isWeekend = dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY || dayOfWeek == Calendar.FRIDAY
            val isPromoDay = dayOfWeek == Calendar.TUESDAY || dayOfWeek == Calendar.WEDNESDAY

            val multiplier = when {
                isWeekend -> 1.0 + (weekendMarkupPercent / 100.0)
                isPromoDay -> 1.0 - (midweekDiscountPercent / 100.0)
                else -> 1.0
            }
            val dayPrice = ((basePrice * multiplier) * 10.0).roundToInt() / 10.0
            val daySpots = if (baseSpots == 0) 0 else (baseSpots - (offset % 3)).coerceAtLeast(1)

            DynamicAvailabilityOption(
                id = offset,
                title = sdfDayNum.format(cal.time).uppercase(),
                subtitle = sdfDayName.format(cal.time).replaceFirstChar { it.uppercase() },
                badge = when {
                    isWeekend -> "Weekend (+${weekendMarkupPercent}%)"
                    isPromoDay && midweekDiscountPercent > 0 -> "Promo (-${midweekDiscountPercent}%)"
                    else -> "Tariffa Base"
                },
                pricePerPerson = dayPrice,
                availableSpots = daySpots
            )
        }
    } else {
        // Strategia VIAGGI DI PIÙ GIORNI: Turni di partenza & Formule Pacchetto
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
                availableSpots = baseSpots
            ),
            DynamicAvailabilityOption(
                id = 1,
                title = "2° Turno: ${sdfDayNum.format(cal2Start.time)} → ${sdfDayNum.format(cal2End.time)}",
                subtitle = "Partenza Alta Richiesta + Assicurazione & Flex inclusa",
                badge = "Alta Richiesta (+$weekendMarkupPercent%)",
                pricePerPerson = highSeasonPrice,
                availableSpots = if (baseSpots > 2) baseSpots - 2 else baseSpots
            ),
            DynamicAvailabilityOption(
                id = 2,
                title = "3° Turno: ${sdfDayNum.format(cal3Start.time)} → ${sdfDayNum.format(cal3End.time)}",
                subtitle = "Partenza Smart • Tariffa Early Bird prenotazione anticipata",
                badge = if (midweekDiscountPercent > 0) "Early Bird (-$midweekDiscountPercent%)" else "Disponibile",
                pricePerPerson = earlyBirdPrice,
                availableSpots = baseSpots
            )
        )
    }
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
    var currentTripId by remember { mutableStateOf(tripId) }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDynamicPricingDialog by remember { mutableStateOf(false) }
    var showMockPaymentSheet by remember { mutableStateOf(false) }
    var showAddReviewDialog by remember { mutableStateOf(false) }
    var isFavorite by remember { mutableStateOf(false) }

    var spotsToBook by remember { mutableStateOf(1) }
    var selectedOptionIndex by remember { mutableStateOf(0) }

    // Preferenze per la variabilità prezzi configurabile dall'Agenzia per ogni annuncio
    val pricingPrefs = remember { context.getSharedPreferences("dynamic_pricing_prefs", Context.MODE_PRIVATE) }
    var weekendMarkupPercent by remember(currentTripId) {
        mutableStateOf(pricingPrefs.getInt("markup_$currentTripId", 20))
    }
    var midweekDiscountPercent by remember(currentTripId) {
        mutableStateOf(pricingPrefs.getInt("discount_$currentTripId", 10))
    }

    // Stato per il form di modifica
    var editTitle by remember { mutableStateOf("") }
    var editDescription by remember { mutableStateOf("") }
    var editPrice by remember { mutableStateOf("") }
    var editLocation by remember { mutableStateOf("") }
    var editAvailableSpots by remember { mutableStateOf("") }

    // Recensioni dinamiche dell'esperienza
    val reviews = remember(currentTripId) {
        mutableStateListOf(
            ExperienceReview(
                author = "Giulia M.",
                rating = 5,
                date = "Ottobre 2026",
                comment = "Esperienza impeccabile! Organizzazione puntuale, guida preparatissima e ottimo rapporto qualità/prezzo. Consigliatissimo!"
            ),
            ExperienceReview(
                author = " Alessandro R.",
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
        viewModel.fetchTripDetail(tokenUtente, currentTripId)
    }

    LaunchedEffect(viewModel.isDeleted) {
        if (viewModel.isDeleted) onNavigateBack()
    }

    val trip = viewModel.trip
    val isActivity = trip?.let { isActivityOrExcursion(it) } ?: false

    val dynamicOptions = remember(trip, isActivity, weekendMarkupPercent, midweekDiscountPercent) {
        if (trip != null) {
            buildDynamicOptions(trip, isActivity, weekendMarkupPercent, midweekDiscountPercent)
        } else emptyList()
    }

    val selectedOption = dynamicOptions.getOrNull(selectedOptionIndex) ?: dynamicOptions.firstOrNull()
    val currentUnitPrice = selectedOption?.pricePerPerson ?: (trip?.price ?: 0.0)
    val currentAvailableSpots = selectedOption?.availableSpots ?: (trip?.availableSpots ?: 0)

    LaunchedEffect(trip, showEditDialog) {
        if (trip != null && showEditDialog) {
            editTitle = trip.title
            editDescription = trip.description ?: ""
            editPrice = trip.price.toString()
            editLocation = trip.location ?: ""
            editAvailableSpots = trip.availableSpots.toString()
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
                            "Imposta il prezzo base giornaliero e la variazione automatica per i giorni di Weekend o Promo infrasettimanale:"
                        else
                            "Configura la tariffa base del pacchetto e la variazione per le finestre di Alta Stagione o Early Bird:",
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
                        label = { Text("Posti disponibili per data/turno") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = if (isActivity) "Maggiorazione Weekend (Ven-Dom): +${tempMarkup.roundToInt()}%"
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
                        text = if (isActivity) "Sconto Giorni Feriali Promo: -${tempDiscount.roundToInt()}%"
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
                    Text("Applica Listino Dinamico", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDynamicPricingDialog = false }) { Text("Annulla") }
            }
        )
    }

    // --- BOTTOM SHEET: SIMULAZIONE PAGAMENTO (MOCK CHECKOUT) ---
    if (showMockPaymentSheet && trip != null) {
        var selectedPaymentMethod by remember { mutableStateOf("CARD") }
        var cardNumber by remember { mutableStateOf("4532 •••• •••• 8910") }
        var cardHolder by remember { mutableStateOf("Mario Rossi") }
        val totalAmount = currentUnitPrice * spotsToBook

        ModalBottomSheet(
            onDismissRequest = { showMockPaymentSheet = false },
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text("Riepilogo e Pagamento Sicuro", fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, color = TripBlack)
                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = TripWarmBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(trip.title, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = TripBlack)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Opzione scelta: ${selectedOption?.title ?: trip.startDate} (${selectedOption?.badge ?: "Standard"})",
                            fontSize = 13.sp,
                            color = TripGreen,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("$spotsToBook partecipanti × €$currentUnitPrice", fontSize = 13.sp, color = Color.DarkGray)
                            Text("Totale: €${String.format(Locale.ITALY, "%.2f", totalAmount)}", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = TripDarkGreen)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Metodo di pagamento", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TripBlack)
                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OfferTypeChip("💳 Carta", selectedPaymentMethod == "CARD") { selectedPaymentMethod = "CARD" }
                    OfferTypeChip("🅿️ PayPal", selectedPaymentMethod == "PAYPAL") { selectedPaymentMethod = "PAYPAL" }
                    OfferTypeChip("📱 Google Pay", selectedPaymentMethod == "GPAY") { selectedPaymentMethod = "GPAY" }
                }

                if (selectedPaymentMethod == "CARD") {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = cardHolder,
                        onValueChange = { cardHolder = it },
                        label = { Text("Intestatario Carta") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = cardNumber,
                        onValueChange = { cardNumber = it },
                        label = { Text("Numero Carta (Simulazione Mock)") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = {
                        showMockPaymentSheet = false
                        viewModel.bookTrip(
                            token = tokenUtente,
                            tripId = currentTripId,
                            spots = spotsToBook,
                            selectedSlotLabel = selectedOption?.title ?: ""
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TripGreen),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Conferma e Paga €${String.format(Locale.ITALY, "%.2f", totalAmount)}",
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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = selectedOption?.badge ?: "Prezzo Totale",
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

                        if (ruoloUtente == "ORGANIZZATORE") {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { showDynamicPricingDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = TripGreen),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.height(50.dp)
                                ) {
                                    Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Prezzi & Date", fontWeight = FontWeight.Bold)
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
                        } else {
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
                        .verticalScroll(rememberScrollState())
                ) {
                    // 1. GALLERIA FOTOGRAFICA FULL-BLEED
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp)
                            .background(Color.LightGray)
                    ) {
                        val pagerState = rememberPagerState(pageCount = { images.size })

                        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                            AsyncImage(
                                model = images[page],
                                contentDescription = "${trip.title} - foto ${page + 1}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
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
                        ) {
                            Text(
                                "${pagerState.currentPage + 1}/${images.size}",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = TripAmber, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "${trip.averageRating}", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = TripBlack)
                                Text(text = " (${reviews.size + trip.reviewCount} recensioni)", color = Color.Gray, fontSize = 13.sp)
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

                        Spacer(modifier = Modifier.height(16.dp))

                        // BADGE PUNTI DI FORZA (STILE TRIPADVISOR / AIRBNB EXPERIENCES)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            HighlightPill(icon = Icons.Default.CheckCircle, text = "Cancellazione flessibile")
                            HighlightPill(icon = Icons.Default.DateRange, text = "Esportabile .ics")
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // =========================================================================
                        // 3. SELETTORE GIORNI E PREZZI DINAMICI (ATTIVITÀ VS VIAGGI)
                        // =========================================================================
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isActivity) "Scegli il Giorno (Prezzi Dinamici)"
                                    else "Scegli Finestra di Partenza & Formula",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TripBlack
                                )
                                Text(
                                    text = if (isActivity) "Il prezzo e i posti variano in base alla giornata selezionata"
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
                            // CALENDARIO ORIZZONTALE GIORNALIERO PER LE ATTIVITÀ
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                itemsIndexed(dynamicOptions) { index, option ->
                                    val isSelected = selectedOptionIndex == index
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = if (isSelected) TripGreen.copy(alpha = 0.12f) else TripWarmBg,
                                        border = if (isSelected) BorderStroke(2.dp, TripGreen) else null,
                                        modifier = Modifier
                                            .width(126.dp)
                                            .clickable { selectedOptionIndex = index }
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = option.subtitle,
                                                fontSize = 12.sp,
                                                color = if (isSelected) TripDarkGreen else Color.Gray,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = option.title,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = TripBlack
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Surface(
                                                color = if (isSelected) TripGreen else Color.White,
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(
                                                    text = "€${option.pricePerPerson}",
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = if (isSelected) Color.White else TripDarkGreen,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "${option.availableSpots} posti",
                                                fontSize = 11.sp,
                                                color = Color.DarkGray
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            // FINESTRE DI PARTENZA & FORMULE PER I VIAGGI DI PIÙ GIORNI
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                dynamicOptions.forEachIndexed { index, option ->
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
                        // 6. SEZIONE RECENSIONI DEI VIAGGIATORI (STILE TRIPADVISOR)
                        // =========================================================================
                        Row(
                            modifier = Modifier.fillMaxWidth(),
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
                                text = "Altri viaggi e attività che potrebbero piacerti",
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