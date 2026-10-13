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

    fun fetchMyTrips(token: String) {
        if (token.isBlank()) return

        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            try {
                val authHeader = "Bearer $token"
                val response = RetrofitClient.apiService.getMyTrips(authHeader)

                if (response.isSuccessful && response.body() != null) {
                    // Mostriamo i viaggi più recenti in cima
                    myTrips = response.body()!!.reversed()
                } else {
                    errorMessage = "Impossibile caricare i pacchetti (${response.code()})"
                }
            } catch (e: Exception) {
                errorMessage = "Errore di connessione al server."
            } finally {
                isLoading = false
            }
        }
    }
}