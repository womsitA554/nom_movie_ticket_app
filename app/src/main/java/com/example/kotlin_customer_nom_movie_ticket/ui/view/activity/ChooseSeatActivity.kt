package com.example.kotlin_customer_nom_movie_ticket.ui.view.activity

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.text.HorizontalTextInVerticalContextSpan
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.kotlin_customer_nom_movie_ticket.R
import com.example.kotlin_customer_nom_movie_ticket.databinding.ActivityChooseSeatBinding
import com.example.kotlin_customer_nom_movie_ticket.helper.HorizontalSpaceItemDecoration
import com.example.kotlin_customer_nom_movie_ticket.ui.adapter.SeatAdapter
import com.example.kotlin_customer_nom_movie_ticket.util.SessionManager
import com.example.kotlin_customer_nom_movie_ticket.viewmodel.SeatViewModel
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.NumberFormat
import java.util.Locale

class ChooseSeatActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChooseSeatBinding
    private lateinit var seatAdapter: SeatAdapter
    private lateinit var seatViewModel: SeatViewModel
    private lateinit var countDownTimer: CountDownTimer
    private var totalPrice = 0.0
    private var showtimeId: String? = null
    private var roomId: String? = null
    private lateinit var userId: String
    private var timeLeftInMillis: Long = 0
    private val ticketIds = mutableMapOf<String, String>()
    private var selectedSeatId = emptyList<String>()

    private var isSeatsLoaded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChooseSeatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.progressBar.visibility = View.VISIBLE
        binding.progressBar.playAnimation()
        binding.rcvSeat.visibility = View.GONE

        val cinemaId = intent.getStringExtra("cinema_id")
        val cinemaName = intent.getStringExtra("cinema_name")
        showtimeId = intent.getStringExtra("showtime_id")
        roomId = intent.getStringExtra("room_id")
        val showtimeTime = intent.getStringExtra("showtime_time")
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
        val movieActorIds = intent.getStringArrayListExtra("actor_ids")?.toList()
        val movieAgeRating = intent.getStringExtra("age_rating")
        val movieRating = intent.getFloatExtra("rating", 0f)
        val movieBanner = intent.getStringExtra("banner")

        userId = SessionManager.getUserId(this).toString()
        setupRecycleView()

        seatViewModel = ViewModelProvider(this).get(SeatViewModel::class.java)
        if (showtimeId != null && roomId != null) {
            seatViewModel.fetchSeat(roomId!!, showtimeId)
        }

        seatViewModel.seats.observe(this) { seats ->
            seatAdapter.updateSeats(seats)
            isSeatsLoaded = true
            updateUI(cinemaId!!, movieId!!, showtimeTime!!)
            checkAllDataLoaded()
            Log.d("CSeatList", "seatList: ${seats}")
        }

        seatAdapter = SeatAdapter(emptyList(), userId, seatViewModel, showtimeId!!)
        binding.rcvSeat.adapter = seatAdapter

        seatAdapter.onClickItem = { seatList, position ->
            val seat = seatList[position]
            val isSelected = seatAdapter.getSelectedSeats().contains(seat)
            selectedSeatId = seatAdapter.getSelectedSeats().map { it.seat_id }
            val userIdFromBooking = seatViewModel.getSeatBookingUserId(seat.seat_id)

            if (isSelected && seat.status == "available") {
                seatViewModel.updateSeatStatus(
                    showtimeId!!,
                    seat.seat_id,
                    "reserved",
                    userId
                ) { success ->
                    if (!success) {
                        seatAdapter.clearSelection()
                        Toast.makeText(this, "Failed to reserve seat", Toast.LENGTH_SHORT).show()
                    }
                    Log.d("ChooseSeatActivity", "Reserved seat: ${seat.seat_id}")
                }
            } else if (!isSelected && seat.status == "reserved" && userIdFromBooking == userId) {
                seatViewModel.updateSeatStatus(
                    showtimeId!!,
                    seat.seat_id,
                    "available",
                    userId
                ) { success ->
                    if (success) {
                        lifecycleScope.launch {
                            cleanupTicketForSeat(seat.seat_id)
                            updateUI(cinemaId!!, movieId!!, showtimeTime!!)
                        }
                    } else {
                        Toast.makeText(this, "Failed to release seat", Toast.LENGTH_SHORT).show()
                    }
                    Log.d("ChooseSeatActivity", "Released seat: ${seat.seat_id}")
                }
            }
        }

        binding.btnBack.setOnClickListener {
            resetAllSeats()
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        binding.tvSeatQuantity.text = "0"
        binding.tvPrice.text = "$0.0"

        startTimer()

        binding.btnContinue.setOnClickListener {
            if (seatAdapter.getSelectedSeatsCount() == 0) {
                showChooseSeatFailDialog("Vui lòng chọn ít nhất một chỗ ngồi")
                return@setOnClickListener
            } else {
                val (isValid, errorMessage) = checkSeatSelectionValidity()
                if (!isValid) {
                    showChooseSeatFailDialog("Vui lòng không để trống 1 ghế bên trái hoặc bên phải chỗ ngồi mà bạn đã chọn.")
                    return@setOnClickListener
                } else {
                    lifecycleScope.launch {
                        createTickets(cinemaId!!)
                        val intent = Intent(this@ChooseSeatActivity, FoodAndDrinkActivity::class.java)
                        intent.putExtra("cinema_id", cinemaId)
                        intent.putExtra("cinema_name", cinemaName)
                        intent.putExtra("showtime_id", showtimeId)
                        intent.putExtra("showtime_time", showtimeTime)
                        intent.putExtra("total_price_seats", totalPrice)
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
                        intent.putStringArrayListExtra("actor_ids", movieActorIds?.let { ArrayList(it) })
                        intent.putExtra("seat_price", binding.tvPrice.text.toString())
                        intent.putExtra("seat_name", binding.tvSeatQuantity.text)
                        intent.putExtra("time_left", timeLeftInMillis)
                        intent.putStringArrayListExtra("ticket_ids", ArrayList(ticketIds.keys.toList()))
                        intent.putStringArrayListExtra("selected_seat_ids", ArrayList(selectedSeatId))
                        Log.d("ChooseSeatActivity", "Selected seat ids: ${ArrayList(selectedSeatId)}")
                        startActivityForResult(intent, REQUEST_CODE_FOOD)
                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    }
                }
            }
        }
    }

    private fun checkAllDataLoaded() {
        if (isSeatsLoaded) {
            stopAnimation()
            binding.rcvSeat.visibility = View.VISIBLE
            Log.d("ChooseSeatActivity", "Seats loaded, showing nestedScrollView")
        }
    }

    private fun stopAnimation() {
        binding.progressBar.cancelAnimation()
        binding.progressBar.visibility = View.GONE
    }

    private fun setupRecycleView() {
        val spaceInPixels = resources.getDimensionPixelSize(R.dimen.item_seat_spacing_3)
        binding.rcvSeat.apply {
            setHasFixedSize(true)
            layoutManager = GridLayoutManager(this@ChooseSeatActivity, 10)
            binding.rcvSeat.addItemDecoration(GridSpacingItemDecoration(spaceInPixels))
        }
    }

    class GridSpacingItemDecoration(private val spaceInPixels: Int) :
        RecyclerView.ItemDecoration() {
        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State
        ) {
            outRect.left = spaceInPixels
            outRect.right = spaceInPixels
            outRect.top = spaceInPixels
            outRect.bottom = spaceInPixels
        }
    }

    private fun startTimer(): CharSequence? {
        countDownTimer = object : CountDownTimer(7 * 60 * 1000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeftInMillis = millisUntilFinished
                val minutes = (millisUntilFinished / 1000) / 60
                val seconds = (millisUntilFinished / 1000) % 60
                binding.tvTimeLimit.text = String.format("%02d:%02d", minutes, seconds)
            }

            override fun onFinish() {
                binding.tvTimeLimit.text = "00:00"
                resetAllSeats()
                showDialog()
            }
        }.start()

        return binding.tvTimeLimit.text
    }

    private fun resetAllSeats() {
        seatViewModel.resetSeats(showtimeId!!, userId) { success ->
            if (!success) {
                Toast.makeText(this, "Failed to reset seats", Toast.LENGTH_SHORT).show()
            } else {
                lifecycleScope.launch {
                    Log.d(
                        "ResetAllSeats",
                        "Starting to clean up tickets. ticketIds size: ${ticketIds.size}"
                    )
                    val dbTickets = FirebaseDatabase.getInstance().reference.child("Tickets")
                    try {
                        val updates = mutableMapOf<String, Any?>()
                        ticketIds.forEach { (ticketId, seatId) ->
                            updates["$ticketId"] = null
                            Log.d(
                                "ResetAllSeats",
                                "Queuing ticket for deletion: ticketId=$ticketId, seatId=$seatId"
                            )
                        }
                        dbTickets.updateChildren(updates).await()
                        ticketIds.clear()
                        Log.d(
                            "ResetAllSeats",
                            "Batch deleted tickets. Cleared ticketIds. New size: ${ticketIds.size}"
                        )
                    } catch (e: Exception) {
                        Log.e("ResetAllSeats", "Error batch deleting tickets", e)
                    }
                    seatAdapter.clearSelection()
                    updateUI(
                        cinemaId = intent.getStringExtra("cinema_id")!!,
                        movieId = intent.getStringExtra("movie_id")!!,
                        showtimeTime = intent.getStringExtra("showtime_time")!!
                    )
                }
            }
        }
    }

    private fun showDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_time_limit_finish)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCancelable(false)
        dialog.show()

        dialog.findViewById<Button>(R.id.btnOk).setOnClickListener {
            dialog.dismiss()
            binding.tvSeatQuantity.text = "0"
            totalPrice = 0.0
            binding.tvPrice.text = "0đ"
            countDownTimer.cancel()
            onBackPressed()
        }
    }

    private fun updateUI(cinemaId: String, movieId: String, showtimeTime: String) {
        binding.tvSeatQuantity.text = seatAdapter.getSelectedSeatNames()
        val selectedSeats = seatAdapter.getSelectedSeats()

        lifecycleScope.launch {
            var totalPrice = 0.0
            Log.d("UpdateUI", "Selected seats: ${selectedSeats.map { it.seat_id }}")
            for (seat in selectedSeats) {
                val price = pricePerSeat(cinemaId, movieId, seat.seat_type, showtimeTime)
                Log.d("UpdateUI", "Seat ${seat.seat_id}, type=${seat.seat_type}, price=$price")
                if (price != null) {
                    totalPrice += price
                }
            }
            this@ChooseSeatActivity.totalPrice = totalPrice
            val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))
            val price = totalPrice.toInt()
            binding.tvPrice.text = formatter.format(price) + "đ"
            Log.d("ChooseSeatActivity", "Total price for ${selectedSeats.size} seats: $totalPrice")
        }
    }

    private suspend fun createTickets(cinemaId: String) {
        val selectedSeats = seatAdapter.getSelectedSeats()
        val createdAt = System.currentTimeMillis()
        val dbTickets = FirebaseDatabase.getInstance().reference.child("Tickets")
        val showtimeTime = intent.getStringExtra("showtime_time") ?: ""
        val movieId = intent.getStringExtra("movie_id") ?: ""

        selectedSeats.forEachIndexed { index, seat ->
            val existingTicketSnapshot = dbTickets
                .orderByChild("seat_id")
                .equalTo(seat.seat_id)
                .get()
                .await()

            var ticketId: String? = null
            var shouldCreateNewTicket = true

            for (ticketSnapshot in existingTicketSnapshot.children) {
                val existingShowtimeId =
                    ticketSnapshot.child("showtime_id").getValue(String::class.java)
                val paymentStatus =
                    ticketSnapshot.child("payment_status").getValue(String::class.java)

                if (existingShowtimeId == showtimeId && paymentStatus == "Pending") {
                    ticketId = ticketSnapshot.key
                    shouldCreateNewTicket = false
                    break
                }
            }

            if (shouldCreateNewTicket) {
                ticketId = "T${createdAt}$index"
                val pricePerSeat = pricePerSeat(cinemaId, movieId, seat.seat_type, showtimeTime)
                val ticketData = hashMapOf(
                    "ticket_id" to ticketId,
                    "cinema_id" to cinemaId,
                    "payment_method" to "Stripe",
                    "created_at" to createdAt,
                    "payment_status" to "Pending",
                    "showtime_id" to showtimeId,
                    "seat_id" to seat.seat_id,
                    "customer_id" to userId,
                    "price" to pricePerSeat
                )
                Log.d("CreateTicket", "Ticket created: $ticketData")
                dbTickets.child(ticketId).setValue(ticketData).await()
            }

            ticketIds[ticketId!!] = seat.seat_id
        }
    }

    private suspend fun cleanupTicketForSeat(seatId: String) {
        val dbTickets = FirebaseDatabase.getInstance().reference.child("Tickets")
        val ticketToRemove = ticketIds.entries.find { it.value == seatId }
        if (ticketToRemove != null) {
            val ticketId = ticketToRemove.key
            Log.d("CleanupTicket", "Found ticket to remove: ticketId=$ticketId, seatId=$seatId")
            try {
                val ticketSnapshot = dbTickets.child(ticketId).get().await()
                val paymentStatus =
                    ticketSnapshot.child("payment_status").getValue(String::class.java)
                if (paymentStatus == "Pending") {
                    dbTickets.child(ticketId).removeValue().await()
                    ticketIds.remove(ticketId)
                    Log.d(
                        "CleanupTicket",
                        "Successfully deleted pending ticket $ticketId for seat $seatId"
                    )
                } else {
                    Log.d(
                        "CleanupTicket",
                        "Ticket $ticketId for seat $seatId is not Pending (status: $paymentStatus), skipping deletion"
                    )
                }
            } catch (e: Exception) {
                Log.e("CleanupTicket", "Error deleting ticket $ticketId for seat $seatId", e)
            }
        } else {
            Log.d("CleanupTicket", "No ticket found for seatId: $seatId")
        }
    }

    private suspend fun pricePerSeat(
        cinemaId: String,
        movieId: String,
        seatType: String,
        showtimeTime: String
    ): Double? {
        val db = FirebaseDatabase.getInstance().reference
        val snapshot = db.child("TicketPrices")
            .orderByChild("cinema_id")
            .equalTo(cinemaId)
            .get()
            .await()

        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
        val showtimeDate = try {
            sdf.parse(showtimeTime)
        } catch (e: Exception) {
            Log.e("PricePerSeat", "Failed to parse showtimeTime: $showtimeTime", e)
            return null
        }

        val isWeekend = showtimeDate?.let {
            val calendar = java.util.Calendar.getInstance().apply { time = it }
            calendar.get(java.util.Calendar.DAY_OF_WEEK) in listOf(
                java.util.Calendar.SATURDAY,
                java.util.Calendar.SUNDAY
            )
        } ?: false
        val timeRange = showtimeDate?.let {
            val hour = it.hours
            when {
                hour < 12 -> "Morning"
                hour < 18 -> "Afternoon"
                else -> "Evening"
            }
        } ?: "Morning"
        val showtimeDateStr = showtimeDate?.let {
            java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(it)
        }

        Log.d("PricePerSeat", "Querying TicketPrices for cinemaId=$cinemaId, movieId=$movieId, seatType=$seatType, isWeekend=$isWeekend, timeRange=$timeRange, showtimeDate=$showtimeDateStr")

        for (priceSnapshot in snapshot.children) {
            val priceCinemaId = priceSnapshot.child("cinema_id").getValue(String::class.java)
            val priceMovieId = priceSnapshot.child("movie_id").getValue(String::class.java)
            val priceSeatType = priceSnapshot.child("seat_type").getValue(String::class.java)
            val priceIsWeekend = priceSnapshot.child("is_weekend").getValue(Boolean::class.java) ?: false
            val priceTimeRange = priceSnapshot.child("time_range").getValue(String::class.java)
            val price = priceSnapshot.child("price").getValue(Double::class.java)
            val priceEffectiveDate = priceSnapshot.child("effective_date").getValue(String::class.java)
            val ticketPriceId = priceSnapshot.child("ticket_price_id").getValue(String::class.java)

            Log.d(
                "PricePerSeat",
                "Found entry: ticket_price_id=$ticketPriceId, cinemaId=$priceCinemaId, movieId=$priceMovieId, seatType=$priceSeatType, " +
                        "isWeekend=$priceIsWeekend, timeRange=$priceTimeRange, price=$price, effectiveDate=$priceEffectiveDate"
            )

            if (price == null) {
                Log.e(
                    "PricePerSeat",
                    "No price value for ticket_price_id=$ticketPriceId, cinemaId=$cinemaId, movieId=$movieId, seatType=$seatType"
                )
                continue
            }

            if (priceCinemaId == cinemaId &&
                priceMovieId == movieId &&
                priceSeatType == seatType &&
                priceIsWeekend == isWeekend &&
                priceTimeRange == timeRange &&
                priceEffectiveDate == showtimeDateStr
            ) {
                Log.d("PricePerSeat", "Matched price: $price for ticket_price_id=$ticketPriceId")
                return price
            }
        }

        Log.e(
            "PricePerSeat",
            "No matching price found for cinemaId=$cinemaId, movieId=$movieId, seatType=$seatType, isWeekend=$isWeekend, timeRange=$timeRange, showtimeDate=$showtimeDateStr"
        )
        return null
    }

    private fun checkSeatSelectionValidity(): Pair<Boolean, String> {
        val allSeats = seatAdapter.getAllSeats()
        val rows = ('A'..'J').toList()

        for (row in rows) {
            val seat1 = allSeats.find { it.row_number == row.toString() && it.seat_number == "1" }
            val seat2 = allSeats.find { it.row_number == row.toString() && it.seat_number == "2" }
            val seat9 = allSeats.find { it.row_number == row.toString() && it.seat_number == "9" }
            val seat10 = allSeats.find { it.row_number == row.toString() && it.seat_number == "10" }

            if (seat1?.status == "available" && seat2?.status in listOf("reserved", "booked")) {
                return Pair(false, "")
            }

            if (seat10?.status == "available" && seat9?.status in listOf("reserved", "booked")) {
                return Pair(false, "")
            }
        }

        for (row in rows) {
            val seatsInRow = allSeats.filter { it.row_number == row.toString() }
                .sortedBy { it.seat_number.toInt() }
            val reservedOrBookedSeats = seatsInRow.filter { it.status in listOf("reserved", "booked") }

            if (reservedOrBookedSeats.size >= 2) {
                for (i in 0 until reservedOrBookedSeats.size - 1) {
                    val firstSeat = reservedOrBookedSeats[i]
                    val secondSeat = reservedOrBookedSeats[i + 1]
                    val firstSeatNumber = firstSeat.seat_number.toInt()
                    val secondSeatNumber = secondSeat.seat_number.toInt()

                    val gapSize = secondSeatNumber - firstSeatNumber - 1
                    if (gapSize == 1) {
                        val middleSeatNumber = firstSeatNumber + 1
                        val middleSeat = seatsInRow.find { it.seat_number == middleSeatNumber.toString() }
                        if (middleSeat?.status == "available") {
                            return Pair(false, "")
                        }
                    }
                }
            }
        }

        return Pair(true, "Valid selection")
    }

    private fun showChooseSeatFailDialog(message :String) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_choose_seat)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCancelable(false)

        val btnClose = dialog.findViewById<Button>(R.id.btnClose)
        val tvError = dialog.findViewById<TextView>(R.id.tvError)

        tvError.text = message
        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAnimation()
        if (timeLeftInMillis > 0) {
            countDownTimer.cancel()
        } else {
            resetAllSeats()
            seatViewModel.stopListening(showtimeId)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_FOOD) {
            when (resultCode) {
                RESULT_OK -> {
                    finish()
                }

                RESULT_CANCELED -> {
                    resetAllSeats()
                    finish()
                }

                RESULT_FIRST_USER -> {

                }
            }
        }
    }

    companion object {
        private const val REQUEST_CODE_FOOD = 1002
    }
}