package com.example.travelapp.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelapp.network.LoginRequest
import com.example.travelapp.network.RetrofitClient
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {

    // 1. LA MEMORIA DEL CERVELLO (Gli Stati)
    // Queste variabili dicono alla grafica cosa mostrare (es. la rotellina di caricamento)
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)
    var loginSuccess by mutableStateOf(false)
    var token by mutableStateOf<String?>(null)

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
}