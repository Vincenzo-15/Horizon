package com.example.travelapp.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
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
import java.util.Locale

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
    var selectedTab by remember { mutableStateOf("Console") }
    var catalogFilter by remember { mutableStateOf("TUTTI") } // "TUTTI", "VIAGGI", "ATTIVITA", "LOW_SPOTS"
    var showNotificationsSheet by remember { mutableStateOf(false) }
    var showFinancialSummary by remember { mutableStateOf(false) }

    LaunchedEffect(tokenUtente) {
        if (tokenUtente.isNotEmpty()) {
            viewModel.fetchMyTrips(tokenUtente)
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

    val totalRemainingSpots = remember(viewModel.myTrips) {
        viewModel.myTrips.sumOf { it.availableSpots.coerceAtLeast(0) }
    }
    val soldOutOrLowCount = remember(viewModel.myTrips) {
        viewModel.myTrips.count { it.availableSpots <= 5 }
    }
    val hasNotifications = viewModel.agencyOrders.isNotEmpty() || viewModel.myNotifications.isNotEmpty()

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
                    label = { Text("Oggi", fontWeight = FontWeight.SemiBold) },
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
                    label = { Text("Prenotazioni", fontWeight = FontWeight.SemiBold) },
                    selected = selectedTab == "Ordini",
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = TripGreen,
                        selectedTextColor = TripGreen,
                        indicatorColor = TripGreen.copy(alpha = 0.15f)
                    ),
                    onClick = { selectedTab = "Ordini" }
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
                                            text = "HORIZON PARTNER HUB",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = TripGreen,
                                            letterSpacing = 1.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Console Operativa",
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

                        // 2. CARD PRINCIPALE: STATO OPERATIVO & OSPITI (STILE AIRBNB HOST "TODAY" / VIATOR SUPPLIER)
                        item {
                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = Color.White,
                                shadowElevation = 4.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 6.dp)
                            ) {
                                Column(modifier = Modifier.padding(22.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            color = TripGreen.copy(alpha = 0.12f),
                                            shape = RoundedCornerShape(50)
                                        ) {
                                            Text(
                                                text = "✓ TOUR OPERATOR VERIFICATO",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = TripDarkGreen,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Star, contentDescription = null, tint = TripAmber, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = String.format(Locale.US, "%.1f", viewModel.averageCatalogRating),
                                                color = TripBlack,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = " qualità media",
                                                color = Color.Gray,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))
                                    Text(
                                        text = "Panoramica Attività & Ospiti",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = TripBlack
                                    )
                                    Text(
                                        text = "Monitoraggio in tempo reale di capienza, adesioni e catalogo",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Griglia KPI Operativi (Ospiti, Prenotazioni, Annunci, Posti liberi)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        OperationalMetricBox(
                                            modifier = Modifier.weight(1f),
                                            value = "${viewModel.totalBookedSpots}",
                                            label = "Ospiti Confermati",
                                            accentColor = TripGreen
                                        )
                                        OperationalMetricBox(
                                            modifier = Modifier.weight(1f),
                                            value = "${viewModel.totalOrders}",
                                            label = "Prenotazioni",
                                            accentColor = TripDarkGreen
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        OperationalMetricBox(
                                            modifier = Modifier.weight(1f),
                                            value = "${viewModel.myTrips.size}",
                                            label = "Offerte a Catalogo",
                                            accentColor = TripBlack
                                        )
                                        OperationalMetricBox(
                                            modifier = Modifier.weight(1f),
                                            value = "$totalRemainingSpots",
                                            label = "Posti Liberi Totali",
                                            accentColor = if (soldOutOrLowCount > 0) TripOrangeActivity else TripGreen
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))
                                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.35f))
                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Sezione contabile discreta espandibile a richiesta
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { showFinancialSummary = !showFinancialSummary }
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (showFinancialSummary) "Nascondi riepilogo contabile" else "Mostra riepilogo contabile riservato",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TripGreen
                                        )
                                        Icon(
                                            imageVector = if (showFinancialSummary) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                            contentDescription = null,
                                            tint = TripGreen
                                        )
                                    }

                                    AnimatedVisibility(visible = showFinancialSummary) {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = TripWarmBg,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(14.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("Volume prenotazioni confermate:", fontSize = 13.sp, color = Color.DarkGray)
                                                Text(
                                                    text = "€${String.format(Locale.ITALY, "%.2f", viewModel.totalRevenue)}",
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = TripDarkGreen
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                        }

                        // 3. CAROSELLO ORIZZONTALE: ULTIME PRENOTAZIONI RICEVUTE
                        if (viewModel.agencyOrders.isNotEmpty()) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 24.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Ultime Adesioni Ricevute",
                                            fontSize = 19.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = TripBlack
                                        )
                                        Text(
                                            text = "Tocca una prenotazione per gestire l'offerta associata",
                                            fontSize = 12.sp,
                                            color = Color.Gray
                                        )
                                    }
                                    TextButton(onClick = { selectedTab = "Ordini" }) {
                                        Text("Vedi tutte →", color = TripGreen, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))

                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 24.dp),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    items(viewModel.agencyOrders.take(6)) { order ->
                                        RecentOrderCompactCard(
                                            order = order,
                                            onClick = { onTripClick(order.tripId) }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(24.dp))
                            }
                        }

                        // 4. CATALOGO AGENZIA CON FILTRI RAPIDI
                        item {
                            SectionHeader(
                                title = "Il tuo Catalogo Esperienze",
                                subtitle = "Filtra i tuoi annunci e gestisci calendario prezzi e posti disponibili"
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
                                    OfferTypeChip("✈️ Viaggi", catalogFilter == "VIAGGI") {
                                        catalogFilter = "VIAGGI"
                                    }
                                }
                                item {
                                    OfferTypeChip("🎟️ Attività", catalogFilter == "ATTIVITA") {
                                        catalogFilter = "ATTIVITA"
                                    }
                                }
                                item {
                                    OfferTypeChip("⚠️ Pochi posti / Esauriti ($soldOutOrLowCount)", catalogFilter == "LOW_SPOTS") {
                                        catalogFilter = "LOW_SPOTS"
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
                                        Box(modifier = Modifier.fillMaxWidth().height(170.dp).clip(RoundedCornerShape(16.dp)).shimmerEffect())
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Box(modifier = Modifier.fillMaxWidth(0.7f).height(22.dp).clip(RoundedCornerShape(6.dp)).shimmerEffect())
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
                                val ordersForTrip = viewModel.agencyOrders.count { it.tripId == trip.id }
                                OrganizerTripCard(
                                    trip = trip,
                                    ordersCount = ordersForTrip,
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
                                Column {
                                    Text(
                                        text = "Registro Prenotazioni",
                                        fontSize = 26.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = TripBlack
                                    )
                                    Text(
                                        text = "${viewModel.totalOrders} ordini confermati • ${viewModel.totalBookedSpots} ospiti totali",
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
                                        Text("Nessun ordine ricevuto", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = TripBlack)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text("Le prenotazioni effettuate dai viaggiatori appariranno qui e nella campanella notifiche.", fontSize = 13.sp, color = Color.Gray, textAlign = TextAlign.Center)
                                    }
                                }
                            }
                        } else {
                            items(viewModel.agencyOrders) { order ->
                                AgencyOrderCard(order = order, onTripClick = onTripClick)
                            }
                        }
                    }
                }
            }
        }
    }

    // --- BOTTOM SHEET NOTIFICHE AGENZIA ---
    if (showNotificationsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showNotificationsSheet = false },
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Notifiche & Nuove Prenotazioni",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TripBlack
                    )
                    Surface(
                        color = TripGreen.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = "${viewModel.agencyOrders.size} ordini",
                            color = TripDarkGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (viewModel.agencyOrders.isEmpty() && viewModel.myNotifications.isEmpty()) {
                    Text(
                        text = "Non hai ancora ricevuto nuove prenotazioni sui tuoi viaggi.",
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                        items(viewModel.agencyOrders) { order ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showNotificationsSheet = false
                                        onTripClick(order.tripId)
                                    }
                                    .padding(vertical = 6.dp)
                            ) {
                                NotificationItem(
                                    title = "Prenotazione #${order.id} • ${order.bookedSpots} ospiti",
                                    message = "Confermata per \"${order.tripTitle}\". Tocca per aprire l'annuncio.",
                                    time = order.reservationDate.take(10)
                                )
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.LightGray.copy(alpha = 0.4f))
                        }

                        items(viewModel.myNotifications) { notifica ->
                            NotificationItem(
                                title = notifica.title,
                                message = notifica.message,
                                time = notifica.timestamp.take(10)
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.LightGray.copy(alpha = 0.4f))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }
}

@Composable
private fun OperationalMetricBox(
    modifier: Modifier = Modifier,
    value: String,
    label: String,
    accentColor: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = TripWarmBg
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = accentColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.DarkGray
            )
        }
    }
}

@Composable
private fun RecentOrderCompactCard(
    order: ReservationDTO,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        shadowElevation = 3.dp,
        modifier = Modifier
            .width(240.dp)
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = TripGreen.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "ORDINE #${order.id}",
                        color = TripDarkGreen,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Text(
                    text = "${order.bookedSpots} ospiti",
                    color = TripGreen,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = order.tripTitle,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp,
                color = TripBlack,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Confermato il ${order.reservationDate.take(10)}",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun AgencyOrderCard(order: ReservationDTO, onTripClick: (Long) -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .clickable { onTripClick(order.tripId) }
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = TripGreen.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "PRENOTAZIONE #${order.id} • ${order.status}",
                        color = TripDarkGreen,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Text(text = order.reservationDate.take(10), fontSize = 12.sp, color = Color.Gray)
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = order.tripTitle,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TripBlack,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = TripGreen, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "${order.bookedSpots} posti prenotati", fontSize = 13.sp, color = Color.DarkGray, fontWeight = FontWeight.SemiBold)
                }
                Text(
                    text = "Totale: €${order.totalPaid}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TripDarkGreen
                )
            }
        }
    }
}

@Composable
fun OrganizerTripCard(
    trip: TripDTO,
    ordersCount: Int = 0,
    onClick: () -> Unit = {}
) {
    val isActivity = isActivityOrExcursion(trip)
    val isSoldOut = trip.availableSpots <= 0
    val lowSpots = trip.availableSpots in 1..5

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 10.dp)
            .bounceClick { onClick() },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(175.dp)
                    .background(Color.LightGray)
            ) {
                AsyncImage(
                    model = resolveTripImageUrl(trip),
                    contentDescription = trip.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)), startY = 180f))
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopStart)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        color = if (isActivity) TripOrangeActivity else TripGreen,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (isActivity) "🎟️ ATTIVITÀ GIORNALIERA" else "✈️ PACCHETTO VIAGGIO",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    if (ordersCount > 0) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = "$ordersCount prenotazioni",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Text(
                    text = "da €${trip.price} / pers.",
                    color = Color.White,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(14.dp)
                )
            }

            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = TripGreen, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = trip.location ?: "Destinazione",
                            fontSize = 12.sp,
                            color = TripGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = TripAmber, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${trip.averageRating} (${trip.reviewCount})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TripBlack
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = trip.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TripBlack,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.35f))
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DateRange, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (isActivity) "Dal ${trip.startDate} • Calendario attivo" else "${trip.startDate} → ${trip.endDate}",
                            fontSize = 12.sp,
                            color = Color.DarkGray,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Surface(
                        color = when {
                            isSoldOut -> MaterialTheme.colorScheme.error.copy(alpha = 0.14f)
                            lowSpots -> TripOrangeActivity.copy(alpha = 0.12f)
                            else -> TripGreen.copy(alpha = 0.12f)
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (isSoldOut) "🚫 ESAURITO (0 posti)" else "${trip.availableSpots} posti liberi",
                            fontSize = 11.sp,
                            color = when {
                                isSoldOut -> MaterialTheme.colorScheme.error
                                lowSpots -> TripOrangeActivity
                                else -> TripDarkGreen
                            },
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}