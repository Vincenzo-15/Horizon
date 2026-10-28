package com.example.travelapp.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.travelapp.network.ChatConversationDTO
import com.example.travelapp.network.ChatMessageDTO
import com.example.travelapp.network.RetrofitClient
import com.example.travelapp.network.SendChatMessageRequest
import com.example.travelapp.network.TripDTO
import com.example.travelapp.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LiveChatTabScreen(
    tokenUtente: String,
    ruoloUtente: String, // "VIAGGIATORE" oppure "ORGANIZZATORE"
    initialTrips: List<TripDTO> = emptyList(),
    onUnreadChatsCountChanged: (Int) -> Unit = {}
) {
    val isOrganizer = ruoloUtente == "ORGANIZZATORE"
    val coroutineScope = rememberCoroutineScope()

    var conversations by remember { mutableStateOf<List<ChatConversationDTO>>(emptyList()) }
    var isLoadingConversations by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }

    // Conversazione attualmente aperta
    var activeConversation by remember { mutableStateOf<ChatConversationDTO?>(null) }
    var messages by remember { mutableStateOf<List<ChatMessageDTO>>(emptyList()) }
    var draftMessage by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }

    var myUserName by remember { mutableStateOf("") }
    var myUserEmail by remember { mutableStateOf("") }

    BackHandler(enabled = activeConversation != null) {
        activeConversation = null
    }

    // Carica i dati del profilo dell'utente loggato
    LaunchedEffect(tokenUtente) {
        if (tokenUtente.isNotBlank()) {
            try {
                val profRes = RetrofitClient.apiService.getMyProfile("Bearer $tokenUtente")
                if (profRes.isSuccessful && profRes.body() != null) {
                    myUserName = profRes.body()!!.name
                    myUserEmail = profRes.body()!!.email
                }
            } catch (_: Exception) {}
        }
    }

    // Polling in tempo reale della lista conversazioni (Inbox stile WhatsApp) quando siamo nella lista
    LaunchedEffect(activeConversation, tokenUtente) {
        if (activeConversation == null && tokenUtente.isNotBlank()) {
            while (activeConversation == null) {
                try {
                    val res = RetrofitClient.apiService.getChatConversations("Bearer $tokenUtente")
                    if (res.isSuccessful && res.body() != null) {
                        conversations = res.body()!!
                        onUnreadChatsCountChanged(conversations.count { it.unreadCount > 0 })
                    }
                } catch (_: Exception) {
                } finally {
                    isLoadingConversations = false
                }
                delay(1500)
            }
        }
    }

    // Polling in tempo reale dei messaggi quando una singola chat è aperta (e li segna come letti)
    LaunchedEffect(activeConversation, tokenUtente) {
        val conv = activeConversation ?: return@LaunchedEffect
        while (activeConversation?.tripId == conv.tripId && activeConversation?.travelerEmail == conv.travelerEmail) {
            try {
                val res = RetrofitClient.apiService.getChatMessages(
                    token = "Bearer $tokenUtente",
                    tripId = conv.tripId,
                    travelerEmail = conv.travelerEmail
                )
                if (res.isSuccessful && res.body() != null) {
                    messages = res.body()!!
                }
            } catch (_: Exception) {}
            delay(1500)
        }
    }

    if (activeConversation == null) {
        // =========================================================================
        // VISTA 1: INBOX STILE WHATSAPP / MESSENGER (SOLO CHAT REALMENTE AVVIATE)
        // =========================================================================
        val filteredConversations = remember(conversations, searchQuery) {
            if (searchQuery.isBlank()) conversations
            else conversations.filter {
                it.agencyName.contains(searchQuery, ignoreCase = true) ||
                        it.travelerName.contains(searchQuery, ignoreCase = true) ||
                        it.tripTitle.contains(searchQuery, ignoreCase = true) ||
                        it.lastMessage.contains(searchQuery, ignoreCase = true)
            }
        }

        val totalUnread = remember(conversations) {
            conversations.sumOf { it.unreadCount }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(TripWarmBg),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .clip(CircleShape)
                                    .background(TripGreen)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "HORIZON DIRECT MESSENGER",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TripGreen,
                                letterSpacing = 1.sp
                            )
                        }

                        if (totalUnread > 0) {
                            Surface(
                                color = TripGreen,
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = "$totalUnread non letti",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Messaggi",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TripBlack
                    )
                    Text(
                        text = if (isOrganizer)
                            "Conversazioni attive con i viaggiatori ordinate per messaggi non letti e orario"
                        else
                            "Le tue conversazioni con le agenzie che hai contattato",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )

                    if (conversations.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = {
                                Text("Cerca")
                            },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TripGreen) },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            if (isLoadingConversations) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = TripGreen)
                    }
                }
            } else if (filteredConversations.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = Color.White,
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(TripGreen.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Email, contentDescription = null, tint = TripGreen, modifier = Modifier.size(32.dp))
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = if (isOrganizer) "Nessun messaggio ricevuto" else "Nessuna chat avviata",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = TripBlack
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (isOrganizer)
                                    "Quando un viaggiatore ti contatterà dalla pagina dettagli di un tuo pacchetto, la chat apparirà qui in tempo reale."
                                else
                                    "Apri la pagina di un viaggio o di un'attività e premi su 'Contatta l'Agenzia Organizzatrice' per avviare una conversazione!",
                                fontSize = 13.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center,
                                lineHeight = 19.sp
                            )
                        }
                    }
                }
            } else {
                items(filteredConversations) { conv ->
                    val matchedTrip = initialTrips.find { it.id == conv.tripId }
                    val hasUnread = conv.unreadCount > 0
                    val counterpartTitle = if (isOrganizer) conv.travelerName else conv.agencyName

                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color.White,
                        shadowElevation = if (hasUnread) 4.dp else 1.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 5.dp)
                            .clickable {
                                messages = emptyList()
                                activeConversation = conv
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Avatar con miniatura del viaggio/agenzia
                            Box(
                                modifier = Modifier
                                    .size(58.dp)
                                    .clip(CircleShape)
                                    .background(TripGreen.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (matchedTrip != null) {
                                    AsyncImage(
                                        model = resolveTripImageUrl(matchedTrip),
                                        contentDescription = conv.tripTitle,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Text(
                                        text = counterpartTitle.take(1).uppercase(),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 22.sp,
                                        color = TripDarkGreen
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                // Prima riga: Nome interlocutore + Orario ultimo messaggio
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = counterpartTitle,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp,
                                        color = TripBlack,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = conv.lastTimestamp,
                                        fontSize = 11.sp,
                                        fontWeight = if (hasUnread) FontWeight.ExtraBold else FontWeight.Normal,
                                        color = if (hasUnread) TripGreen else Color.Gray
                                    )
                                }

                                Spacer(modifier = Modifier.height(2.dp))


                                // Terza riga: Anteprima dell'ultimo messaggio + Badge Verde Non Letti stile WhatsApp
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = conv.lastMessage,
                                        fontSize = 13.sp,
                                        fontWeight = if (hasUnread) FontWeight.ExtraBold else FontWeight.Normal,
                                        color = if (hasUnread) TripBlack else Color.DarkGray,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )

                                    if (hasUnread) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(22.dp)
                                                .clip(CircleShape)
                                                .background(TripGreen),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "${conv.unreadCount}",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.ExtraBold
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
    } else {
        // =========================================================================
        // VISTA 2: STANZA DI CHAT DIRETTA (STILE WHATSAPP)
        // =========================================================================
        val conv = activeConversation!!
        val counterpartName = if (isOrganizer) conv.travelerName else conv.agencyName


        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(TripWarmBg)
        ) {
            // Intestazione Conversazione
            Surface(
                color = Color.White,
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { activeConversation = null },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(TripWarmBg)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Indietro", tint = TripBlack)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = counterpartName,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp,
                            color = TripBlack,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = conv.tripTitle,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TripGreen,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Messaggi della Conversazione
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messages) { msg ->
                    val isMe = (isOrganizer && msg.senderRole == "ORGANIZZATORE") ||
                            (!isOrganizer && msg.senderRole != "ORGANIZZATORE")

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isMe) "Tu" else msg.senderName,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isMe) TripDarkGreen else TripOrangeActivity
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = msg.timestamp,
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Surface(
                            shape = RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = if (isMe) 16.dp else 4.dp,
                                bottomEnd = if (isMe) 4.dp else 16.dp
                            ),
                            color = if (isMe) TripGreen else Color.White,
                            shadowElevation = 1.dp,
                            modifier = Modifier.widthIn(max = 290.dp)
                        ) {
                            Text(
                                text = msg.message,
                                fontSize = 14.sp,
                                color = if (isMe) Color.White else TripBlack,
                                lineHeight = 19.sp,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                            )
                        }
                    }
                }
            }

            // Barra di invio messaggio
            Surface(
                color = Color.White,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = draftMessage,
                            onValueChange = { draftMessage = it },
                            placeholder = { Text("Scrivi un messaggio a $counterpartName...") },
                            singleLine = false,
                            maxLines = 5,
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        IconButton(
                            onClick = {
                                val textToSend = draftMessage.trim()
                                if (textToSend.isNotEmpty() && !isSending) {
                                    draftMessage = ""
                                    coroutineScope.launch {
                                        isSending = true
                                        try {
                                            val res = RetrofitClient.apiService.sendChatMessage(
                                                token = "Bearer $tokenUtente",
                                                tripId = conv.tripId,
                                                request = SendChatMessageRequest(
                                                    message = textToSend,
                                                    travelerEmail = conv.travelerEmail
                                                )
                                            )
                                            if (res.isSuccessful) {
                                                val updated = RetrofitClient.apiService.getChatMessages(
                                                    token = "Bearer $tokenUtente",
                                                    tripId = conv.tripId,
                                                    travelerEmail = conv.travelerEmail
                                                )
                                                if (updated.isSuccessful && updated.body() != null) {
                                                    messages = updated.body()!!
                                                }
                                            }
                                        } catch (_: Exception) {
                                        } finally {
                                            isSending = false
                                        }
                                    }
                                }
                            },
                            enabled = draftMessage.isNotBlank() && !isSending,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(if (draftMessage.isNotBlank()) TripGreen else Color.LightGray)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Invia", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}