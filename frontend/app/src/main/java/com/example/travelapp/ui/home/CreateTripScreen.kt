package com.example.travelapp.ui.home

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.travelapp.viewmodel.CreateTripViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTripScreen(
    tokenUtente: String,
    viewModel: CreateTripViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onNavigateBack: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var spots by remember { mutableStateOf("") }

    var imageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }

    val context = LocalContext.current

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 5),
        onResult = { uris ->
            if (uris.isNotEmpty()) {
                uris.forEach { uri ->
                    try {
                        context.contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                imageUris = uris
            }
        }
    )

    LaunchedEffect(viewModel.isSuccess) {
        if (viewModel.isSuccess) onNavigateBack()
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .clickable { photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
        ) {
            if (imageUris.isNotEmpty()) {
                val pagerState = rememberPagerState(pageCount = { imageUris.size })

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    AsyncImage(
                        model = imageUris[page],
                        contentDescription = "Immagine Viaggio $page",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 60.dp, end = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${pagerState.currentPage + 1}/${imageUris.size}",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Add Photo",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Tocca per aggiungere fino a 5 foto dalla galleria",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        FilledTonalButton(
                            onClick = {
                                imageUris = listOf(
                                    Uri.parse("https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=800&q=80"),
                                    Uri.parse("https://images.unsplash.com/photo-1476514525535-07fb3b4ae5f1?w=800&q=80"),
                                    Uri.parse("https://images.unsplash.com/photo-1499856871958-5b9627545d1a?w=800&q=80")
                                )
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Usa 3 Foto Demo HD", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)), startY = 400f))
            )
        }

        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier
                .padding(top = 48.dp, start = 16.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.8f))
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Indietro", tint = Color.Black)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 280.dp)
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .background(MaterialTheme.colorScheme.surface)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text("Dettagli del Pacchetto", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)

            OutlinedTextField(
                value = title, onValueChange = { title = it },
                label = { Text("Titolo (es. Fuga alle Maldive)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color.LightGray)
            )

            OutlinedTextField(
                value = location, onValueChange = { location = it },
                label = { Text("Località Esatta") },
                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color.LightGray)
            )

            OutlinedTextField(
                value = description, onValueChange = { description = it },
                label = { Text("Descrizione dell'esperienza") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color.LightGray)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = price, onValueChange = { price = it },
                    label = { Text("Prezzo (€)") },
                    leadingIcon = { Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color.LightGray)
                )
                OutlinedTextField(
                    value = spots, onValueChange = { spots = it },
                    label = { Text("Posti") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color.LightGray)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = startDate, onValueChange = { startDate = it },
                    label = { Text("Inizio (YYYY-MM-DD)") },
                    leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color.LightGray)
                )
                OutlinedTextField(
                    value = endDate, onValueChange = { endDate = it },
                    label = { Text("Fine (YYYY-MM-DD)") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color.LightGray)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            viewModel.errorMessage?.let { error ->
                Text(text = error, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
            }

            Button(
                onClick = {
                    // --- MODIFICA APPLICATA QUI: aggiunta la variabile context! ---
                    viewModel.createTrip(context, tokenUtente, title, description, price, startDate, endDate, spots, location, imageUris)
                },
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                enabled = !viewModel.isLoading
            ) {
                if (viewModel.isLoading) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                } else {
                    Text("Pubblica Viaggio", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}