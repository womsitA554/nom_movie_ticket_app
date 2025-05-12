package com.example.kotlin_customer_nom_movie_ticket.ui.view.activity

import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.kotlin_customer_nom_movie_ticket.R
import com.example.kotlin_customer_nom_movie_ticket.data.model.Cart
import com.example.kotlin_customer_nom_movie_ticket.databinding.ActivityFoodAndDrinkBinding
import com.example.kotlin_customer_nom_movie_ticket.helper.HorizontalSpaceItemDecoration
import com.example.kotlin_customer_nom_movie_ticket.ui.adapter.FoodAdapter
import com.example.kotlin_customer_nom_movie_ticket.ui.adapter.FoodOrderAdapter
import com.example.kotlin_customer_nom_movie_ticket.util.CartManager
import com.example.kotlin_customer_nom_movie_ticket.util.SessionManager
import com.example.kotlin_customer_nom_movie_ticket.viewmodel.FoodViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.text.NumberFormat
import java.util.Locale

class FoodAndDrinkActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFoodAndDrinkBinding
    private lateinit var foodAdapter: FoodAdapter
    private lateinit var cartAdapter: FoodOrderAdapter
    private lateinit var foodViewModel: FoodViewModel
    private lateinit var countDownTimer: CountDownTimer
    private lateinit var userId: String
    private var quantity = 0
    private lateinit var cartManager: CartManager
    private lateinit var broadcastReceiver: BroadcastReceiver
    private var totalPriceSeats = 0.0
    private var currentTimeLeft = 0L
    private var listCart = mutableListOf<Cart>()
    private var isShowed = false

    // Biến để theo dõi trạng thái load của từng danh mục
    private var isPopularFoodLoaded = false
    private var isFoodLoaded = false
    private var isDrinkLoaded = false
    private var isComboLoaded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFoodAndDrinkBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Khởi tạo UI ban đầu: hiển thị progressBar, ẩn scrollView
        binding.progressBar.visibility = View.VISIBLE
        binding.progressBar.playAnimation() // Bắt đầu animation
        binding.scrollView.visibility = View.GONE

        userId = SessionManager.getUserId(this) ?: run {
            finish()
            return
        }
        foodViewModel = ViewModelProvider(this)[FoodViewModel::class.java]
        cartManager = CartManager(this)

        totalPriceSeats = intent.getDoubleExtra("total_price_seats", 0.0)
        val cinemaId = intent.getStringExtra("cinema_id")
        val cinemaName = intent.getStringExtra("cinema_name")
        val showtimeId = intent.getStringExtra("showtime_id")
        val showtimeTime = intent.getStringExtra("showtime_time")
        val roomId = intent.getStringExtra("room_id")
        val movieId = intent.getStringExtra("movie_id")
        val movieCountry = intent.getStringExtra("country")
        val moviePosterUrl = intent.getStringExtra("poster_url")
        val movieTitle = intent.getStringExtra("title")
        val movieReleaseYear = intent.getStringExtra("release_year")
        val movieLanguage = intent.getStringExtra("language")
        val movieDuration = intent.getIntExtra("duration", 0)
        val movieGenre = intent.getStringExtra("genre")
        val movieSynopsis = intent.getStringExtra("synopsis")
        val movieDirectorId = intent.getStringExtra("director_id")
        val movieStatus = intent.getStringExtra("status")
        val movieTrailerUrl = intent.getStringExtra("trailer_url")
        val movieBanner = intent.getStringExtra("banner")
        val movieAgeRating = intent.getStringExtra("age_rating")
        val movieRating = intent.getFloatExtra("rating", 0f)
        val movieActorIds = intent.getStringArrayListExtra("actor_ids")
        val seatPrice = intent.getStringExtra("seat_price")
        val seatName = intent.getStringExtra("seat_name")
        val timeLeft = intent.getLongExtra("time_left", 0L)
        val ticketIds = intent.getStringArrayListExtra("ticket_ids")
        val selectedSeatIds = intent.getStringArrayListExtra("selected_seat_ids")

        startTimer(timeLeft)
        setupRecycleView()

        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    "PAYMENT_SUCCESS" -> {
                        updateCartUI()
                        setResult(RESULT_OK)
                        finish()
                    }
                    "UPDATE_CART", "CART_UPDATED" -> updateCartUI()
                }
            }
        }
        val intentFilter = IntentFilter().apply {
            addAction("PAYMENT_SUCCESS")
            addAction("UPDATE_CART")
            addAction("CART_UPDATED")
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, intentFilter)

        foodViewModel.fetchAllFood()
        foodViewModel.fetchAllDrink()
        foodViewModel.fetchAllCombo()

        // Thiết lập observers
        setupObservers(
            cinemaId, cinemaName, showtimeId, showtimeTime, roomId, movieId, movieCountry,
            moviePosterUrl, movieTitle, movieReleaseYear, movieLanguage, movieDuration,
            movieGenre, movieSynopsis, movieDirectorId, movieStatus, movieTrailerUrl,
            movieBanner, movieAgeRating, movieRating, movieActorIds, seatPrice, seatName,
            ticketIds, selectedSeatIds
        )

        listCart = cartManager.getCart(userId)
        cartAdapter = FoodOrderAdapter(listCart.toMutableList()).apply {
            onClickDeleteItem = { food, position ->
                cartManager.removeItemFromCart(userId, food.itemId!!)
                updateCartUI()
            }
            onClickAddItem = { cart, i ->
                val currentQuantity = cart.quantity ?: 1
                val newQuantity = currentQuantity + 1
                val unitPrice = cart.price!! / currentQuantity
                val newPrice = unitPrice * newQuantity
                cart.quantity = newQuantity
                cart.price = newPrice
                cartManager.updateQuantity(userId, cart.itemId!!, newQuantity)
                cartManager.updatePrice(userId, cart.itemId, newPrice)
                cartAdapter.notifyItemChanged(i)
                updateCartUI()
            }
            onClickRemoveItem = { cart, i ->
                val currentQuantity = cart.quantity ?: 1
                if (currentQuantity > 1) {
                    val newQuantity = currentQuantity - 1
                    val unitPrice = cart.price!! / currentQuantity
                    val newPrice = unitPrice * newQuantity
                    cart.quantity = newQuantity
                    cart.price = newPrice
                    cartManager.updateQuantity(userId, cart.itemId!!, newQuantity)
                    cartManager.updatePrice(userId, cart.itemId, newPrice)
                    cartAdapter.notifyItemChanged(i)
                }
                updateCartUI()
            }
        }

        binding.rcvCart.layoutManager = LinearLayoutManager(this)
        binding.rcvCart.adapter = cartAdapter
        updateCartUI()

        binding.btnCart.setOnClickListener {
            if (!isShowed) {
                binding.dimBackground.visibility = View.VISIBLE
                binding.dimBackground.alpha = 0f
                binding.dimBackground.animate().alpha(1f).setDuration(300).start()
                binding.cartLayout.visibility = View.VISIBLE
                binding.cartLayout.alpha = 0f
                binding.cartLayout.translationY = 300f
                binding.cartLayout.scaleX = 0.8f
                binding.cartLayout.scaleY = 0.8f
                binding.cartLayout.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(400)
                    .start()
                isShowed = true
            } else {
                hideCartLayout()
            }
        }

        binding.dimBackground.setOnClickListener {
            hideCartLayout()
        }

        binding.btnBack.setOnClickListener {
            countDownTimer.cancel()
            setResult(RESULT_FIRST_USER)
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
    }

    private fun setupObservers(
        cinemaId: String?, cinemaName: String?, showtimeId: String?, showtimeTime: String?,
        roomId: String?, movieId: String?, movieCountry: String?, moviePosterUrl: String?,
        movieTitle: String?, movieReleaseYear: String?, movieLanguage: String?,
        movieDuration: Int, movieGenre: String?, movieSynopsis: String?, movieDirectorId: String?,
        movieStatus: String?, movieTrailerUrl: String?, movieBanner: String?,
        movieAgeRating: String?, movieRating: Float, movieActorIds: ArrayList<String>?,
        seatPrice: String?, seatName: String?, ticketIds: ArrayList<String>?,
        selectedSeatIds: ArrayList<String>?
    ) {

        foodViewModel.food.observe(this) { foodList ->
            isFoodLoaded = true
            foodAdapter = FoodAdapter(foodList, false)
            binding.rcvFood.adapter = foodAdapter
            foodAdapter.onClickItem = { food, _ ->
                showBottomSheetDialog(food.itemId, food.picUrl, food.title, food.description, food.price ?: 9.00)
            }
            checkAllDataLoaded()
        }

        foodViewModel.drink.observe(this) { foodList ->
            isDrinkLoaded = true
            foodAdapter = FoodAdapter(foodList, false)
            binding.rcvDrink.adapter = foodAdapter
            foodAdapter.onClickItem = { food, _ ->
                showBottomSheetDialog(food.itemId, food.picUrl, food.title, food.description, food.price ?: 9.00)
            }
            checkAllDataLoaded()
        }

        foodViewModel.combo.observe(this) { foodList ->
            isComboLoaded = true
            foodAdapter = FoodAdapter(foodList, false)
            binding.rcvCombo.adapter = foodAdapter
            foodAdapter.onClickItem = { food, _ ->
                showBottomSheetDialog(food.itemId, food.picUrl, food.title, food.description, food.price ?: 9.00)
            }
            checkAllDataLoaded()
        }

        binding.btnContinue.setOnClickListener {
            val intent = Intent(this, PaymentDetailActivity::class.java)
            intent.putExtra("cinema_id", cinemaId)
            intent.putExtra("cinema_name", cinemaName)
            intent.putExtra("showtime_id", showtimeId)
            intent.putExtra("showtime_time", showtimeTime)
            intent.putExtra("room_id", roomId)
            intent.putExtra("movie_id", movieId)
            intent.putExtra("country", movieCountry)
            intent.putExtra("title", movieTitle)
            intent.putExtra("poster_url", moviePosterUrl)
            intent.putExtra("language", movieLanguage)
            intent.putExtra("release_year", movieReleaseYear)
            intent.putExtra("duration", movieDuration)
            intent.putExtra("genre", movieGenre)
            intent.putExtra("synopsis", movieSynopsis)
            intent.putExtra("director_id", movieDirectorId)
            intent.putExtra("status", movieStatus)
            intent.putExtra("trailer_url", movieTrailerUrl)
            intent.putExtra("banner", movieBanner)
            intent.putExtra("age_rating", movieAgeRating)
            intent.putExtra("rating", movieRating)
            intent.putStringArrayListExtra("actor_ids", movieActorIds)
            intent.putExtra("seat_price", seatPrice)
            intent.putExtra("seat_name", seatName)
            intent.putExtra("time_left", currentTimeLeft)
            intent.putStringArrayListExtra("ticket_ids", ticketIds)
            intent.putStringArrayListExtra("selected_seat_ids", selectedSeatIds)
            intent.putExtra("total_price_seats", totalPriceSeats)
            intent.putExtra("total_price_food", cartManager.totalPriceOfCart(userId))
            startActivityForResult(intent, REQUEST_CODE_PAYMENT)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
    }

    private fun checkAllDataLoaded() {
        if (isFoodLoaded && isDrinkLoaded && isComboLoaded) {
            // Tất cả dữ liệu đã load, cập nhật UI
            stopAnimation()
            binding.scrollView.visibility = View.VISIBLE
            binding.rcvFood.visibility = View.VISIBLE
            binding.rcvDrink.visibility = View.VISIBLE
            binding.rcvCombo.visibility = View.VISIBLE
            Log.d("FoodAndDrinkActivity", "All data loaded, showing scrollView")
        }
    }

    private fun stopAnimation() {
        binding.progressBar.cancelAnimation() // Dừng animation
        binding.progressBar.visibility = View.GONE // Ẩn progressBar
    }

    private fun hideCartLayout() {
        binding.cartLayout.animate()
            .alpha(0f)
            .translationY(300f)
            .scaleX(0.8f)
            .scaleY(0.8f)
            .setDuration(400)
            .withEndAction {
                binding.cartLayout.visibility = View.GONE
            }
            .start()

        binding.dimBackground.animate()
            .alpha(0f)
            .setDuration(300)
            .withEndAction {
                binding.dimBackground.visibility = View.GONE
            }
            .start()

        isShowed = false
    }

    private fun updateCartUI() {
        val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))
        val totalPrice = cartManager.totalPriceOfCart(userId)
        binding.tvTotalPrice.text = formatter.format(totalPrice) + "đ"
        val cartItems = cartManager.getCart(userId)
        listCart.clear()
        listCart.addAll(cartItems)
        cartAdapter.updateCarts(cartItems)
        val totalQuantity = cartItems.sumOf { it.quantity ?: 0 }
        binding.tvQuantity.text = totalQuantity.toString()
        binding.btnCart.visibility = if (totalQuantity > 0) View.VISIBLE else View.GONE
        if (cartItems.isEmpty() && isShowed) {
            hideCartLayout()
        }
        cartItems.forEach {
            Log.d("CartItem", "Item: ${it.title}, Quantity: ${it.quantity}, Price: ${it.price}")
        }
    }

    private fun setupRecycleView() {
        val spaceInPixels = resources.getDimensionPixelSize(R.dimen.item_spacing)
        binding.rcvFood.setHasFixedSize(true)
        binding.rcvFood.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rcvFood.addItemDecoration(HorizontalSpaceItemDecoration(spaceInPixels))
        binding.rcvDrink.setHasFixedSize(true)
        binding.rcvDrink.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rcvDrink.addItemDecoration(HorizontalSpaceItemDecoration(spaceInPixels))
        binding.rcvCombo.setHasFixedSize(true)
        binding.rcvCombo.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rcvCombo.addItemDecoration(HorizontalSpaceItemDecoration(spaceInPixels))
    }

    private fun showBottomSheetDialog(itemId: String, img: String, title: String, description: String, price: Double) {
        val dialog = BottomSheetDialog(this)
        dialog.setContentView(R.layout.bottom_sheet_food)
        val picFood = dialog.findViewById<ImageView>(R.id.picFood)
        val tvTitle = dialog.findViewById<TextView>(R.id.tvTitle)
        val tvDescription = dialog.findViewById<TextView>(R.id.tvDescription)
        val btnRemove = dialog.findViewById<ImageView>(R.id.btnRemove)
        val tvQuantity = dialog.findViewById<TextView>(R.id.tvQuantity)
        val btnAdd = dialog.findViewById<ImageView>(R.id.btnAdd)
        val btnCancel = dialog.findViewById<Button>(R.id.btnCancel)
        val btnContinue = dialog.findViewById<Button>(R.id.btnContinue)
        quantity = 1
        Glide.with(dialog.context).load(img).into(picFood!!)
        tvTitle?.text = title
        tvDescription?.text = description
        tvQuantity?.text = quantity.toString()
        btnContinue?.text = "Thêm vào giỏ hàng - ${String.format("%.2f", price * quantity).toInt()}đ"
        btnAdd?.setOnClickListener {
            quantity++
            tvQuantity?.text = quantity.toString()
            btnContinue?.text = "Thêm vào giỏ hàng - ${String.format("%.2f", price * quantity).toInt()}đ}"
        }
        btnRemove?.setOnClickListener {
            if (quantity > 1) {
                quantity--
                tvQuantity?.text = quantity.toString()
                btnContinue?.text = "Thêm vào giỏ hàng - ${String.format("%.2f", price * quantity).toInt()}đ}"
            }
        }
        btnCancel?.setOnClickListener {
            dialog.dismiss()
        }
        btnContinue?.setOnClickListener {
            val cartItem = Cart(
                itemId = itemId,
                title = title,
                picUrl = img,
                price = price * quantity,
                quantity = quantity
            )
            cartManager.addItemToCart(userId, cartItem, quantity)
            updateCartUI()
            dialog.dismiss()
        }
        dialog.show()
        val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        bottomSheet?.let {
            val behavior = BottomSheetBehavior.from(it)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.peekHeight = 0
            behavior.isHideable = true
            behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                        dialog.dismiss()
                    }
                }
                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    if (slideOffset < 0.01) {
                        dialog.dismiss()
                    }
                    Log.d("BottomSheet", "Slide offset: $slideOffset")
                }
            })
        }
    }

    private fun startTimer(timeLeft: Long) {
        currentTimeLeft = timeLeft
        countDownTimer = object : CountDownTimer(timeLeft, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                currentTimeLeft = millisUntilFinished
                val minutes = (millisUntilFinished / 1000) / 60
                val seconds = (millisUntilFinished / 1000) % 60
                binding.tvTimeLimit.text = String.format("%02d:%02d", minutes, seconds)
            }
            override fun onFinish() {
                currentTimeLeft = 0L
                binding.tvTimeLimit.text = "00:00"
                countDownTimer.cancel()
                setResult(RESULT_CANCELED)
                showDialog()
            }
        }.start()
    }

    private fun showDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_time_limit_finish)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCancelable(false)
        dialog.show()
        dialog.findViewById<Button>(R.id.btnOk).setOnClickListener {
            dialog.dismiss()
            countDownTimer.cancel()
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_PAYMENT) {
            when (resultCode) {
                RESULT_OK -> {
                    setResult(RESULT_OK)
                    finish()
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                }
                RESULT_CANCELED -> {
                    setResult(RESULT_CANCELED)
                    finish()
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                }
                RESULT_FIRST_USER -> {
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver)
        countDownTimer.cancel()
        stopAnimation()
    }

    companion object {
        private const val REQUEST_CODE_PAYMENT = 1001
    }
}