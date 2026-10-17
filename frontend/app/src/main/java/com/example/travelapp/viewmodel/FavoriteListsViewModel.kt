package com.example.travelapp.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelapp.network.ItineraryListDTO
import com.example.travelapp.network.RetrofitClient
import kotlinx.coroutines.launch

class FavoriteListsViewModel : ViewModel() {

    var myLists by mutableStateOf<List<ItineraryListDTO>>(emptyList())
    var isLoading by mutableStateOf(true)
    var errorMessage by mutableStateOf<String?>(null)

    var isSubmitting by mutableStateOf(false)
    var feedbackMessage by mutableStateOf<String?>(null)
    var isFeedbackError by mutableStateOf(false)

    fun fetchMyLists(token: String) {
        if (token.isBlank()) return

        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val response = RetrofitClient.apiService.getMyLists("Bearer $token")
                if (response.isSuccessful && response.body() != null) {
                    myLists = response.body()!!.reversed()
                } else {
                    errorMessage = "Impossibile caricare le tue liste (${response.code()})."
                }
            } catch (e: Exception) {
                errorMessage = "Errore di connessione al server."
            } finally {
                isLoading = false
            }
        }
    }

    fun createNewList(
        token: String,
        name: String,
        visibility: String,
        initialFriendEmail: String? = null
    ) {
        if (token.isBlank() || name.isBlank()) return

        viewModelScope.launch {
            isSubmitting = true
            try {
                val authHeader = "Bearer $token"
                val createRes = RetrofitClient.apiService.createList(authHeader, name.trim(), visibility)

                if (createRes.isSuccessful && createRes.body() != null) {
                    val createdList = createRes.body()!!

                    // Se l'utente ha scelto "SHARED" e ha inserito subito l'email di un amico, condividiamola
                    if (visibility == "SHARED" && !initialFriendEmail.isNullOrBlank()) {
                        val shareRes = RetrofitClient.apiService.shareList(
                            authHeader,
                            createdList.id,
                            initialFriendEmail.trim()
                        )
                        if (shareRes.isSuccessful) {
                            isFeedbackError = false
                            feedbackMessage = "Lista \"${createdList.name}\" creata e condivisa con $initialFriendEmail!"
                        } else {
                            isFeedbackError = true
                            feedbackMessage = "Lista creata, ma impossibile aggiungere $initialFriendEmail (verifica che l'utente sia registrato)."
                        }
                    } else {
                        isFeedbackError = false
                        feedbackMessage = "Lista \"${createdList.name}\" creata con successo!"
                    }
                    fetchMyLists(token)
                } else {
                    isFeedbackError = true
                    feedbackMessage = "Errore nella creazione della lista (${createRes.code()})."
                }
            } catch (e: Exception) {
                isFeedbackError = true
                feedbackMessage = "Errore di rete durante la creazione della lista."
            } finally {
                isSubmitting = false
            }
        }
    }

    fun shareListWithFriend(token: String, listId: Long, listName: String, friendEmail: String) {
        if (token.isBlank() || friendEmail.isBlank()) return

        viewModelScope.launch {
            isSubmitting = true
            try {
                val response = RetrofitClient.apiService.shareList(
                    "Bearer $token",
                    listId,
                    friendEmail.trim()
                )
                if (response.isSuccessful) {
                    isFeedbackError = false
                    feedbackMessage = "Hai condiviso \"$listName\" con $friendEmail!"
                    fetchMyLists(token)
                } else {
                    isFeedbackError = true
                    feedbackMessage = "Impossibile condividere la lista: controlla che l'email \"$friendEmail\" corrisponda a un utente registrato."
                }
            } catch (e: Exception) {
                isFeedbackError = true
                feedbackMessage = "Errore di connessione durante la condivisione."
            } finally {
                isSubmitting = false
            }
        }
    }

    fun clearFeedback() {
        feedbackMessage = null
    }
}