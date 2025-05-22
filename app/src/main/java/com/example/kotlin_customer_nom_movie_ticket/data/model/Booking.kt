package com.example.kotlin_customer_nom_movie_ticket.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.text.SimpleDateFormat
import java.util.*

@Parcelize
data class Booking(
    val bill_id: String,
    val movie_id: String,
    val title: String,
    val age_rating: String,
    val cinema_name: String,
    val showtime_time: String?,
    val seat_ids: List<String>,
    val duration: Int,
    val poster_url: String,
    val director: String? = null,
    val genre: String? = null,
    val room_name: String? = null,
    val seat_price: Double = 0.0,
    val food_price: Double = 0.0,
    val convenience_fee: Double = 0.0,
    val total_price: Double = 0.0,
    val discount: Double = 0.0,
    val payment_method: String? = null,
    val payment_status: String? = null,
    val order_id: String? = null,

    var isReminderEnabled: Boolean = false
) : Parcelable {
    val isUpcoming: Boolean
        get() {
            if (showtime_time == null) return false
            return try {
                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault())
                val showtime = sdf.parse(showtime_time) ?: return false
                showtime.after(Date())
            } catch (e: Exception) {
                false
            }
        }

    val date: String? get() = showtime_time?.substringBefore("T")
    val time: String? get() = showtime_time?.substringAfter("T")
}