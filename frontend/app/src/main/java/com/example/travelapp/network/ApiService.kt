package com.example.travelapp.network

import com.google.gson.annotations.SerializedName
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
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

    @PUT("/api/trips/{id}")
    suspend fun updateTrip(
        @Header("Authorization") token: String,
        @Path("id") tripId: Long,
        @Body request: TripRequest
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

    @GET("/api/reservations/my-reservations")
    suspend fun getMyReservations(
        @Header("Authorization") token: String
    ): Response<List<com.example.travelapp.network.ReservationDTO>>

    // --- LISTE PREFERITI ---

    // 1. Legge le liste dell'utente (create da lui o condivise con lui)
    @GET("/api/lists/my-lists")
    suspend fun getMyLists(
        @Header("Authorization") token: String
    ): retrofit2.Response<List<com.example.travelapp.network.ItineraryListDTO>>

    // 2. Crea una nuova lista (PRIVATE, SHARED, PUBLIC)
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

    // 4. Condivide una lista con un amico tramite email
    @POST("/api/lists/{listId}/share")
    suspend fun shareList(
        @Header("Authorization") token: String,
        @Path("listId") listId: Long,
        @Query("friendEmail") friendEmail: String
    ): retrofit2.Response<ResponseBody>

    // 5. Dettagli singola lista
    @GET("/api/lists/{listId}")
    suspend fun getListDetails(
        @Header("Authorization") token: String,
        @Path("listId") listId: Long
    ): retrofit2.Response<com.example.travelapp.network.ItineraryListDTO>

    // 6. Scarica tutti gli oggetti TripDTO completi dentro una lista
    @GET("/api/lists/{listId}/trips")
    suspend fun getTripsInList(
        @Header("Authorization") token: String,
        @Path("listId") listId: Long
    ): retrofit2.Response<List<TripDTO>>

    // 7. Modifica nome e visibilità di una lista (es. da SHARED a PRIVATE o viceversa)
    @PUT("/api/lists/{listId}")
    suspend fun updateList(
        @Header("Authorization") token: String,
        @Path("listId") listId: Long,
        @Query("name") name: String,
        @Query("visibility") visibility: String
    ): retrofit2.Response<com.example.travelapp.network.ItineraryListDTO>

    // 8. Elimina una lista
    @DELETE("/api/lists/{listId}")
    suspend fun deleteList(
        @Header("Authorization") token: String,
        @Path("listId") listId: Long
    ): retrofit2.Response<Unit>

    // --- NOTIFICHE ---
    @GET("/api/notifications")
    suspend fun getMyNotifications(
        @Header("Authorization") token: String
    ): retrofit2.Response<List<com.example.travelapp.network.NotificationDTO>>

    @POST("/api/notifications/review/{tripId}")
    suspend fun addReviewToTrip(
        @Header("Authorization") token: String,
        @Path("tripId") tripId: Long,
        @Body request: CreateReviewRequest
    ): Response<TripDTO>

    // --- LIVE CHAT IN TEMPO REALE ---
    @GET("/api/notifications/chat/conversations")
    suspend fun getChatConversations(
        @Header("Authorization") token: String
    ): Response<List<ChatConversationDTO>>

    @GET("/api/notifications/chat/{tripId}")
    suspend fun getChatMessages(
        @Header("Authorization") token: String,
        @Path("tripId") tripId: Long,
        @Query("travelerEmail") travelerEmail: String? = null
    ): Response<List<ChatMessageDTO>>

    @POST("/api/notifications/chat/{tripId}")
    suspend fun sendChatMessage(
        @Header("Authorization") token: String,
        @Path("tripId") tripId: Long,
        @Body request: SendChatMessageRequest
    ): Response<ChatMessageDTO>

    // --- PROFILO UTENTE E IMPOSTAZIONI ---

    @GET("/api/users/me")
    suspend fun getMyProfile(
        @Header("Authorization") token: String
    ): Response<UserProfileDTO>

    @PUT("/api/users/me")
    suspend fun updateMyProfile(
        @Header("Authorization") token: String,
        @Body request: UpdateProfileRequest
    ): Response<UserProfileDTO>

    @PUT("/api/users/me/password")
    suspend fun changePassword(
        @Header("Authorization") token: String,
        @Body request: ChangePasswordRequest
    ): Response<ResponseBody>

    @POST("/api/auth/forgot-password")
    suspend fun requestPasswordReset(
        @Body request: ForgotPasswordRequest
    ): Response<ResponseBody>
}

data class TripRequest(
    val title: String,
    val description: String,
    val price: Double,
    val startDate: String,
    val endDate: String,
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
    @SerializedName("partitaIva")
    val partitaIva: String? = null,
    @SerializedName("telefono")
    val telefono: String? = null
)

data class UserProfileDTO(
    val id: Long,
    val email: String,
    val name: String,
    val role: String,
    val telefono: String? = null,
    val partitaIva: String? = null
)

data class UpdateProfileRequest(
    val name: String,
    val telefono: String?,
    val partitaIva: String? = null
)

data class ChangePasswordRequest(
    val oldPassword: String,
    val newPassword: String
)

data class CreateReviewRequest(
    val rating: Int,
    val comment: String
)
data class ForgotPasswordRequest(
    val email: String,
)

data class SendChatMessageRequest(
    val message: String,
    val travelerEmail: String? = null
)

data class ChatMessageDTO(
    val id: Long,
    val tripId: Long,
    val senderName: String,
    val senderEmail: String,
    val senderRole: String,
    val message: String,
    val timestamp: String
)

data class ChatConversationDTO(
    val tripId: Long,
    val tripTitle: String,
    val tripLocation: String,
    val agencyName: String,
    val agencyEmail: String,
    val travelerName: String,
    val travelerEmail: String,
    val lastMessage: String,
    val lastTimestamp: String,
    val lastUpdatedEpoch: Long,
    val unreadCount: Int
)