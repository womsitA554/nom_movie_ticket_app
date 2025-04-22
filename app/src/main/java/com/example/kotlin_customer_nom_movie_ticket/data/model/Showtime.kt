package com.example.kotlin_customer_nom_movie_ticket.data.model

import java.time.LocalDateTime

data class Showtime(
    var showtime_id: String = "",
    val movie_id: String = "",
    val room_id: String = "",
    val showtime_time: String = "",
    val empty_chair: Int = 0,
    val status: String = "",
    val created_at: String = "",
)