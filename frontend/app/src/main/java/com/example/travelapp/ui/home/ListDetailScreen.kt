package com.example.travelapp.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelapp.network.RetrofitClient
import com.example.travelapp.viewmodel.ListDetailViewModel
import com.example.travelapp.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListDetailScreen(
    listId: Long,
    tokenUtente: String,
    onNavigateBack: () -> Unit,
    onTripClick: (Long) -> Unit,
    viewModel: ListDetailViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    var showShareDialog by remember { mutableStateOf(false) }
    var shareMessage by remember { mutableStateOf<String?>(null) }
    var isSharing by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(listId) {
        viewModel.fetchListDetails(tokenUtente, listId)
    }

    val details = viewModel.listDetails

    // --- POPUP INVITA AMICO TRAMITE EMAIL ---
    if (showShareDialog) {
        var friendEmail by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showShareDialog = false },
            containerColor = Color.White,
            title = {
                Text("Invita un amico nella lista", fontWeight = FontWeight.ExtraBold, color = TripBlack)
            },
            text = {
                Column {
                    Text(
                        text = "Inserisci l'indirizzo email dell'utente con cui vuoi condividere \"${details?.name ?: "questa lista"}\":",
                        fontSize = 13.sp,
                        color = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = friendEmail,
                        onValueChange = { friendEmail = it },
                        label = { Text("Email amico registrato") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = TripGreen) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            isSharing = true
                            try {
                                val res = RetrofitClient.apiService.shareList(
                                    "Bearer $tokenUtente",
                                    listId,
                                    friendEmail.trim()
                                )
                                shareMessage = if (res.isSuccessful) {
                                    "Lista condivisa con successo con $friendEmail!"
                                } else {
                                    "Utente non trovato o errore nella condivisione (${res.code()})."
                                }
                            } catch (e: Exception) {
                                shareMessage = "Errore di connessione al server."
                            } finally {
                                isSharing = false
                                showShareDialog = false
                                viewModel.fetchListDetails(tokenUtente, listId)
                            }
                        }
                    },
                    enabled = friendEmail.contains("@") && !isSharing,
                    colors = ButtonDefaults.buttonColors(containerColor = TripGreen)
                ) {
                    Text("Invita", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showShareDialog = false }) {
                    Text("Annulla", color = Color.Gray)
                }
            }
        )
    }

    if (shareMessage != null) {
        AlertDialog(
            onDismissRequest = { shareMessage = null },
            containerColor = Color.White,
            text = { Text(shareMessage!!, color = TripBlack) },
            confirmButton = {
                Button(
                    onClick = { shareMessage = null },
                    colors = ButtonDefaults.buttonColors(containerColor = TripGreen)
                ) {
                    Text("OK", color = Color.White)
                }
            }
        )
    }

    Scaffold(
        containerColor = TripWarmBg,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
                title = {
                    Column {
                        Text(
                            text = details?.name ?: "Dettaglio Lista",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 19.sp,
                            color = TripBlack
                        )
                        if (details != null) {
                            Text(
                                text = when (details.visibility) {
                                    "PRIVATE" -> "🔒 Lista Privata"
                                    "SHARED" -> "👥 Lista Condivisa con amici"
                                    else -> "🌐 Lista Pubblica"
                                },
                                fontSize = 12.sp,
                                color = TripGreen,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Indietro", tint = TripBlack)
                    }
                },
                actions = {
                    IconButton(onClick = { showShareDialog = true }) {
                        Icon(Icons.Default.Person, contentDescription = "Invita Amico", tint = TripGreen)
                    }
                    if (details != null) {
                        IconButton(onClick = { shareCapabilityLink(context, details) }) {
                            Icon(Icons.Default.Share, contentDescription = "Condividi Link", tint = TripBlack)
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (viewModel.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = TripGreen)
                }
            } else if (viewModel.errorMessage != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(viewModel.errorMessage!!, color = MaterialTheme.colorScheme.error)
                }
            } else if (viewModel.tripsInList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White,
                        shadowElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.FavoriteBorder, contentDescription = null, tint = TripGreen, modifier = Modifier.size(56.dp))
                            Spacer(modifier = Modifier.height(14.dp))
                            Text("Questa lista è ancora vuota", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = TripBlack)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Esplora i viaggi e le attività nella Home e premi sul cuore in alto a destra per salvarli qui!",
                                fontSize = 13.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
                ) {
                    item {
                        SectionHeader(
                            title = "Itinerari salvati (${viewModel.tripsInList.size})",
                            subtitle = "Tocca un'offerta per visualizzare tutti i dettagli o prenotare"
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    items(viewModel.tripsInList) { trip ->
                        TripAdvisorFullCard(
                            trip = trip,
                            onClick = { onTripClick(trip.id) }
                        )
                    }
                }
            }
        }
    }
}