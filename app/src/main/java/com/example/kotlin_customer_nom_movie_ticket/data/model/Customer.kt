package com.example.kotlin_customer_nom_movie_ticket.data.model

data class Customer(
    val customer_id: String? = null,
    val full_name: String? = null,
    val phone_number: String? = null,
    val email: String? = null,
    val avatar: String? = null,
    val fcm_token: String? = null,
    val point: Int? = null,
)

