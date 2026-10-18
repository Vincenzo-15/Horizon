package com.example.travelapp.network

data class ItineraryListDTO(
    val id: Long,
    val name: String,
    val visibility: String,
    val travelerId: Long,
    val ownerName: String? = null,
    val ownerEmail: String? = null,
    val tripIds: List<Long>? = emptyList()
)