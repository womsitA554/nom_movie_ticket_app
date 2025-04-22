package com.example.kotlin_customer_nom_movie_ticket.data.model

data class PointTransaction(
    val transaction_id: String = "",
    val customer_id: String = "",
    val points: Int = 0,
    val type: String = "",
    val amount: Double = 0.0,
    val description: String = "",
    val created_at: Long = 0L
)