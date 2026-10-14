package com.example.travelapp.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.travelapp.viewmodel.TripDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDetailScreen(
    tripId: Long,
    tokenUtente: String,
    ruoloUtente: String?,
    viewModel: TripDetailViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onNavigateBack: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isFavorite by remember { mutableStateOf(false) }

    // Variabile per il contatore dei posti da acquistare
    var spotsToBook by remember { mutableStateOf(1) }

    LaunchedEffect(tripId) {
        viewModel.fetchTripDetail(tokenUtente, tripId)
    }

    LaunchedEffect(viewModel.isDeleted) {
        if (viewModel.isDeleted) onNavigateBack()
    }

    val trip = viewModel.trip

    // Pop-up: Conferma di Eliminazione (Solo per Agenzia)
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Elimina Pacchetto", fontWeight = FontWeight.Bold) },
            text = { Text("Sei sicuro di voler rimuovere definitivamente \"${trip?.title}\" dal catalogo?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteTrip(tokenUtente, tripId)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Elimina") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Annulla") }
            }
        )
    }

    // Pop-up: Messaggio Esito Prenotazione (Solo per Viaggiatore)
    if (viewModel.bookingMessage != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearBookingMessage() },
            title = {
                Text(
                    text = if (viewModel.bookingSuccess) "Prenotazione Confermata!" else "Attenzione",
                    fontWeight = FontWeight.Bold,
                    color = if (viewModel.bookingSuccess) Color(0xFF10B981) else MaterialTheme.colorScheme.error
                )
            },
            text = { Text(viewModel.bookingMessage!!) },
            confirmButton = {
                Button(onClick = { viewModel.clearBookingMessage() }) { Text("OK") }
            }
        )
    }

    Scaffold(
        bottomBar = {
            if (trip != null) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 12.dp,
                    shadowElevation = 16.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        // PREZZO TOTALE DINAMICO
                        Column {
                            Text("Prezzo totale", fontSize = 12.sp, color = Color.Gray)
                            Row(verticalAlignment = Alignment.Bottom) {
                                val total = (trip.price ?: 0.0) * spotsToBook
                                Text(
                                    text = "${total}€",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // BARRA DELLE AZIONI IN BASE AL RUOLO
                        if (ruoloUtente == "ORGANIZZATORE") {
                            Button(
                                onClick = { showDeleteDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.height(54.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Elimina Annuncio", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        } else {
                            // Sezione Viaggiatore: Selettore Posti + Pulsante Prenota
                            Row(verticalAlignment = Alignment.CenterVertically) {

                                // Selettore Posti ( + / - )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 4.dp)
                                ) {
                                    TextButton(onClick = { if (spotsToBook > 1) spotsToBook-- }) {
                                        Text("-", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Text("$spotsToBook", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    TextButton(onClick = { if (spotsToBook < (trip.availableSpots ?: 1)) spotsToBook++ }) {
                                        Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                // Pulsante di Prenotazione
                                Button(
                                    onClick = { viewModel.bookTrip(tokenUtente, tripId, spotsToBook) },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.height(54.dp),
                                    enabled = !viewModel.isBooking && (trip.availableSpots ?: 0) > 0
                                ) {
                                    if (viewModel.isBooking) {
                                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                                    } else {
                                        Text(
                                            text = if ((trip.availableSpots ?: 0) > 0) "Prenota" else "Esaurito",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp)
                                        )
                                    }
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
                CircularProgressIndicator()
            }
        } else if (viewModel.errorMessage != null) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text(text = viewModel.errorMessage!!, color = MaterialTheme.colorScheme.error)
            }
        } else if (trip != null) {
            val images = trip.imageUrls ?: emptyList()

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(paddingValues)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(360.dp)
                            .background(Color.LightGray)
                    ) {
                        if (images.isNotEmpty()) {
                            val pagerState = rememberPagerState(pageCount = { images.size })

                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.fillMaxSize()
                            ) { page ->
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
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(Color.Black.copy(alpha = 0.3f), Color.Transparent, Color.Black.copy(alpha = 0.65f))
                                        )
                                    )
                            )

                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 44.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                repeat(images.size) { index ->
                                    val isSelected = pagerState.currentPage == index
                                    Box(
                                        modifier = Modifier
                                            .height(8.dp)
                                            .width(if (isSelected) 24.dp else 8.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) Color.White else Color.White.copy(alpha = 0.5f))
                                    )
                                }
                            }

                            Surface(
                                color = Color.Black.copy(alpha = 0.65f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(bottom = 40.dp, end = 20.dp)
                            ) {
                                Text(
                                    text = "${pagerState.currentPage + 1}/${images.size}",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.White)
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(y = (-28).dp)
                            .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(24.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(50)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = trip.location ?: "Destinazione",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFC107), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "${trip.averageRating}", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                                Text(text = " (${trip.reviewCount} recensioni)", color = Color.Gray, fontSize = 13.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = trip.title,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            lineHeight = 34.sp
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.DateRange, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text("Date Viaggio", fontSize = 11.sp, color = Color.Gray)
                                        Text("${trip.startDate ?: "N/D"}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text("→ ${trip.endDate ?: "N/D"}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF10B981).copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text("Disponibilità", fontSize = 11.sp, color = Color.Gray)
                                        Text("${trip.availableSpots ?: 0} posti", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                                        Text("ancora liberi", fontSize = 11.sp, color = Color(0xFF10B981))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White)
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = "Organizzato da ${trip.organizerName ?: "Agenzia Partner"}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    Text(
                                        text = "Tour Operator Certificato Horizon",
                                        fontSize = 13.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = "Descrizione dell'esperienza",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = trip.description ?: "Nessuna descrizione fornita per questo pacchetto.",
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            lineHeight = 22.sp
                        )
                    }
                }

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
                            .background(Color.White.copy(alpha = 0.85f))
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Indietro", tint = Color.Black)
                    }

                    IconButton(
                        onClick = { isFavorite = !isFavorite },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.85f))
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Preferiti",
                            tint = if (isFavorite) Color.Red else Color.Black
                        )
                    }
                }
            }
        }
    }
}