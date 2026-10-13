package com.example.travelapp.viewmodel

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelapp.network.RetrofitClient
import com.example.travelapp.network.TripRequest
import kotlinx.coroutines.launch

class CreateTripViewModel : ViewModel() {
    var isLoading by mutableStateOf(false)
    var isSuccess by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    fun createTrip(
        token: String,
        title: String,
        description: String,
        priceStr: String,
        startDate: String,
        endDate: String,
        spotsStr: String,
        location: String,
        imageUris: List<Uri> // <-- Ora accetta una lista
    ) {
        if (title.isBlank() || priceStr.isBlank() || startDate.isBlank() || endDate.isBlank() || spotsStr.isBlank() || location.isBlank()) {
            errorMessage = "Compila tutti i campi obbligatori."
            return
        }

        if (imageUris.isEmpty()) {
            errorMessage = "Seleziona almeno un'immagine per il tuo pacchetto."
            return
        }

        val price = priceStr.toDoubleOrNull()
        val spots = spotsStr.toIntOrNull()

        if (price == null || spots == null) {
            errorMessage = "Prezzo e posti disponibili devono essere valori numerici validi."
            return
        }

        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            try {
                // Convertiamo gli URI in stringhe temporaneamente
                val request = TripRequest(
                    title = title,
                    description = description,
                    price = price,
                    startDate = startDate,
                    endDate = endDate,
                    availableSpots = spots,
                    location = location,
                    imageUrls = imageUris.map { it.toString() } // <-- Mappiamo la lista di URI a una lista di String
                )

                val authHeader = "Bearer $token"
                val response = RetrofitClient.apiService.createTrip(authHeader, request)

                if (response.isSuccessful) {
                    isSuccess = true
                } else {
                    errorMessage = "Errore nella creazione del viaggio: ${response.code()}"
                }
            } catch (e: Exception) {
                errorMessage = "Impossibile contattare il server."
            } finally {
                isLoading = false
            }
        }
    }
}