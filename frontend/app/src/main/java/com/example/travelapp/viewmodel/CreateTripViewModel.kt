package com.example.travelapp.viewmodel

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelapp.network.RetrofitClient
import com.example.travelapp.network.TripRequest
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class CreateTripViewModel : ViewModel() {
    var isLoading by mutableStateOf(false)
    var isSuccess by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    // Funzione helper per copiare la foto dalla galleria in un File temporaneo
    private fun uriToFile(context: Context, uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            // Crea un file temporaneo nascosto nella cache dell'app
            val tempFile = File(context.cacheDir, "upload_${UUID.randomUUID()}.jpg")
            val outputStream = FileOutputStream(tempFile)
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()
            tempFile
        } catch (e: Exception) {
            null
        }
    }

    fun createTrip(
        context: Context, // Aggiunto il Context per leggere i file
        token: String,
        title: String,
        description: String,
        priceStr: String,
        startDate: String,
        endDate: String,
        spotsStr: String,
        location: String,
        imageUris: List<Uri>
    ) {
        if (title.isBlank() || priceStr.isBlank() || startDate.isBlank() || endDate.isBlank() || spotsStr.isBlank() || location.isBlank()) {
            errorMessage = "Compila tutti i campi obbligatori."
            return
        }

        val price = priceStr.toDoubleOrNull()
        val spots = spotsStr.toIntOrNull()

        if (price == null || spots == null) {
            errorMessage = "Prezzo e posti disponibili devono essere numerici."
            return
        }

        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            try {
                var finalImageUrls: List<String>? = null

                // FASE 1: Se ci sono immagini dalla galleria, le inviamo al server!
                if (imageUris.isNotEmpty()) {
                    val multipartList = mutableListOf<MultipartBody.Part>()

                    for (uri in imageUris) {
                        // Saltiamo i link HTTP (quelli del bottone "Foto Demo HD")
                        if (uri.toString().startsWith("http")) {
                            if (finalImageUrls == null) finalImageUrls = mutableListOf()
                            (finalImageUrls as MutableList).add(uri.toString())
                            continue
                        }

                        // Trasformiamo le foto della galleria (content://) in veri file
                        val file = uriToFile(context, uri)
                        if (file != null) {
                            val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                            val body = MultipartBody.Part.createFormData("files", file.name, requestFile)
                            multipartList.add(body)
                        }
                    }

                    // Eseguiamo la chiamata di Upload
                    if (multipartList.isNotEmpty()) {
                        val uploadResponse = RetrofitClient.apiService.uploadImages(multipartList)
                        if (uploadResponse.isSuccessful && uploadResponse.body() != null) {
                            val uploadedUrls = uploadResponse.body()!!
                            if (finalImageUrls == null) finalImageUrls = uploadedUrls
                            else (finalImageUrls as MutableList).addAll(uploadedUrls)
                        } else {
                            errorMessage = "Errore durante il caricamento delle immagini."
                            isLoading = false
                            return@launch
                        }
                    }
                }

                // FASE 2: Creiamo il viaggio nel Database usando gli URL definitivi
                val request = TripRequest(
                    title = title,
                    description = description,
                    price = price,
                    startDate = startDate,
                    endDate = endDate,
                    availableSpots = spots,
                    location = location,
                    imageUrls = finalImageUrls
                )

                val response = RetrofitClient.apiService.createTrip("Bearer $token", request)

                if (response.isSuccessful) {
                    isSuccess = true
                } else {
                    errorMessage = "Errore nella creazione del viaggio: ${response.code()}"
                }
            } catch (e: Exception) {
                errorMessage = "Impossibile contattare il server. Controlla la connessione."
            } finally {
                isLoading = false
            }
        }
    }
}