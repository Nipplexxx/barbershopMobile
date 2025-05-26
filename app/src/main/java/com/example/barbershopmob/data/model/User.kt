package com.example.barbershopmob.data.model

data class User(
    val userId: String = "",
    val email: String = "",
    val role: String = "",
    val fullName: String = "",
    val phone: String = "",
    val points: Int = 0
)