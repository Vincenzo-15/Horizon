package com.example.travelapp.ui.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.travelapp.ui.theme.TravelAppTheme
import com.example.travelapp.viewmodel.HomeViewModel

fun Modifier.bounceClick(
    scaleDown: Float = 0.92f,
    onClick: () -> Unit
) = composed {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) scaleDown else 1f,
        label = "BounceAnimation"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        )
        .pointerInput(isPressed) {
            awaitPointerEventScope {
                isPressed = if (isPressed) {
                    waitForUpOrCancellation()
                    false
                } else {
                    awaitFirstDown(false)
                    true
                }
            }
        }
}

fun Modifier.shimmerEffect(): Modifier = composed {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val transition = rememberInfiniteTransition(label = "shimmer")
    val startOffsetX by transition.animateFloat(
        initialValue = -2 * size.width.toFloat(),
        targetValue = 2 * size.width.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_animation"
    )

    background(
        brush = Brush.linearGradient(
            colors = listOf(
                Color(0xFFE0E0E0),
                Color(0xFFF5F5F5),
                Color(0xFFE0E0E0)
            ),
            start = Offset(startOffsetX, 0f),
            end = Offset(startOffsetX + size.width.toFloat(), size.height.toFloat())
        )
    ).onGloballyPositioned { size = it.size }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    tokenUtente: String = "",
    onTripClick: (Long) -> Unit = {} // --- AGGIUNTO ---
) {
    LaunchedEffect(Unit) {
        if (tokenUtente.isNotEmpty()) {
            viewModel.fetchTrips(tokenUtente)
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                NavigationBarItem(icon = { Icon(Icons.Default.Home, "Home") }, label = { Text("Home") }, selected = true, onClick = {})
                NavigationBarItem(icon = { Icon(Icons.Default.Send, "Chat") }, label = { Text("Chat") }, selected = false, onClick = {})
                NavigationBarItem(icon = { Icon(Icons.Default.DateRange, "Schedule") }, label = { Text("Viaggi") }, selected = false, onClick = {})
                NavigationBarItem(icon = { Icon(Icons.Default.Favorite, "Saved") }, label = { Text("Preferiti") }, selected = false, onClick = {})
                NavigationBarItem(icon = { Icon(Icons.Default.Person, "Profile") }, label = { Text("Profilo") }, selected = false, onClick = {})
            }
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(top = 24.dp, bottom = 24.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Person, contentDescription = "Profile", tint = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Ciao, Viaggiatore!", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                }
                Row {
                    IconButton(onClick = {}) { Icon(Icons.Default.Settings, contentDescription = "Impostazioni") }
                    IconButton(onClick = {}) { Icon(Icons.Default.Notifications, contentDescription = "Notifiche") }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Text("Esplora Fantastiche", fontSize = 28.sp, fontWeight = FontWeight.Normal)
                Text("Destinazioni!", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = "", onValueChange = {}, placeholder = { Text("Dove vuoi andare?") }, leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Cerca") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Box(modifier = Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)).bounceClick {  }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Menu, contentDescription = "Filtri", tint = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            LazyRow(contentPadding = PaddingValues(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val categories = listOf("Tutte", "Mare", "Montagna", "Città d'arte", "Avventura")
                items(categories.size) { index ->
                    val isSelected = index == 0
                    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp), modifier = Modifier.bounceClick { }) {
                        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            val iconTint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                            Icon(Icons.Default.Place, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(categories[index], fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("I Più Popolari", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Vedi tutto", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable { })
            }
            Spacer(modifier = Modifier.height(16.dp))

            if (viewModel.isLoading) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    items(3) {
                        Column(modifier = Modifier.width(260.dp)) {
                            Box(modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(20.dp)).shimmerEffect())
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(modifier = Modifier.fillMaxWidth(0.8f).height(20.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(modifier = Modifier.fillMaxWidth(0.5f).height(16.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                        }
                    }
                }
            } else if (viewModel.errorMessage != null) {
                Text(text = viewModel.errorMessage!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 24.dp))
            } else if (viewModel.trips.isEmpty()) {
                Text(text = "Nessun viaggio disponibile al momento.", color = Color.Gray, modifier = Modifier.padding(horizontal = 24.dp))
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    items(viewModel.trips.size) { index ->
                        val trip = viewModel.trips[index]

                        Column(
                            modifier = Modifier
                                .width(260.dp)
                                .bounceClick { onTripClick(trip.id) } // --- AGGIORNATO ---
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color.LightGray)
                            ) {
                                val coverUrl = trip.imageUrls?.firstOrNull()
                                if (!coverUrl.isNullOrEmpty()) {
                                    AsyncImage(
                                        model = coverUrl,
                                        contentDescription = trip.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(12.dp)
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.85f))
                                        .clickable { },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.FavoriteBorder, contentDescription = "Salva", tint = Color.Black, modifier = Modifier.size(20.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(text = trip.title, fontWeight = FontWeight.Bold, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)

                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, contentDescription = "Posizione", tint = Color.Gray, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(trip.location ?: "N/D", color = Color.Gray, fontSize = 13.sp)
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, contentDescription = "Star", tint = Color(0xFFFFC107), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(trip.averageRating.toString(), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(" (${trip.reviewCount} recensioni)", color = Color.Gray, fontSize = 13.sp)
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "Da ${trip.price}€ / persona", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text("Esplora per Città", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 24.dp))
            Spacer(modifier = Modifier.height(16.dp))

            LazyRow(contentPadding = PaddingValues(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                val cities = listOf("Roma", "Parigi", "Londra", "Tokyo", "New York")
                items(cities.size) { index ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.bounceClick { }) {
                        Box(modifier = Modifier.size(80.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(cities[index], fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    TravelAppTheme {
        HomeScreen(tokenUtente = "")
    }
}