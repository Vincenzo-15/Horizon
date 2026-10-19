package com.example.travelapp.ui.login

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelapp.R
import com.example.travelapp.ui.theme.*
import com.example.travelapp.utils.TokenManager
import com.example.travelapp.viewmodel.LoginViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: LoginViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onNavigateToRegister: () -> Unit = {},
    onNavigateToForgotPassword: () -> Unit = {},
    onLoginSuccess: () -> Unit = {}
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    var showForgotPasswordSheet by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }

    LaunchedEffect(viewModel.loginSuccess) {
        if (viewModel.loginSuccess) {
            val tokenDaSalvare = viewModel.token
            if (!tokenDaSalvare.isNullOrEmpty()) {
                tokenManager.saveToken(tokenDaSalvare)
                Log.d("AUTH_DEBUG", "Token salvato con successo nella memoria!")
            }
            onLoginSuccess()
        }
    }

    // --- BOTTOM SHEET: RECUPERO PASSWORD FUNZIONANTE ---
    if (showForgotPasswordSheet) {
        var resetEmail by remember { mutableStateOf(email) }
        var newPass by remember { mutableStateOf("") }
        var confirmNewPass by remember { mutableStateOf("") }
        var showResetPass by remember { mutableStateOf(false) }

        ModalBottomSheet(
            onDismissRequest = { showForgotPasswordSheet = false },
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(TripGreen)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "SICUREZZA HORIZON",
                        style = MaterialTheme.typography.labelSmall,
                        color = TripGreen
                    )
                }

                Text(
                    text = "Reimposta la tua Password",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TripBlack
                )
                Text(
                    text = "Inserisci l'indirizzo email associato al tuo account e scegli una nuova password sicura.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )

                OutlinedTextField(
                    value = resetEmail,
                    onValueChange = { resetEmail = it },
                    label = { Text("Email registrata") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = TripGreen) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                val resetVisualTransformation = if (showResetPass) VisualTransformation.None else PasswordVisualTransformation()

                OutlinedTextField(
                    value = newPass,
                    onValueChange = { newPass = it },
                    label = { Text("Nuova Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = TripGreen) },
                    trailingIcon = {
                        TextButton(onClick = { showResetPass = !showResetPass }) {
                            Text(
                                text = if (showResetPass) "Nascondi" else "Mostra",
                                color = TripGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    },
                    visualTransformation = resetVisualTransformation,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = confirmNewPass,
                    onValueChange = { confirmNewPass = it },
                    label = { Text("Conferma Nuova Password") },
                    leadingIcon = { Icon(Icons.Default.Check, contentDescription = null, tint = TripGreen) },
                    visualTransformation = resetVisualTransformation,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (viewModel.resetFeedbackMessage != null && viewModel.isResetError) {
                    Text(
                        text = viewModel.resetFeedbackMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Button(
                    onClick = {
                        viewModel.resetPassword(
                            email = resetEmail,
                            newPassword = newPass,
                            confirmPassword = confirmNewPass,
                            onSuccess = {
                                email = resetEmail
                                password = ""
                                showForgotPasswordSheet = false
                            }
                        )
                    },
                    enabled = !viewModel.isResettingPassword,
                    colors = ButtonDefaults.buttonColors(containerColor = TripGreen),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    if (viewModel.isResettingPassword) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White)
                    } else {
                        Text("Aggiorna Password", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color.White)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Dialog di conferma quando la password è stata reimpostata con successo
    if (viewModel.resetFeedbackMessage != null && !viewModel.isResetError) {
        AlertDialog(
            onDismissRequest = { viewModel.clearResetFeedback() },
            containerColor = Color.White,
            title = {
                Text("Password Aggiornata!", fontWeight = FontWeight.ExtraBold, color = TripDarkGreen)
            },
            text = {
                Text(viewModel.resetFeedbackMessage!!, color = TripBlack)
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.clearResetFeedback() },
                    colors = ButtonDefaults.buttonColors(containerColor = TripGreen)
                ) {
                    Text("Accedi ora", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TripWarmBg)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = TripGreen.copy(alpha = 0.14f),
                radius = size.width / 1.3f,
                center = Offset(x = size.width, y = 0f)
            )
            drawCircle(
                color = TripGreen.copy(alpha = 0.08f),
                radius = size.width / 1.8f,
                center = Offset(x = 0f, y = size.height / 1.5f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(TripGreen.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_logo_horizon),
                    contentDescription = "Logo Horizon",
                    modifier = Modifier.size(64.dp),
                    colorFilter = ColorFilter.tint(TripGreen)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(TripGreen))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "HORIZON TRAVEL",
                    style = MaterialTheme.typography.labelSmall,
                    color = TripGreen
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Bentornato su Horizon",
                style = MaterialTheme.typography.headlineLarge,
                color = TripBlack
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Accedi per esplorare viaggi, attività e liste condivise.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(36.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                shadowElevation = 6.dp,
                color = Color.White
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email", tint = TripGreen) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Password", tint = TripGreen) },
                        trailingIcon = {
                            TextButton(onClick = { showPassword = !showPassword }) {
                                Text(
                                    text = if (showPassword) "Nascondi" else "Mostra",
                                    color = TripGreen,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        },
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true
                    )

                    TextButton(
                        onClick = {
                            viewModel.clearResetFeedback()
                            showForgotPasswordSheet = true
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(
                            text = "Hai dimenticato la password?",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TripGreen
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    viewModel.errorMessage?.let { error ->
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }

                    Button(
                        onClick = { viewModel.eseguiLogin(email, password) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TripGreen),
                        enabled = !viewModel.isLoading
                    ) {
                        if (viewModel.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                        } else {
                            Text("Accedi", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Non hai ancora un account?",
                    color = Color.DarkGray,
                    style = MaterialTheme.typography.bodyMedium
                )
                TextButton(onClick = { onNavigateToRegister() }) {
                    Text("Registrati", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = TripGreen)
                }
            }
        }
    }
}