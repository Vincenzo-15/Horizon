package com.example.travelapp.utils

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject
import android.util.Base64
import android.util.Log


class TokenManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("TravelAppPrefs", Context.MODE_PRIVATE)

    // Funzione per salvare il token
    fun saveToken(token: String) {
        prefs.edit().putString("jwt_token", token).apply()
    }

    // Funzione per leggere il token (restituisce null se non c'è)
    fun getToken(): String? {
        return prefs.getString("jwt_token", null)
    }

    // Funzione per il Logout (cancella il token)
    fun clearToken() {
        prefs.edit().remove("jwt_token").apply()
    }

    fun getRoleFromToken(): String? {
        val token = getToken() ?: return null

        return try {
            // Dividiamo il token usando il punto come separatore
            val parts = token.split(".")
            if (parts.size != 3) return null

            // Il payload è la seconda parte (indice 1)
            val payload = parts[1]
            val decodedBytes = Base64.decode(payload, Base64.URL_SAFE)
            val decodedString = String(decodedBytes, Charsets.UTF_8)

            // Convertiamo la stringa in JSON e cerchiamo "role"
            val jsonObject = JSONObject(decodedString)
            jsonObject.optString("role", null)

        } catch (e: Exception) {
            Log.e("JWT_DECODE", "Errore nella decodifica del token", e)
            null
        }
    }

    // Verifica se il token esiste e NON è scaduto
    fun isTokenValid(): Boolean {
        val token = getToken() ?: return false
        return try {
            val parts = token.split(".")
            if (parts.size < 2) return false
            val payload = String(android.util.Base64.decode(parts[1], android.util.Base64.URL_SAFE))
            val jsonObject = org.json.JSONObject(payload)

            // "exp" nel JWT è espresso in secondi, lo convertiamo in millisecondi
            val expirationTimeMillis = jsonObject.getLong("exp") * 1000
            System.currentTimeMillis() < expirationTimeMillis
        } catch (e: Exception) {
            false
        }
    }
}