package com.example.barbershopmob.models

data class ServiceItem(
    val id: String = "",
    val name: String = "",
    val dayOfWeek: String = "",
    val time: String = "",
    val barberId: String = "",
    val barberName: String = "",
    val price: Double = 0.0,
    val pointsPercent: Int = 0
)