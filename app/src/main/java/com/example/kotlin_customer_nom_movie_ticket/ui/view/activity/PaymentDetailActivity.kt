package com.example.kotlin_customer_nom_movie_ticket.ui.view.activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.airbnb.lottie.LottieAnimationView
import com.bumptech.glide.Glide
import com.example.kotlin_customer_nom_movie_ticket.R
import com.example.kotlin_customer_nom_movie_ticket.data.model.Cart
import com.example.kotlin_customer_nom_movie_ticket.data.model.Room // Added import
import com.example.kotlin_customer_nom_movie_ticket.databinding.ActivityPaymentDetailBinding
import com.example.kotlin_customer_nom_movie_ticket.service.stripe.ApiUtilities
import com.example.kotlin_customer_nom_movie_ticket.service.stripe.Utils
import com.example.kotlin_customer_nom_movie_ticket.ui.adapter.FoodOrderAdapter
import com.example.kotlin_customer_nom_movie_ticket.util.CartManager
import com.example.kotlin_customer_nom_movie_ticket.util.SessionManager
import com.example.kotlin_customer_nom_movie_ticket.viewmodel.CinemaViewModel
import com.example.kotlin_customer_nom_movie_ticket.viewmodel.DirectorViewModel
import com.example.kotlin_customer_nom_movie_ticket.viewmodel.SeatViewModel
import com.example.kotlin_customer_nom_movie_ticket.viewmodel.TicketViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import com.google.auth.oauth2.GoogleCredentials
import java.io.IOException
import java.lang.reflect.Field
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.min

class PaymentDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPaymentDetailBinding
    private lateinit var directorViewModel: DirectorViewModel
    private lateinit var cinemaViewModel: CinemaViewModel
    private lateinit var seatViewModel: SeatViewModel
    private lateinit var countDownTimer: CountDownTimer
    private lateinit var cartManager: CartManager
    private lateinit var viewModel: TicketViewModel
    private lateinit var cartAdapter: FoodOrderAdapter
    private var actualPay = 0.0
    private var billId = ""
    private var totalPriceSeats = 0.0
    private var totalPriceFood = 0.0
    private var totalPrice = 0.0
    private var fee = 0.0
    private var discountApplied = 0.0
    private var pointsToDeduct = 0
    private var listCart: MutableList<Cart> = mutableListOf()
    private var roomName: String = ""

    private lateinit var paymentSheet: PaymentSheet
    private var customerId: String? = null
    private var ephemeralKey: String? = null
    private var clientSecretKey: String? = null
    private val apiInterface = ApiUtilities.getApiInterface()

    private val calendar = Calendar.getInstance()
    private val todayCalendar = Calendar.getInstance()
    private var lastHour: Int = 14
    private var pickUpTime: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
        val movieActorIds = intent.getStringArrayListExtra("actor_ids")
        val movieAgeRating = intent.getStringExtra("age_rating")
        val movieRating = intent.getFloatExtra("rating", 0f)
        val movieBanner = intent.getStringExtra("banner")
        val seatName = intent.getStringExtra("seat_name")
        val seatPrice = intent.getStringExtra("seat_price")
        val timeLeft = intent.getLongExtra("time_left", 0L)
        val ticketIds = intent.getStringArrayListExtra("ticket_ids")
        val selectedSeatIds = intent.getStringArrayListExtra("selected_seat_ids")
        totalPriceSeats = intent.getDoubleExtra("total_price_seats", 0.0)
        totalPriceFood = intent.getDoubleExtra("total_price_food", 0.0)

        viewModel = ViewModelProvider(this).get(TicketViewModel::class.java)

        val userId = SessionManager.getUserId(this).toString()
        cartManager = CartManager(this)
        cartAdapter = FoodOrderAdapter(mutableListOf())
        directorViewModel = DirectorViewModel()
        cinemaViewModel = CinemaViewModel()
        seatViewModel = ViewModelProvider(this)[SeatViewModel::class.java]

        totalPrice = totalPriceSeats + totalPriceFood
        binding.tvPriceOfSeat.text = seatPrice
        updateUi(totalPriceSeats, totalPriceFood)

        listCart = cartManager.getCart(userId)
        if (listCart.isNotEmpty()) {
            binding.rcvCart.visibility = View.VISIBLE
            binding.viewLine.visibility = View.VISIBLE
            binding.viewLine1.visibility = View.VISIBLE
            binding.btnChoosePickUpTime.visibility = View.VISIBLE
            binding.linearLayoutFoodPrice.visibility = View.VISIBLE
            cartAdapter = FoodOrderAdapter(listCart)
            binding.rcvCart.adapter = cartAdapter
            setUpRecyclerView()
        } else {
            binding.btnChoosePickUpTime.visibility = View.GONE
            binding.viewLine1.visibility = View.GONE
            binding.viewLine.visibility = View.GONE
            binding.rcvCart.visibility = View.GONE
            binding.linearLayoutFoodPrice.visibility = View.GONE
        }

        cartAdapter.onClickDeleteItem = { cart, i ->
            cartManager.removeItemFromCart(userId, cart.itemId!!)
            listCart.removeAt(i)
            cartAdapter.notifyItemRemoved(i)
            totalPriceFood -= cart.price ?: 0.0
            updateUi(totalPriceSeats, totalPriceFood)
            if (listCart.isEmpty()) {
                updateCartVisibility()
            }
        }

        cartAdapter.onClickAddItem = { cart, i ->
            val currentQuantity = cart.quantity ?: 1
            val newQuantity = currentQuantity + 1
            val unitPrice = cart.price!! / currentQuantity
            val newPrice = unitPrice * newQuantity

            cart.quantity = newQuantity
            cart.price = newPrice

            cartManager.updateQuantity(userId, cart.itemId!!, newQuantity)
            cartManager.updatePrice(userId, cart.itemId, newPrice)

            cartAdapter.notifyItemChanged(i)
            totalPriceFood = cartManager.totalPriceOfCart(userId).toDouble()
            updateUi(totalPriceSeats, totalPriceFood)
        }

        cartAdapter.onClickRemoveItem = { cart, i ->
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

            totalPriceFood = cartManager.totalPriceOfCart(userId).toDouble()
            updateUi(totalPriceSeats, totalPriceFood)
            if (listCart.isEmpty()) {
                updateCartVisibility()
            }
        }

        if (movieDirectorId != null) {
            directorViewModel.fetchDirectorNameById(movieDirectorId)
        } else {
            Toast.makeText(this, "Không có ID đạo diễn", Toast.LENGTH_SHORT).show()
        }

        directorViewModel.directorName.observe(this) { name ->
            binding.tvDirectorName.text = name
        }

        Glide.with(this).load(moviePosterUrl).into(binding.picMovie)
        binding.tvTitle.text = movieTitle
        binding.tvAgeRate.text = movieAgeRating
        binding.tvDuration.text = "$movieDuration phút"
        binding.tvGenre.text = movieGenre
        binding.tvCinemaName.text = cinemaName

        val parts = showtimeTime!!.split("T")
        val date = parts[0]
        val time = parts[1].substring(0, 5)

        binding.tvDate.text = date
        binding.tvShowtimeTime.text = time
        binding.tvSeats.text = seatName

        // Fetch roomName from Firebase
        if (roomId != null) {
            fetchRoomName(roomId)
        } else {
            Log.e("PaymentDetailActivity", "roomId is null")
            roomName = "Unknown Room" // Fallback
        }

        binding.btnBack.setOnClickListener {
            countDownTimer.cancel()
            setResult(RESULT_FIRST_USER)
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        startTimer(timeLeft)

        PaymentConfiguration.init(applicationContext, Utils.PUBLISHABLE_KEY)
        paymentSheet = PaymentSheet(this, ::onPaymentSheetResult)
        getCustomerId()
        ticketIds?.let { Log.d("PaymentDetail", "Received ticket IDs: $it") }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1
                )
            }
        }

        binding.btnContinue.setOnClickListener {
            if (listCart.isNotEmpty()) {
                if (pickUpTime.isNotEmpty()) {
                    if (clientSecretKey != null) {
                        paymentFlow()
                    } else {
                        Toast.makeText(
                            this,
                            "Thanh toán chưa sẵn sàng, vui lòng đợi",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(this, "Vui lòng chọn thời gian nhận đồ", Toast.LENGTH_SHORT)
                        .show()
                }
            } else {
                if (clientSecretKey != null) {
                    paymentFlow()
                } else {
                    Toast.makeText(
                        this,
                        "Thanh toán chưa sẵn sàng, vui lòng đợi",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        binding.btnChoosePickUpTime.setOnClickListener {
            showPickUpTimeDialog()
        }

        binding.btnUsePoint.setOnClickListener {
            showUsePointDialog()
        }

        viewModel.paymentStatus.observe(this) { status ->
            if (status.startsWith("Payment saved successfully")) {
                showCompletedDialog()
                countDownTimer.cancel()
                val intent = Intent("PAYMENT_SUCCESS")
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
            }
        }

        viewModel.error.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            }
        }

        binding.linearLayoutPoint.visibility = View.GONE
    }

    private fun fetchRoomName(roomId: String) {
        val dbRooms = FirebaseDatabase.getInstance().getReference("Rooms").child(roomId)
        dbRooms.get().addOnSuccessListener { snapshot ->
            val room = snapshot.getValue(Room::class.java)
            roomName = room?.room_name ?: "Unknown Room"
        }.addOnFailureListener { exception ->
            Log.e("PaymentDetailActivity", "Error fetching roomName", exception)
            roomName = "Unknown Room"
        }
    }

    private fun showUsePointDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_use_point)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCancelable(false)
        dialog.show()

        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val etPoints = dialog.findViewById<EditText>(R.id.etPoints)
        val btnApply = dialog.findViewById<Button>(R.id.btnApply)
        val btnCancel = dialog.findViewById<Button>(R.id.btnCancel)
        val tvAvailablePoints = dialog.findViewById<TextView>(R.id.tvAvailablePoints)
        val tvMaxPointUse = dialog.findViewById<TextView>(R.id.tvMaxPointUse)

        lifecycleScope.launch {
            val userId = SessionManager.getUserId(this@PaymentDetailActivity).toString()
            val availablePoints = withContext(Dispatchers.IO) {
                viewModel.getCustomerPoints(userId)
            }
            val maxDiscount = totalPrice * 0.07
            val maxPoints = (maxDiscount * 1).toInt()
            val maxUsablePoints = min(availablePoints, maxPoints)

            tvAvailablePoints?.text = availablePoints.toString()
            tvMaxPointUse?.text = maxUsablePoints.toString()

            btnApply?.setOnClickListener {
                val inputPoints = etPoints?.text.toString().toIntOrNull() ?: 0
                if (inputPoints <= 0 || inputPoints > maxPoints) {
                    Toast.makeText(applicationContext, "Vui lòng nhập số điểm hợp lệ", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                lifecycleScope.launch {
                    val userId = SessionManager.getUserId(this@PaymentDetailActivity).toString()
                    val availablePoints = withContext(Dispatchers.IO) {
                        viewModel.getCustomerPoints(userId)
                    }
                    val maxDiscount = totalPrice * 0.07
                    val maxPoints = (maxDiscount * 100).toInt()
                    val maxUsablePoints = min(availablePoints, maxPoints)

                    if (inputPoints > maxUsablePoints) {
                        Toast.makeText(
                            this@PaymentDetailActivity,
                            "Không thể sử dụng nhiều hơn $maxUsablePoints điểm cho đơn hàng này",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@launch
                    }

                    if (inputPoints > availablePoints) {
                        Toast.makeText(
                            this@PaymentDetailActivity,
                            "Không đủ điểm",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@launch
                    }

                    pointsToDeduct = inputPoints
                    discountApplied = inputPoints * 1.0
                    actualPay = totalPrice + fee - discountApplied

                    binding.linearLayoutPoint.visibility = View.VISIBLE
                    binding.tvPointUse.text = "($inputPoints)"
                    val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))
                    val pointPrice = discountApplied
                    binding.tvPointToPrice.text = "-" + formatter.format(pointPrice) + "đ"
                    updateUi(totalPriceSeats, totalPriceFood)

                    dialog.dismiss()
                }
            }
        }

        btnCancel?.setOnClickListener {
            pointsToDeduct = 0
            discountApplied = 0.0
            actualPay = totalPrice + fee
            binding.linearLayoutPoint.visibility = View.GONE
            updateUi(totalPriceSeats, totalPriceFood)
            dialog.dismiss()
        }
    }

    private fun updateUi(totalPriceSeats: Double, totalPriceFood: Double) {
        val formatter =
            NumberFormat.getNumberInstance(Locale("vi", "VN"))
        formatter.maximumFractionDigits = 0

        val fees = (totalPriceSeats + totalPriceFood) * 0.03
        actualPay = totalPriceSeats + totalPriceFood + fees - discountApplied
        fee = fees

        if (actualPay < 0) {
            Log.e("PaymentDetailActivity", "Invalid actualPay: $actualPay")
            Toast.makeText(
                this,
                "Số tiền thanh toán không hợp lệ. Vui lòng kiểm tra đơn hàng của bạn.",
                Toast.LENGTH_LONG
            ).show()
            binding.btnContinue.isEnabled = false
            return
        }

        binding.tvPriceOfSeat.text = formatter.format(totalPriceSeats.toLong()) + " đ"
        binding.tvFoodPrice.text = formatter.format(totalPriceFood.toLong()) + " đ"
        binding.tvFee.text = formatter.format(fees.toLong()) + " đ"
        binding.tvActualPay.text = formatter.format(actualPay.toLong()) + " đ"

        val intent = Intent("UPDATE_CART")
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun updateCartVisibility() {
        if (listCart.isEmpty()) {
            binding.rcvCart.visibility = View.GONE
            binding.linearLayoutFoodPrice.visibility = View.GONE
            binding.viewLine.visibility = View.GONE
            binding.viewLine1.visibility = View.GONE
            binding.btnChoosePickUpTime.visibility = View.GONE
        } else {
            binding.rcvCart.visibility = View.VISIBLE
            binding.linearLayoutFoodPrice.visibility = View.VISIBLE
            binding.viewLine.visibility = View.VISIBLE
            binding.viewLine1.visibility = View.VISIBLE
            binding.btnChoosePickUpTime.visibility = View.VISIBLE
        }
    }

    private fun setUpRecyclerView() {
        binding.rcvCart.setHasFixedSize(false)
        binding.rcvCart.layoutManager = LinearLayoutManager(this)
    }

    private fun startTimer(timeLeft: Long) {
        countDownTimer = object : CountDownTimer(timeLeft, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val minutes = (millisUntilFinished / 1000) / 60
                val seconds = (millisUntilFinished / 1000) % 60
                binding.tvTime.text = String.format("%02d:%02d", minutes, seconds)
            }

            override fun onFinish() {
                binding.tvTime.text = "00:00"
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

    private fun showCompletedDialog() {
        val cinemaName = intent.getStringExtra("cinema_name")
        val showtimeTime = intent.getStringExtra("showtime_time")
        val movieTitle = intent.getStringExtra("title")
        val movieDuration = intent.getIntExtra("duration", 0)
        val movieAgeRating = intent.getStringExtra("age_rating")
        val seatName = intent.getStringExtra("seat_name")
        val moviePosterUrl = intent.getStringExtra("poster_url")
        val movieGenre = intent.getStringExtra("genre")
        val movieDirector = intent.getStringExtra("director")

        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_pay_completed)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCancelable(false)
        dialog.show()

        dialog.findViewById<LottieAnimationView>(R.id.lottieAnimationView).playAnimation()

        dialog.findViewById<Button>(R.id.btnBackToHome).setOnClickListener {
            dialog.dismiss()
            setResult(RESULT_OK)
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        dialog.findViewById<Button>(R.id.btnViewTicket).setOnClickListener {
            dialog.dismiss()
            val intent = Intent(this, ViewTicketActivity::class.java)
            if (billId.isNotEmpty()) {
                intent.putExtra("bill_id", billId)
            }
            intent.putExtra("total_price", totalPrice)
            intent.putExtra("poster_url", moviePosterUrl)
            intent.putExtra("director", movieDirector)
            intent.putExtra("genre", movieGenre)
            intent.putExtra("title", movieTitle)
            intent.putExtra("cinema_name", cinemaName)
            intent.putExtra("duration", movieDuration)
            intent.putExtra("showtime_time", showtimeTime)
            intent.putExtra("age_rating", movieAgeRating)
            intent.putExtra("seat_name", seatName)
            intent.putExtra("room_name", roomName)
            startActivity(intent)
            setResult(RESULT_OK)
            countDownTimer.cancel()
            finish()
            overridePendingTransition(R.anim.slide_out_right, R.anim.slide_in_left)
        }
    }

    private fun paymentFlow() {
        clientSecretKey?.let { secret ->
            val appearance = PaymentSheet.Appearance(
                colorsLight = PaymentSheet.Colors(
                    primary = ContextCompat.getColor(this, R.color.orange),
                    surface = Color.WHITE,
                    component = Color.parseColor("#F5F5F5"),
                    componentBorder = Color.parseColor("#D3D3D3"),
                    componentDivider = Color.parseColor("#E0E0E0"),
                    onComponent = Color.BLACK,
                    subtitle = Color.BLACK,
                    placeholderText = ContextCompat.getColor(this, R.color.dark_background),
                    onSurface = Color.BLACK,
                    appBarIcon = Color.BLACK,
                    error = Color.RED
                ),
                colorsDark = PaymentSheet.Colors(
                    primary = ContextCompat.getColor(this, R.color.orange),
                    surface = ContextCompat.getColor(this, R.color.black),
                    component = ContextCompat.getColor(this, R.color.dark_edit_text),
                    componentBorder = Color.parseColor("#616161"),
                    componentDivider = Color.parseColor("#757575"),
                    onComponent = Color.WHITE,
                    subtitle = Color.WHITE,
                    placeholderText = ContextCompat.getColor(this, R.color.light_grey),
                    onSurface = Color.WHITE,
                    appBarIcon = Color.WHITE,
                    error = Color.RED
                ),
                shapes = PaymentSheet.Shapes(
                    cornerRadiusDp = 12.0f,
                    borderStrokeWidthDp = 0f
                ),
                primaryButton = PaymentSheet.PrimaryButton(
                    colorsLight = PaymentSheet.PrimaryButtonColors(
                        background = Color.parseColor("#FE3323"),
                        onBackground = Color.WHITE,
                        border = Color.parseColor("#FE3323")
                    ),
                    colorsDark = PaymentSheet.PrimaryButtonColors(
                        background = Color.parseColor("#FE3323"),
                        onBackground = Color.WHITE,
                        border = Color.parseColor("#FE3323")
                    ),
                    shape = PaymentSheet.PrimaryButtonShape(
                        cornerRadiusDp = 20.0f
                    )
                ),
                typography = PaymentSheet.Typography.default.copy(
                    fontResId = R.font.urbanist_bold,
                    sizeScaleFactor = 1.15f
                )
            )

            val configuration = PaymentSheet.Configuration(
                merchantDisplayName = "Movie Ticket App",
                customer = customerId?.let { id ->
                    ephemeralKey?.let { key ->
                        PaymentSheet.CustomerConfiguration(id, key)
                    }
                },
                appearance = appearance
            )

            paymentSheet.presentWithPaymentIntent(secret, configuration)
        } ?: run {
            Log.e("PaymentFlow", "Client secret key is null")
            Toast.makeText(this, "Lỗi cấu hình thanh toán", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getCustomerId() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = apiInterface.getCustomer().execute()
                if (response.isSuccessful) {
                    customerId = response.body()?.id
                    customerId?.let { getEphemeralKey(it) }
                } else {
                    withContext(Dispatchers.Main) {
                        Log.e("getCustomerId", "Failed to get customer ID: ${response.code()}")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("getCustomerId", "Error: ${e.message}")
                }
            }
        }
    }

    private fun getEphemeralKey(customerId: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = apiInterface.getEphemeralKey(customerId).execute()
                if (response.isSuccessful) {
                    ephemeralKey = response.body()?.id
                    ephemeralKey?.let { getPaymentIntent(customerId, it) }
                } else {
                    withContext(Dispatchers.Main) {
                        Log.e("getEphemeralKey", "Failed to get ephemeral key: ${response.code()}")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("getEphemeralKey", "Error: ${e.message}")
                }
            }
        }
    }

    private fun getPaymentIntent(customerId: String, ephemeralKey: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = apiInterface.getPaymentIntents(
                    customerId,
                    (actualPay - discountApplied).toInt().toString()
                ).execute()
                if (response.isSuccessful) {
                    clientSecretKey = response.body()?.client_secret
                    withContext(Dispatchers.Main) {
                        Log.d("PaymentIntent", "Client Secret: $clientSecretKey")
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Log.e(
                            "getPaymentIntent",
                            "Failed to get payment intent: ${response.code()}"
                        )
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("getPaymentIntent", "Error: ${e.message}")
                }
            }
        }
    }

    private fun onPaymentSheetResult(paymentSheetResult: PaymentSheetResult) {
        when (paymentSheetResult) {
            is PaymentSheetResult.Completed -> {
                // Deduct points only on successful payment
                if (pointsToDeduct > 0) {
                    val userId = SessionManager.getUserId(this).toString()
                    viewModel.deductCustomerPoints(userId, pointsToDeduct)
                }

                val ticketIds = intent.getStringArrayListExtra("ticket_ids") ?: return
                val selectedSeatIds = intent.getStringArrayListExtra("selected_seat_ids") ?: return
                val showtimeId = intent.getStringExtra("showtime_id") ?: return
                val userId = SessionManager.getUserId(this).toString()

                viewModel.savePaymentData(
                    ticketIds = ticketIds,
                    selectedSeatIds = selectedSeatIds,
                    showtimeId = showtimeId,
                    userId = userId,
                    totalPriceSeats = totalPriceSeats,
                    totalPriceFood = totalPriceFood,
                    fee = fee,
                    discount = discountApplied,
                    actualPay = actualPay,
                    cartManager = cartManager,
                    pickUpTime = pickUpTime
                )
            }

            is PaymentSheetResult.Failed -> {
                Log.e("PaymentSheetResult", "Payment failed: ${paymentSheetResult.error.message}")
                Toast.makeText(
                    this,
                    "Thanh toán không thành công: ${paymentSheetResult.error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }

            is PaymentSheetResult.Canceled -> {
                Log.d("PaymentSheetResult", "Payment canceled")
            }
        }
    }

    fun sendNotificationWithFCMv1(
        context: Context,
        recipientToken: String,
        senderName: String,
        messageText: String
    ) {
        val cinemaName = intent.getStringExtra("cinema_name")
        val showtimeTime = intent.getStringExtra("showtime_time")
        val movieTitle = intent.getStringExtra("title")
        val movieDuration = intent.getIntExtra("duration", 0)
        val movieAgeRating = intent.getStringExtra("age_rating")
        val seatName = intent.getStringExtra("seat_name")

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.e("FCM", "Lấy token thất bại", task.exception)
                return@addOnCompleteListener
            }
            val token = task.result
            Log.d("FCM", "Token của thiết bị: $token")
        }
        CoroutineScope(Dispatchers.IO).launch {
            val projectId = "shoponline-f6905"

            val googleCredentials = try {
                val inputStream = context.assets.open("service-account.json")
                GoogleCredentials.fromStream(inputStream)
                    .createScoped(listOf("https://www.googleapis.com/auth/firebase.messaging"))
            } catch (e: IOException) {
                Log.e("RoomChatViewModel", "Error reading service-account.json", e)
                return@launch
            }

            try {
                googleCredentials.refreshIfExpired()
            } catch (e: IOException) {
                Log.e("RoomChatViewModel", "Error refreshing Google credentials", e)
                return@launch
            }

            val accessToken = googleCredentials.accessToken.tokenValue

            val notificationJson = JSONObject().apply {
                put("title", senderName)
                put("body", messageText)
            }

            val dataJson = JSONObject().apply {
                put("title", senderName)
                put("message", messageText)
                put("cinema_name", cinemaName)
                put("showtime_time", showtimeTime)
                put("movie_title", movieTitle)
                put("duration", movieDuration)
                put("age_rating", movieAgeRating)
                put("seat_name", seatName)
                put("bill_id", billId)
            }

            val messageJson = JSONObject().apply {
                put("token", recipientToken)
                put("notification", notificationJson)
                put("data", dataJson)
            }

            val requestBodyJson = JSONObject().apply {
                put("message", messageJson)
            }

            val client = OkHttpClient()
            val mediaType = "application/json; UTF-8".toMediaTypeOrNull()
            val requestBody = requestBodyJson.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url("https://fcm.googleapis.com/v1/projects/$projectId/messages:send")
                .addHeader("Authorization", "Bearer $accessToken")
                .addHeader("Content-Type", "application/json; UTF-8")
                .post(requestBody)
                .build()

            try {
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    Log.d("RoomChatViewModel", "Notification sent successfully!")
                } else {
                    Log.e("RoomChatViewModel", "Error sending notification: ${response.message}")
                }
            } catch (e: IOException) {
                Log.e("RoomChatViewModel", "Error executing FCM request", e)
            }
        }
    }

    private fun showPickUpTimeDialog() {
        val dialog = BottomSheetDialog(this)
        dialog.setContentView(R.layout.bottom_sheet_choose_time_pick_up)
        dialog.show()

        val bottomSheet =
            dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        bottomSheet?.let {
            val behavior = BottomSheetBehavior.from(it)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.peekHeight = 0
            behavior.isHideable = true

            behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(customSheet: View, newState: Int) {
                    if (newState == BottomSheetBehavior.STATE_HIDDEN) dialog.dismiss()
                }

                override fun onSlide(customSheet: View, slideOffset: Float) {
                    if (slideOffset < 0.01) dialog.dismiss()
                    Log.d("BottomSheet", "Slide offset: $slideOffset")
                }
            })
        }

        val btnPickUpNow = dialog.findViewById<Button>(R.id.btnContinue)
        val btnPickUpLater = dialog.findViewById<LinearLayout>(R.id.btnPickUpLater)

        btnPickUpNow?.setOnClickListener {
            Log.d("PickUpNow", "Picked up now")
            dialog.dismiss()

            val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"))
            calendar.add(Calendar.MINUTE, 10)
            val currentTime =
                SimpleDateFormat("HH:mm", Locale("vi", "VN")).format(calendar.time)
            val dateString = "Hôm nay, ${
                SimpleDateFormat(
                    "dd MMM, yyyy",
                    Locale("vi", "VN")
                ).format(calendar.time)
            }"
            val dayOfWeekFormat = SimpleDateFormat("EEEE", Locale.getDefault())

            val dayOfWeek = "${dayOfWeekFormat.format(calendar.time)}, ${
                SimpleDateFormat(
                    "dd MMM, yyyy",
                    Locale("vi", "VN")
                ).format(calendar.time)
            }"
            binding.tvTimePickUp.text = "Nhận đồ lúc $currentTime"
            binding.tvTimeValue.text = dateString
            pickUpTime = "$dayOfWeek $currentTime"
        }

        btnPickUpLater?.setOnClickListener {
            dialog.dismiss()
            showSetTimeDialog()
        }
    }

    private fun showSetTimeDialog() {
        val dialog = BottomSheetDialog(this)
        dialog.setContentView(R.layout.bottom_sheet_set_pick_up_time)
        dialog.show()

        val bottomSheet =
            dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        bottomSheet?.let {
            val behavior = BottomSheetBehavior.from(it)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.peekHeight = 0
            behavior.isHideable = true

            behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(customSheet: View, newState: Int) {
                    if (newState == BottomSheetBehavior.STATE_HIDDEN) dialog.dismiss()
                }

                override fun onSlide(customSheet: View, slideOffset: Float) {
                    Log.d("BottomSheet", "Slide offset: $slideOffset")
                }
            })
        }

        val dateText = dialog.findViewById<TextView>(R.id.dateText)
        val hourPicker = dialog.findViewById<NumberPicker>(R.id.hourPicker)
        val minutePicker = dialog.findViewById<NumberPicker>(R.id.minutePicker)
        val btnSetTime = dialog.findViewById<Button>(R.id.btnSetTime)

        val currentCalendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"))
        currentCalendar.add(Calendar.MINUTE, 10)
        val minHour = currentCalendar.get(Calendar.HOUR_OF_DAY)
        val minMinute = currentCalendar.get(Calendar.MINUTE)

        hourPicker?.minValue = 0
        hourPicker?.maxValue = 23
        if (isToday(calendar)) {
            hourPicker?.minValue = minHour
        }
        hourPicker?.value = if (isToday(calendar) && lastHour < minHour) minHour else lastHour
        hourPicker?.setFormatter { value -> String.format("%02d", value) }

        minutePicker?.minValue = 0
        minutePicker?.maxValue = 59
        if (isToday(calendar) && hourPicker?.value == minHour) {
            minutePicker?.minValue = minMinute
        }
        minutePicker?.value =
            if (isToday(calendar) && hourPicker?.value == minHour && minutePicker?.value ?: 0 < minMinute) {
                minMinute
            } else {
                minutePicker?.value ?: 30
            }
        minutePicker?.setFormatter { value -> String.format("%02d", value) }

        hourPicker?.let { removeSelectionDivider(it) }
        minutePicker?.let { removeSelectionDivider(it) }

        if (dateText != null && hourPicker != null && minutePicker != null) {
            updateDateText(dateText, hourPicker.value, minutePicker.value)
        }

        hourPicker?.setOnValueChangedListener { _, oldHour, newHour ->
            if (oldHour == 23 && newHour == 0) {
                calendar.add(Calendar.DAY_OF_MONTH, 1)
                hourPicker.minValue = 0
                minutePicker?.minValue = 0
            } else if (oldHour == 0 && newHour == 23) {
                val newDate = calendar.clone() as Calendar
                newDate.add(Calendar.DAY_OF_MONTH, -1)
                if (isBeforeToday(newDate)) {
                    calendar.set(
                        todayCalendar.get(Calendar.YEAR),
                        todayCalendar.get(Calendar.MONTH),
                        todayCalendar.get(Calendar.DAY_OF_MONTH)
                    )
                    hourPicker.minValue = minHour
                    if (newHour == minHour) {
                        minutePicker?.minValue = minMinute
                        if (minutePicker?.value ?: 0 < minMinute) {
                            minutePicker?.value = minMinute
                        }
                    }
                } else {
                    calendar.add(Calendar.DAY_OF_MONTH, -1)
                }
            }
            if (isToday(calendar) && newHour == minHour) {
                minutePicker?.minValue = minMinute
                if (minutePicker?.value ?: 0 < minMinute) {
                    minutePicker?.value = minMinute
                }
            } else {
                minutePicker?.minValue = 0
            }
            lastHour = newHour
            if (dateText != null && minutePicker != null) {
                updateDateText(dateText, newHour, minutePicker.value)
            }
        }

        minutePicker?.setOnValueChangedListener { _, _, newMinute ->
            if (dateText != null && hourPicker != null) {
                if (isToday(calendar) && hourPicker.value == minHour && newMinute < minMinute) {
                    minutePicker.value = minMinute
                }
                updateDateText(dateText, hourPicker.value, minutePicker.value)
            }
        }

        btnSetTime?.setOnClickListener {
            val selectedCalendar = calendar.clone() as Calendar
            selectedCalendar.set(Calendar.HOUR_OF_DAY, hourPicker?.value ?: 0)
            selectedCalendar.set(Calendar.MINUTE, minutePicker?.value ?: 0)
            if (isToday(calendar) && selectedCalendar.before(currentCalendar)) {
                Toast.makeText(
                    this,
                    "Thời gian không hợp lệ, phải sau ${
                        String.format(
                            "%02d:%02d",
                            minHour,
                            minMinute
                        )
                    } hôm nay",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            updateDateText(binding.tvTimeValue, hourPicker?.value ?: 0, minutePicker?.value ?: 0)
            dialog.dismiss()
        }
    }

    private fun updateDateText(dateText: TextView, hour: Int, minute: Int) {
        val dateFormat = SimpleDateFormat("dd MMM, yyyy", Locale("vi", "VN")).format(calendar.time)
        val dayOfWeekFormat = SimpleDateFormat("EEEE", Locale("vi", "VN"))
        val dateString = if (isToday(calendar)) {
            "Hôm nay, ${SimpleDateFormat("dd MMM, yyyy", Locale("vi", "VN")).format(calendar.time)}"
        } else {
            "${dayOfWeekFormat.format(calendar.time)}, ${dateFormat.format(calendar.time)}"
        }

        val time = String.format("%02d:%02d", hour, minute)
        dateText.text = "$dateString"

        val dayOfWeek =
            "${dayOfWeekFormat.format(calendar.time)}, ${dateFormat.format(calendar.time)}"
        binding.tvTimePickUp.text = "Nhận đồ lúc $time"
        binding.tvTimeValue.text = "$dateString"
        pickUpTime = "$dayOfWeek $time"
    }

    private fun isToday(calendar: Calendar): Boolean {
        return calendar.get(Calendar.YEAR) == todayCalendar.get(Calendar.YEAR) &&
                calendar.get(Calendar.MONTH) == todayCalendar.get(Calendar.MONTH) &&
                calendar.get(Calendar.DAY_OF_MONTH) == todayCalendar.get(Calendar.DAY_OF_MONTH)
    }

    private fun isBeforeToday(date: Calendar): Boolean {
        val today = todayCalendar.clone() as Calendar
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)

        val compareDate = date.clone() as Calendar
        compareDate.set(Calendar.HOUR_OF_DAY, 0)
        compareDate.set(Calendar.MINUTE, 0)
        compareDate.set(Calendar.SECOND, 0)
        compareDate.set(Calendar.MILLISECOND, 0)

        return compareDate.before(today)
    }

    @SuppressLint("SoonBlockedPrivateApi")
    private fun removeSelectionDivider(numberPicker: NumberPicker) {
        try {
            val dividerField: Field = NumberPicker::class.java.getDeclaredField("mSelectionDivider")
            dividerField.isAccessible = true
            dividerField.set(numberPicker, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d("Permission", "Notification permission granted")
        } else {
            Log.d("Permission", "Notification permission denied")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer.cancel()
    }
}