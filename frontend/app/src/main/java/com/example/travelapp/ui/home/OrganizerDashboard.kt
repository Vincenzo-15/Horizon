package com.example.travelapp.ui.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.example.travelapp.viewmodel.OrganizerViewModel

// --- ANIMAZIONI ---
fun Modifier.bounceClick(scaleDown: Float = 0.92f, onClick: () -> Unit) = composed {
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
    // Stati Navigazione BottomBar
    var selectedTab by remember { mutableStateOf("Console") }

    LaunchedEffect(tokenUtente) {
        if (tokenUtente.isNotEmpty()) {
            viewModel.fetchMyTrips(tokenUtente)
        }
    }

    Scaffold(
        containerColor = TripWarmBg, // Sfondo grigio caldo come la Home
        floatingActionButton = {
            if (selectedTab == "Console") {
                ExtendedFloatingActionButton(
                    onClick = { onNavigateToCreateTrip() },
                    containerColor = TripBlack, // Bottone scuro ed elegante
                    contentColor = Color.White,
                    shape = RoundedCornerShape(20.dp),
                    elevation = FloatingActionButtonDefaults.elevation(8.dp),
                    icon = { Icon(Icons.Default.Add, contentDescription = "Nuovo") },
                    text = { Text("Nuova Offerta", fontWeight = FontWeight.Bold) }
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
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = TripGreen, selectedTextColor = TripGreen, indicatorColor = TripGreen.copy(alpha = 0.15f)),
                    onClick = { selectedTab = "Console" }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Outlined.ShoppingCart, "Ordini") },
                    label = { Text("Ordini", fontWeight = FontWeight.SemiBold) },
                    selected = selectedTab == "Ordini",
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = TripGreen, selectedTextColor = TripGreen, indicatorColor = TripGreen.copy(alpha = 0.15f)),
                    onClick = { selectedTab = "Ordini" }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, "Profilo") },
                    label = { Text("Profilo", fontWeight = FontWeight.SemiBold) },
                    selected = selectedTab == "Profilo",
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = TripGreen, selectedTextColor = TripGreen, indicatorColor = TripGreen.copy(alpha = 0.15f)),
                    onClick = { onNavigateToProfile() }
                )
            }
        }
    ) { paddingValues ->

        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            when (selectedTab) {
                "Console" -> {
                    // --- TAB 1: CONSOLE E CATALOGO ---
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 96.dp) // Spazio per il FAB
                    ) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 24.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(TripBlack),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Star, contentDescription = "Partner", tint = TripAmber, modifier = Modifier.size(28.dp))
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Surface(
                                            color = TripGreen.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "PARTNER VERIFICATO",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = TripDarkGreen,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Console Agenzia", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = TripBlack)
                                    }
                                }

                                IconButton(
                                    onClick = { viewModel.fetchMyTrips(tokenUtente) },
                                    modifier = Modifier.clip(CircleShape).background(Color.White)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Aggiorna", tint = TripGreen)
                                }
                            }
                        }

                        // --- CARD STATISTICHE ---
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Card(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(20.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(TripGreen)
                                            .padding(20.dp)
                                    ) {
                                        Column {
                                            Box(
                                                modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.Place, contentDescription = null, tint = Color.White)
                                            }
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Text(text = "${viewModel.myTrips.size}", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                                            Text(text = "Annunci Attivi", fontSize = 13.sp, color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.Medium)
                                        }
                                    }
                                }

                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { selectedTab = "Ordini" }, // Cliccabile per andare agli ordini
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Column(modifier = Modifier.padding(20.dp)) {
                                        Box(
                                            modifier = Modifier.size(40.dp).clip(CircleShape).background(TripOrangeActivity.copy(alpha = 0.1f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = TripOrangeActivity)
                                        }
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(text = "${viewModel.totalOrders}", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = TripBlack)
                                        Text(text = "Ordini Ricevuti", fontSize = 13.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(32.dp))
                        }

                        item {
                            SectionHeader(title = "Il tuo Catalogo", subtitle = "Gestisci i viaggi e le attività che hai pubblicato")
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        if (viewModel.isLoading) {
                            items(2) {
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 10.dp),
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
                        } else if (viewModel.errorMessage != null) {
                            item { Text(text = viewModel.errorMessage!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(24.dp)) }
                        } else if (viewModel.myTrips.isEmpty()) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = Color.White,
                                    modifier = Modifier.fillMaxWidth().padding(24.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Box(
                                            modifier = Modifier.size(64.dp).clip(CircleShape).background(TripGreen.copy(alpha = 0.1f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(32.dp), tint = TripGreen)
                                        }
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text("Nessun pacchetto pubblicato", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TripBlack)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("Crea la tua prima offerta per raggiungere i viaggiatori.", fontSize = 13.sp, color = Color.Gray, textAlign = TextAlign.Center)
                                    }
                                }
                            }
                        } else {
                            items(viewModel.myTrips) { trip ->
                                OrganizerTripCard(trip = trip, onClick = { onTripClick(trip.id) })
                            }
                        }
                    }
                }

                "Ordini" -> {
                    // --- TAB 2: ORDINI E PRENOTAZIONI RICEVUTE ---
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
                                        text = "Prenotazioni Ricevute",
                                        fontSize = 26.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = TripBlack
                                    )
                                    Text(
                                        text = "Elenco degli ordini dei viaggiatori",
                                        fontSize = 13.sp,
                                        color = Color.Gray
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.fetchMyTrips(tokenUtente) },
                                    modifier = Modifier.clip(CircleShape).background(Color.White)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Aggiorna", tint = TripGreen)
                                }
                            }
                        }

                        if (viewModel.isLoading) {
                            item { Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = TripGreen) } }
                        } else if (viewModel.agencyOrders.isEmpty()) {
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
                                        Text("Le prenotazioni dei viaggiatori appariranno qui.", fontSize = 13.sp, color = Color.Gray, textAlign = TextAlign.Center)
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
}

// Card per le prenotazioni ricevute dall'agenzia
@Composable
fun AgencyOrderCard(order: ReservationDTO, onTripClick: (Long) -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .clickable { onTripClick(order.tripId) }
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
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Text(text = order.reservationDate.take(10), fontSize = 12.sp, color = Color.Gray)
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = order.tripTitle,
                fontSize = 16.sp,
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
                    Text(text = "${order.bookedSpots} posti prenotati", fontSize = 13.sp, color = Color.DarkGray)
                }
                Text(
                    text = "Totale: €${order.totalPaid}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TripBlack
                )
            }
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
            .padding(horizontal = 24.dp, vertical = 10.dp)
            .bounceClick { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(Color.LightGray)
            ) {
                AsyncImage(
                    model = resolveTripImageUrl(trip), // Usa l'helper di HomeScreen
                    contentDescription = trip.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f)), startY = 200f))
                )

                // Badge Viaggio/Attività
                Surface(
                    color = if (isActivity) TripOrangeActivity else TripGreen,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.align(Alignment.TopStart).padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (isActivity) "ATTIVITÀ" else "VIAGGIO", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }

                Text(
                    text = "€${trip.price} / pers.",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
                )
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = trip.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TripBlack,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = TripGreen, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = trip.location ?: "Destinazione non specificata",
                        fontSize = 13.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DateRange, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${trip.startDate ?: "N/D"} → ${trip.endDate ?: "N/D"}",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Surface(
                        color = TripWarmBg,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "${trip.availableSpots ?: 0} liberi",
                            fontSize = 11.sp,
                            color = TripBlack,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}