package com.example.travelapp.network

data class NotificationDTO(
    val id: Long,
    val title: String,
    val message: String,
    val timestamp: String,
    val isRead: Boolean
)