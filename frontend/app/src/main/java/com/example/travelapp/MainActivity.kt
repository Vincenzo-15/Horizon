package com.example.travelapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.travelapp.ui.home.HomeScreen
import com.example.travelapp.ui.home.OrganizerDashboard
import com.example.travelapp.ui.login.LoginScreen
import com.example.travelapp.ui.onboarding.OnboardingScreen
import com.example.travelapp.ui.theme.TravelAppTheme
import com.example.travelapp.utils.TokenManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TravelAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    val context = LocalContext.current
                    val tokenManager = TokenManager(context)

                    val tokenSalvato = tokenManager.getToken()
                    val isTokenValido = tokenManager.isTokenValid()

                    val eraScaduto = !tokenSalvato.isNullOrEmpty() && !isTokenValido
                    if (eraScaduto) {
                        tokenManager.clearToken()
                    }

                    val ruoloUtente = if (isTokenValido) tokenManager.getRoleFromToken() else null

                    val destinazioneIniziale = when {
                        eraScaduto -> "login"
                        tokenSalvato.isNullOrEmpty() -> "onboarding"
                        ruoloUtente == "ORGANIZZATORE" -> "organizer_dashboard"
                        else -> "home"
                    }

                    NavHost(
                        navController = navController,
                        startDestination = destinazioneIniziale,
                        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(400)) },
                        exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(400)) },
                        popEnterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(400)) },
                        popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(400)) }
                    ) {

                        composable("onboarding") {
                            OnboardingScreen(
                                onFinish = {
                                    navController.navigate("login") {
                                        popUpTo("onboarding") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("login") {
                            LoginScreen(
                                onNavigateToRegister = {
                                    navController.navigate("registrazione")
                                },
                                onLoginSuccess = {
                                    val nuovoRuolo = tokenManager.getRoleFromToken()
                                    val rotta = if (nuovoRuolo == "ORGANIZZATORE") "organizer_dashboard" else "home"

                                    navController.navigate(rotta) {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("registrazione") {
                            com.example.travelapp.ui.register.RegisterScreen(
                                onNavigateToLogin = { navController.navigateUp() }
                            )
                        }

                        // ROTTA VIAGGIATORE
                        composable("home") {
                            val currentToken = tokenManager.getToken() ?: ""
                            HomeScreen(
                                tokenUtente = currentToken,
                                onTripClick = { tripId ->
                                    navController.navigate("trip_detail/$tripId") // --- AGGIUNTO ---
                                },
                                onNavigateToProfile = {navController.navigate("profile")}
                            )
                        }

                        // ROTTA ORGANIZZATORE
                        composable("organizer_dashboard") {
                            val currentToken = tokenManager.getToken() ?: ""
                            OrganizerDashboard(
                                tokenUtente = currentToken,
                                onNavigateToCreateTrip = { navController.navigate("create_trip") },
                                onTripClick = { tripId ->
                                    navController.navigate("trip_detail/$tripId") // --- AGGIUNTO ---
                                },
                                onNavigateToProfile = {navController.navigate("profile")}
                            )
                        }

                        // --- NUOVA ROTTA: PROFILO & LOGOUT ---
                        composable("profile") {
                            val currentRole = tokenManager.getRoleFromToken()
                            com.example.travelapp.ui.profile.ProfileScreen(
                                ruoloUtente = currentRole,
                                onNavigateBack = { navController.navigateUp() },
                                onLogout = {
                                    tokenManager.clearToken()
                                    navController.navigate("login") {
                                        popUpTo(0) { inclusive = true } // Pulisce tutta la cronologia
                                    }
                                }
                            )
                        }

                        composable("create_trip") {
                            val currentToken = tokenManager.getToken() ?: ""
                            com.example.travelapp.ui.home.CreateTripScreen(
                                tokenUtente = currentToken,
                                onNavigateBack = { navController.navigateUp() }
                            )
                        }

                        // --- NUOVA ROTTA: DETTAGLIO VIAGGIO ---
                        composable("trip_detail/{tripId}") { backStackEntry ->
                            val tripId = backStackEntry.arguments?.getString("tripId")?.toLongOrNull() ?: 0L
                            val currentToken = tokenManager.getToken() ?: ""
                            val currentRole = tokenManager.getRoleFromToken()
                            com.example.travelapp.ui.home.TripDetailScreen(
                                tripId = tripId,
                                tokenUtente = currentToken,
                                ruoloUtente = currentRole,
                                onNavigateBack = { navController.navigateUp() }
                            )
                        }
                    }
                }
            }
        }
    }
}