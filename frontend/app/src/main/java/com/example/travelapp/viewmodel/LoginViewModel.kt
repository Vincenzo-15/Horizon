package com.example.travelapp.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelapp.network.LoginRequest
import com.example.travelapp.network.ResetPasswordRequest
import com.example.travelapp.network.RetrofitClient
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {

    // 1. LA MEMORIA DEL CERVELLO (Gli Stati)
    // Queste variabili dicono alla grafica cosa mostrare (es. la rotellina di caricamento)
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)
    var loginSuccess by mutableStateOf(false)
    var token by mutableStateOf<String?>(null)

    // Stati per il Recupero Password
    var isResettingPassword by mutableStateOf(false)
    var resetFeedbackMessage by mutableStateOf<String?>(null)
    var isResetError by mutableStateOf(false)

    private fun isPasswordStrong(password: String): Boolean {
        val passwordPattern = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).{8,}$"
        return password.matches(passwordPattern.toRegex())
    }

    // 2. L'AZIONE (Il click del bottone)
    fun eseguiLogin(emailInserita: String, passwordInserita: String) {
        // viewModelScope.launch lancia il compito in "background" per non bloccare il telefono
        viewModelScope.launch {
            isLoading = true
            errorMessage = null // Resettiamo eventuali errori precedenti

            try {
                // Prepariamo il pacco (DTO)
                val request = LoginRequest(emailInserita, passwordInserita)

                // Chiediamo al postino (Retrofit) di portarlo al backend
                val response = RetrofitClient.apiService.login(request)

                // Controlliamo la risposta del server
                if (response.isSuccessful && response.body() != null) {
                    token = response.body()!!.token // Preso!
                    loginSuccess = true
                } else {
                    errorMessage = "Credenziali errate (Errore 400/403)"
                }
            } catch (e: Exception) {
                // Questo scatta se il server è spento o non c'è internet
                errorMessage = "Impossibile connettersi al server"
            } finally {
                isLoading = false // Spegniamo la rotellina di caricamento
            }
        }
    }

    fun resetPassword(email: String, newPassword: String, confirmPassword: String, onSuccess: () -> Unit) {
        if (email.isBlank() || newPassword.isBlank()) {
            isResetError = true
            resetFeedbackMessage = "Inserisci l'email del tuo account e la nuova password."
            return
        }
        if (!isPasswordStrong(newPassword)) {
            isResetError = true
            resetFeedbackMessage = "La password deve avere almeno 8 caratteri, una maiuscola, un numero e un carattere speciale (!@#$%^&+=!)."
            return
        }
        if (newPassword != confirmPassword) {
            isResetError = true
            resetFeedbackMessage = "Le due password non coincidono."
            return
        }

        viewModelScope.launch {
            isResettingPassword = true
            resetFeedbackMessage = null
            try {
                val response = RetrofitClient.apiService.resetPassword(
                    ResetPasswordRequest(email = email.trim(), newPassword = newPassword)
                )
                if (response.isSuccessful) {
                    isResetError = false
                    resetFeedbackMessage = "Password reimpostata con successo! Ora puoi accedere con la nuova password."
                    onSuccess()
                } else {
                    val err = response.errorBody()?.string()
                    isResetError = true
                    resetFeedbackMessage = if (!err.isNullOrBlank()) err else "Nessun account trovato con questa email."
                }
            } catch (e: Exception) {
                isResetError = true
                resetFeedbackMessage = "Errore di connessione al server."
            } finally {
                isResettingPassword = false
            }
        }
    }

    fun clearResetFeedback() {
        resetFeedbackMessage = null
    }
}
