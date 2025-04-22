package com.example.kotlin_customer_nom_movie_ticket.data.model

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class PointHistory(
    val customer_id: String = "",
    val transaction_id: String = "",
    val created_at: Long = 0,
    val points: Int = 0,
    val type: String = "",
    val description: String = "",
    val amout: Double = 0.0,
)
