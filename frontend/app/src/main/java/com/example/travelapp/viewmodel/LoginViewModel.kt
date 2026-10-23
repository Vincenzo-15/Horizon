package com.example.travelapp.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelapp.network.ForgotPasswordRequest
import com.example.travelapp.network.LoginRequest
import com.example.travelapp.network.RetrofitClient
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {

    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)
    var loginSuccess by mutableStateOf(false)
    var token by mutableStateOf<String?>(null)

    // Stati per l'invio dell'email di recupero password
    var isSendingResetEmail by mutableStateOf(false)
    var resetFeedbackMessage by mutableStateOf<String?>(null)
    var isResetError by mutableStateOf(false)

    fun eseguiLogin(emailInserita: String, passwordInserita: String) {
        if (emailInserita.isBlank() || passwordInserita.isBlank()) {
            errorMessage = "Inserisci sia l'email che la password."
            return
        }

        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            try {
                val request = LoginRequest(emailInserita.trim(), passwordInserita)
                val response = RetrofitClient.apiService.login(request)

                if (response.isSuccessful && response.body() != null) {
                    token = response.body()!!.token
                    loginSuccess = true
                } else {
                    errorMessage = "Credenziali errate (Errore 400/403)"
                }
            } catch (e: Exception) {
                errorMessage = "Impossibile connettersi al server"
            } finally {
                isLoading = false
            }
        }
    }

    fun sendPasswordResetEmail(email: String, onEmailSent: () -> Unit) {
        if (email.isBlank() || !email.contains("@")) {
            isResetError = true
            resetFeedbackMessage = "Inserisci un indirizzo email valido."
            return
        }

        viewModelScope.launch {
            isSendingResetEmail = true
            resetFeedbackMessage = null
            try {
                val response = RetrofitClient.apiService.requestPasswordReset(
                    ForgotPasswordRequest(email = email.trim())
                )
                if (response.isSuccessful) {
                    isResetError = false
                    resetFeedbackMessage = "Abbiamo inviato un'email a ${email.trim()} con il link sicuro per reimpostare la password. Apri il link dalla tua casella di posta per scegliere la nuova password."
                    onEmailSent()
                } else {
                    val err = response.errorBody()?.string()
                    isResetError = true
                    resetFeedbackMessage = if (!err.isNullOrBlank()) err else "Nessun account trovato con questa email."
                }
            } catch (e: Exception) {
                isResetError = true
                resetFeedbackMessage = "Errore di connessione al server."
            } finally {
                isSendingResetEmail = false
            }
        }
    }

    fun clearResetFeedback() {
        resetFeedbackMessage = null
    }
}