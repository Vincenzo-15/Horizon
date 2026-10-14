package com.example.travelapp.network

data class Reservation(
    val id: Long,
    val reservationDate: String,
    val status: String,
    val travelerId: Long,
    val tripId: Long,
    val tripTitle: String,
    val bookedSpots: Int,
    val totalPaid: Double
)