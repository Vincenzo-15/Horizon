package com.example.travelapp.network

import com.google.gson.annotations.SerializedName // <-- Import fondamentale da aggiungere
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

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
    ): Response<List<TripDTO>>

    @GET("/api/trips/my-trips")
    suspend fun getMyTrips(@Header("Authorization") token: String): Response<List<TripDTO>>

    @POST("/api/trips/create")
    suspend fun createTrip(
        @Header("Authorization") token: String,
        @Body request: TripRequest
    ): Response<TripDTO>

    @GET("/api/trips/{id}")
    suspend fun getTripById(
        @Header("Authorization") token: String,
        @Path("id") tripId: Long
    ): Response<TripDTO>

    @DELETE("/api/trips/{id}")
    suspend fun deleteTrip(
        @Header("Authorization") token: String,
        @Path("id") tripId: Long
    ): Response<Unit>

    @Multipart
    @POST("/api/upload")
    suspend fun uploadImages(
        @Part files: List<MultipartBody.Part>
    ): Response<List<String>>

    @POST("/api/reservations/create/{tripId}")
    suspend fun bookTrip(
        @Header("Authorization") token: String,
        @Path("tripId") tripId: Long,
        @Query("spots") spots: Int
    ): retrofit2.Response<ResponseBody>

    @GET("/api/reservations/agency-orders")
    suspend fun getAgencyOrders(
        @Header("Authorization") token: String
    ): retrofit2.Response<List<com.example.travelapp.network.ReservationDTO>>

    // --- LISTE PREFERITI ---

    // 1. Legge le liste dell'utente
    @GET("/api/lists/my-lists")
    suspend fun getMyLists(
        @Header("Authorization") token: String
    ): retrofit2.Response<List<com.example.travelapp.network.ItineraryListDTO>>

    // 2. Crea una nuova lista
    @POST("/api/lists/create")
    suspend fun createList(
        @Header("Authorization") token: String,
        @Query("name") name: String,
        @Query("visibility") visibility: String
    ): retrofit2.Response<com.example.travelapp.network.ItineraryListDTO>

    // 3. Aggiunge il viaggio alla lista selezionata
    @POST("/api/lists/{listId}/add-trip/{tripId}")
    suspend fun addTripToList(
        @Header("Authorization") token: String,
        @Path("listId") listId: Long,
        @Path("tripId") tripId: Long
    ): retrofit2.Response<com.example.travelapp.network.ItineraryListDTO>

    @GET("/api/lists/{listId}")
    suspend fun getListDetails(
        @Header("Authorization") token: String,
        @Path("listId") listId: Long
    ): retrofit2.Response<com.example.travelapp.network.ItineraryListDTO>

    // 5. Scarica tutti gli oggetti TripDTO completi che sono dentro una lista
    @GET("/api/lists/{listId}/trips")
    suspend fun getTripsInList(
        @Header("Authorization") token: String,
        @Path("listId") listId: Long
    ): retrofit2.Response<List<TripDTO>>
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