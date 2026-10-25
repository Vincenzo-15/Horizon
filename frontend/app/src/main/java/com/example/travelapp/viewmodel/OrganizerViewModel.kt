package com.example.travelapp.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelapp.network.NotificationDTO
import com.example.travelapp.network.ReservationDTO
import com.example.travelapp.network.RetrofitClient
import com.example.travelapp.network.TripDTO
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class OrganizerViewModel : ViewModel() {

    var isLoading by mutableStateOf(true)
    var errorMessage by mutableStateOf<String?>(null)

    var myTrips by mutableStateOf<List<TripDTO>>(emptyList())
    var agencyOrders by mutableStateOf<List<ReservationDTO>>(emptyList())
    var myNotifications by mutableStateOf<List<NotificationDTO>>(emptyList())

    var totalOrders by mutableStateOf(0)
    var totalRevenue by mutableStateOf(0.0)
    var totalBookedSpots by mutableStateOf(0)
    var averageCatalogRating by mutableStateOf(0.0)
    var totalAgencyReviews by mutableStateOf(0)

    fun fetchMyTrips(token: String) {
        if (token.isBlank()) return

        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            try {
                val authHeader = "Bearer $token"

                // 1. Carica i pacchetti pubblicati dall'agenzia e calcola il Rating Partner reale dal DB
                val response = RetrofitClient.apiService.getMyTrips(authHeader)
                if (response.isSuccessful && response.body() != null) {
                    myTrips = response.body()!!.reversed()

                    val tripsWithReviews = myTrips.filter { it.reviewCount > 0 && it.averageRating > 0.0 }
                    val sumReviews = tripsWithReviews.sumOf { it.reviewCount }
                    totalAgencyReviews = sumReviews

                    averageCatalogRating = if (sumReviews > 0) {
                        val weightedSum = tripsWithReviews.sumOf { it.averageRating * it.reviewCount }
                        ((weightedSum / sumReviews) * 10.0).roundToInt() / 10.0
                    } else {
                        0.0
                    }
                } else {
                    errorMessage = "Impossibile caricare i pacchetti (${response.code()})"
                }

                // 2. Carica le prenotazioni ricevute e calcola i KPI
                val ordersResponse = RetrofitClient.apiService.getAgencyOrders(authHeader)
                if (ordersResponse.isSuccessful && ordersResponse.body() != null) {
                    agencyOrders = ordersResponse.body()!!.reversed()
                    totalOrders = agencyOrders.size
                    totalRevenue = agencyOrders.sumOf { it.totalPaid }
                    totalBookedSpots = agencyOrders.sumOf { it.bookedSpots }
                }

                // 3. Carica eventuali notifiche dal backend
                val notifResponse = RetrofitClient.apiService.getMyNotifications(authHeader)
                if (notifResponse.isSuccessful && notifResponse.body() != null) {
                    myNotifications = notifResponse.body()!!
                }

            } catch (e: Exception) {
                errorMessage = "Errore di connessione al server."
            } finally {
                isLoading = false
            }
        }
    }
}