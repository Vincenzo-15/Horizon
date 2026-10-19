package com.example.travelapp.ui.profile

import android.content.Context
import android.content.Intent
import android.util.Base64
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.travelapp.network.ChangePasswordRequest
import com.example.travelapp.network.RetrofitClient
import com.example.travelapp.network.UpdateProfileRequest
import com.example.travelapp.network.UserProfileDTO
import com.example.travelapp.ui.theme.*
import kotlinx.coroutines.launch
import org.json.JSONObject



// Pagine interne della sezione Profilo
private enum class ProfileDestination {
    MAIN,
    ACCOUNT_SETTINGS,
    PRIVACY_SECURITY,
    HELP_CENTER,
    PARTNER_PLAN
}

fun extractFullEmailFromJwt(token: String): String {
    return try {
        val parts = token.split(".")
        if (parts.size < 2) return ""
        val payload = String(Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_WRAP))
        JSONObject(payload).optString("sub")
    } catch (e: Exception) {
        ""
    }
}

fun extractFallbackNameFromJwt(token: String): String {
    return try {
        val parts = token.split(".")
        if (parts.size < 2) return "Utente"
        val payload = String(Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_WRAP))
        val json = JSONObject(payload)
        json.optString("name").takeIf { it.isNotBlank() }
            ?: json.optString("sub").substringBefore("@").replaceFirstChar { it.uppercase() }
    } catch (e: Exception) {
        "Utente Horizon"
    }
}

@Composable
fun ProfileScreen(
    ruoloUtente: String?,
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val tokenManager = remember { com.example.travelapp.utils.TokenManager(context) }
    val currentToken = remember { tokenManager.getToken() ?: "" }
    val fallbackEmail = remember(currentToken) { extractFullEmailFromJwt(currentToken) }
    val fallbackName = remember(currentToken) { extractFallbackNameFromJwt(currentToken) }

    // Stato del profilo caricato dal Backend
    var userProfile by remember {
        mutableStateOf(
            UserProfileDTO(
                id = 0L,
                email = fallbackEmail,
                name = fallbackName,
                role = ruoloUtente ?: "VIAGGIATORE",
                telefono = "",
                partitaIva = ""
            )
        )
    }
    var isLoadingProfile by remember { mutableStateOf(false) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    var isFeedbackError by remember { mutableStateOf(false) }

    // Navigazione tra le sottopagine del profilo
    var currentPage by remember { mutableStateOf(ProfileDestination.MAIN) }

    // Intercetta il tasto Indietro del telefono se siamo in una sottopagina
    BackHandler(enabled = currentPage != ProfileDestination.MAIN) {
        currentPage = ProfileDestination.MAIN
    }

    // Carica i dati reali dell'utente dal server all'apertura
    LaunchedEffect(currentToken) {
        if (currentToken.isNotBlank()) {
            isLoadingProfile = true
            try {
                val res = RetrofitClient.apiService.getMyProfile("Bearer $currentToken")
                if (res.isSuccessful && res.body() != null) {
                    userProfile = res.body()!!
                }
            } catch (_: Exception) {
                // Mantiene il fallback estratto dal JWT se il backend non risponde
            } finally {
                isLoadingProfile = false
            }
        }
    }

    // Dialog di conferma/errore per le operazioni
    if (feedbackMessage != null) {
        AlertDialog(
            onDismissRequest = { feedbackMessage = null },
            containerColor = Color.White,
            title = {
                Text(
                    text = if (isFeedbackError) "Attenzione" else "Operazione completata",
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isFeedbackError) MaterialTheme.colorScheme.error else TripDarkGreen
                )
            },
            text = { Text(feedbackMessage!!, color = TripBlack) },
            confirmButton = {
                Button(
                    onClick = { feedbackMessage = null },
                    colors = ButtonDefaults.buttonColors(containerColor = TripGreen)
                ) {
                    Text("OK", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Transizione scorrevole orizzontale tra la pagina principale e le sottopagine dedicate
    AnimatedContent(
        targetState = currentPage,
        transitionSpec = {
            if (targetState != ProfileDestination.MAIN) {
                (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                    slideOutHorizontally { width -> -width / 3 } + fadeOut()
                )
            } else {
                (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                    slideOutHorizontally { width -> width / 3 } + fadeOut()
                )
            }
        },
        label = "ProfilePageTransition"
    ) { page ->
        when (page) {
            ProfileDestination.MAIN -> {
                ProfileMainPage(
                    userProfile = userProfile,
                    isLoading = isLoadingProfile,
                    onNavigateBack = onNavigateBack,
                    onOpenPage = { destination -> currentPage = destination },
                    onLogout = onLogout
                )
            }

            ProfileDestination.ACCOUNT_SETTINGS -> {
                AccountSettingsPage(
                    userProfile = userProfile,
                    onBack = { currentPage = ProfileDestination.MAIN },
                    onSaveProfile = { newName, newPhone, newPiva ->
                        coroutineScope.launch {
                            try {
                                val res = RetrofitClient.apiService.updateMyProfile(
                                    "Bearer $currentToken",
                                    UpdateProfileRequest(
                                        name = newName,
                                        telefono = newPhone,
                                        partitaIva = newPiva
                                    )
                                )
                                if (res.isSuccessful && res.body() != null) {
                                    userProfile = res.body()!!
                                    isFeedbackError = false
                                    feedbackMessage = "Le impostazioni del tuo account sono state aggiornate con successo!"
                                    currentPage = ProfileDestination.MAIN
                                } else {
                                    isFeedbackError = true
                                    feedbackMessage = "Impossibile salvare le modifiche (${res.code()})."
                                }
                            } catch (e: Exception) {
                                isFeedbackError = true
                                feedbackMessage = "Errore di connessione al server."
                            }
                        }
                    }
                )
            }

            ProfileDestination.PRIVACY_SECURITY -> {
                PrivacySecurityPage(
                    userEmail = userProfile.email,
                    onBack = { currentPage = ProfileDestination.MAIN },
                    onChangePassword = { oldPass, newPass ->
                        coroutineScope.launch {
                            try {
                                val res = RetrofitClient.apiService.changePassword(
                                    "Bearer $currentToken",
                                    ChangePasswordRequest(oldPassword = oldPass, newPassword = newPass)
                                )
                                if (res.isSuccessful) {
                                    isFeedbackError = false
                                    feedbackMessage = "Password modificata con successo!"
                                    currentPage = ProfileDestination.MAIN
                                } else {
                                    val errBody = res.errorBody()?.string()
                                    isFeedbackError = true
                                    feedbackMessage = if (!errBody.isNullOrBlank()) errBody else "Verifica che la password attuale sia corretta."
                                }
                            } catch (e: Exception) {
                                isFeedbackError = true
                                feedbackMessage = "Errore di connessione durante il cambio password."
                            }
                        }
                    }
                )
            }

            ProfileDestination.HELP_CENTER -> {
                HelpCenterPage(
                    onBack = { currentPage = ProfileDestination.MAIN }
                )
            }

            ProfileDestination.PARTNER_PLAN -> {
                PartnerPlanPage(
                    userProfile = userProfile,
                    onBack = { currentPage = ProfileDestination.MAIN },
                    onEditFiscalData = { currentPage = ProfileDestination.ACCOUNT_SETTINGS }
                )
            }
        }
    }
}

// =========================================================================
// 1. PAGINA PRINCIPALE DEL PROFILO (OMOGENEA ALLA HOME)
// =========================================================================
@Composable
private fun ProfileMainPage(
    userProfile: UserProfileDTO,
    isLoading: Boolean,
    onNavigateBack: () -> Unit,
    onOpenPage: (ProfileDestination) -> Unit,
    onLogout: () -> Unit
) {
    val isOrganizer = userProfile.role == "ORGANIZZATORE"
    val context = LocalContext.current

    val sharedPrefs = remember { context.getSharedPreferences("profile_prefs", Context.MODE_PRIVATE) }
    var profileImageUri by remember(userProfile.email) {
        mutableStateOf(sharedPrefs.getString("profile_image_${userProfile.email}", null))
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
            profileImageUri = uri.toString()
            sharedPrefs.edit().putString("profile_image_${userProfile.email}", uri.toString()).apply()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(TripWarmBg),
        contentPadding = PaddingValues(bottom = 36.dp)
    ) {
        // HEADER IN STILE HOMESCREEN
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
                            text = "HORIZON TRAVEL • PROFILO",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TripGreen,
                            letterSpacing = 1.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Il tuo Account",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TripBlack
                    )
                }

                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Indietro", tint = TripBlack)
                }
            }
        }

        // CARD IDENTITÀ UTENTE + FOTO PROFILO
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                shadowElevation = 4.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clickable {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(if (isOrganizer) TripBlack else TripGreen.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (profileImageUri != null) {
                                AsyncImage(
                                    model = profileImageUri,
                                    contentDescription = "Foto Profilo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    imageVector = if (isOrganizer) Icons.Default.Star else Icons.Default.Person,
                                    contentDescription = null,
                                    tint = if (isOrganizer) TripAmber else TripGreen,
                                    modifier = Modifier.size(52.dp)
                                )
                            }
                        }

                        // Badge Modifica Foto
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .padding(3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(TripGreen),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Cambia foto",
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Surface(
                        color = if (isOrganizer) TripOrangeActivity.copy(alpha = 0.12f) else TripGreen.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = if (isOrganizer) "AGENZIA PARTNER" else "VIAGGIATORE HORIZON",
                            color = if (isOrganizer) TripOrangeActivity else TripDarkGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = userProfile.name,
                        fontSize = 23.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TripBlack
                    )

                    if (userProfile.email.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = userProfile.email,
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }

                    if (!userProfile.telefono.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Phone, contentDescription = null, tint = TripGreen, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = userProfile.telefono, fontSize = 13.sp, color = Color.DarkGray, fontWeight = FontWeight.Medium)
                        }
                    }

                    if (isLoading) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(color = TripGreen, modifier = Modifier.fillMaxWidth(0.5f))
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // SEZIONE MENU DEDICATO
        item {
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Text(
                    text = "Gestione e Preferenze",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TripBlack
                )
                Text(
                    text = "Tocca una sezione per aprire la pagina dedicata",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        item {
            ProfileNavigationCard(
                icon = Icons.Default.Person,
                title = "Impostazioni Account",
                subtitle = "Modifica nome, recapito telefonico e dati personali",
                onClick = { onOpenPage(ProfileDestination.ACCOUNT_SETTINGS) }
            )
            ProfileNavigationCard(
                icon = Icons.Default.Lock,
                title = "Privacy e Sicurezza",
                subtitle = "Cambia password, visibilità liste e notifiche",
                onClick = { onOpenPage(ProfileDestination.PRIVACY_SECURITY) }
            )
            ProfileNavigationCard(
                icon = Icons.Default.Info,
                title = "Centro Assistenza",
                subtitle = "Domande frequenti su prenotazioni, file .ics e liste",
                onClick = { onOpenPage(ProfileDestination.HELP_CENTER) }
            )
            if (isOrganizer) {
                ProfileNavigationCard(
                    icon = Icons.Default.Star,
                    title = "Piano Partner & Dati Agenzia",
                    subtitle = "Stato verifica partner, Partita IVA e vantaggi",
                    onClick = { onOpenPage(ProfileDestination.PARTNER_PLAN) }
                )
            }
        }

        // PULSANTE DISCONNETTITI BEN VISIBILE
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onLogout,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape = RoundedCornerShape(18.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(58.dp)
            ) {
                Icon(
                    Icons.Default.ExitToApp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Disconnettiti dall'account",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun ProfileNavigationCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(TripGreen.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = TripGreen, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TripBlack
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            Icon(
                Icons.Default.KeyboardArrowRight,
                contentDescription = "Apri",
                tint = Color.Gray
            )
        }
    }
}

// =========================================================================
// 2. PAGINA DEDICATA: IMPOSTAZIONI ACCOUNT
// =========================================================================
@Composable
private fun AccountSettingsPage(
    userProfile: UserProfileDTO,
    onBack: () -> Unit,
    onSaveProfile: (name: String, phone: String?, partitaIva: String?) -> Unit
) {
    val isOrganizer = userProfile.role == "ORGANIZZATORE"
    var name by remember(userProfile) { mutableStateOf(userProfile.name) }
    var phone by remember(userProfile) { mutableStateOf(userProfile.telefono ?: "") }
    var partitaIva by remember(userProfile) { mutableStateOf(userProfile.partitaIva ?: "") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(TripWarmBg),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            SubPageHeader(
                badge = "IMPOSTAZIONI PROFILO",
                title = "Impostazioni Account",
                subtitle = "Aggiorna i tuoi dati personali salvati su Horizon",
                onBack = onBack
            )
        }

        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = Color.White,
                shadowElevation = 3.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Informazioni Anagrafiche",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TripBlack
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(if (isOrganizer) "Nome Agenzia / Organizzatore" else "Nome e Cognome") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = TripGreen) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = userProfile.email,
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        label = { Text("Indirizzo Email (Account Verificato)") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = TripGreen) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Recapito Telefonico") },
                        placeholder = { Text("es. +39 333 1234567") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = TripGreen) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (isOrganizer) {
                        OutlinedTextField(
                            value = partitaIva,
                            onValueChange = { partitaIva = it },
                            label = { Text("Partita IVA Agenzia") },
                            leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, tint = TripGreen) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Button(
                        onClick = { onSaveProfile(name, phone, partitaIva) },
                        enabled = name.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = TripGreen),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Salva Modifiche", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    }
                }
            }
        }
    }
}

// =========================================================================
// 3. PAGINA DEDICATA: PRIVACY E SICUREZZA
// =========================================================================
@Composable
private fun PrivacySecurityPage(
    userEmail: String,
    onBack: () -> Unit,
    onChangePassword: (oldPass: String, newPass: String) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("privacy_prefs_$userEmail", Context.MODE_PRIVATE) }

    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPasswords by remember { mutableStateOf(false) }

    var notifySharedLists by remember { mutableStateOf(prefs.getBoolean("notify_shared", true)) }
    var notifyTripReminders by remember { mutableStateOf(prefs.getBoolean("notify_reminders", true)) }

    val passwordMismatch = confirmPassword.isNotEmpty() && newPassword != confirmPassword

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(TripWarmBg),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            SubPageHeader(
                badge = "SICUREZZA ACCOUNT",
                title = "Privacy e Sicurezza",
                subtitle = "Gestisci la tua password e i permessi di condivisione",
                onBack = onBack
            )
        }

        // CARD CAMBIO PASSWORD REALE
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = Color.White,
                shadowElevation = 3.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Modifica Password",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TripBlack
                        )
                        TextButton(onClick = { showPasswords = !showPasswords }) {
                            Text(
                                text = if (showPasswords) "Nascondi" else "Mostra",
                                color = TripGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    val transformation = if (showPasswords) VisualTransformation.None else PasswordVisualTransformation()

                    OutlinedTextField(
                        value = oldPassword,
                        onValueChange = { oldPassword = it },
                        label = { Text("Password attuale") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = TripGreen) },
                        visualTransformation = transformation,
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("Nuova password (min. 8 caratteri)") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = TripGreen) },
                        visualTransformation = transformation,
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Conferma nuova password") },
                        leadingIcon = { Icon(Icons.Default.Check, contentDescription = null, tint = TripGreen) },
                        visualTransformation = transformation,
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (passwordMismatch) {
                        Text(
                            text = "Le due password non coincidono.",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    }

                    Button(
                        onClick = { onChangePassword(oldPassword, newPassword) },
                        enabled = oldPassword.isNotBlank() && newPassword.length >= 8 && newPassword == confirmPassword,
                        colors = ButtonDefaults.buttonColors(containerColor = TripGreen),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text("Aggiorna Password", fontWeight = FontWeight.ExtraBold, color = Color.White, fontSize = 15.sp)
                    }
                }
            }
        }

        // CARD PREFERENZE PRIVACY E NOTIFICHE
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = Color.White,
                shadowElevation = 3.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Column(modifier = Modifier.padding(22.dp)) {
                    Text(
                        text = "Preferenze Privacy e Notifiche",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TripBlack
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Notifiche Liste Condivise", fontWeight = FontWeight.Bold, color = TripBlack, fontSize = 15.sp)
                            Text("Ricevi un avviso quando un amico condivide un itinerario con te", fontSize = 12.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = notifySharedLists,
                            onCheckedChange = {
                                notifySharedLists = it
                                prefs.edit().putBoolean("notify_shared", it).apply()
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = TripGreen)
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.LightGray.copy(alpha = 0.4f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Promemoria Prenotazioni (.ics)", fontWeight = FontWeight.Bold, color = TripBlack, fontSize = 15.sp)
                            Text("Mostra avvisi e suggerimenti di esportazione calendario", fontSize = 12.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = notifyTripReminders,
                            onCheckedChange = {
                                notifyTripReminders = it
                                prefs.edit().putBoolean("notify_reminders", it).apply()
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = TripGreen)
                        )
                    }
                }
            }
        }
    }
}

// =========================================================================
// 4. PAGINA DEDICATA: CENTRO ASSISTENZA
// =========================================================================
@Composable
private fun HelpCenterPage(
    onBack: () -> Unit
) {
    val faqs = listOf(
        "Come funzionano le Liste Preferiti Condivise?" to
                "Dalla sezione Preferiti puoi creare liste Private, Condivise o Pubbliche. Inserendo l'email di un amico registrato, lui riceverà una notifica sulla campanella della Home e vedrà la lista con il tuo nome come creatore.",
        "Se rendo Privata o elimino una lista condivisa cosa succede?" to
                "Solo il creatore della lista può modificarne la visibilità o eliminarla. Se riporti una lista da Condivisa a Privata o la elimini, gli amici non avranno più accesso.",
        "Come esporto una prenotazione nel Calendario (.ics)?" to
                "Vai nella tab 'Viaggi' della barra inferiore e premi su 'Esporta Calendario (.ics)' sotto la tua prenotazione per generare e condividere lo standard iCalendar.",
        "Qual è la differenza tra Viaggi e Attività?" to
                "I Viaggi coprono pacchetti di più giorni con data di partenza e ritorno, mentre le Attività ed Escursioni si svolgono in una singola giornata."
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(TripWarmBg),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            SubPageHeader(
                badge = "SUPPORTO VIAGGIATORI E AGENZIE",
                title = "Centro Assistenza",
                subtitle = "Guide rapide all'utilizzo di Horizon Travel",
                onBack = onBack
            )
        }

        items(faqs.size) { index ->
            val (question, answer) = faqs[index]
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color.White,
                shadowElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 6.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = TripGreen, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = question,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = TripBlack
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = answer,
                        fontSize = 13.sp,
                        color = Color.DarkGray,
                        lineHeight = 19.sp
                    )
                }
            }
        }
    }
}

// =========================================================================
// 5. PAGINA DEDICATA: PIANO PARTNER (SOLO ORGANIZZATORE)
// =========================================================================
@Composable
private fun PartnerPlanPage(
    userProfile: UserProfileDTO,
    onBack: () -> Unit,
    onEditFiscalData: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(TripWarmBg),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            SubPageHeader(
                badge = "AGENZIA ORGANIZZATRICE",
                title = "Piano Partner Horizon",
                subtitle = "Riepilogo abilitazioni per la pubblicazione di viaggi e attività",
                onBack = onBack
            )
        }

        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = Color.White,
                shadowElevation = 3.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Column(modifier = Modifier.padding(22.dp)) {
                    Surface(
                        color = TripGreen.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "✓ ACCOUNT PARTNER VERIFICATO",
                            color = TripDarkGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = userProfile.name,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TripBlack
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Partita IVA: ${userProfile.partitaIva?.takeIf { it.isNotBlank() } ?: "Non specificata"}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.DarkGray
                    )
                    Text(
                        text = "Telefono Agenzia: ${userProfile.telefono?.takeIf { it.isNotBlank() } ?: "Non specificato"}",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(18.dp))
                    Button(
                        onClick = onEditFiscalData,
                        colors = ButtonDefaults.buttonColors(containerColor = TripGreen),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Modifica Dati Fiscali in Impostazioni Account", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// =========================================================================
// HEADER CONDIVISO PER LE SOTTOPAGINE (STILE HOMESCREEN)
// =========================================================================
@Composable
private fun SubPageHeader(
    badge: String,
    title: String,
    subtitle: String,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.White)
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Indietro", tint = TripBlack)
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = badge,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TripGreen,
                letterSpacing = 1.sp
            )
            Text(
                text = title,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TripBlack
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}