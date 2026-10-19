package com.example.travelapp.ui.theme

import androidx.compose.ui.graphics.Color

// --- PALETTE UFFICIALE HORIZON TRAVEL (STILE TRIPADVISOR / AIRBNB) ---
val TripGreen = Color(0xFF00AA6C)
val TripDarkGreen = Color(0xFF004F32)
val TripBlack = Color(0xFF1A1A1A)
val TripWarmBg = Color(0xFFF4F6F5)
val TripAmber = Color(0xFFFFB800)
val TripOrangeActivity = Color(0xFFE65100)

// --- TEMA CHIARO (Allineato alla palette Horizon) ---
val PrimaryLight = TripGreen
val OnPrimaryLight = Color(0xFFFFFFFF)
val SecondaryLight = TripOrangeActivity
val TertiaryLight = TripAmber
val BackgroundLight = TripWarmBg
val SurfaceLight = Color(0xFFFFFFFF)
val OnSurfaceLight = TripBlack
val ErrorLight = Color(0xFFD32F2F)

// --- TEMA SCURO ---
val PrimaryDark = Color(0xFF34D399)
val OnPrimaryDark = Color(0xFF003822)
val SecondaryDark = Color(0xFFFF8A50)
val TertiaryDark = TripAmber
val BackgroundDark = Color(0xFF121212)
val SurfaceDark = Color(0xFF1E1E1E)
val OnSurfaceDark = Color(0xFFE0E0E0)
val ErrorDark = Color(0xFFCF6679)

// Colori per il velo (scrim) sopra l'immagine di sfondo nell'onboarding/login
val ScrimLight = Color(0xFF000000).copy(alpha = 0.5f)
val ScrimDark = Color(0xFF000000).copy(alpha = 0.8f)