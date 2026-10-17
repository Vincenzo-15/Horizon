package com.example.travelapp.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelapp.network.RetrofitClient
import com.example.travelapp.network.TripDTO
import com.example.travelapp.network.TripRequest
import kotlinx.coroutines.launch

class TripDetailViewModel : ViewModel() {

    var isLoading by mutableStateOf(true)
    var errorMessage by mutableStateOf<String?>(null)
    var trip by mutableStateOf<TripDTO?>(null)

    // Stati per l'eliminazione (Agenzia)
    var isDeleted by mutableStateOf(false)

    // Stati per la modifica (Agenzia)
    var isUpdating by mutableStateOf(false)
    var updateMessage by mutableStateOf<String?>(null)

    // Stati per la prenotazione (Viaggiatore)
    var isBooking by mutableStateOf(false)
    var bookingSuccess by mutableStateOf(false)
    var bookingMessage by mutableStateOf<String?>(null)

    // --- STATI PER I PREFERITI ---
    var showFavoriteDialog by mutableStateOf(false)
    var myLists by mutableStateOf<List<com.example.travelapp.network.ItineraryListDTO>>(emptyList())
    var favoriteMessage by mutableStateOf<String?>(null)

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

    // --- NUOVA FUNZIONE: Aggiorna Viaggio ---
    fun updateTrip(token: String, tripId: Long, request: TripRequest) {
        viewModelScope.launch {
            isUpdating = true
            updateMessage = null
            try {
                val response = RetrofitClient.apiService.updateTrip("Bearer $token", tripId, request)
                if (response.isSuccessful && response.body() != null) {
                    // Aggiorniamo i dati locali con quelli appena restituiti dal server
                    trip = response.body()
                    updateMessage = "Viaggio aggiornato con successo!"
                } else {
                    updateMessage = "Errore durante l'aggiornamento (${response.code()})"
                }
            } catch (e: Exception) {
                updateMessage = "Errore di rete durante il salvataggio."
            } finally {
                isUpdating = false
            }
        }
    }

    fun clearUpdateMessage() {
        updateMessage = null
    }

    fun bookTrip(token: String, tripId: Long, spots: Int) {
        viewModelScope.launch {
            isBooking = true
            bookingMessage = null
            try {
                val response = RetrofitClient.apiService.bookTrip("Bearer $token", tripId, spots)
                if (response.isSuccessful) {
                    bookingSuccess = true
                    bookingMessage = "Prenotazione confermata con successo! Ti abbiamo inviato un'email con il riepilogo."
                    fetchTripDetail(token, tripId)
                } else {
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

    fun fetchMyListsForDialog(token: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.getMyLists("Bearer $token")
                if (response.isSuccessful && response.body() != null) {
                    myLists = response.body()!!
                }
            } catch (e: Exception) {
                favoriteMessage = "Errore nel caricamento delle liste"
            }
        }
    }

    fun addTripToList(token: String, listId: Long, tripId: Long) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.addTripToList("Bearer $token", listId, tripId)
                if (response.isSuccessful) {
                    favoriteMessage = "Aggiunto ai preferiti!"
                    showFavoriteDialog = false
                } else {
                    favoriteMessage = "Errore: impossibile aggiungere il viaggio."
                }
            } catch (e: Exception) {
                favoriteMessage = "Errore di connessione."
            }
        }
    }

    fun createListAndAddTrip(token: String, name: String, visibility: String, tripId: Long) {
        viewModelScope.launch {
            try {
                val createResponse = RetrofitClient.apiService.createList("Bearer $token", name, visibility)
                if (createResponse.isSuccessful && createResponse.body() != null) {
                    val newListId = createResponse.body()!!.id
                    addTripToList(token, newListId, tripId)
                } else {
                    favoriteMessage = "Impossibile creare la lista."
                }
            } catch (e: Exception) {
                favoriteMessage = "Errore durante la creazione."
            }
        }
    }

    fun clearBookingMessage() {
        bookingMessage = null
        bookingSuccess = false
    }
}