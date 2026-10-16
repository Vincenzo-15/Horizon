package com.example.travelapp.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelapp.network.RetrofitClient
import com.example.travelapp.network.ItineraryListDTO
import com.example.travelapp.network.TripDTO
import kotlinx.coroutines.launch

class ListDetailViewModel : ViewModel() {

    var listDetails by mutableStateOf<ItineraryListDTO?>(null)
    var tripsInList by mutableStateOf<List<TripDTO>>(emptyList())
    var isLoading by mutableStateOf(true)
    var errorMessage by mutableStateOf<String?>(null)

    fun fetchListDetails(token: String, listId: Long) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val authHeader = "Bearer $token"

                // 1. Chiediamo al server i dettagli base della lista (Nome, Visibilità)
                val detailsResponse = RetrofitClient.apiService.getListDetails(authHeader, listId)
                if (detailsResponse.isSuccessful && detailsResponse.body() != null) {
                    listDetails = detailsResponse.body()
                }

                // 2. Chiediamo al server DIRETTAMENTE i viaggi salvati in questa lista (Senza trucchetti!)
                val tripsResponse = RetrofitClient.apiService.getTripsInList(authHeader, listId)
                if (tripsResponse.isSuccessful && tripsResponse.body() != null) {
                    tripsInList = tripsResponse.body()!!
                } else {
                    errorMessage = "Impossibile caricare i viaggi di questa lista."
                }

            } catch (e: Exception) {
                errorMessage = "Errore di connessione al server."
            } finally {
                isLoading = false
            }
        }
    }
}