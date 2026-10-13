package com.example.travelapp.network

import com.google.gson.annotations.SerializedName

data class TripResponse(
    val id: Long,
    val title: String,
    val description: String?,
    val price: Double,
    val startDate: String,
    val endDate: String,
    val availableSpots: Int,
    val location: String?,
    @SerializedName("imageUrls")
    val imageUrls: List<String>? = null,
    val averageRating: Double,
    val reviewCount: Int,
    val organizerId: Long,
    val organizerName: String?
)