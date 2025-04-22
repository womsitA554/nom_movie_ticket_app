package com.example.kotlin_customer_nom_movie_ticket.ui.view.activity

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kotlin_customer_nom_movie_ticket.R
import com.example.kotlin_customer_nom_movie_ticket.data.model.Booking
import com.example.kotlin_customer_nom_movie_ticket.data.model.FoodBooking
import com.example.kotlin_customer_nom_movie_ticket.databinding.ActivityFoodPaymentDetailBinding
import com.example.kotlin_customer_nom_movie_ticket.databinding.ActivityOrderFoodDetailBinding
import com.example.kotlin_customer_nom_movie_ticket.ui.adapter.FoodOrderDetailAdapter

class OrderFoodDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOrderFoodDetailBinding
    private lateinit var adapter: FoodOrderDetailAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrderFoodDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = FoodOrderDetailAdapter(listOf())
        val foodBooking = intent.getParcelableExtra<FoodBooking>("FOOD_BOOKING")
        if (foodBooking == null) {
            android.widget.Toast.makeText(
                this,
                "Booking data not found",
                android.widget.Toast.LENGTH_SHORT
            ).show()
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
            return
        }

        binding.rcvCart.isNestedScrollingEnabled = false
        binding.rcvCart.layoutManager = LinearLayoutManager(this)
        adapter = FoodOrderDetailAdapter(foodBooking.food_items)
        binding.rcvCart.adapter = adapter

        val pickUpTime = foodBooking.pick_up_time
        val parts = pickUpTime.split(" ")
        val time = parts.last() // "11:30"
        val date = parts.dropLast(1).joinToString(" ")
        binding.tvTimePickUp.text = "Pick up at $time"
        binding.tvTimeValue.text = date

        val foodPrice = foodBooking.total_price
        binding.tvFoodPrice.text = "$$foodPrice"
        binding.tvFee.text = "$${foodPrice * 0.01}"
        binding.tvActualPay.text = "$${foodPrice + foodPrice * 0.01}"

        binding.tvTotalPrice.text = "$${foodPrice + foodPrice * 0.01}"
        binding.tvStatusPayment.text = foodBooking.payment_status
        binding.tvPaymentMethod.text = foodBooking.payment_method
        binding.tvBookingId.text = foodBooking.food_bill_id

        binding.btnBack.setOnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

    }

}