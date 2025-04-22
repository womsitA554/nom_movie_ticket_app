package com.example.kotlin_customer_nom_movie_ticket.data.model

import java.time.LocalDate

data class Day(
    val dayNumber: String,
    val dayName: String,
    val fullDate: LocalDate,
    val isSelected: Boolean
)