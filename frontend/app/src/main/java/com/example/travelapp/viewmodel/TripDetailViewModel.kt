package com.example.travelapp.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelapp.network.ItineraryListDTO
import com.example.travelapp.network.ReservationDTO
import com.example.travelapp.network.RetrofitClient
import com.example.travelapp.network.TripDTO
import com.example.travelapp.network.TripRequest
import kotlinx.coroutines.launch

class TripDetailViewModel : ViewModel() {

    var isLoading by mutableStateOf(true)
    var errorMessage by mutableStateOf<String?>(null)
    var trip by mutableStateOf<TripDTO?>(null)
    var similarTrips by mutableStateOf<List<TripDTO>>(emptyList())

    // Prenotazioni già effettuate dall'utente corrente per questo specifico viaggio/attività
    var userReservationsForThisTrip by mutableStateOf<List<ReservationDTO>>(emptyList())

    var isDeleted by mutableStateOf(false)
    var isUpdating by mutableStateOf(false)
    var updateMessage by mutableStateOf<String?>(null)

    var isBooking by mutableStateOf(false)
    var bookingSuccess by mutableStateOf(false)
    var bookingMessage by mutableStateOf<String?>(null)

    var showFavoriteDialog by mutableStateOf(false)
    var myLists by mutableStateOf<List<ItineraryListDTO>>(emptyList())
    var favoriteMessage by mutableStateOf<String?>(null)

    fun fetchTripDetail(token: String, tripId: Long) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val authHeader = "Bearer $token"
                val response = RetrofitClient.apiService.getTripById(authHeader, tripId)
                if (response.isSuccessful && response.body() != null) {
                    val loadedTrip = response.body()!!
                    trip = loadedTrip

                    // 1. Verifica se il viaggiatore ha già prenotato questo viaggio
                    try {
                        val myResResponse = RetrofitClient.apiService.getMyReservations(authHeader)
                        if (myResResponse.isSuccessful && myResResponse.body() != null) {
                            userReservationsForThisTrip = myResResponse.body()!!.filter { it.tripId == tripId }
                        }
                    } catch (_: Exception) {}

                    // 2. Carica esperienze simili con posti disponibili (> 0)
                    val popularRes = RetrofitClient.apiService.getPopularTrips(authHeader)
                    if (popularRes.isSuccessful && popularRes.body() != null) {
                        val others = popularRes.body()!!.filter { it.id != tripId && it.availableSpots > 0 }
                        val sameLocation = others.filter {
                            !loadedTrip.location.isNullOrBlank() &&
                                    it.location?.contains(loadedTrip.location, ignoreCase = true) == true
                        }
                        similarTrips = (sameLocation + others).distinctBy { it.id }.take(6)
                    }
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

    fun updateTrip(token: String, tripId: Long, request: TripRequest) {
        viewModelScope.launch {
            isUpdating = true
            updateMessage = null
            try {
                val response = RetrofitClient.apiService.updateTrip("Bearer $token", tripId, request)
                if (response.isSuccessful && response.body() != null) {
                    trip = response.body()
                    updateMessage = "Offerta e listino aggiornati con successo!"
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

    fun bookTrip(token: String, tripId: Long, spots: Int, selectedSlotLabel: String = "") {
        viewModelScope.launch {
            isBooking = true
            bookingMessage = null
            try {
                val response = RetrofitClient.apiService.bookTrip("Bearer $token", tripId, spots)
                if (response.isSuccessful) {
                    bookingSuccess = true
                    val slotInfo = if (selectedSlotLabel.isNotBlank()) " per \"$selectedSlotLabel\"" else ""
                    bookingMessage = "Pagamento autorizzato e prenotazione confermata$slotInfo! I posti disponibili sono stati aggiornati e puoi esportare il file .ics dalla tab Viaggi."
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
                    favoriteMessage = "Aggiunto alla tua lista preferiti!"
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