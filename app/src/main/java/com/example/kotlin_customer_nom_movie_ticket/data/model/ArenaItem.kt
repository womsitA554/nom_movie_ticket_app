package com.example.kotlin_customer_nom_movie_ticket.data.model

data class ArenaItem(
    val arenaName: String,
    val cinemas: List<Cinema>,
    var isExpanded: Boolean = false
)
