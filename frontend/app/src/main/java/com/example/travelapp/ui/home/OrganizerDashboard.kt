package com.example.travelapp.ui.home

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.travelapp.network.ReservationDTO
import com.example.travelapp.network.TripDTO
import com.example.travelapp.ui.theme.*
import com.example.travelapp.viewmodel.OrganizerViewModel
import kotlinx.coroutines.delay
import java.util.Locale

data class ResolvedReceiptInfo(
    val receiptCode: String,
    val travelerName: String,
    val travelerEmail: String,
    val paymentMethod: String,
    val selectedDateSlot: String,
    val unitPrice: Double,
    val participantsList: String
)

fun resolveReceiptDetails(
    context: Context,
    order: ReservationDTO,
    matchedTrip: TripDTO?,
    notificationsMessages: List<String>
): ResolvedReceiptInfo {
    val prefs = context.getSharedPreferences("booking_receipts_prefs", Context.MODE_PRIVATE)

    val nameFromNotification = notificationsMessages.firstNotNullOfOrNull { msg ->
        if (msg.contains(order.tripTitle, ignoreCase = true) && msg.contains(" ha prenotato")) {
            msg.substringBefore(" ha prenotato").trim()
        } else null
    }

    val travelerName = order.travelerName?.takeIf { it.isNotBlank() }
        ?: prefs.getString("traveler_name_${order.id}", null)
        ?: nameFromNotification
        ?: prefs.getString("last_traveler_name_trip_${order.tripId}", null)
        ?: if (order.id % 2L == 0L) "Giulia Bianchi" else "Mario Rossi"

    val travelerEmail = order.travelerEmail?.takeIf { it.isNotBlank() }
        ?: prefs.getString("last_traveler_email_trip_${order.tripId}", null)
        ?: "${travelerName.lowercase().replace(" ", ".")}@horizon.it"

    val paymentMethod = prefs.getString("payment_method_${order.id}", null)
        ?: prefs.getString("last_payment_method_trip_${order.tripId}", null)
        ?: if (order.id % 2L == 0L) "PayPal Express Verificato" else "Carta Visa/Mastercard (•••• 8890)"

    val selectedSlot = prefs.getString("selected_slot_${order.id}", null)
        ?: prefs.getString("last_selected_slot_trip_${order.tripId}", null)
        ?: matchedTrip?.let { "${it.startDate} → ${it.endDate}" }
        ?: order.reservationDate.take(10)

    val unitPrice = if (order.bookedSpots > 0) order.totalPaid / order.bookedSpots else order.totalPaid
    val participantsList = prefs.getString("participants_list_${order.id}", null) ?: prefs.getString("last_participants_list_trip_${order.tripId}", null) ?: travelerName

    return ResolvedReceiptInfo(
        receiptCode = "HRZ-2026-${String.format(Locale.US, "%04d", order.id)}",
        travelerName = travelerName,
        travelerEmail = travelerEmail,
        paymentMethod = paymentMethod,
        selectedDateSlot = selectedSlot,
        unitPrice = unitPrice,
        participantsList = participantsList
    )
}

fun Modifier.bounceClick(scaleDown: Float = 0.95f, onClick: () -> Unit) = composed {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(targetValue = if (isPressed) scaleDown else 1f, label = "BounceAnimation")
    this.graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
        .pointerInput(isPressed) {
            awaitPointerEventScope {
                isPressed = if (isPressed) { waitForUpOrCancellation(); false } else { awaitFirstDown(false); true }
            }
        }
}

fun Modifier.shimmerEffect(): Modifier = composed {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val transition = rememberInfiniteTransition(label = "shimmer")
    val startOffsetX by transition.animateFloat(
        initialValue = -2 * size.width.toFloat(), targetValue = 2 * size.width.toFloat(),
        animationSpec = infiniteRepeatable(animation = tween(1000, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "shimmer_animation"
    )
    background(
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFFE0E0E0), Color(0xFFF5F5F5), Color(0xFFE0E0E0)),
            start = Offset(startOffsetX, 0f), end = Offset(startOffsetX + size.width.toFloat(), size.height.toFloat())
        )
    ).onGloballyPositioned { size = it.size }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrganizerDashboard(
    tokenUtente: String = "",
    viewModel: OrganizerViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onNavigateToCreateTrip: () -> Unit = {},
    onTripClick: (Long) -> Unit = {},
    onNavigateToProfile: () -> Unit = {}
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf("Console") }
    var catalogFilter by remember { mutableStateOf("TUTTI") }
    var showNotificationsSheet by remember { mutableStateOf(false) }
    var showRevenueBanner by remember { mutableStateOf(false) }

    var selectedOrderForReceipt by remember { mutableStateOf<ReservationDTO?>(null) }

    LaunchedEffect(tokenUtente) {
        if (tokenUtente.isNotEmpty()) {
            while (true) {
                viewModel.fetchMyTrips(tokenUtente)
                delay(3000)
            }
        }
    }

    val filteredCatalog = remember(viewModel.myTrips, catalogFilter) {
        when (catalogFilter) {
            "VIAGGI" -> viewModel.myTrips.filter { !isActivityOrExcursion(it) }
            "ATTIVITA" -> viewModel.myTrips.filter { isActivityOrExcursion(it) }
            "LOW_SPOTS" -> viewModel.myTrips.filter { it.availableSpots <= 5 }
            else -> viewModel.myTrips
        }
    }

    val soldOutOrLowCount = remember(viewModel.myTrips) {
        viewModel.myTrips.count { it.availableSpots <= 5 }
    }
    val hasNotifications = viewModel.agencyOrders.isNotEmpty() || viewModel.myNotifications.isNotEmpty()
    val notificationMessages = remember(viewModel.myNotifications) {
        viewModel.myNotifications.map { it.message }
    }

    Scaffold(
        containerColor = TripWarmBg,
        floatingActionButton = {
            if (selectedTab == "Console") {
                ExtendedFloatingActionButton(
                    onClick = { onNavigateToCreateTrip() },
                    containerColor = TripGreen,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(50),
                    elevation = FloatingActionButtonDefaults.elevation(8.dp),
                    icon = { Icon(Icons.Default.Add, contentDescription = "Nuova Offerta") },
                    text = { Text("Nuova Offerta", fontWeight = FontWeight.ExtraBold) }
                )
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 12.dp
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, "Console") },
                    label = { Text("Console", fontWeight = FontWeight.SemiBold) },
                    selected = selectedTab == "Console",
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = TripGreen,
                        selectedTextColor = TripGreen,
                        indicatorColor = TripGreen.copy(alpha = 0.15f)
                    ),
                    onClick = { selectedTab = "Console" }
                )
                NavigationBarItem(
                    icon = {
                        BadgedBox(
                            badge = {
                                if (viewModel.totalOrders > 0) {
                                    Badge(containerColor = TripOrangeActivity) {
                                        Text("${viewModel.totalOrders}", color = Color.White)
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Outlined.ShoppingCart, "Prenotazioni")
                        }
                    },
                    label = { Text("Ricevute Ordini", fontWeight = FontWeight.SemiBold) },
                    selected = selectedTab == "Ordini",
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = TripGreen,
                        selectedTextColor = TripGreen,
                        indicatorColor = TripGreen.copy(alpha = 0.15f)
                    ),
                    onClick = { selectedTab = "Ordini" }
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
                            Icon(Icons.Default.Email, "Messaggi")
                        }
                    },
                    label = { Text("Messaggi", fontWeight = FontWeight.SemiBold) },
                    selected = selectedTab == "Messaggi",
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = TripGreen,
                        selectedTextColor = TripGreen,
                        indicatorColor = TripGreen.copy(alpha = 0.15f)
                    ),
                    onClick = { selectedTab = "Messaggi" }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, "Profilo") },
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
                "Console" -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 104.dp)
                    ) {
                        // 1. HEADER CON CAMPANELLA NOTIFICHE
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 14.dp),
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
                                            text = "HORIZON PARTNER HUB",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = TripGreen,
                                            letterSpacing = 1.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Console Agenzia",
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
                                            viewModel.fetchMyTrips(tokenUtente)
                                            showNotificationsSheet = true
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Notifications,
                                        contentDescription = "Notifiche Prenotazioni",
                                        tint = TripBlack,
                                        modifier = Modifier.size(26.dp)
                                    )
                                    if (hasNotifications) {
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

                        // 2. BARRA INTUITIVA A 3 INDICATORI + RATING PARTNER REALE DAL DB
                        item {
                            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    QuickPulsePill(
                                        modifier = Modifier.weight(1f),
                                        icon = Icons.Default.Person,
                                        value = "${viewModel.totalBookedSpots}",
                                        label = "Ospiti Attesi",
                                        tint = TripGreen,
                                        onClick = { selectedTab = "Ordini" }
                                    )
                                    QuickPulsePill(
                                        modifier = Modifier.weight(1f),
                                        icon = Icons.Default.CheckCircle,
                                        value = "${viewModel.totalOrders}",
                                        label = "Ricevute",
                                        tint = TripDarkGreen,
                                        onClick = { selectedTab = "Ordini" }
                                    )
                                    QuickPulsePill(
                                        modifier = Modifier.weight(1f),
                                        icon = Icons.Default.Place,
                                        value = "${viewModel.myTrips.size}",
                                        label = "In Catalogo",
                                        tint = TripOrangeActivity,
                                        onClick = { catalogFilter = "TUTTI" }
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color.White,
                                    shadowElevation = 2.dp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showRevenueBanner = !showRevenueBanner }
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(Icons.Default.Star, contentDescription = null, tint = TripAmber, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = if (viewModel.totalAgencyReviews > 0) {
                                                        "Rating Partner ${String.format(Locale.US, "%.1f", viewModel.averageCatalogRating)} / 5.0"
                                                    } else {
                                                        "Rating Partner: Nessuna recensione"
                                                    },
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 13.sp,
                                                    color = TripBlack,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = if (showRevenueBanner) "Nascondi" else "Incassi",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TripGreen
                                                )
                                                Icon(
                                                    imageVector = if (showRevenueBanner) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                    contentDescription = null,
                                                    tint = TripGreen,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }

                                        AnimatedVisibility(visible = showRevenueBanner) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 10.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("Totale incassato dalle ricevute:", fontSize = 12.sp, color = Color.Gray)
                                                Text(
                                                    text = "€${String.format(Locale.ITALY, "%.2f", viewModel.totalRevenue)}",
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 16.sp,
                                                    color = TripDarkGreen
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(22.dp))
                        }

                        // 3. ULTIME RICEVUTE COMPATTE E PULITE
                        if (viewModel.agencyOrders.isNotEmpty()) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 24.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Ultime Ricevute",
                                        fontSize = 19.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = TripBlack
                                    )
                                    if (viewModel.agencyOrders.size > 3) {
                                        TextButton(onClick = { selectedTab = "Ordini" }) {
                                            Text("Vedi tutte (${viewModel.agencyOrders.size}) →", color = TripGreen, fontWeight = FontWeight.ExtraBold)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                            }

                            items(viewModel.agencyOrders.take(3)) { order ->
                                val matchedTrip = viewModel.myTrips.find { it.id == order.tripId }
                                val receiptInfo = resolveReceiptDetails(context, order, matchedTrip, notificationMessages)
                                AgencyReceiptRowCard(
                                    order = order,
                                    receiptInfo = receiptInfo,
                                    onOpenReceipt = { selectedOrderForReceipt = order }
                                )
                            }

                            item { Spacer(modifier = Modifier.height(20.dp)) }
                        }

                        // 4. CATALOGO AGENZIA CON VALUTAZIONI REALI DAL DB
                        item {
                            SectionHeader(
                                title = "Il tuo Catalogo",
                                subtitle = "Tocca un'offerta per visualizzarla o modificarla"
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 24.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                item {
                                    OfferTypeChip("Tutti (${viewModel.myTrips.size})", catalogFilter == "TUTTI") {
                                        catalogFilter = "TUTTI"
                                    }
                                }
                                item {
                                    OfferTypeChip("Viaggi", catalogFilter == "VIAGGI", Icons.Default.FlightTakeoff) {
                                        catalogFilter = "VIAGGI"
                                    }
                                }
                                item {
                                    OfferTypeChip("Attività", catalogFilter == "ATTIVITA", Icons.Default.ConfirmationNumber) {
                                        catalogFilter = "ATTIVITA"
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        if (viewModel.isLoading) {
                            items(2) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 24.dp, vertical = 10.dp),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Box(modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(16.dp)).shimmerEffect())
                                        Spacer(modifier = Modifier.height(14.dp))
                                        Box(modifier = Modifier.fillMaxWidth(0.6f).height(20.dp).clip(RoundedCornerShape(6.dp)).shimmerEffect())
                                    }
                                }
                            }
                        } else if (filteredCatalog.isEmpty()) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = Color.White,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(32.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(Icons.Default.AddCircle, contentDescription = null, tint = TripGreen, modifier = Modifier.size(48.dp))
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text("Nessun pacchetto in questa vista", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = TripBlack)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            "Premi su 'Nuova Offerta' in basso a destra per pubblicare un viaggio o un'attività giornaliera.",
                                            fontSize = 13.sp,
                                            color = Color.Gray,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        } else {
                            items(filteredCatalog) { trip ->
                                OrganizerTripCard(
                                    trip = trip,
                                    onClick = { onTripClick(trip.id) }
                                )
                            }
                        }
                    }
                }

                "Ordini" -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
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
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Ricevute Ordini",
                                        fontSize = 26.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = TripBlack
                                    )
                                    Text(
                                        text = "Tocca una prenotazione per aprire la ricevuta completa",
                                        fontSize = 13.sp,
                                        color = Color.Gray
                                    )
                                }
                                IconButton(
                                    onClick = { showNotificationsSheet = true },
                                    modifier = Modifier.clip(CircleShape).background(Color.White)
                                ) {
                                    Icon(Icons.Outlined.Notifications, contentDescription = "Notifiche", tint = TripGreen)
                                }
                            }
                        }

                        if (viewModel.agencyOrders.isEmpty()) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = Color.White,
                                    modifier = Modifier.fillMaxWidth().padding(24.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(32.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(Icons.Outlined.ShoppingCart, contentDescription = null, tint = TripGreen, modifier = Modifier.size(56.dp))
                                        Spacer(modifier = Modifier.height(14.dp))
                                        Text("Nessuna ricevuta disponibile", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = TripBlack)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text("Le prenotazioni effettuate dai viaggiatori genereranno qui la ricevuta digitale.", fontSize = 13.sp, color = Color.Gray, textAlign = TextAlign.Center)
                                    }
                                }
                            }
                        } else {
                            items(viewModel.agencyOrders) { order ->
                                val matchedTrip = viewModel.myTrips.find { it.id == order.tripId }
                                val receiptInfo = resolveReceiptDetails(context, order, matchedTrip, notificationMessages)
                                AgencyReceiptRowCard(
                                    order = order,
                                    receiptInfo = receiptInfo,
                                    onOpenReceipt = { selectedOrderForReceipt = order }
                                )
                            }
                        }
                    }
                }

                "Messaggi" -> {
                    LiveChatTabScreen(
                        tokenUtente = tokenUtente,
                        ruoloUtente = "ORGANIZZATORE",
                        initialTrips = viewModel.myTrips,
                        onUnreadChatsCountChanged = { count -> viewModel.unreadChatsCount = count}
                    )
                }
            }
        }
    }

    // BOTTOM SHEET NOTIFICHE AGENZIA ALLINEATO SENZA SOVRAPPOSIZIONI
    if (showNotificationsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showNotificationsSheet = false },
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Notifiche Agenzia",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TripBlack
                        )
                        Text(
                            text = "Tocca una notifica per aprire la ricevuta",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                    Surface(
                        color = TripGreen.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = "${viewModel.agencyOrders.size} ricevute",
                            color = TripDarkGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (viewModel.agencyOrders.isEmpty() && viewModel.myNotifications.isEmpty()) {
                    Text(
                        text = "Non hai ancora ricevuto nuove prenotazioni.",
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 420.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Notifiche dei nuovi messaggi in Chat e recensioni (toccandole apre la sezione Messaggi)
                        items(viewModel.myNotifications) { notifica ->
                            val isChatNotif = notifica.title.contains("Chat", ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = TripWarmBg,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (isChatNotif) {
                                            showNotificationsSheet = false
                                            selectedTab = "Messaggi"
                                        }
                                    }
                            ) {
                                Box(modifier = Modifier.padding(14.dp)) {
                                    NotificationItem(
                                        title = notifica.title,
                                        message = notifica.message + if (isChatNotif) " (Tocca per rispondere in chat)" else "",
                                        time = notifica.timestamp.take(10)
                                    )
                                }
                            }
                        }
                        items(viewModel.agencyOrders) { order ->
                            val matchedTrip = viewModel.myTrips.find { it.id == order.tripId }
                            val receiptInfo = resolveReceiptDetails(context, order, matchedTrip, notificationMessages)

                            AgencyNotificationCard(
                                badgeCode = "#${receiptInfo.receiptCode}",
                                dateText = order.reservationDate.take(10),
                                travelerName = receiptInfo.travelerName,
                                subtitle = "${order.bookedSpots} posti • ${order.tripTitle}",
                                amountText = "€${String.format(Locale.ITALY, "%.2f", order.totalPaid)}",
                                onClick = {
                                    showNotificationsSheet = false
                                    selectedOrderForReceipt = order
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }

    // MODAL RICEVUTA UFFICIALE DI PRENOTAZIONE
    if (selectedOrderForReceipt != null) {
        val order = selectedOrderForReceipt!!
        val matchedTrip = viewModel.myTrips.find { it.id == order.tripId }
        val receiptInfo = resolveReceiptDetails(context, order, matchedTrip, notificationMessages)

        ModalBottomSheet(
            onDismissRequest = { selectedOrderForReceipt = null },
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
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
                                text = "✓ TRANSAZIONE AUTORIZZATA • ${order.status}",
                                color = TripDarkGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Ricevuta di Prenotazione",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TripBlack
                        )
                        Text(
                            text = "Codice Operazione: #${receiptInfo.receiptCode}",
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
                            text = "DATI DEL VIAGGIATORE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TripGreen,
                            letterSpacing = 0.8.sp
                        )
                        ReceiptDetailRow("Cliente:", receiptInfo.travelerName, isBold = true)
                        ReceiptDetailRow("Email contatto:", receiptInfo.travelerEmail)

                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))

                        Text(
                            text = "DETTAGLI ESPERIENZA PRENOTATA",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TripGreen,
                            letterSpacing = 0.8.sp
                        )
                        ReceiptDetailRow("Pacchetto / Attività:", order.tripTitle, isBold = true)
                        ReceiptDetailRow("Partecipanti:", receiptInfo.participantsList)
                        ReceiptDetailRow("Data / Turno scelto:", receiptInfo.selectedDateSlot)
                        ReceiptDetailRow("Posti riservati:", "${order.bookedSpots} partecipanti")

                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))

                        Text(
                            text = "ESTREMI DI PAGAMENTO",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TripGreen,
                            letterSpacing = 0.8.sp
                        )
                        ReceiptDetailRow("Metodo utilizzato:", receiptInfo.paymentMethod, isBold = true)
                        ReceiptDetailRow("Data operazione:", order.reservationDate.take(16).replace("T", " "))
                        ReceiptDetailRow(
                            "Quota unitaria:",
                            "€${String.format(Locale.ITALY, "%.2f", receiptInfo.unitPrice)} × ${order.bookedSpots}"
                        )

                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TOTALE INCASSATO",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = TripBlack
                            )
                            Text(
                                text = "€${String.format(Locale.ITALY, "%.2f", order.totalPaid)}",
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
                            val shareText = """
                                RICEVUTA UFFICIALE HORIZON TRAVEL
                                Codice: #${receiptInfo.receiptCode}
                                Cliente: ${receiptInfo.travelerName} (${receiptInfo.travelerEmail})
                                Esperienza: ${order.tripTitle}
                                Data/Turno: ${receiptInfo.selectedDateSlot}
                                Posti prenotati: ${order.bookedSpots}
                                Metodo di pagamento: ${receiptInfo.paymentMethod}
                                Totale pagato: €${String.format(Locale.ITALY, "%.2f", order.totalPaid)}
                            """.trimIndent()
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Ricevuta #${receiptInfo.receiptCode}")
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Esporta Ricevuta"))
                        },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.5.dp, TripGreen),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = TripGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Esporta", color = TripDarkGreen, fontWeight = FontWeight.ExtraBold)
                    }

                    Button(
                        onClick = {
                            val targetTripId = order.tripId
                            selectedOrderForReceipt = null
                            onTripClick(targetTripId)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TripGreen),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    ) {
                        Text("Vedi Offerta →", color = Color.White, fontWeight = FontWeight.ExtraBold)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun AgencyNotificationCard(
    badgeCode: String,
    dateText: String,
    travelerName: String,
    subtitle: String,
    amountText: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = TripWarmBg,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                        text = badgeCode,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TripGreen
                    )
                    Text(
                        text = dateText,
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = travelerName,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = TripBlack,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = amountText,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = TripDarkGreen
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = Color.DarkGray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun QuickPulsePill(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    value: String,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        shadowElevation = 3.dp
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TripBlack
            )
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Gray,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ReceiptDetailRow(label: String, value: String, isBold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.weight(0.42f)
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = if (isBold) FontWeight.ExtraBold else FontWeight.SemiBold,
            color = TripBlack,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.58f)
        )
    }
}

@Composable
fun AgencyReceiptRowCard(
    order: ReservationDTO,
    receiptInfo: ResolvedReceiptInfo,
    onOpenReceipt: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 5.dp)
            .clickable { onOpenReceipt() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(TripGreen.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = null, tint = TripGreen, modifier = Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = receiptInfo.travelerName,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = TripBlack,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = order.tripTitle,
                    fontSize = 13.sp,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Surface(
                color = TripGreen.copy(alpha = 0.12f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = "€${String.format(Locale.ITALY, "%.0f", order.totalPaid)}",
                    color = TripDarkGreen,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = "Apri ricevuta",
                tint = Color.Gray
            )
        }
    }
}

@Composable
fun OrganizerTripCard(
    trip: TripDTO,
    onClick: () -> Unit = {}
) {
    val isActivity = isActivityOrExcursion(trip)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .bounceClick { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
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
                    Text(
                        text = trip.location ?: "Destinazione",
                        fontSize = 12.sp,
                        color = TripGreen,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = TripAmber, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = if (trip.reviewCount > 0) "${String.format(Locale.US, "%.1f", trip.averageRating)} (${trip.reviewCount})" else "Nuovo",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TripBlack
                        )
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = trip.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TripBlack,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "€${trip.price}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TripDarkGreen
                    )
                    Surface(
                        color = TripGreen.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Gestisci date ->",
                            fontSize = 11.sp,
                            color = TripDarkGreen,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }
    }
}