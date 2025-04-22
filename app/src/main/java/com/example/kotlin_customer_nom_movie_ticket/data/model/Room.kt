package com.example.kotlin_customer_nom_movie_ticket.data.model

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class Room(
    val room_id: String = "",
    val room_name: String = "",
    val cinema_id: String = "",
    val capacity: Int = 0,
    val created_at: String = ""
)