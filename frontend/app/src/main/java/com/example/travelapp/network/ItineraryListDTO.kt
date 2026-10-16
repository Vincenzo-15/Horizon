package com.example.travelapp.network

data class ItineraryListDTO(
    val id: Long,
    val name: String,
    val visibility: String,
    val travelerId: Long,
    val tripIds: List<Long>
)