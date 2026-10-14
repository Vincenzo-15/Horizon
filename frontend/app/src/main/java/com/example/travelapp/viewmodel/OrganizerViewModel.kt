package com.example.travelapp.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelapp.network.RetrofitClient
import com.example.travelapp.network.TripResponse
import kotlinx.coroutines.launch

class OrganizerViewModel : ViewModel() {

    var isLoading by mutableStateOf(true)
    var errorMessage by mutableStateOf<String?>(null)
    var myTrips by mutableStateOf<List<TripResponse>>(emptyList())

    var totalOrders by mutableStateOf(0)

    fun fetchMyTrips(token: String) {
        if (token.isBlank()) return

        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            try {
                val authHeader = "Bearer $token"

                // 1. Carica i pacchetti viaggio
                val response = RetrofitClient.apiService.getMyTrips(authHeader)

                if (response.isSuccessful && response.body() != null) {
                    // Mostriamo i viaggi più recenti in cima
                    myTrips = response.body()!!.reversed()
                } else {
                    errorMessage = "Impossibile caricare i pacchetti (${response.code()})"
                }

                // 2. Carica il numero di ordini ricevuti
                val ordersResponse = RetrofitClient.apiService.getAgencyOrders(authHeader)
                if (ordersResponse.isSuccessful && ordersResponse.body() != null) {
                    // Aggiorna il contatore con la dimensione della lista ricevuta
                    totalOrders = ordersResponse.body()!!.size
                }

            } catch (e: Exception) {
                errorMessage = "Errore di connessione al server."
                android.util.Log.e("API_ERROR", "Errore: ${e.message}", e)
            } finally {
                isLoading = false
            }
        }
    }
}