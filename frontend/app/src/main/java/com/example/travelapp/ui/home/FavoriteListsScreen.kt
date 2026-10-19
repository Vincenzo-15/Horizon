package com.example.travelapp.ui.home

import android.content.Context
import android.content.Intent
import android.util.Base64
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelapp.network.ItineraryListDTO
import com.example.travelapp.viewmodel.FavoriteListsViewModel
import com.example.travelapp.ui.theme.*
import org.json.JSONObject

// Estrae l'email ("sub") direttamente dal token JWT senza bisogno di modificare TokenManager
fun extractEmailFromJwt(token: String): String? {
    return try {
        val parts = token.split(".")
        if (parts.size < 2) return null
        val payload = String(Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_WRAP))
        JSONObject(payload).optString("sub").takeIf { it.isNotBlank() }
    } catch (e: Exception) {
        null
    }
}

fun shareCapabilityLink(context: Context, list: ItineraryListDTO) {
    val capabilityToken = "cap_${list.id}_${list.name.hashCode().toUInt().toString(16)}"
    val publicUrl = "https://horizontravel.it/lists/shared/${list.id}?capability=$capabilityToken"
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Itinerario Horizon: ${list.name}")
        putExtra(
            Intent.EXTRA_TEXT,
            "Scopri la mia lista di viaggi e attività \"${list.name}\" su Horizon Travel:\n$publicUrl"
        )
    }
    context.startActivity(Intent.createChooser(sendIntent, "Condividi link lista"))
}

@Composable
fun FavoriteListsScreen(
    tokenUtente: String,
    viewModel: FavoriteListsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onListClick: (Long) -> Unit = {}
) {
    var selectedPrivacyFilter by remember { mutableStateOf("ALL") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var listToShareWithFriend by remember { mutableStateOf<ItineraryListDTO?>(null) }
    var listToEdit by remember { mutableStateOf<ItineraryListDTO?>(null) }
    var listToDelete by remember { mutableStateOf<ItineraryListDTO?>(null) }

    val context = LocalContext.current
    val myEmail = remember(tokenUtente) { extractEmailFromJwt(tokenUtente) }

    LaunchedEffect(tokenUtente) {
        viewModel.fetchMyLists(tokenUtente)
    }

    val displayedLists = remember(viewModel.myLists, selectedPrivacyFilter) {
        if (selectedPrivacyFilter == "ALL") {
            viewModel.myLists
        } else {
            viewModel.myLists.filter { it.visibility.equals(selectedPrivacyFilter, ignoreCase = true) }
        }
    }

    // --- POPUP: ELIMINA LISTA ---
    if (listToDelete != null) {
        AlertDialog(
            onDismissRequest = { listToDelete = null },
            containerColor = Color.White,
            title = {
                Text("Elimina Lista", fontWeight = FontWeight.ExtraBold, color = TripBlack)
            },
            text = {
                Text(
                    text = "Sei sicuro di voler eliminare definitivamente la lista \"${listToDelete!!.name}\"? Se è condivisa, non sarà più visibile nemmeno ai tuoi amici.",
                    color = TripBlack
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteList(tokenUtente, listToDelete!!.id)
                        listToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Elimina", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { listToDelete = null }) {
                    Text("Annulla", color = Color.Gray)
                }
            }
        )
    }

    // --- POPUP: MODIFICA LISTA (CAMBIO VISIBILITÀ PRIVATA <-> CONDIVISA E NOME) ---
    if (listToEdit != null) {
        val currentList = listToEdit!!
        var editName by remember(currentList) { mutableStateOf(currentList.name) }
        var editVisibility by remember(currentList) { mutableStateOf(currentList.visibility) }
        var friendEmailToAdd by remember(currentList) { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { listToEdit = null },
            containerColor = Color.White,
            title = {
                Text("Modifica Lista", fontWeight = FontWeight.ExtraBold, color = TripBlack)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Nome della lista") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Cambia visibilità", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TripBlack)

                    PrivacyOptionRow(
                        title = "Privata",
                        subtitle = "Solo tu (revoca l'accesso agli amici condivisi)",
                        icon = Icons.Default.Lock,
                        selected = editVisibility == "PRIVATE",
                        onClick = { editVisibility = "PRIVATE" }
                    )
                    PrivacyOptionRow(
                        title = "Condivisa con amici",
                        subtitle = "Accessibile agli amici invitati via email",
                        icon = Icons.Default.Person,
                        selected = editVisibility == "SHARED",
                        onClick = { editVisibility = "SHARED" }
                    )
                    PrivacyOptionRow(
                        title = "Pubblica tramite link",
                        subtitle = "Accessibile tramite link con capability",
                        icon = Icons.Default.Share,
                        selected = editVisibility == "PUBLIC",
                        onClick = { editVisibility = "PUBLIC" }
                    )

                    if (editVisibility == "SHARED") {
                        OutlinedTextField(
                            value = friendEmailToAdd,
                            onValueChange = { friendEmailToAdd = it },
                            label = { Text("Aggiungi email amico (opzionale)") },
                            placeholder = { Text("es. amico@email.it") },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = TripGreen) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateList(
                            token = tokenUtente,
                            listId = currentList.id,
                            newName = editName,
                            newVisibility = editVisibility,
                            friendEmailToShare = friendEmailToAdd
                        )
                        listToEdit = null
                    },
                    enabled = editName.isNotBlank() && !viewModel.isSubmitting,
                    colors = ButtonDefaults.buttonColors(containerColor = TripGreen),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Salva Modifiche", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { listToEdit = null }) {
                    Text("Annulla", color = Color.Gray)
                }
            }
        )
    }

    // --- POPUP: CREA NUOVA LISTA ---
    if (showCreateDialog) {
        var newName by remember { mutableStateOf("") }
        var newVisibility by remember { mutableStateOf("PRIVATE") }
        var initialFriendEmail by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            containerColor = Color.White,
            title = {
                Text("Nuova Lista Itinerari", fontWeight = FontWeight.ExtraBold, color = TripBlack)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Nome lista (es. Estate con amici)") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Livello di visibilità", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TripBlack)

                    PrivacyOptionRow(
                        title = "Privata",
                        subtitle = "Visibile esclusivamente a te",
                        icon = Icons.Default.Lock,
                        selected = newVisibility == "PRIVATE",
                        onClick = { newVisibility = "PRIVATE" }
                    )
                    PrivacyOptionRow(
                        title = "Condivisa con amici",
                        subtitle = "Accessibile solo a utenti specifici tramite email",
                        icon = Icons.Default.Person,
                        selected = newVisibility == "SHARED",
                        onClick = { newVisibility = "SHARED" }
                    )
                    PrivacyOptionRow(
                        title = "Pubblica tramite link",
                        subtitle = "Accessibile a chiunque possieda il link capability",
                        icon = Icons.Default.Share,
                        selected = newVisibility == "PUBLIC",
                        onClick = { newVisibility = "PUBLIC" }
                    )

                    if (newVisibility == "SHARED") {
                        OutlinedTextField(
                            value = initialFriendEmail,
                            onValueChange = { initialFriendEmail = it },
                            label = { Text("Email amico (opzionale)") },
                            placeholder = { Text("es. mario@email.it") },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = TripGreen) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.createNewList(
                            token = tokenUtente,
                            name = newName,
                            visibility = newVisibility,
                            initialFriendEmail = initialFriendEmail
                        )
                        showCreateDialog = false
                    },
                    enabled = newName.isNotBlank() && !viewModel.isSubmitting,
                    colors = ButtonDefaults.buttonColors(containerColor = TripGreen),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Crea Lista", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Annulla", color = Color.Gray)
                }
            }
        )
    }

    // --- POPUP: CONDIVIDI LISTA CON UN AMICO TRAMITE EMAIL ---
    if (listToShareWithFriend != null) {
        val targetList = listToShareWithFriend!!
        var friendEmail by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { listToShareWithFriend = null },
            containerColor = Color.White,
            title = {
                Text("Condividi con un amico", fontWeight = FontWeight.ExtraBold, color = TripBlack)
            },
            text = {
                Column {
                    Text(
                        text = "Inserisci l'email dell'utente registrato a cui vuoi dare accesso alla lista \"${targetList.name}\":",
                        fontSize = 13.sp,
                        color = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = friendEmail,
                        onValueChange = { friendEmail = it },
                        label = { Text("Email dell'amico") },
                        placeholder = { Text("es. luca@email.it") },
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
                        viewModel.shareListWithFriend(
                            token = tokenUtente,
                            listId = targetList.id,
                            listName = targetList.name,
                            friendEmail = friendEmail
                        )
                        listToShareWithFriend = null
                    },
                    enabled = friendEmail.contains("@") && !viewModel.isSubmitting,
                    colors = ButtonDefaults.buttonColors(containerColor = TripGreen),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Condividi", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { listToShareWithFriend = null }) {
                    Text("Annulla", color = Color.Gray)
                }
            }
        )
    }

    // --- POPUP DI CONFERMA / ERRORE ---
    if (viewModel.feedbackMessage != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearFeedback() },
            containerColor = Color.White,
            title = {
                Text(
                    text = if (viewModel.isFeedbackError) "Attenzione" else "Operazione completata",
                    fontWeight = FontWeight.ExtraBold,
                    color = if (viewModel.isFeedbackError) MaterialTheme.colorScheme.error else TripDarkGreen
                )
            },
            text = { Text(viewModel.feedbackMessage!!, color = TripBlack) },
            confirmButton = {
                Button(
                    onClick = { viewModel.clearFeedback() },
                    colors = ButtonDefaults.buttonColors(containerColor = TripGreen)
                ) {
                    Text("OK", color = Color.White)
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(TripWarmBg),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // 1. HEADER TRIPADVISOR + PULSANTE NUOVA LISTA
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Le tue Liste",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TripBlack
                    )
                    Text(
                        text = "Itinerari privati, condivisi con amici o pubblici",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }

                Button(
                    onClick = { showCreateDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = TripGreen),
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Nuova Lista", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                }
            }
        }

        // 2. FILTRI PER VISIBILITÀ (Tutte / Private / Condivise / Pubbliche)
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    OfferTypeChip("Tutte (${viewModel.myLists.size})", selectedPrivacyFilter == "ALL") {
                        selectedPrivacyFilter = "ALL"
                    }
                }
                item {
                    OfferTypeChip("🔒 Private", selectedPrivacyFilter == "PRIVATE") {
                        selectedPrivacyFilter = "PRIVATE"
                    }
                }
                item {
                    OfferTypeChip("👥 Condivise", selectedPrivacyFilter == "SHARED") {
                        selectedPrivacyFilter = "SHARED"
                    }
                }
                item {
                    OfferTypeChip("🌐 Pubbliche", selectedPrivacyFilter == "PUBLIC") {
                        selectedPrivacyFilter = "PUBLIC"
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // 3. ELENCO DELLE LISTE
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
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                )
            }
        } else if (displayedLists.isEmpty()) {
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
                            imageVector = Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = TripGreen
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Nessuna lista in questa sezione",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TripBlack
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Crea una lista privata o condivisa con i tuoi amici per salvare viaggi e attività.",
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        } else {
            items(displayedLists) { list ->
                val isPrivate = list.visibility == "PRIVATE"
                val badgeColor = when (list.visibility) {
                    "PRIVATE" -> Color.DarkGray
                    "SHARED" -> TripGreen
                    else -> TripOrangeActivity
                }
                val badgeText = when (list.visibility) {
                    "PRIVATE" -> "🔒 PRIVATA"
                    "SHARED" -> "👥 CONDIVISA CON AMICI"
                    else -> "🌐 PUBBLICA (LINK)"
                }

                // Verifica se l'utente loggato è il creatore della lista
                val isOwner = remember(list, myEmail) {
                    if (list.ownerEmail.isNullOrBlank() || myEmail.isNullOrBlank()) true
                    else list.ownerEmail.equals(myEmail, ignoreCase = true)
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    shadowElevation = 3.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                        .clickable { onListClick(list.id) }
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(badgeColor.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (list.visibility) {
                                        "PRIVATE" -> Icons.Default.Lock
                                        "SHARED" -> Icons.Default.Person
                                        else -> Icons.Default.Share
                                    },
                                    contentDescription = null,
                                    tint = badgeColor,
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Surface(
                                    color = badgeColor.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = badgeText,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = badgeColor,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = list.name,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TripBlack,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${list.tripIds?.size ?: 0} esperienze salvate • Creata da: ${list.ownerName ?: "Te"}",
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Icon(
                                Icons.Default.KeyboardArrowRight,
                                contentDescription = "Apri",
                                tint = Color.Gray
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.35f))
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isOwner) {
                                TextButton(onClick = { listToShareWithFriend = list }) {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        tint = TripGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isPrivate) "Condividi con amico" else "Invita amico",
                                        color = TripGreen,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        tint = TripGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Condivisa da ${list.ownerName ?: "un amico"}",
                                        fontSize = 13.sp,
                                        color = TripDarkGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (!isPrivate) {
                                    IconButton(onClick = { shareCapabilityLink(context, list) }) {
                                        Icon(
                                            Icons.Default.Share,
                                            contentDescription = "Link Capability",
                                            tint = TripBlack,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                if (isOwner) {
                                    // Pulsante Modifica Lista (cambia da Privata a Condivisa o viceversa)
                                    IconButton(onClick = { listToEdit = list }) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "Modifica Lista",
                                            tint = TripGreen,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    // Pulsante Elimina Lista
                                    IconButton(onClick = { listToDelete = list }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Elimina Lista",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PrivacyOptionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (selected) TripGreen.copy(alpha = 0.12f) else TripWarmBg,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(selectedColor = TripGreen)
            )
            Icon(
                icon,
                contentDescription = null,
                tint = if (selected) TripGreen else Color.Gray,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TripBlack)
                Text(subtitle, fontSize = 11.sp, color = Color.DarkGray)
            }
        }
    }
}