package com.example.travelapp.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelapp.network.RegisterRequest
import com.example.travelapp.network.RetrofitClient
import kotlinx.coroutines.launch

class RegisterViewModel : ViewModel() {

    var isLoading by mutableStateOf(false)
    var isSuccess by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    private fun isEmailValid(email: String): Boolean {
        val emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+"
        return email.matches(emailPattern.toRegex())
    }

    private fun isPasswordStrong(password: String): Boolean {
        val passwordPattern = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).{8,}$"
        return password.matches(passwordPattern.toRegex())
    }

    fun eseguiRegistrazione(
        nome: String,
        email: String,
        pass: String,
        confirmPass: String,
        isOrganizzatore: Boolean,
        partitaIva: String,
        telefono: String
    ) {
        if (nome.isBlank() || email.isBlank() || pass.isBlank()) {
            errorMessage = "Per favore, compila tutti i campi obbligatori."
            return
        }

        if (isOrganizzatore && (partitaIva.isBlank() || telefono.isBlank())) {
            errorMessage = "Le agenzie devono inserire Partita IVA e Telefono."
            return
        }

        if (!isEmailValid(email.trim())) {
            errorMessage = "Inserisci un indirizzo email valido."
            return
        }

        if (!isPasswordStrong(pass)) {
            errorMessage = "La password deve avere almeno 8 caratteri, una maiuscola, un numero e un carattere speciale (!@#$%^&*)."
            return
        }

        if (pass != confirmPass) {
            errorMessage = "Le password non coincidono."
            return
        }

        val ruolo = if (isOrganizzatore) "ORGANIZZATORE" else "VIAGGIATORE"

        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            try {
                val pIvaFinale = if (isOrganizzatore) partitaIva.trim() else null
                val telFinale = telefono.trim().takeIf { it.isNotBlank() }

                val request = RegisterRequest(
                    name = nome.trim(),
                    email = email.trim(),
                    password = pass,
                    role = ruolo,
                    partitaIva = pIvaFinale,
                    telefono = telFinale
                )
                val response = RetrofitClient.apiService.register(request)

                if (response.isSuccessful) {
                    isSuccess = true
                } else {
                    errorMessage = when (response.code()) {
                        400 -> "I dati non sono validi o questa email è già registrata."
                        401, 403 -> "Non sei autorizzato a eseguire questa operazione."
                        404 -> "Servizio momentaneamente non trovato."
                        409 -> "Questa email è già in uso. Prova ad accedere."
                        500, 502, 503 -> "Ops! Problemi sui nostri server. Riprova tra poco."
                        else -> "Si è verificato un errore imprevisto. Riprova più tardi."
                    }
                }
            } catch (e: Exception) {
                errorMessage = "Impossibile connettersi al server."
            } finally {
                isLoading = false
            }
        }
    }
}