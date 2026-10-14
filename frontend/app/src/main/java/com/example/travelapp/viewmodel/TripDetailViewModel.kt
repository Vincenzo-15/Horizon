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

    // Stati per l'eliminazione (Agenzia)
    var isDeleted by mutableStateOf(false)

    // Stati per la prenotazione (Viaggiatore)
    var isBooking by mutableStateOf(false)
    var bookingSuccess by mutableStateOf(false)
    var bookingMessage by mutableStateOf<String?>(null)

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

    // --- NUOVA FUNZIONE DI PRENOTAZIONE ---
    fun bookTrip(token: String, tripId: Long, spots: Int) {
        viewModelScope.launch {
            isBooking = true
            bookingMessage = null
            try {
                val response = RetrofitClient.apiService.bookTrip("Bearer $token", tripId, spots)
                if (response.isSuccessful) {
                    bookingSuccess = true
                    bookingMessage = "Prenotazione confermata con successo! Ti abbiamo inviato un'email con il riepilogo."

                    // Ricarichiamo i dettagli per far scendere il numero di posti rimasti a schermo
                    fetchTripDetail(token, tripId)
                } else {
                    // Leggiamo l'errore inviato dal Backend (es. "Posti insufficienti")
                    val errorStr = response.errorBody()?.string() ?: "Errore durante la prenotazione."
                    bookingMessage = errorStr
                }
            } catch (e: Exception) {
                bookingMessage = "Impossibile contattare il server."
            } finally {
                isBooking = false
            }
        }
    }

    fun clearBookingMessage() {
        bookingMessage = null
        bookingSuccess = false
    }
}