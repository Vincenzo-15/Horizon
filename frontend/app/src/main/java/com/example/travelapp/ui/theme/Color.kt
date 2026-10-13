package com.example.travelapp.ui.theme

import androidx.compose.ui.graphics.Color

// --- TEMA CHIARO ---
val PrimaryLight = Color(0xFF0066FF) // Blu elettrico principale
val OnPrimaryLight = Color(0xFFFFFFFF) // Testo bianco sui bottoni blu
val BackgroundLight = Color(0xFFF5F7FA) // Grigio chiarissimo per lo sfondo
val SurfaceLight = Color(0xFFFFFFFF) // Bianco puro per le Card
val OnSurfaceLight = Color(0xFF1A1A1A) // Testo quasi nero
val ErrorLight = Color(0xFFD32F2F)

// --- TEMA SCURO ---
val PrimaryDark = Color(0xFF4D94FF) // Blu leggermente più chiaro per risaltare sul nero
val OnPrimaryDark = Color(0xFF000000) // Testo nero sui bottoni blu
val BackgroundDark = Color(0xFF121212) // Grigio scurissimo (standard Material Dark)
val SurfaceDark = Color(0xFF1E1E1E) // Grigio scuro per le Card (leggermente più chiaro dello sfondo)
val OnSurfaceDark = Color(0xFFE0E0E0) // Testo quasi bianco
val ErrorDark = Color(0xFFCF6679)
// Colori per il velo (scrim) sopra l'immagine di sfondo nell'onboarding/login
val ScrimLight = Color(0xFF000000).copy(alpha = 0.5f) // Velo scuro per tema chiaro
val ScrimDark = Color(0xFF000000).copy(alpha = 0.8f)  // Velo scurissimo per tema scuro