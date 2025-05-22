package com.example.kotlin_customer_nom_movie_ticket.data.model

data class Review(
    val rating_id: String? = null,
    val customer_id: String? = null,
    val content: String? = null,
    val likes: Int = 0,
    val dislikes: Int = 0,
    val liked_by: Map<String, Boolean>? = null,
    val disliked_by: Map<String, Boolean>? = null,
    val full_name: String? = null,
    val avatar: String? = null,
    val timestamp: Long = 0
)
