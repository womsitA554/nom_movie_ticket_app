package com.example.kotlin_customer_nom_movie_ticket.data.model

data class Notification(
    val notification_id: String? = null,
    val title: String? = null,
    val message: String? = null,
    val timestamp: Long? = null,
    val type: String? = null,
    val movie_id: String? = null,
    val bill_id: String? = null
) {
    constructor() : this(null, null, null, null, null, null, null)
}