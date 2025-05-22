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
import java.text.NumberFormat
import java.util.Locale

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
        binding.tvTimePickUp.text = "Nhận đồ lúc $time"
        binding.tvTimeValue.text = date

        val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))

        val foodPrice = foodBooking.total_price
        val fee = foodBooking.fee
        val discount = foodBooking.discount
        val totalPrice = foodBooking.total_price_to_pay
        binding.tvFoodPrice.text = formatter.format(foodPrice) + "đ"
        binding.tvFee.text =  formatter.format(fee) + "đ"
        if (discount > 0) {
            binding.lnDiscount.visibility = android.view.View.VISIBLE
            binding.tvDiscount.text = formatter.format(discount) + "đ"
        } else {
            binding.lnDiscount.visibility = android.view.View.GONE
        }
        binding.tvActualPay.text = formatter.format(totalPrice) + "đ"
        binding.tvTotalPrice.text = formatter.format(totalPrice) + "đ"
        binding.tvStatusPayment.text = foodBooking.payment_status
        binding.tvPaymentMethod.text = foodBooking.payment_method
        binding.tvBookingId.text = foodBooking.food_bill_id

        binding.btnBack.setOnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

    }

}