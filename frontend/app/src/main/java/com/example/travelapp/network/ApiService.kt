package com.example.travelapp.network

import com.google.gson.annotations.SerializedName // <-- Import fondamentale da aggiungere
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

interface ApiService {

    @POST("/api/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>

    @POST("/api/auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<Unit>

    @GET("/api/trips/popular")
    suspend fun getPopularTrips(
        @Header("Authorization") token: String
    ): Response<List<TripResponse>>

    @GET("/api/trips/my-trips")
    suspend fun getMyTrips(@Header("Authorization") token: String): Response<List<TripResponse>>

    @POST("/api/trips/create")
    suspend fun createTrip(
        @Header("Authorization") token: String,
        @Body request: TripRequest
    ): Response<TripResponse>

    @GET("/api/trips/{id}")
    suspend fun getTripById(
        @Header("Authorization") token: String,
        @Path("id") tripId: Long
    ): Response<TripResponse>

    @DELETE("/api/trips/{id}")
    suspend fun deleteTrip(
        @Header("Authorization") token: String,
        @Path("id") tripId: Long
    ): Response<Unit>
}

data class TripRequest(
    val title: String,
    val description: String,
    val price: Double,
    val startDate: String, // Formato richiesto da Spring: "YYYY-MM-DD"
    val endDate: String,   // Formato richiesto da Spring: "YYYY-MM-DD"
    val availableSpots: Int,
    val location: String,

    @SerializedName("imageUrls")
    val imageUrls: List<String>? = null
)

data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    val role: String,

    // Assicuriamo che il JSON inviato abbia esattamente questa chiave
    @SerializedName("partitaIva")
    val partitaIva: String? = null,

    @SerializedName("telefono")
    val telefono: String? = null
)