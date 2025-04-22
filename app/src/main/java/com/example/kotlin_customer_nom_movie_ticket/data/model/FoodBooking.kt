package com.example.kotlin_customer_nom_movie_ticket.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class FoodItem(
    val itemId: String = "",
    val title: String = "",
    val picUrl: String = "",
    val price: Double = 0.0,
    val quantity: Int = 0
) : Parcelable

@Parcelize
data class FoodBooking(
    val food_bill_id: String = "",
    val bill_id: String? = null,
    val customer_id: String = "",
    val food_items: List<FoodItem> = emptyList(),
    val total_price: Double = 0.0,
    val payment_method: String = "",
    val payment_status: String = "",
    val order_time: Long = 0,
    val pick_up_time: String = ""
) : Parcelable