package com.example.travelapp.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelapp.network.RetrofitClient
import com.example.travelapp.network.TripResponse
import kotlinx.coroutines.launch

class TripDetailViewModel : ViewModel() {

    var isLoading by mutableStateOf(true)
    var errorMessage by mutableStateOf<String?>(null)
    var trip by mutableStateOf<TripResponse?>(null)
    var isDeleted by mutableStateOf(false)

    fun fetchTripDetail(token: String, tripId: Long) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val response = RetrofitClient.apiService.getTripById("Bearer $token", tripId)
                if (response.isSuccessful && response.body() != null) {
                    trip = response.body()
                } else {
                    errorMessage = "Impossibile caricare i dettagli del viaggio (${response.code()})"
                }
            } catch (e: Exception) {
                errorMessage = "Errore di connessione al server."
            } finally {
                isLoading = false
            }
        }
    }

    fun deleteTrip(token: String, tripId: Long) {
        viewModelScope.launch {
            isLoading = true
            try {
                val response = RetrofitClient.apiService.deleteTrip("Bearer $token", tripId)
                if (response.isSuccessful) {
                    isDeleted = true
                } else {
                    errorMessage = "Non è stato possibile eliminare il pacchetto (${response.code()})"
                }
            } catch (e: Exception) {
                errorMessage = "Errore di connessione durante l'eliminazione."
            } finally {
                isLoading = false
            }
        }
    }
}