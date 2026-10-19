package com.example.travelapp.ui.register

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
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelapp.R
import com.example.travelapp.ui.theme.*
import com.example.travelapp.viewmodel.RegisterViewModel

@Composable
fun RegisterScreen(
    viewModel: RegisterViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onNavigateToLogin: () -> Unit = {}
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var showPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }

    var isOrganizzatore by remember { mutableStateOf(false) }
    var partitaIva by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }

    LaunchedEffect(viewModel.isSuccess) {
        if (viewModel.isSuccess) {
            onNavigateToLogin()
        }
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
                .padding(horizontal = 28.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(CircleShape)
                    .background(TripGreen.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_logo_horizon),
                    contentDescription = "Logo Horizon",
                    modifier = Modifier.size(54.dp),
                    colorFilter = ColorFilter.tint(TripGreen)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Crea Account",
                style = MaterialTheme.typography.headlineLarge,
                color = TripBlack
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Inizia la tua prossima avventura su Horizon.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(24.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                shadowElevation = 6.dp,
                color = Color.White
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    // 1. SCELTA DEL RUOLO IN CIMA
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Account Agenzia / Tour Operator",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isOrganizzatore) TripOrangeActivity else TripBlack
                            )
                            Text(
                                text = if (isOrganizzatore) "Abilitato a pubblicare viaggi e attività"
                                else "Disattivato (Profilo Viaggiatore)",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                        Switch(
                            checked = isOrganizzatore,
                            onCheckedChange = { isOrganizzatore = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = TripOrangeActivity
                            )
                        )
                    }

                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(if (isOrganizzatore) "Nome Agenzia / Professionista" else "Nome e Cognome") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = TripGreen) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true
                    )

                    if (isOrganizzatore) {
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedTextField(
                            value = partitaIva,
                            onValueChange = { partitaIva = it },
                            label = { Text("Partita IVA / Codice Fiscale") },
                            leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, tint = TripOrangeActivity) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = telefono,
                        onValueChange = { telefono = it },
                        label = { Text(if (isOrganizzatore) "Telefono Agenzia" else "Telefono (Opzionale)") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = TripGreen) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = TripGreen) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = TripGreen) },
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

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Conferma Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = TripGreen) },
                        trailingIcon = {
                            TextButton(onClick = { showConfirmPassword = !showConfirmPassword }) {
                                Text(
                                    text = if (showConfirmPassword) "Nascondi" else "Mostra",
                                    color = TripGreen,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        },
                        visualTransformation = if (showConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    viewModel.errorMessage?.let { error ->
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 14.dp)
                        )
                    }

                    Button(
                        onClick = {
                            viewModel.eseguiRegistrazione(
                                name,
                                email,
                                password,
                                confirmPassword,
                                isOrganizzatore,
                                partitaIva,
                                telefono
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isOrganizzatore) TripOrangeActivity else TripGreen
                        ),
                        enabled = !viewModel.isLoading
                    ) {
                        if (viewModel.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                        } else {
                            Text(
                                text = if (isOrganizzatore) "Registra Agenzia" else "Registrati",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Hai già un account?",
                    color = Color.DarkGray,
                    style = MaterialTheme.typography.bodyMedium
                )
                TextButton(onClick = { onNavigateToLogin() }) {
                    Text("Accedi", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = TripGreen)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RegisterScreenPreview() {
    TravelAppTheme {
        RegisterScreen()
    }
}