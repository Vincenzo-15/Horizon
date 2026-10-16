package com.example.travelapp.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelapp.network.RetrofitClient
import com.example.travelapp.network.TripDTO
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    // Stati della UI
    var isLoading by mutableStateOf(true)
    var errorMessage by mutableStateOf<String?>(null)
    var trips by mutableStateOf<List<TripDTO>>(emptyList())

    fun fetchTrips(token: String) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            try {
                // Aggiungiamo "Bearer " come richiesto da Spring Security
                val authHeader = "Bearer $token"
                val response = RetrofitClient.apiService.getPopularTrips(authHeader)

                if (response.isSuccessful && response.body() != null) {
                    trips = response.body()!!
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
}