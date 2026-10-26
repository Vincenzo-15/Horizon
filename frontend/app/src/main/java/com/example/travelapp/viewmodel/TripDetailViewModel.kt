package com.example.travelapp.viewmodel

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelapp.network.CreateReviewRequest
import com.example.travelapp.network.ItineraryListDTO
import com.example.travelapp.network.ReservationDTO
import com.example.travelapp.network.RetrofitClient
import com.example.travelapp.network.SendChatMessageRequest
import com.example.travelapp.network.TripDTO
import com.example.travelapp.network.TripRequest
import kotlinx.coroutines.launch

class TripDetailViewModel : ViewModel() {

    var isLoading by mutableStateOf(true)
    var errorMessage by mutableStateOf<String?>(null)
    var trip by mutableStateOf<TripDTO?>(null)
    var similarTrips by mutableStateOf<List<TripDTO>>(emptyList())

    var userReservationsForThisTrip by mutableStateOf<List<ReservationDTO>>(emptyList())
    var currentUserName by mutableStateOf("Viaggiatore Horizon")
    var currentUserEmail by mutableStateOf("")

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

                    // Recupera i dati reali dell'utente loggato per intestare correttamente la ricevuta
                    try {
                        val profileRes = RetrofitClient.apiService.getMyProfile(authHeader)
                        if (profileRes.isSuccessful && profileRes.body() != null) {
                            currentUserName = profileRes.body()!!.name
                            currentUserEmail = profileRes.body()!!.email
                        }
                    } catch (_: Exception) {}

                    // Verifica se il viaggiatore ha già prenotato questo viaggio
                    try {
                        val myResResponse = RetrofitClient.apiService.getMyReservations(authHeader)
                        if (myResResponse.isSuccessful && myResResponse.body() != null) {
                            userReservationsForThisTrip = myResResponse.body()!!.filter { it.tripId == tripId }
                        }
                    } catch (_: Exception) {}

                    // Carica esperienze simili con posti disponibili (> 0)
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

    // Salva la recensione nel database PostgreSQL e aggiorna media e conteggio
    fun submitReview(token: String, tripId: Long, rating: Int, comment: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.addReviewToTrip(
                    token = "Bearer $token",
                    tripId = tripId,
                    request = CreateReviewRequest(rating = rating, comment = comment.trim())
                )
                if (response.isSuccessful && response.body() != null) {
                    trip = response.body()
                }
            } catch (_: Exception) {}
        }
    }

    fun startOrSendAgencyChatMessage(
        token: String,
        tripId: Long,
        messageText: String,
        onSuccess: () -> Unit
    ) {
        if (messageText.isBlank()) return
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.sendChatMessage(
                    token = "Bearer $token",
                    tripId = tripId,
                    request = SendChatMessageRequest(message = messageText.trim())
                )
                if (response.isSuccessful) {
                    onSuccess()
                }
            } catch (_: Exception) {}
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

    fun bookTrip(
        context: Context,
        token: String,
        tripId: Long,
        spots: Int,
        selectedDateIso: String,
        selectedSlotLabel: String,
        paymentMethodLabel: String,
        unitPricePaid: Double,
        onDateBookedSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            isBooking = true
            bookingMessage = null
            try {
                val authHeader = "Bearer $token"
                val response = RetrofitClient.apiService.bookTrip(authHeader, tripId, spots)
                if (response.isSuccessful) {
                    // 1. Scala i posti sulla data specifica selezionata nel calendario
                    val datePrefs = context.getSharedPreferences("date_inventory_prefs", Context.MODE_PRIVATE)
                    val key = "booked_${tripId}_$selectedDateIso"
                    val prevBooked = datePrefs.getInt(key, 0)
                    datePrefs.edit().putInt(key, prevBooked + spots).apply()

                    // 2. Recupera l'ID dell'ultima prenotazione per salvare i metadati della Ricevuta
                    try {
                        val myRes = RetrofitClient.apiService.getMyReservations(authHeader)
                        val latestRes = myRes.body()?.maxByOrNull { it.id }
                        val receiptPrefs = context.getSharedPreferences("booking_receipts_prefs", Context.MODE_PRIVATE)
                        val editor = receiptPrefs.edit()
                        if (latestRes != null) {
                            editor.putString("traveler_name_${latestRes.id}", currentUserName)
                            editor.putString("traveler_email_${latestRes.id}", currentUserEmail)
                            editor.putString("payment_method_${latestRes.id}", paymentMethodLabel)
                            editor.putString("selected_slot_${latestRes.id}", selectedSlotLabel)
                            editor.putFloat("unit_price_${latestRes.id}", unitPricePaid.toFloat())
                        }
                        // Salva anche per tripId come fallback immediato
                        editor.putString("last_traveler_name_trip_$tripId", currentUserName)
                        editor.putString("last_traveler_email_trip_$tripId", currentUserEmail)
                        editor.putString("last_payment_method_trip_$tripId", paymentMethodLabel)
                        editor.putString("last_selected_slot_trip_$tripId", selectedSlotLabel)
                        editor.apply()
                    } catch (_: Exception) {}

                    bookingSuccess = true
                    val slotInfo = if (selectedSlotLabel.isNotBlank()) " per \"$selectedSlotLabel\"" else ""
                    bookingMessage = "Pagamento autorizzato ($paymentMethodLabel) e prenotazione confermata$slotInfo! I posti della data scelta sono stati aggiornati."
                    onDateBookedSuccess()
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