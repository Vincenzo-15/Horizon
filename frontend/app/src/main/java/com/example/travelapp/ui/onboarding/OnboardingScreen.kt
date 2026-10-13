package com.example.travelapp.ui.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelapp.R // <-- Assicurati che questo sia il TUO pacchetto base
import kotlinx.coroutines.launch

// 1. IL MODELLO DATI
data class OnboardingPage(
    val imageRes: Int,
    val title: String,
    val description: String
)

@Composable
fun OnboardingScreen(onFinish: () -> Unit = {}) {
    // 2. I CONTENUTI DELLE 3 PAGINE
    val pages = listOf(
        OnboardingPage(
            imageRes = R.drawable.onboarding1,
            title = "Pianifica la tua rotta",
            description = "Scopri destinazioni mozzafiato e crea il tuo itinerario perfetto con Horizon."
        ),
        OnboardingPage(
            imageRes = R.drawable.onboarding2,
            title = "Inizia l'Avventura",
            description = "Parti da solo o condividi esperienze indimenticabili con i tuoi amici."
        ),
        OnboardingPage(
            imageRes = R.drawable.onboarding3,
            title = "Vivi il Momento",
            description = "Rilassati e goditi ogni singolo istante."
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val coroutineScope = rememberCoroutineScope()

    // IL BOX PRINCIPALE: Contiene il carosello a tutto schermo
    Box(modifier = Modifier.fillMaxSize()) {

        // IL CAROSELLO (Orizzontale)[cite: 4]
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { position ->
            // Chiamiamo la funzione che disegna la singola pagina
            PagerScreen(page = pages[position])
        }

        // TASTO SKIP (In alto a destra, fluttuante)
        TextButton(
            onClick = { onFinish() },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 20.dp, end = 16.dp)
        ) {
            Text("Salta", color = Color.White.copy(alpha = 0.8f), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }

        // ELEMENTI IN BASSO (Pallini e Bottone)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 48.dp, start = 32.dp, end = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // --- I PALLINI ANIMATI ---
            Row(
                modifier = Modifier.padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(pages.size) { iteration ->
                    // ANIMAZIONI: Calcolano la larghezza e il colore fluidamente
                    val isSelected = pagerState.currentPage == iteration
                    val color by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.5f),
                        animationSpec = tween(durationMillis = 300)
                    )
                    val width by animateDpAsState(
                        targetValue = if (isSelected) 32.dp else 10.dp, // Si allunga dolcemente
                        animationSpec = tween(durationMillis = 300)
                    )

                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .clip(CircleShape)
                            .background(color)
                            .height(10.dp)
                            .width(width)
                    )
                }
            }

            // --- IL BOTTONE DINAMICO ---
            Button(
                onClick = {
                    if (pagerState.currentPage == pages.size - 1) {
                        onFinish()
                    } else {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp), // Bottone leggermente più alto ed elegante
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    text = if (pagerState.currentPage == pages.size - 1) "Inizia il Viaggio" else "Avanti",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

// LA SINGOLA PAGINA (Sfondo + Sfumatura + Testo)
@Composable
fun PagerScreen(page: OnboardingPage) {
    Box(modifier = Modifier.fillMaxSize()) {

        // 1. IMMAGINE DI SFONDO (Occupa tutto lo spazio)
        Image(
            painter = painterResource(id = page.imageRes),
            contentDescription = page.title,
            contentScale = ContentScale.Crop, // Taglia l'immagine per coprire tutto
            modifier = Modifier.fillMaxSize()
        )

        // 2. LA SFUMATURA NERA (Gradient Scrim)
        // Parte da trasparente a metà schermo e diventa nero al 90% verso il basso
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.9f)
                        ),
                        startY = 500f // Inizia la sfumatura circa a metà schermo
                    )
                )
        )

        // 3. I TESTI SOPRA LA SFUMATURA
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter) // Allineati in basso
                .padding(bottom = 180.dp, start = 32.dp, end = 32.dp), // Alzati per lasciare spazio al bottone e ai pallini
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = page.title,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = page.description,
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.8f), // Bianco leggermente trasparente
                textAlign = TextAlign.Center
            )
        }
    }
}