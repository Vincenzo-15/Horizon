package com.example.travelapp.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelapp.network.RetrofitClient
import com.example.travelapp.network.ItineraryListDTO
import com.example.travelapp.network.TripDTO // Assicurati di usare il nome che hai dato, es. TripResponse o TripDTO
import kotlinx.coroutines.launch

class ListDetailViewModel : ViewModel() {

    var listDetails by mutableStateOf<ItineraryListDTO?>(null)

    // Lista completa dei viaggi contenuti per poter disegnare le Card
    var tripsInList by mutableStateOf<List<TripDTO>>(emptyList())

    var isLoading by mutableStateOf(true)
    var errorMessage by mutableStateOf<String?>(null)

    fun fetchListDetails(token: String, listId: Long) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                // 1. Chiediamo al server i dettagli di QUESTA lista
                val response = RetrofitClient.apiService.getListDetails("Bearer $token", listId)

                if (response.isSuccessful && response.body() != null) {
                    listDetails = response.body()

                    // 2. MOCK TEMPORANEO: Dato che l'API restituisce solo gli ID (List<Long>),
                    // per ora chiamiamo l'API generica dei viaggi e filtriamo quelli che ci interessano.
                    // In un'app reale, il backend ti restituirebbe direttamente gli oggetti completi.

                    val allTripsResponse = RetrofitClient.apiService.getPopularTrips("Bearer $token")
                    if (allTripsResponse.isSuccessful && allTripsResponse.body() != null) {
                        val allTrips = allTripsResponse.body()!!
                        // Filtriamo solo i viaggi il cui ID è contenuto nella nostra lista dei preferiti
                        tripsInList = allTrips.filter { trip -> listDetails!!.tripIds.contains(trip.id) }
                    }

                } else {
                    errorMessage = "Impossibile trovare questa lista."
                }
            } catch (e: Exception) {
                errorMessage = "Errore di connessione al server."
            } finally {
                isLoading = false
            }
        }
    }
}