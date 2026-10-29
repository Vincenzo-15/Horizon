package com.example.travelapp.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// Usando 'object', Kotlin crea un Singleton: un'unica istanza condivisa in tutta l'app
object RetrofitClient {

    // Questo è l'indirizzo magico per collegare l'emulatore Android al localhost
    private const val BASE_URL = "http://10.0.2.2:8080"


    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
