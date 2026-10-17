package com.example.travelapp.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelapp.network.ReservationDTO
import com.example.travelapp.network.RetrofitClient
import com.example.travelapp.network.TripDTO
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    var isLoading by mutableStateOf(true)
    var errorMessage by mutableStateOf<String?>(null)
    var trips by mutableStateOf<List<TripDTO>>(emptyList())

    // Lista dinamica per gli ultimi viaggi visti di recente
    var recentlyViewedTrips = mutableStateListOf<TripDTO>()
        private set

    // Stato per la tab "Viaggi" (Prenotazioni effettuate dal viaggiatore)
    var myReservations by mutableStateOf<List<ReservationDTO>>(emptyList())
    var isLoadingReservations by mutableStateOf(false)
    var reservationsError by mutableStateOf<String?>(null)

    fun fetchTrips(token: String) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            try {
                val authHeader = "Bearer $token"
                val response = RetrofitClient.apiService.getPopularTrips(authHeader)

                if (response.isSuccessful && response.body() != null) {
                    trips = response.body()!!
                    if (recentlyViewedTrips.isEmpty() && trips.isNotEmpty()) {
                        recentlyViewedTrips.addAll(trips.take(3))
                    }
                } else {
                    errorMessage = "Errore nel caricamento dei viaggi (${response.code()})"
                }
            } catch (e: Exception) {
                errorMessage = "Impossibile connettersi al server."
            } finally {
                isLoading = false
            }
        }
    }

    fun fetchMyReservations(token: String) {
        viewModelScope.launch {
            isLoadingReservations = true
            reservationsError = null

            try {
                val authHeader = "Bearer $token"
                val response = RetrofitClient.apiService.getMyReservations(authHeader)
                if (response.isSuccessful && response.body() != null) {
                    myReservations = response.body()!!
                } else {
                    reservationsError = "Impossibile caricare le prenotazioni (${response.code()})"
                }
            } catch (e: Exception) {
                reservationsError = "Errore di connessione durante il recupero delle prenotazioni."
            } finally {
                isLoadingReservations = false
            }
        }
    }

    fun markTripAsViewed(trip: TripDTO) {
        recentlyViewedTrips.removeAll { it.id == trip.id }
        recentlyViewedTrips.add(0, trip)
        if (recentlyViewedTrips.size > 6) {
            recentlyViewedTrips.removeAt(recentlyViewedTrips.lastIndex)
        }
    }
}