package com.example.kotlin_customer_nom_movie_ticket.data.model

data class Food(
    val itemId:String = "",
    val title:String ="",
    val category: String = "",
    val picUrl: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val isAvailable:Boolean = true
)
