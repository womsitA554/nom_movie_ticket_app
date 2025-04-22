package com.example.kotlin_customer_nom_movie_ticket.data.model

import java.io.Serializable

data class Cart(
    val itemId: String? = null,
    val picUrl: String? = null,
    val title: String? = null,
    var price: Double? = null,
    var quantity: Int? = null
) : Serializable {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "itemId" to itemId,
            "picUrl" to picUrl,
            "title" to title,
            "price" to price,
            "quantity" to quantity
        )
    }
}
