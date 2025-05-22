package com.example.kotlin_customer_nom_movie_ticket.ui.view.activity

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.kotlin_customer_nom_movie_ticket.R
import com.example.kotlin_customer_nom_movie_ticket.data.model.Booking
import com.example.kotlin_customer_nom_movie_ticket.databinding.ActivityBookingDetailBinding
import java.text.NumberFormat
import java.util.Locale

class BookingDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBookingDetailBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookingDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val booking = intent.getParcelableExtra<Booking>("BOOKING")
        if (booking == null) {
            android.widget.Toast.makeText(this, "Booking data not found", android.widget.Toast.LENGTH_SHORT).show()
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
            return
        } else {
            Log.d("BookingDetailActivity", "Booking data: $booking")
        }

        val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))

        Glide.with(this)
            .load(booking.poster_url)
            .into(binding.picMovie)
        binding.tvTitle.text = booking.title
        binding.tvShowtimeTime.text = booking.showtime_time
        binding.tvCinemaName.text = booking.cinema_name
        binding.tvSeats.text = booking.seat_ids.toString()
        binding.tvDuration.text = booking.duration.toString() + " phút"
        binding.tvDirectorName.text = booking.director
        binding.tvAgeRate.text = booking.age_rating
        binding.tvGenre.text = booking.genre
        binding.tvRoomName.text = booking.room_name
        binding.tvSeats.text = booking.seat_ids.joinToString()
        binding.tvDate.text = booking.date
        binding.tvShowtimeTime.text = booking.time
        binding.tvFee.text = formatter.format(booking.convenience_fee) + "đ"
        if (booking.food_price > 0) {
            binding.lnFoodPrice.visibility = android.view.View.VISIBLE
            binding.tvPriceOfFood.text = formatter.format(booking.food_price) + "đ"
        } else {
            binding.lnFoodPrice.visibility = android.view.View.GONE
        }
        if (booking.discount > 0) {
            binding.lnDiscount.visibility = android.view.View.VISIBLE
            binding.tvDiscount.text = formatter.format(booking.discount) + "đ"
        } else {
            binding.lnDiscount.visibility = android.view.View.GONE
        }
        binding.tvActualPay.text = formatter.format(booking.total_price) + "đ"
        binding.tvTotalPrice.text = formatter.format(booking.total_price) + "đ"
        binding.tvStatusPayment.text = booking.payment_status
        binding.tvPaymentMethod.text = booking.payment_method
        binding.tvBookingId.text = booking.bill_id
        binding.tvPriceOfSeat.text = formatter.format(booking.seat_price) + "đ"
        binding.tvPriceOfFood.text = formatter.format(booking.food_price) + "đ"

        binding.btnViewTicket.setOnClickListener {
            val intent = android.content.Intent(this, ViewTicketActivity::class.java)
            intent.putExtra("total_price", booking.total_price)
            intent.putExtra("poster_url", booking.poster_url)
            intent.putExtra("director", booking.director)
            intent.putExtra("genre", booking.genre)
            intent.putExtra("cinema_name", booking.cinema_name)
            intent.putExtra("showtime_time", booking.showtime_time)
            intent.putExtra("title", booking.title)
            intent.putExtra("duration", booking.duration)
            intent.putExtra("age_rating", booking.age_rating)
            intent.putExtra("seat_name", booking.seat_ids.joinToString())
            intent.putExtra("bill_id", booking.bill_id)
            intent.putExtra("room_name", booking.room_name)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        binding.btnBack.setOnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

    }
}