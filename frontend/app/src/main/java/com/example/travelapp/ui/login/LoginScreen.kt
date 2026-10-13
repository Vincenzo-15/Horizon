package com.example.travelapp.ui.login

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelapp.R
import com.example.travelapp.ui.theme.TravelAppTheme
import com.example.travelapp.utils.TokenManager
import com.example.travelapp.viewmodel.LoginViewModel

@Composable
fun LoginScreen(
    viewModel: LoginViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onNavigateToRegister: () -> Unit = {},
    onNavigateToForgotPassword: () -> Unit = {},
    onLoginSuccess: () -> Unit = {}
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val bgColor = MaterialTheme.colorScheme.background
    val primaryColor = MaterialTheme.colorScheme.primary

    // --- Inizializziamo il TokenManager ---
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }

    // --- SALVATAGGIO TOKEN E NAVIGAZIONE ---
    LaunchedEffect(viewModel.loginSuccess) {
        if (viewModel.loginSuccess) {
            val tokenDaSalvare = viewModel.token
            if (!tokenDaSalvare.isNullOrEmpty()) {
                tokenManager.saveToken(tokenDaSalvare)
                Log.d("AUTH_DEBUG", "Token salvato con successo nella memoria!")
            } else {
                Log.e("AUTH_DEBUG", "Errore: Il ViewModel non ha un token valido da salvare.")
            }
            onLoginSuccess()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(bgColor)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(color = primaryColor.copy(alpha = 0.15f), radius = size.width / 1.3f, center = Offset(x = size.width, y = 0f))
            drawCircle(color = primaryColor.copy(alpha = 0.08f), radius = size.width / 1.8f, center = Offset(x = 0f, y = size.height / 1.5f))
        }

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 32.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            Image(painter = painterResource(id = R.drawable.ic_logo_horizon), contentDescription = "Logo", modifier = Modifier.size(120.dp), colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground))
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Horizon", fontSize = 42.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = "Bentornato! Accedi per continuare.", fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
            Spacer(modifier = Modifier.height(48.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email", tint = MaterialTheme.colorScheme.primary) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Password", tint = MaterialTheme.colorScheme.primary) }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)

                    TextButton(onClick = { onNavigateToForgotPassword() }, modifier = Modifier.align(Alignment.End)) {
                        Text("Hai dimenticato la password?", fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    viewModel.errorMessage?.let { error ->
                        Text(text = error, color = MaterialTheme.colorScheme.error, fontSize = 14.sp, modifier = Modifier.padding(bottom = 16.dp))
                    }

                    Button(
                        onClick = { viewModel.eseguiLogin(email, password) },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        enabled = !viewModel.isLoading
                    ) {
                        if (viewModel.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Text("Accedi", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                Text(text = "Non hai un account?", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f), fontSize = 15.sp)
                TextButton(onClick = { onNavigateToRegister() }) {
                    Text("Registrati", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}