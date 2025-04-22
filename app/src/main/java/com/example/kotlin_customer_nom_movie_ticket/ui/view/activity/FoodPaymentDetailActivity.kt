package com.example.kotlin_customer_nom_movie_ticket.ui.view.activity

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint.Style
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.airbnb.lottie.LottieAnimationView
import com.example.kotlin_customer_nom_movie_ticket.R
import com.example.kotlin_customer_nom_movie_ticket.data.model.Cart
import com.example.kotlin_customer_nom_movie_ticket.databinding.ActivityFoodPaymentDetailBinding
import com.example.kotlin_customer_nom_movie_ticket.service.stripe.ApiUtilities
import com.example.kotlin_customer_nom_movie_ticket.service.stripe.Utils
import com.example.kotlin_customer_nom_movie_ticket.ui.adapter.FoodOrderAdapter
import com.example.kotlin_customer_nom_movie_ticket.util.CartManager
import com.example.kotlin_customer_nom_movie_ticket.util.SessionManager
import com.example.kotlin_customer_nom_movie_ticket.viewmodel.TicketViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.reflect.Field
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.min

class FoodPaymentDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFoodPaymentDetailBinding
    private lateinit var cartManager: CartManager
    private lateinit var userId: String
    private lateinit var cartAdapter: FoodOrderAdapter
    private lateinit var listCart: MutableList<Cart>
    private lateinit var paymentSheet: PaymentSheet
    private lateinit var viewModel: TicketViewModel
    private var customerId: String? = null
    private var ephemeralKey: String? = null
    private var clientSecretKey: String? = null
    private val apiInterface = ApiUtilities.getApiInterface()

    private var totalPriceOfCart: Double = 0.0
    private var totalPriceToPay: Double = 0.0
    private var discountApplied: Double = 0.0
    private var pointsToDeduct: Int = 0

    private val calendar = Calendar.getInstance()
    private val todayCalendar = Calendar.getInstance()
    private var lastHour: Int = 14
    private var pickUpTime: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFoodPaymentDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this).get(TicketViewModel::class.java)

        userId = SessionManager.getUserId(this).toString()
        cartManager = CartManager(this)

        listCart = cartManager.getCart(userId).toMutableList()
        cartAdapter = FoodOrderAdapter(listCart)
        binding.rcvCart.adapter = cartAdapter
        setUpRecyclerView()

        cartAdapter.onClickDeleteItem = { cart, i ->
            cartManager.removeItemFromCart(userId, cart.itemId!!)
            listCart.removeAt(i)
            cartAdapter.notifyItemRemoved(i)
            totalPriceOfCart -= cart.price ?: 0.0
            updateUi(totalPriceOfCart, totalPriceOfCart * 0.01)
            if (listCart.isEmpty()) updateCartVisibility()
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
            totalPriceOfCart = cartManager.totalPriceOfCart(userId)
            updateUi(totalPriceOfCart, totalPriceOfCart * 0.01)
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

            totalPriceOfCart = cartManager.totalPriceOfCart(userId)
            updateUi(totalPriceOfCart, totalPriceOfCart * 0.01)
            if (listCart.isEmpty()) updateCartVisibility()
        }

        for (cart in listCart) {
            totalPriceOfCart += cart.price!!
        }

        val fee = totalPriceOfCart * 0.01
        totalPriceToPay = totalPriceOfCart + fee
        updateUi(totalPriceOfCart, fee)

        binding.btnBack.setOnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
        binding.btnBackToFood.setOnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        PaymentConfiguration.init(applicationContext, Utils.PUBLISHABLE_KEY)
        paymentSheet = PaymentSheet(this, ::onPaymentSheetResult)
        getCustomerId()

        binding.btnContinue.setOnClickListener {
            if (pickUpTime.isEmpty()) {
                Toast.makeText(this, "Please choose a pick-up time", Toast.LENGTH_SHORT).show()
            } else {
                if (clientSecretKey != null) {
                    paymentFlow()
                } else {
                    Log.e("PaymentFlow", "Client secret key is not initialized")
                    Toast.makeText(this, "Payment not ready, please wait", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }

        binding.btnUsePoint.setOnClickListener {
            showUsePointDialog()
        }

        binding.btnChoosePickUpTime.setOnClickListener {
            showPickUpTimeDialog()
        }

        viewModel.foodBookingStatus.observe(this) { status ->
            if (status.startsWith("Order placed successfully")) {
                cartManager.clearCart(userId)
                val intent = Intent("PAYMENT_SUCCESS")
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
                showCompletedDialog()
            }
        }

        // Initially hide linearLayoutPoint
        binding.linearLayoutPoint.visibility = View.GONE
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

        // Fetch available points and calculate max usable points
        lifecycleScope.launch {
            val availablePoints = withContext(Dispatchers.IO) {
                viewModel.getCustomerPoints(userId)
            }
            // Max points = min(available points, 10% of totalPriceOfCart * 100)
            val maxDiscount = totalPriceOfCart * 0.10 // Max discount in dollars
            val maxPoints = (maxDiscount * 100).toInt() // 1 point = $0.01
            val maxUsablePoints = min(availablePoints, maxPoints)

            tvAvailablePoints?.text = availablePoints.toString()
            tvMaxPointUse?.text = maxUsablePoints.toString()
        }

        btnApply?.setOnClickListener {
            val inputPoints = etPoints?.text.toString().toIntOrNull() ?: 0
            if (inputPoints <= 0) {
                Toast.makeText(this, "Please enter a valid number of points", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val availablePoints = withContext(Dispatchers.IO) {
                    viewModel.getCustomerPoints(userId)
                }
                // Calculate max usable points again for validation
                val maxDiscount = totalPriceOfCart * 0.10
                val maxPoints = (maxDiscount * 100).toInt()
                val maxUsablePoints = min(availablePoints, maxPoints)

                if (inputPoints > maxUsablePoints) {
                    Toast.makeText(
                        this@FoodPaymentDetailActivity,
                        "Cannot use more than $maxUsablePoints points for this order",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }

                if (inputPoints > availablePoints) {
                    Toast.makeText(this@FoodPaymentDetailActivity, "Not enough points", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // Store points to deduct and apply discount
                pointsToDeduct = inputPoints
                discountApplied = inputPoints * 0.01
                totalPriceToPay = totalPriceOfCart + (totalPriceOfCart * 0.01) - discountApplied

                // Update UI to show points used and discount
                binding.linearLayoutPoint.visibility = View.VISIBLE
                binding.tvPointUse.text = "($inputPoints)"
                binding.tvPointToPrice.text = "-$${String.format("%.2f", discountApplied)}"
                updateUi(totalPriceOfCart, totalPriceOfCart * 0.01)

                dialog.dismiss()
                Toast.makeText(this@FoodPaymentDetailActivity, "Discount applied: $${String.format("%.2f", discountApplied)}", Toast.LENGTH_SHORT).show()
            }
        }

        btnCancel?.setOnClickListener {
            // Reset points and discount if canceled
            pointsToDeduct = 0
            discountApplied = 0.0
            totalPriceToPay = totalPriceOfCart + (totalPriceOfCart * 0.01)
            binding.linearLayoutPoint.visibility = View.GONE
            updateUi(totalPriceOfCart, totalPriceOfCart * 0.01)
            dialog.dismiss()
        }
    }

    private fun showCompletedDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_food_payment_completed)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCancelable(false)
        dialog.show()

        dialog.findViewById<LottieAnimationView>(R.id.lottieAnimationView).playAnimation()

        dialog.findViewById<Button>(R.id.btnOk).setOnClickListener {
            dialog.dismiss()
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
    }

    private fun updateCartVisibility() {
        if (listCart.isEmpty()) {
            binding.rcvCart.visibility = View.GONE
            binding.emptyCartView.visibility = View.VISIBLE
            binding.view1.visibility = View.GONE
            binding.view2.visibility = View.GONE
            binding.linearLayoutFoodPrice.visibility = View.GONE
            binding.tvPromoTitle.visibility = View.GONE
            binding.etPromoCode.visibility = View.GONE
            binding.btnContinue.visibility = View.GONE
            binding.btnChoosePickUpTime.visibility = View.GONE
            binding.btnUsePoint.visibility = View.GONE
        } else {
            binding.rcvCart.visibility = View.VISIBLE
            binding.emptyCartView.visibility = View.GONE
            binding.view1.visibility = View.VISIBLE
            binding.view2.visibility = View.VISIBLE
            binding.linearLayoutFoodPrice.visibility = View.VISIBLE
            binding.tvPromoTitle.visibility = View.VISIBLE
            binding.etPromoCode.visibility = View.VISIBLE
            binding.btnContinue.visibility = View.VISIBLE
            binding.btnChoosePickUpTime.visibility = View.VISIBLE
            binding.btnUsePoint.visibility = View.VISIBLE
        }
    }

    private fun updateUi(totalPrice: Double, fee: Double) {
        binding.tvPriceOfItem.text = "$${String.format("%.2f", totalPrice)}"
        binding.tvFee.text = "$${String.format("%.2f", fee)}"
        binding.tvActualPay.text = "$${String.format("%.2f", totalPrice + fee - discountApplied)}"

        binding.btnContinue.isEnabled = totalPrice > 0

        val intent = Intent("UPDATE_CART")
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun setUpRecyclerView() {
        binding.rcvCart.setHasFixedSize(false)
        binding.rcvCart.layoutManager = LinearLayoutManager(this)
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
                        onBackground = Color.WHITE, // Màu văn bản trên nút
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
            Toast.makeText(this, "Payment configuration error", Toast.LENGTH_SHORT).show()
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
                    ((totalPriceToPay - discountApplied) * 100).toInt().toString()
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
                    viewModel.deductCustomerPoints(userId, pointsToDeduct)
                }
                viewModel.saveFoodBooking("", cartManager, userId, totalPriceToPay - discountApplied, pickUpTime)
                Log.d("PaymentSheetResult", "Payment completed")
            }

            is PaymentSheetResult.Failed -> {
                Log.e("PaymentSheetResult", "Payment failed: ${paymentSheetResult.error.message}")
                Toast.makeText(
                    this,
                    "Payment failed: ${paymentSheetResult.error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }

            is PaymentSheetResult.Canceled -> {
                Log.d("PaymentSheetResult", "Payment canceled")
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
            dialog.dismiss()
            val currentTime =
                SimpleDateFormat("HH:mm", Locale.getDefault()).format(Calendar.getInstance().time)
            val dateString = "Today, ${
                SimpleDateFormat(
                    "MMM dd, yyyy",
                    Locale.getDefault()
                ).format(calendar.time)
            }"
            val dayOfWeekFormat = SimpleDateFormat("EEEE", Locale.getDefault())

            val dayOfWeek = "${dayOfWeekFormat.format(calendar.time)}, ${
                SimpleDateFormat(
                    "MMM dd, yyyy",
                    Locale.getDefault()
                ).format(calendar.time)
            }"
            binding.tvTimePickUp.text = "Pick up at $currentTime"
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
                    if (slideOffset < 0.01) dialog.dismiss()
                    Log.d("BottomSheet", "Slide offset: $slideOffset")
                }
            })
        }

        val dateText = dialog.findViewById<TextView>(R.id.dateText)
        val hourPicker = dialog.findViewById<NumberPicker>(R.id.hourPicker)
        val minutePicker = dialog.findViewById<NumberPicker>(R.id.minutePicker)
        val btnSetTime = dialog.findViewById<Button>(R.id.btnSetTime)

        hourPicker?.minValue = 0
        hourPicker?.maxValue = 23
        hourPicker?.value = 14
        lastHour = hourPicker?.value ?: 14
        hourPicker?.setFormatter { value -> String.format("%02d", value) }

        minutePicker?.minValue = 0
        minutePicker?.maxValue = 59
        minutePicker?.value = 30
        minutePicker?.setFormatter { value -> String.format("%02d", value) }

        hourPicker?.let { removeSelectionDivider(it) }
        minutePicker?.let { removeSelectionDivider(it) }

        if (dateText != null && hourPicker != null && minutePicker != null) {
            updateDateText(dateText, hourPicker.value, minutePicker.value)
        }

        hourPicker?.setOnValueChangedListener { _, oldHour, newHour ->
            if (oldHour == 23 && newHour == 0) {
                calendar.add(Calendar.DAY_OF_MONTH, 1)
            } else if (oldHour == 0 && newHour == 23) {
                val newDate = calendar.clone() as Calendar
                newDate.add(Calendar.DAY_OF_MONTH, -1)
                if (isBeforeToday(newDate)) {
                    calendar.set(
                        todayCalendar.get(Calendar.YEAR),
                        todayCalendar.get(Calendar.MONTH),
                        todayCalendar.get(Calendar.DAY_OF_MONTH)
                    )
                } else {
                    calendar.add(Calendar.DAY_OF_MONTH, -1)
                }
            }
            lastHour = newHour
            if (dateText != null && minutePicker != null) {
                updateDateText(dateText, newHour, minutePicker.value)
            }
        }

        minutePicker?.setOnValueChangedListener { _, _, newMinute ->
            if (dateText != null && hourPicker != null) {
                updateDateText(dateText, hourPicker.value, newMinute)
            }
        }

        btnSetTime?.setOnClickListener {
            val selectedDate =
                SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(calendar.time)
            val selectedTime = String.format("%02d:%02d", hourPicker?.value, minutePicker?.value)
            updateDateText(binding.tvTimeValue, hourPicker?.value ?: 0, minutePicker?.value ?: 0)
            dialog.dismiss()
        }
    }

    private fun updateDateText(dateText: TextView, hour: Int, minute: Int) {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val dayOfWeekFormat = SimpleDateFormat("EEEE", Locale.getDefault())
        val dateString = if (isToday(calendar)) {
            "Today, ${dateFormat.format(calendar.time)}"
        } else {
            "${dayOfWeekFormat.format(calendar.time)}, ${dateFormat.format(calendar.time)}"
        }

        val time = String.format("%02d:%02d", hour, minute)
        dateText.text = "$dateString"

        val dayOfWeek =
            "${dayOfWeekFormat.format(calendar.time)}, ${dateFormat.format(calendar.time)}"
        binding.tvTimePickUp.text = "Pick up at $time"
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
}