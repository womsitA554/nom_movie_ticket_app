package com.example.kotlin_customer_nom_movie_ticket.data.model

data class Seat(
    val seat_id: String = "",
    val row_number: String = "",
    val seat_number: String = "",
    val room_id: String = "",
    val seat_type: String = "",
    var status: String = "",
)
