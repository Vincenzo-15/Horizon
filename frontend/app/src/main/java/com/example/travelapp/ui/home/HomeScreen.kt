package com.example.travelapp.ui.home

import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.travelapp.network.ReservationDTO
import com.example.travelapp.network.TripDTO
import com.example.travelapp.ui.theme.*
import com.example.travelapp.viewmodel.HomeViewModel
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.roundToInt
import java.text.SimpleDateFormat

//fun isActivityOrExcursion(trip: TripDTO): Boolean {
//    val sameDay = !trip.startDate.isNullOrBlank() && trip.startDate == trip.endDate
//    val text = "${trip.title} ${trip.description ?: ""}".lowercase()
//
//    val hasActivityKeyword = text.contains("attività") ||
//            text.contains("attivita") ||
//            text.contains("escursion") ||
//            text.contains("tour") ||
//            text.contains("visita") ||
//            text.contains("degustazion") ||
//            text.contains("biglietto")
//    return sameDay || hasActivityKeyword
//}

fun isActivityOrExcursion(trip: TripDTO): Boolean {
    if (!trip.startDate.isNullOrBlank() && !trip.endDate.isNullOrBlank()) {
        return trip.startDate == trip.endDate
    }
    val titleLower = trip.title.lowercase()
    return titleLower.contains("attività") ||
            titleLower.contains("attivita") ||
            titleLower.contains("escursion")
}

fun buildIcsContent(
    reservation: ReservationDTO,
    trip: TripDTO?,
    receiptInfo: ResolvedReceiptInfo? = null
): String {
    val rawStart = (trip?.startDate ?: reservation.reservationDate.take(10)).replace("-", "")
    val rawEnd = (trip?.endDate ?: rawStart).replace("-", "")
    val location = trip?.location ?: "Destinazione Horizon"
    val slotText = receiptInfo?.selectedDateSlot ?: "${trip?.startDate ?: ""} → ${trip?.endDate ?: ""}"
    val participantsText = receiptInfo?.participantsList ?: "${reservation.bookedSpots} partecipanti"
    val description = "Prenotazione #${reservation.id} | Data/Turno: $slotText | Partecipanti: $participantsText | Totale: €${reservation.totalPaid}"

    return """
        BEGIN:VCALENDAR
        VERSION:2.0
        PRODID:-//Horizon Travel App//IT
        BEGIN:VEVENT
        UID:horizon-res-${reservation.id}@horizontravel.it
        DTSTART;VALUE=DATE:$rawStart
        DTEND;VALUE=DATE:$rawEnd
        SUMMARY:${reservation.tripTitle}
        LOCATION:$location
        DESCRIPTION:$description
        STATUS:${reservation.status}
        END:VEVENT
        END:VCALENDAR
    """.trimIndent()
}

fun shareIcsCalendar(context: Context, icsContent: String, title: String) {
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/calendar"
        putExtra(Intent.EXTRA_SUBJECT, "Calendario Viaggio: $title.ics")
        putExtra(Intent.EXTRA_TEXT, icsContent)
    }
    context.startActivity(Intent.createChooser(sendIntent, "Esporta file .ics"))
}

fun openSystemCalendar(
    context: Context,
    title: String,
    location: String,
    description: String,
    startDateIso: String?,
    endDateIso: String?
) {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val startMillis = try {
        startDateIso?.let { sdf.parse(it)?.time } ?: System.currentTimeMillis()
    } catch (_: Exception) {
        System.currentTimeMillis()
    }
    val endMillis = try {
        endDateIso?.let { sdf.parse(it)?.time?.plus(86_400_000L) } ?: (startMillis + 86_400_000L)
    } catch (_: Exception) {
        startMillis + 86_400_000L
    }

    val calIntent = Intent(Intent.ACTION_INSERT).apply {
        data = CalendarContract.Events.CONTENT_URI
        putExtra(CalendarContract.Events.TITLE, title)
        putExtra(CalendarContract.Events.EVENT_LOCATION, location)
        putExtra(CalendarContract.Events.DESCRIPTION, description)
        putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
        putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
        putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, true)
    }
    try {
        context.startActivity(calIntent)
    } catch (_: Exception) {
        shareIcsCalendar(context, description, title)
    }
}
data class FamousDestination(
    val name: String,
    val country: String,
    val tagline: String,
    val description: String,
    val imageUrl: String
)

data class TravelStyleCategory(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val keyword: String
)

val travelStyleCategories = listOf(
    TravelStyleCategory("CULTURA", "Arte & Storia", Icons.Default.AccountBalance, "roma"),
    TravelStyleCategory("FOOD", "Enogastronomia", Icons.Default.WineBar, "degustazion"),
    TravelStyleCategory("MARE", "Mare & Coste", Icons.Default.BeachAccess, "amalfi"),
    TravelStyleCategory("NATURA", "Alpi & Laghi", Icons.Default.Terrain, "losanna"),
    TravelStyleCategory("METROPOLI", "Grandi Metropoli", Icons.Default.LocationCity, "tokyo")
)

val famousDestinations = listOf(
    FamousDestination(
        name = "Roma",
        country = "Italia",
        tagline = "La Città Eterna tra storia e sapori",
        description = "Capitale d'Italia e culla di una delle civiltà più antiche del mondo. Dal Colosseo ai vicoli di Trastevere, Roma offre un museo a cielo aperto, tour enogastronomici autentici ed escursioni storiche indimenticabili.",
        imageUrl = "https://images.unsplash.com/photo-1552832230-c0197dd311b5?auto=format&fit=crop&w=800&q=80"
    ),
    FamousDestination(
        name = "Losanna",
        country = "Svizzera",
        tagline = "Perla alpina affacciata sul Lago di Ginevra",
        description = "Adagiata sulle rive del Lago Lemano con vista spettacolare sulle Alpi, Losanna unisce il fascino medievale della Cité, i vigneti terrazzati di Lavaux (Patrimonio UNESCO) e lo spirito della Capitale Olimpica.",
        imageUrl = "https://images.unsplash.com/photo-1527668752968-14dc70a27c95?auto=format&fit=crop&w=800&q=80"
    ),
    FamousDestination(
        name = "Parigi",
        country = "Francia",
        tagline = "Arte, cultura e crociere sulla Senna",
        description = "La Ville Lumière incanta con i suoi boulevard alberati, i capolavori del Louvre e il fascino bohémien di Montmartre. Ideale per weekend culturali, tour architettonici ed esperienze gastronomiche d'eccellenza.",
        imageUrl = "https://images.unsplash.com/photo-1502602898657-3e91760cbb34?auto=format&fit=crop&w=800&q=80"
    ),
    FamousDestination(
        name = "Tokyo",
        country = "Giappone",
        tagline = "Dove i templi antichi incontrano il futuro",
        description = "Una metropoli straordinaria che fonde santuari millenari immersi nella natura, quartieri illuminati dai neon come Shinjuku e una cultura culinaria unica al mondo.",
        imageUrl = "https://images.unsplash.com/photo-1540959733332-eab4deabeeaf?auto=format&fit=crop&w=800&q=80"
    ),
    FamousDestination(
        name = "Amalfi",
        country = "Italia",
        tagline = "Scogliere a picco sul mare e borghi colorati",
        description = "La Costiera Amalfitana regala panorami mozzafiato, escursioni in barca verso Capri, trekking sul Sentiero degli Dei e profumi di limoni lungo una delle coste più amate al mondo.",
        imageUrl = "https://images.unsplash.com/photo-1533105079780-92b9be482077?auto=format&fit=crop&w=800&q=80"
    )
)

fun resolveTripImageUrl(trip: TripDTO?): String {
    val rawUrl = trip?.imageUrls?.firstOrNull()
    if (!rawUrl.isNullOrBlank() && rawUrl.startsWith("http")) {
        return rawUrl.replace("localhost", "10.0.2.2")
    }
    return ""
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    tokenUtente: String = "",
    onTripClick: (Long) -> Unit = {},
    onNavigateToProfile: () -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf("Home") }
    var viewingListId by remember { mutableStateOf<Long?>(null) }
    var selectedDestination by remember { mutableStateOf<FamousDestination?>(null) }

    // --- STATI DI RICERCA E MODALITÀ ESPLORAZIONE COMPLETA ---
    var showSearchSheet by remember { mutableStateOf(false) }
    var showNotificationsSheet by remember { mutableStateOf(false) }
    var showFullCatalogMode by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedOfferType by remember { mutableStateOf("TUTTI") } // "TUTTI", "VIAGGI", "ATTIVITA"
    var maxPriceFilter by remember { mutableStateOf<Double?>(null) }
    var minParticipants by remember { mutableStateOf(1) }
    var sortOption by remember { mutableStateOf("RATING") } // "RATING", "PRICE_ASC", "PRICE_DESC"

    val isSearchActive = showFullCatalogMode ||
            searchQuery.isNotBlank() ||
            selectedOfferType != "TUTTI" ||
            maxPriceFilter != null ||
            minParticipants > 1 ||
            sortOption != "RATING"

    var icsPreviewReservation by remember { mutableStateOf<ReservationDTO?>(null) }
    var selectedTravelerReceipt by remember { mutableStateOf<ReservationDTO?>(null) }
    val context = LocalContext.current

    LaunchedEffect(tokenUtente) {
        if (tokenUtente.isNotEmpty()) {
            viewModel.fetchTrips(tokenUtente)
            viewModel.fetchMyReservations(tokenUtente)
            while (true) {
                viewModel.fetchMyNotifications(tokenUtente)
                viewModel.fetchUnreadChatsCount(tokenUtente)
                delay(3000)
            }
        }
    }

    LaunchedEffect(selectedTab) {
        if (tokenUtente.isNotEmpty()) {
            if (selectedTab == "Viaggi") viewModel.fetchMyReservations(tokenUtente)
            if (selectedTab == "Home") viewModel.fetchTrips(tokenUtente)
            viewModel.fetchUnreadChatsCount(tokenUtente)
        }
    }

    // Mostriamo nel catalogo solo le esperienze con posti effettivamente disponibili (> 0)
    val bookableTrips = remember(viewModel.trips) {
        viewModel.trips.filter { it.availableSpots > 0 }
    }

    // Collezioni tematiche per la Home (Stile GetYourGuide / TripAdvisor / Airbnb)
    val topRatedTrips = remember(bookableTrips) {
        bookableTrips.filter { it.reviewCount > 0 }.sortedWith(compareByDescending<TripDTO> { it.averageRating} .thenByDescending { it.reviewCount }).take(5)
    }
    val dailyActivities = remember(bookableTrips) {
        bookableTrips.filter { isActivityOrExcursion(it) }
    }
    val multiDayTrips = remember(bookableTrips) {
        bookableTrips.filter { !isActivityOrExcursion(it) }
    }

    // Risultati filtrati quando l'utente usa la ricerca o esplora il catalogo
    val filteredTrips = remember(
        bookableTrips,
        searchQuery,
        selectedOfferType,
        maxPriceFilter,
        minParticipants,
        sortOption
    ) {
        val base = bookableTrips.filter { trip ->
            val q = searchQuery.trim()
            val matchesQuery = q.isEmpty() ||
                    trip.title.contains(q, ignoreCase = true) ||
                    (trip.location?.contains(q, ignoreCase = true) == true) ||
                    (trip.description?.contains(q, ignoreCase = true) == true)

            val isActivity = isActivityOrExcursion(trip)
            val matchesType = when (selectedOfferType) {
                "VIAGGI" -> !isActivity
                "ATTIVITA" -> isActivity
                else -> true
            }

            val matchesPrice = maxPriceFilter == null || trip.price <= maxPriceFilter!!
            val matchesSpots = trip.availableSpots >= minParticipants

            matchesQuery && matchesType && matchesSpots && matchesPrice
        }

        when (sortOption) {
            "PRICE_ASC" -> base.sortedBy { it.price }
            "PRICE_DESC" -> base.sortedByDescending { it.price }
            else -> base.sortedByDescending { it.averageRating }
        }
    }

    Scaffold(
        containerColor = TripWarmBg,
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 12.dp
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Esplora", fontWeight = FontWeight.SemiBold) },
                    selected = selectedTab == "Home",
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = TripGreen,
                        selectedTextColor = TripGreen,
                        indicatorColor = TripGreen.copy(alpha = 0.15f)
                    ),
                    onClick = {
                        selectedTab = "Home"
                        selectedDestination = null
                        viewingListId = null
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.FavoriteBorder, contentDescription = "Preferiti") },
                    label = { Text("Preferiti", fontWeight = FontWeight.SemiBold) },
                    selected = selectedTab == "Preferiti",
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = TripGreen,
                        selectedTextColor = TripGreen,
                        indicatorColor = TripGreen.copy(alpha = 0.15f)
                    ),
                    onClick = {
                        selectedTab = "Preferiti"
                        selectedDestination = null
                        viewingListId = null
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Outlined.DateRange, contentDescription = "Viaggi") },
                    label = { Text("Viaggi", fontWeight = FontWeight.SemiBold) },
                    selected = selectedTab == "Viaggi",
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = TripGreen,
                        selectedTextColor = TripGreen,
                        indicatorColor = TripGreen.copy(alpha = 0.15f)
                    ),
                    onClick = {
                        selectedTab = "Viaggi"
                        selectedDestination = null
                    }
                )
                NavigationBarItem(
                    icon = {
                        BadgedBox(
                            badge = {
                                if (viewModel.unreadChatsCount > 0) {
                                    Badge(containerColor = Color(0xFFE53935)) {
                                        Text("${viewModel.unreadChatsCount}", color = Color.White)
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Email, contentDescription = "Messaggi")
                        }
                    },
                    label = { Text("Messaggi", fontWeight = FontWeight.SemiBold) },
                    selected = selectedTab == "Messaggi",
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = TripGreen,
                        selectedTextColor = TripGreen,
                        indicatorColor = TripGreen.copy(alpha = 0.15f)
                    ),
                    onClick = {
                        selectedTab = "Messaggi"
                        selectedDestination = null
                        viewingListId = null
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profilo") },
                    label = { Text("Profilo", fontWeight = FontWeight.SemiBold) },
                    selected = selectedTab == "Profilo",
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = TripGreen,
                        selectedTextColor = TripGreen,
                        indicatorColor = TripGreen.copy(alpha = 0.15f)
                    ),
                    onClick = { onNavigateToProfile() }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            when (selectedTab) {
                "Home" -> {
                    if (selectedDestination != null) {
                        CityDetailScreen(
                            destination = selectedDestination!!,
                            allTrips = bookableTrips,
                            onBackClick = { selectedDestination = null },
                            onTripClick = { trip ->
                                viewModel.markTripAsViewed(trip)
                                onTripClick(trip.id)
                            }
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 36.dp)
                        ) {
                            // 1. HEADER BENVENUTO + CAMPANELLA NOTIFICHE
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(TripGreen)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "HORIZON TRAVEL",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = TripGreen,
                                                letterSpacing = 1.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Dove andiamo oggi?",
                                            fontSize = 26.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = TripBlack
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                            .clickable {
                                                viewModel.fetchMyNotifications(tokenUtente)
                                                showNotificationsSheet = true
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Notifications,
                                            contentDescription = "Notifiche",
                                            tint = TripBlack,
                                            modifier = Modifier.size(26.dp)
                                        )
                                        if (viewModel.myNotifications.isNotEmpty()) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(top = 10.dp, end = 12.dp)
                                                    .size(9.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFFE53935))
                                            )
                                        }
                                    }
                                }
                            }

                            // 2. BARRA DI RICERCA A PILLOLA INTERATTIVA
                            item {
                                Surface(
                                    shape = RoundedCornerShape(100.dp),
                                    color = Color.White,
                                    shadowElevation = 6.dp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 24.dp, vertical = 8.dp)
                                        .clickable { showSearchSheet = true }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Outlined.Search,
                                            contentDescription = "Cerca",
                                            tint = TripGreen,
                                            modifier = Modifier.size(28.dp)
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (searchQuery.isBlank()) "Cerca città, viaggio o escursione"
                                                else "\"$searchQuery\"",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = Color.Gray,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
//                                            val subSummary = buildList {
//                                                if (selectedOfferType != "TUTTI") add(if (selectedOfferType == "VIAGGI") "Solo Viaggi" else "Solo Attività")
//                                                if (maxPriceFilter != null) add("Max €${maxPriceFilter!!.toInt()}")
//                                                if (minParticipants > 1) add("$minParticipants+ posti")
//                                            }.joinToString(" • ").ifEmpty { "Ovunque • Qualsiasi data • Prezzi dinamici" }
//
//                                            Text(
//                                                text = subSummary,
//                                                fontSize = 13.sp,
//                                                color = if (isSearchActive) TripGreen else Color.Gray,
//                                                fontWeight = if (isSearchActive) FontWeight.SemiBold else FontWeight.Normal
//                                            )
                                        }
                                        if (isSearchActive) {
                                            IconButton(onClick = {
                                                searchQuery = ""
                                                maxPriceFilter = null
                                                selectedOfferType = "TUTTI"
                                                minParticipants = 1
                                                sortOption = "RATING"
                                                showFullCatalogMode = false
                                            }) {
                                                Icon(Icons.Default.Close, contentDescription = "Reset", tint = Color.Gray)
                                            }
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(38.dp)
                                                    .clip(CircleShape)
                                                    .background(TripWarmBg),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.Search, contentDescription = null, tint = TripBlack, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                            }

                            // 3. CATEGORIE ISPIRAZIONALI (STILE AIRBNB / GETYOURGUIDE)
                            item {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 24.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    item {
                                        OfferTypeChip("Scopri", !isSearchActive, Icons.Default.Explore) {
                                            searchQuery = ""
                                            selectedOfferType = "TUTTI"
                                            showFullCatalogMode = false
                                        }
                                    }
                                    item {
                                        OfferTypeChip("Viaggi", selectedOfferType == "VIAGGI" && searchQuery.isEmpty(), Icons.Default.FlightTakeoff) {
                                            searchQuery = ""
                                            selectedOfferType = "VIAGGI"
                                        }
                                    }
                                    item {
                                        OfferTypeChip("Attività in Giornata", selectedOfferType == "ATTIVITA" && searchQuery.isEmpty(), Icons.Default.ConfirmationNumber) {
                                            searchQuery = ""
                                            selectedOfferType = "ATTIVITA"
                                        }
                                    }
                                    items(travelStyleCategories) { styleCat ->
                                        val isSelected = searchQuery.equals(styleCat.keyword, ignoreCase = true)
                                        OfferTypeChip(styleCat.label, isSelected, styleCat.icon) {
                                            selectedOfferType = "TUTTI"
                                            searchQuery = if (isSelected) "" else styleCat.keyword
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(22.dp))
                            }

                            if (viewModel.isLoading) {
                                item {
                                    Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(color = TripGreen)
                                    }
                                }
                            } else if (viewModel.errorMessage != null) {
                                item {
                                    Text(
                                        text = viewModel.errorMessage!!,
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.padding(horizontal = 24.dp)
                                    )
                                }
                            } else if (!isSearchActive) {
                                // =========================================================================
                                // MODALITÀ HOME DI SCOPERTA CURATA (NON ELENCA TUTTO IL DB IN VERTICALE!)
                                // =========================================================================

                                // SEZIONE A: METE ICONICHE
                                item {
                                    SectionHeader(
                                        title = "Destinazioni Iconiche",
                                        subtitle = "Esplora le guide città con viaggi organizzati e tour giornalieri"
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))

                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 24.dp),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        items(famousDestinations) { destination ->
                                            FamousDestinationCard(
                                                destination = destination,
                                                onClick = { selectedDestination = destination }
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(28.dp))
                                }

                                // SEZIONE B: SCELTE DAI VIAGGIATORI (TRAVELLERS' CHOICE CAROUSEL)
                                if (topRatedTrips.isNotEmpty()) {
                                    item {
                                        SectionHeader(
                                            title = " Scelte dai Viaggiatori 2026",
                                            subtitle = "Le esperienze con le recensioni più alte della community Horizon"
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))

                                        LazyRow(
                                            contentPadding = PaddingValues(horizontal = 24.dp),
                                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                            items(topRatedTrips) { trip ->
                                                SpotlightExperienceCard(
                                                    trip = trip,
                                                    badgeText = " TOP RATED • ${trip.averageRating}",
                                                    onClick = {
                                                        viewModel.markTripAsViewed(trip)
                                                        onTripClick(trip.id)
                                                    }
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(28.dp))
                                    }
                                }

                                // SEZIONE C: VISTI DI RECENTE
                                val availableRecent = viewModel.recentlyViewedTrips.filter { it.availableSpots > 0 }
                                if (availableRecent.isNotEmpty()) {
                                    item {
                                        SectionHeader(
                                            title = "Visti di recente",
                                            subtitle = "Riprendi la pianificazione da dove avevi lasciato"
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))

                                        LazyRow(
                                            contentPadding = PaddingValues(horizontal = 24.dp),
                                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                                        ) {
                                            items(availableRecent) { trip ->
                                                RecentlyViewedTripCard(
                                                    trip = trip,
                                                    onClick = {
                                                        viewModel.markTripAsViewed(trip)
                                                        onTripClick(trip.id)
                                                    }
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(28.dp))
                                    }
                                }

                                // SEZIONE D: ATTIVITÀ & ESCURSIONI IN GIORNATA (CAROSELLO DEDICATO)
                                if (dailyActivities.isNotEmpty()) {
                                    item {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 24.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = " Attività & Tour in Giornata",
                                                    fontSize = 21.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = TripBlack
                                                )
                                                Text(
                                                    text = "Prezzi dinamici giornalieri e prenotazione immediata",
                                                    fontSize = 13.sp,
                                                    color = Color.Gray
                                                )
                                            }
                                            TextButton(onClick = { selectedOfferType = "ATTIVITA" }) {
                                                Text("Vedi tutte →", color = TripOrangeActivity, fontWeight = FontWeight.ExtraBold)
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))

                                        LazyRow(
                                            contentPadding = PaddingValues(horizontal = 24.dp),
                                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                            items(dailyActivities) { activity ->
                                                SpotlightExperienceCard(
                                                    trip = activity,
                                                    badgeText = " IN GIORNATA",
                                                    onClick = {
                                                        viewModel.markTripAsViewed(activity)
                                                        onTripClick(activity.id)
                                                    }
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(28.dp))
                                    }
                                }

                                // SEZIONE F: SELEZIONE DI 3 VIAGGI MULTI-GIORNO + PULSANTE CATALOGO COMPLETO
                                if (multiDayTrips.isNotEmpty()) {
                                    item {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 24.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = " Grandi Viaggi Organizzati",
                                                    fontSize = 21.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = TripBlack
                                                )
                                                Text(
                                                    text = "I pacchetti multi-giorno più amati in partenza",
                                                    fontSize = 13.sp,
                                                    color = Color.Gray
                                                )
                                            }
                                            TextButton(onClick = { selectedOfferType = "VIAGGI" }) {
                                                Text("Tutti (${multiDayTrips.size}) →", color = TripGreen, fontWeight = FontWeight.ExtraBold)
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }

                                    // Mostriamo solo i primi 3 viaggi per non appesantire la Home
                                    items(multiDayTrips.take(3)) { trip ->
                                        TripAdvisorFullCard(
                                            trip = trip,
                                            onClick = {
                                                viewModel.markTripAsViewed(trip)
                                                onTripClick(trip.id)
                                            }
                                        )
                                    }

                                    item {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        OutlinedButton(
                                            onClick = { showFullCatalogMode = true },
                                            shape = RoundedCornerShape(16.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 24.dp)
                                                .height(52.dp)
                                        ) {
                                            Icon(Icons.Outlined.Search, contentDescription = null, tint = TripGreen)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Esplora l'intero catalogo (${bookableTrips.size} offerte disponibili)",
                                                fontWeight = FontWeight.ExtraBold,
                                                color = TripBlack
                                            )
                                        }
                                    }
                                }
                            } else {
                                // =========================================================================
                                // MODALITÀ RICERCA / FILTRO ATTIVO (MOSTRA RISULTATI PERTINENTI)
                                // =========================================================================
                                item {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 24.dp, vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${filteredTrips.size} esperienze disponibili",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = TripBlack
                                        )
                                        TextButton(onClick = {
                                            searchQuery = ""
                                            maxPriceFilter = null
                                            selectedOfferType = "TUTTI"
                                            minParticipants = 1
                                            sortOption = "RATING"
                                            showFullCatalogMode = false
                                        }) {
                                            Text("Torna a Scopri", color = TripGreen, fontWeight = FontWeight.ExtraBold)
                                        }
                                    }
                                }

                                if (filteredTrips.isEmpty()) {
                                    item {
                                        Surface(
                                            shape = RoundedCornerShape(18.dp),
                                            color = Color.White,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 24.dp, vertical = 16.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(28.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Icon(Icons.Outlined.Search, contentDescription = null, tint = TripGreen, modifier = Modifier.size(48.dp))
                                                Spacer(modifier = Modifier.height(12.dp))
                                                Text(
                                                    text = "Nessuna offerta disponibile con questi filtri",
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 17.sp,
                                                    color = TripBlack
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "Prova ad ampliare il budget o rimuovere qualche filtro di ricerca.",
                                                    fontSize = 13.sp,
                                                    color = Color.Gray
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    items(filteredTrips) { trip ->
                                        TripAdvisorFullCard(
                                            trip = trip,
                                            onClick = {
                                                viewModel.markTripAsViewed(trip)
                                                onTripClick(trip.id)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                "Preferiti" -> {
                    if (viewingListId == null) {
                        FavoriteListsScreen(
                            tokenUtente = tokenUtente,
                            onListClick = { listId -> viewingListId = listId }
                        )
                    } else {
                        ListDetailScreen(
                            listId = viewingListId!!,
                            tokenUtente = tokenUtente,
                            onNavigateBack = { viewingListId = null },
                            onTripClick = onTripClick
                        )
                    }
                }

                "Viaggi" -> {
                    MyReservationsSection(
                        reservations = viewModel.myReservations,
                        allTrips = viewModel.trips,
                        isLoading = viewModel.isLoadingReservations,
                        errorMessage = viewModel.reservationsError,
                        onRefresh = { viewModel.fetchMyReservations(tokenUtente) },
                        onTripClick = onTripClick,
                        onOpenReceiptClick = { reservation -> selectedTravelerReceipt = reservation },
                        onExportIcsClick = { reservation -> icsPreviewReservation = reservation }
                    )
                }

                "Messaggi" -> {
                    LiveChatTabScreen(
                        tokenUtente = tokenUtente,
                        ruoloUtente = "VIAGGIATORE",
                        initialTrips = viewModel.trips,
                        onUnreadChatsCountChanged = { count -> viewModel.unreadChatsCount = count }
                    )
                }
            }
        }
    }

    // --- DIALOG ESPORTAZIONE CALENDARIO (.ICS + APP CALENDARIO DI SISTEMA) ---
    if (icsPreviewReservation != null) {
        val res = icsPreviewReservation!!
        val matchedTrip = viewModel.trips.find {
            it.id == res.tripId || it.title.equals(res.tripTitle, ignoreCase = true)
        }
        val receiptInfo = resolveReceiptDetails(
            context = context,
            order = res,
            matchedTrip = matchedTrip,
            notificationsMessages = viewModel.myNotifications.map { it.message }
        )
        val icsContent = buildIcsContent(res, matchedTrip, receiptInfo)

        AlertDialog(
            onDismissRequest = { icsPreviewReservation = null },
            containerColor = Color.White,
            title = {
                Text("Esporta in Calendario", fontWeight = FontWeight.ExtraBold, color = TripBlack)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Puoi aggiungere direttamente l'evento nell'app Calendario del telefono oppure condividere il tracciato standard iCalendar (.ics):",
                        fontSize = 13.sp,
                        color = Color.DarkGray
                    )
                    Surface(
                        color = TripWarmBg,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = icsContent,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TripBlack,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            openSystemCalendar(
                                context = context,
                                title = res.tripTitle,
                                location = matchedTrip?.location ?: "Destinazione Horizon",
                                description = "Prenotazione #${receiptInfo.receiptCode} • Partecipanti: ${receiptInfo.participantsList} • Turno: ${receiptInfo.selectedDateSlot}",
                                startDateIso = matchedTrip?.startDate,
                                endDateIso = matchedTrip?.endDate
                            )
                            icsPreviewReservation = null
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.5.dp, TripGreen),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.DateRange, contentDescription = null, tint = TripGreen, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Apri in App Calendario", color = TripDarkGreen, fontWeight = FontWeight.ExtraBold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        shareIcsCalendar(context, icsContent, res.tripTitle)
                        icsPreviewReservation = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TripGreen)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Condividi .ics")
                }
            },
            dismissButton = {
                TextButton(onClick = { icsPreviewReservation = null }) {
                    Text("Chiudi")
                }
            }
        )
    }

    if (selectedTravelerReceipt != null) {
        val res = selectedTravelerReceipt!!
        val matchedTrip = viewModel.trips.find {
            it.id == res.tripId || it.title.equals(res.tripTitle, ignoreCase = true)
        }
        val receiptInfo = resolveReceiptDetails(
            context = context,
            order = res,
            matchedTrip = matchedTrip,
            notificationsMessages = viewModel.myNotifications.map { it.message }
        )
        val participantsArray = remember(receiptInfo.participantsList) {
            receiptInfo.participantsList.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        }

        ModalBottomSheet(
            onDismissRequest = { selectedTravelerReceipt = null },
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
                    Column(modifier = Modifier.weight(1f)) {
                        Surface(
                            color = TripGreen.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "✓ PRENOTAZIONE CONFERMATA • ${res.status}",
                                color = TripDarkGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "La tua Ricevuta di Viaggio",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TripBlack
                        )
                        Text(
                            text = "Codice Prenotazione: #${receiptInfo.receiptCode}",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color.Gray
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(TripGreen.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = TripGreen, modifier = Modifier.size(26.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = TripWarmBg,
                    border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "DATI PRENOTAZIONE E PARTECIPANTI",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TripGreen,
                            letterSpacing = 0.8.sp
                        )
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Partecipanti registrati (${participantsArray.size}):",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                            participantsArray.forEachIndexed { index, personName ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color.White,
                                    border = BorderStroke(1.dp, TripGreen.copy(alpha = 0.25f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(22.dp)
                                                .clip(CircleShape)
                                                .background(TripGreen.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "${index + 1}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = TripDarkGreen
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = personName,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TripBlack,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))

                        Text(
                            text = "DETTAGLI ESPERIENZA",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TripGreen,
                            letterSpacing = 0.8.sp
                        )
                        Text(res.tripTitle, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = TripBlack)
                        Text("Data / Turno: ${receiptInfo.selectedDateSlot}", fontSize = 13.sp, color = Color.DarkGray)
                        Text("Metodo di pagamento: ${receiptInfo.paymentMethod}", fontSize = 13.sp, color = Color.DarkGray)

                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("TOTALE PAGATO", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = TripBlack)
                            Text(
                                text = "€${String.format(java.util.Locale.ITALY, "%.2f", res.totalPaid)}",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 22.sp,
                                color = TripDarkGreen
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            selectedTravelerReceipt = null
                            icsPreviewReservation = res
                        },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.5.dp, TripGreen),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    ) {
                        Icon(Icons.Default.DateRange, contentDescription = null, tint = TripGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("File .ics", color = TripDarkGreen, fontWeight = FontWeight.ExtraBold)
                    }

                    Button(
                        onClick = {
                            val targetId = matchedTrip?.id ?: res.tripId
                            selectedTravelerReceipt = null
                            onTripClick(targetId)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TripGreen),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    ) {
                        Text("Vedi Dettagli →", color = Color.White, fontWeight = FontWeight.ExtraBold)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // --- BOTTOM SHEET: NOTIFICHE SEMPLIFICATE ---
    if (showNotificationsSheet) {
        val visibleNotifications = remember(viewModel.myNotifications) {
            viewModel.myNotifications.filterNot { it.title.startsWith("Dettagli Ricevuta:") }
        }
        ModalBottomSheet(
            onDismissRequest = { showNotificationsSheet = false },
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Text("Notifiche", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = TripBlack)
                Spacer(modifier = Modifier.height(14.dp))

                if (visibleNotifications.isEmpty()) {
                    Text("Non hai nuove notifiche.", color = Color.Gray, modifier = Modifier.padding(vertical = 16.dp))
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                        items(visibleNotifications) { notifica ->
                            val timeText = if (notifica.timestamp.length >= 10) notifica.timestamp.take(10) else "Oggi"
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showNotificationsSheet = false
                                        when {
                                            notifica.title.contains("Chat", true) || notifica.title.contains("risposta", true) -> selectedTab = "Messaggi"
                                            notifica.title.contains("Prenotazione", true) -> selectedTab = "Viaggi"
                                            notifica.title.contains("lista", true) -> selectedTab = "Preferiti"
                                        }
                                    }
                            ) {
                                NotificationItem(
                                    title = notifica.title,
                                    message = notifica.message,
                                    time = timeText
                                )
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.LightGray.copy(alpha = 0.4f))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }

    // --- BOTTOM SHEET: RICERCA AVANZATA ---
    if (showSearchSheet) {
        var tempQuery by remember { mutableStateOf(searchQuery) }
        var tempType by remember { mutableStateOf(selectedOfferType) }
        var sliderPrice by remember { mutableStateOf(maxPriceFilter?.toFloat() ?: 3000f) }
        var tempParticipants by remember { mutableStateOf(minParticipants) }
        var tempSort by remember { mutableStateOf(sortOption) }

        ModalBottomSheet(
            onDismissRequest = { showSearchSheet = false },
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Cerca e Filtra", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = TripBlack)
                    TextButton(onClick = {
                        tempQuery = ""
                        tempType = "TUTTI"
                        sliderPrice = 3000f
                        tempParticipants = 1
                        tempSort = "RATING"
                    }) {
                        Text("Ripristina", color = Color.Gray, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = tempQuery,
                    onValueChange = { tempQuery = it },
                    label = { Text("Destinazione o esperienza") },
                    leadingIcon = { Icon(Icons.Default.Place, contentDescription = null, tint = TripGreen) },
                    trailingIcon = {
                        if (tempQuery.isNotEmpty()) {
                            IconButton(onClick = { tempQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Cancella")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(18.dp))
                Text("Tipologia di esperienza", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TripBlack)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OfferTypeChip("Tutti", tempType == "TUTTI") { tempType = "TUTTI" }
                    OfferTypeChip(" Viaggi", tempType == "VIAGGI") { tempType = "VIAGGI" }
                    OfferTypeChip("️ Attività", tempType == "ATTIVITA") { tempType = "ATTIVITA" }
                }


                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Partecipanti", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TripBlack)
                        Text("Mostra solo con posti disponibili", fontSize = 12.sp, color = Color.Gray)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(TripWarmBg)
                            .padding(horizontal = 4.dp)
                    ) {
                        IconButton(onClick = { if (tempParticipants > 1) tempParticipants-- }) {
                            Text("-", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = TripBlack)
                        }
                        Text(
                            text = "$tempParticipants",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = TripBlack,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        IconButton(onClick = { tempParticipants++ }) {
                            Text("+", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = TripGreen)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
                Text("Ordina risultati per", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TripBlack)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OfferTypeChip(" Più votati", tempSort == "RATING") { tempSort = "RATING" }
                    OfferTypeChip("€ Crescente", tempSort == "PRICE_ASC") { tempSort = "PRICE_ASC" }
                    OfferTypeChip("€ Decrescente", tempSort == "PRICE_DESC") { tempSort = "PRICE_DESC" }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        searchQuery = tempQuery
                        selectedOfferType = tempType
                        maxPriceFilter = if (sliderPrice >= 3000f) null else sliderPrice.toDouble()
                        minParticipants = tempParticipants
                        sortOption = tempSort
                        showFullCatalogMode = true
                        showSearchSheet = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TripGreen),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Mostra risultati", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun SpotlightExperienceCard(
    trip: TripDTO,
    badgeText: String,
    onClick: () -> Unit
) {
    val isActivity = isActivityOrExcursion(trip)
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 4.dp,
        modifier = Modifier
            .width(255.dp)
            .clickable { onClick() }
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(148.dp)
                    .background(Color.LightGray)
            ) {
                AsyncImage(
                    model = resolveTripImageUrl(trip),
                    contentDescription = trip.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isActivity) TripOrangeActivity else TripGreen,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                ) {
                    Text(
                        text = badgeText,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = TripGreen, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = trip.location ?: "Destinazione",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TripGreen
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = trip.title,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = TripBlack,
                    maxLines = 2,
                    minLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        repeat(trip.averageRating.roundToInt().coerceIn(1, 5)) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = TripAmber, modifier = Modifier.size(13.dp))
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (trip.reviewCount > 0) "${String.format(java.util.Locale.US, "%.1f", trip.averageRating)} (${trip.reviewCount})" else "Nuovo (0)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TripBlack
                        )
                    }
                    Text(
                        text = "da €${trip.price}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = TripDarkGreen
                    )
                }
            }
        }
    }
}

// =========================================================================
// SEZIONE "VIAGGI": LISTA PRENOTAZIONI EFFETTUATE DAL VIAGGIATORE
// =========================================================================
@Composable
fun MyReservationsSection(
    reservations: List<ReservationDTO>,
    allTrips: List<TripDTO>,
    isLoading: Boolean,
    errorMessage: String?,
    onRefresh: () -> Unit,
    onTripClick: (Long) -> Unit,
    onOpenReceiptClick: (ReservationDTO) -> Unit = {},
    onExportIcsClick: (ReservationDTO) -> Unit
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(TripWarmBg),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "I Miei Viaggi e Attività",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TripBlack
                    )
                    Text(
                        text = "Riepilogo prenotazioni ed esportazione calendario (.ics)",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }
                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Aggiorna", tint = TripGreen)
                }
            }
        }

        if (isLoading) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = TripGreen)
                }
            }
        } else if (errorMessage != null) {
            item {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        } else if (reservations.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DateRange,
                            contentDescription = null,
                            tint = TripGreen,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Nessuna prenotazione attiva",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TripBlack
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Esplora la Home per prenotare il tuo prossimo viaggio o un'escursione giornaliera.",
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        } else {
            items(reservations) { reservation ->
                val matchedTrip = allTrips.find {
                    it.id == reservation.tripId || it.title.equals(reservation.tripTitle, ignoreCase = true)
                }
                val isActivity = matchedTrip?.let { isActivityOrExcursion(it) } ?: false
                val receiptInfo = resolveReceiptDetails(context, reservation, matchedTrip, emptyList())

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    shadowElevation = 3.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                        .clickable { onOpenReceiptClick(reservation) }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(84.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color.LightGray)
                            ) {
                                AsyncImage(
                                    model = resolveTripImageUrl(matchedTrip),
                                    contentDescription = reservation.tripTitle,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = if (isActivity) TripOrangeActivity.copy(alpha = 0.12f) else TripGreen.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = if (isActivity) "ATTIVITÀ" else "VIAGGIO",
                                            color = if (isActivity) TripOrangeActivity else TripGreen,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }

                                    Surface(
                                        color = Color(0xFF10B981).copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "✓ PRENOTATO",
                                            color = TripDarkGreen,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = reservation.tripTitle,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TripBlack,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (matchedTrip != null) "Date: ${matchedTrip.startDate} → ${matchedTrip.endDate}"
                                    else "Prenotato il: ${reservation.reservationDate.take(10)}",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )

                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${reservation.bookedSpots} pers.: ${receiptInfo.participantsList}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.DarkGray,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "Pagato: €${reservation.totalPaid}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = TripDarkGreen
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.4f))
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { onExportIcsClick(reservation) }) {
                                Icon(Icons.Default.DateRange, contentDescription = null, tint = TripGreen, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Esporta Calendario (.ics)", color = TripGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }

                            TextButton(onClick = { onOpenReceiptClick(reservation) }) {
                                Text("Apri Ricevuta →", color = TripBlack, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OfferTypeChip(
    label: String,
    selected: Boolean,
    icon: ImageVector? = null,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (selected) TripGreen else Color.White,
        shadowElevation = if (selected) 4.dp else 1.dp,
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) Color.White else TripGreen,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = label,
                color = if (selected) Color.White else TripBlack,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
fun SectionHeader(title: String, subtitle: String) {
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Text(
            text = title,
            fontSize = 21.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TripBlack
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = subtitle,
            fontSize = 13.sp,
            color = Color.Gray
        )
    }
}

@Composable
fun RecentlyViewedTripCard(
    trip: TripDTO,
    onClick: () -> Unit
) {
    val isActivity = isActivityOrExcursion(trip)
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 3.dp,
        modifier = Modifier
            .width(180.dp)
            .clickable { onClick() }
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(115.dp)
                    .background(Color.LightGray)
            ) {
                AsyncImage(
                    model = resolveTripImageUrl(trip),
                    contentDescription = trip.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isActivity) TripOrangeActivity else TripGreen,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Text(
                        text = if (isActivity) "ATTIVITÀ" else "VIAGGIO",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = trip.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = TripBlack,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = TripAmber, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (trip.reviewCount > 0) "${String.format(java.util.Locale.US, "%.1f", trip.averageRating)} (${trip.reviewCount})" else "Nuovo (0)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.DarkGray
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "da €${trip.price}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    color = TripDarkGreen
                )
            }
        }
    }
}

@Composable
fun FamousDestinationCard(
    destination: FamousDestination,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 5.dp,
        modifier = Modifier
            .width(190.dp)
            .height(235.dp)
            .clickable { onClick() }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = destination.imageUrl,
                contentDescription = destination.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                            startY = 220f
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                Surface(
                    color = TripGreen,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = destination.country.uppercase(),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = destination.name,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = destination.tagline,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun TripAdvisorFullCard(
    trip: TripDTO,
    onClick: () -> Unit
) {
    val isActivity = isActivityOrExcursion(trip)

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        shadowElevation = 3.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(112.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.LightGray)
            ) {
                AsyncImage(
                    model = resolveTripImageUrl(trip),
                    contentDescription = trip.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Surface(
                    color = if (isActivity) TripOrangeActivity else TripGreen,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                ) {
                    Text(
                        text = if (isActivity) "ATTIVITÀ" else "VIAGGIO",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = TripGreen, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = trip.location ?: "Destinazione",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TripGreen
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = trip.title,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = TripBlack,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (trip.reviewCount > 0) {
                        repeat(trip.averageRating.roundToInt().coerceIn(1, 5)) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = TripAmber, modifier = Modifier.size(14.dp))
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = String.format(java.util.Locale.US, "%.1f", trip.averageRating),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = TripBlack
                        )
                        Text(
                            text = " (${trip.reviewCount} recensioni)",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    } else {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Nuovo (0 recensioni)",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "da €${trip.price} / persona",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = TripDarkGreen
                )
            }
        }
    }
}

@Composable
fun CityDetailScreen(
    destination: FamousDestination,
    allTrips: List<TripDTO>,
    onBackClick: () -> Unit,
    onTripClick: (TripDTO) -> Unit
) {
    var cityFilter by remember { mutableStateOf("TUTTI") }

    val cityTrips = remember(destination, allTrips, cityFilter) {
        val base = allTrips.filter {
            it.availableSpots > 0 && (
                    (it.location?.contains(destination.name, ignoreCase = true) == true) ||
                            it.title.contains(destination.name, ignoreCase = true)
                    )
        }
        when (cityFilter) {
            "VIAGGI" -> base.filter { !isActivityOrExcursion(it) }
            "ATTIVITA" -> base.filter { isActivityOrExcursion(it) }
            else -> base
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(TripWarmBg),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(270.dp)
            ) {
                AsyncImage(
                    model = destination.imageUrl,
                    contentDescription = destination.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.3f), Color.Black.copy(alpha = 0.75f))
                            )
                        )
                )

                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.TopStart)
                        .clip(CircleShape)
                        .background(Color.White)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Indietro", tint = TripBlack)
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(24.dp)
                ) {
                    Surface(color = TripGreen, shape = RoundedCornerShape(6.dp)) {
                        Text(
                            text = "GUIDA DESTINAZIONE • ${destination.country.uppercase()}",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = destination.name,
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = destination.tagline,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 14.sp
                    )
                }
            }
        }

        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color.White,
                shadowElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = TripGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Scopri ${destination.name}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TripBlack
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = destination.description,
                        fontSize = 14.sp,
                        color = Color.DarkGray,
                        lineHeight = 21.sp
                    )
                }
            }
        }

        item {
            SectionHeader(
                title = "Viaggi e Attività a ${destination.name}",
                subtitle = "Filtra tra pacchetti viaggio ed escursioni giornaliere"
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OfferTypeChip("Tutti", cityFilter == "TUTTI") { cityFilter = "TUTTI" }
                OfferTypeChip(" Viaggi", cityFilter == "VIAGGI") { cityFilter = "VIAGGI" }
                OfferTypeChip(" Attività", cityFilter == "ATTIVITA") { cityFilter = "ATTIVITA" }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (cityTrips.isNotEmpty()) {
            items(cityTrips) { trip ->
                TripAdvisorFullCard(trip = trip, onClick = { onTripClick(trip) })
            }
        } else {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Nessuna offerta specifica trovata per questo filtro a ${destination.name}.",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = TripBlack
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Ti mostriamo altre esperienze disponibili sul catalogo:",
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
            items(allTrips) { trip ->
                TripAdvisorFullCard(trip = trip, onClick = { onTripClick(trip) })
            }
        }
    }
}

@Composable
fun NotificationItem(title: String, message: String, time: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(TripGreen.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Notifications, contentDescription = null, tint = TripGreen, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = TripBlack,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(time, fontSize = 11.sp, color = Color.Gray)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(message, fontSize = 13.sp, color = Color.DarkGray, lineHeight = 18.sp)
        }
    }
}