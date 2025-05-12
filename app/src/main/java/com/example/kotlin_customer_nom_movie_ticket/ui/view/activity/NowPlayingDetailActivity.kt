package com.example.kotlin_customer_nom_movie_ticket.ui.view.activity

import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.bumptech.glide.Glide
import com.example.kotlin_customer_nom_movie_ticket.R
import com.example.kotlin_customer_nom_movie_ticket.databinding.ActivityNowPlayingDetailBinding
import com.example.kotlin_customer_nom_movie_ticket.ui.adapter.ActorAdapter
import com.example.kotlin_customer_nom_movie_ticket.viewmodel.ActorViewModel
import com.example.kotlin_customer_nom_movie_ticket.viewmodel.DirectorViewModel
import com.example.kotlin_customer_nom_movie_ticket.viewmodel.TicketViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.GenericTypeIndicator
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class NowPlayingDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNowPlayingDetailBinding
    private lateinit var directorViewModel: DirectorViewModel
    private lateinit var actorAdapter: ActorAdapter
    private lateinit var actorViewModel: ActorViewModel
    private lateinit var ticketViewModel: TicketViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNowPlayingDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Khởi tạo ViewModel
        directorViewModel = ViewModelProvider(this)[DirectorViewModel::class.java]
        actorViewModel = ViewModelProvider(this)[ActorViewModel::class.java]
        ticketViewModel = ViewModelProvider(this)[TicketViewModel::class.java]

        // Lấy dữ liệu từ Intent
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
        val movieQuantityVote = intent.getIntExtra("quantity_vote", 0)
        val movieBanner = intent.getStringExtra("banner")

        if (movieDirectorId != null) {
            directorViewModel.fetchDirectorNameById(movieDirectorId)
        } else {
        }

        directorViewModel.directorName.observe(this) { name ->
            binding.tvDirectorName.text = name
        }

        binding.tvTitle.text = movieTitle
        binding.tvAgeRate.text = movieAgeRating
        binding.tvDuration.text = "$movieDuration minutes"
        binding.tvGenre.text = movieGenre
        binding.tvSynopsis.text = movieSynopsis
        binding.tvRating.text = String.format("%.1f", movieRating)
        binding.tvReviewer.text = "($movieQuantityVote reviews)"
        Glide.with(this).load(moviePosterUrl).into(binding.picMovie)
        Glide.with(this).load(movieBanner).into(binding.bannerMovie)

        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        if (movieTrailerUrl.isNullOrEmpty()) {
            Toast.makeText(this, "Không có URL trailer", Toast.LENGTH_SHORT).show()
        } else {
            binding.trailerVideo.setOnClickListener {
                val intent = Intent(this, VideoPlayerActivity::class.java)
                intent.putExtra("trailer_url", movieTrailerUrl)
                startActivity(intent)
            }
        }

        setupRecycleView()
        actorAdapter = ActorAdapter(emptyList())
        binding.rcvCast.adapter = actorAdapter

        if (movieActorIds != null && movieActorIds.isNotEmpty()) {
            actorViewModel.fetchActorsByIds(movieActorIds)
            actorViewModel.actors.observe(this) { actors ->
                actorAdapter = ActorAdapter(actors)
                binding.rcvCast.adapter = actorAdapter
            }
        } else {
            Toast.makeText(this, "Không có diễn viên nào", Toast.LENGTH_SHORT).show()
        }

        // Cập nhật RatingBar
        val ratingNumber = movieRating / 2
        binding.ratingBar.rating = ratingNumber

        // Nút Book
        binding.btnBook.setOnClickListener {
            val intent = Intent(this, ChooseCinemaActivity::class.java)
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
            intent.putStringArrayListExtra("actor_ids", ArrayList(movieActorIds))
            startActivity(intent)
        }

        binding.btnRate.setOnClickListener {
            if (movieId == null) {
                Toast.makeText(this, "Không tìm thấy ID phim", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            checkRatingEligibility(movieId)
        }

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            ticketViewModel.fetchBookings(userId)
        } else {
        }
    }

    private fun checkRatingEligibility(movieId: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            bottomSheetCanNotRate()
            return
        }

        ticketViewModel.passedBookings.observe(this) { bookings ->
            val relevantBooking = bookings?.find { it.movie_id == movieId }
            if (relevantBooking == null) {
                bottomSheetCanNotRate()
            } else {
                bottomSheetRate(movieId)
            }
        }

        ticketViewModel.error.observe(this) { error ->
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
                bottomSheetCanNotRate()
            }
        }
    }

    private fun bottomSheetRate(movieId: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            return
        }

        val database = FirebaseDatabase.getInstance().reference
        val userRatingRef = database.child("UserRatings").child(userId).child(movieId)

        userRatingRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                bottomSheetAlreadyRated()
            } else {
                val dialog = BottomSheetDialog(this)
                dialog.setContentView(R.layout.bottom_sheet_show_rate)
                dialog.show()

                val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
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
                        }
                    })
                }

                val ratingBar1 = dialog.findViewById<RatingBar>(R.id.ratingBar1)
                val ratingBar2 = dialog.findViewById<RatingBar>(R.id.ratingBar2)
                val btnCancel = dialog.findViewById<Button>(R.id.btnCancel)
                val btnRateNow = dialog.findViewById<Button>(R.id.btnRateNow)

                ratingBar1?.rating = 0f
                ratingBar2?.rating = 0f

                ratingBar1?.setOnRatingBarChangeListener { _, rating, fromUser ->
                    if (fromUser && rating > 0) {
                        ratingBar2?.rating = 0f
                    }
                }

                ratingBar2?.setOnRatingBarChangeListener { _, rating, fromUser ->
                    if (fromUser && rating > 0) {
                        ratingBar1?.rating = 5f
                    }
                }

                btnCancel?.setOnClickListener {
                    dialog.dismiss()
                }

                btnRateNow?.setOnClickListener {
                    val userRating = calculateTotalRating(ratingBar1, ratingBar2)
                    if (userRating == 0) {
                        return@setOnClickListener
                    }

                    // Reference to the movie's ratings
                    val movieRef = database.child("Movies").child(movieId).child("ratings")

                    // Update movie ratings and store user rating
                    movieRef.runTransaction(object : Transaction.Handler {
                        override fun doTransaction(currentData: MutableData): Transaction.Result {
                            val totalScore = currentData.child("total_score").getValue(Int::class.java) ?: 0
                            val totalVotes = currentData.child("total_votes").getValue(Int::class.java) ?: 0

                            val newTotalScore = totalScore + userRating
                            val newTotalVotes = totalVotes + 1
                            val newAverageRating = newTotalScore.toFloat() / newTotalVotes

                            currentData.child("total_score").value = newTotalScore
                            currentData.child("total_votes").value = newTotalVotes
                            currentData.child("average_rating").value = newAverageRating

                            return Transaction.success(currentData)
                        }

                        override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                            if (error != null) {
                            } else if (committed) {
                                userRatingRef.setValue(userRating).addOnSuccessListener {
                                    val newAverageRating = currentData?.child("average_rating")?.getValue(Float::class.java) ?: 0f
                                    val movieQuantityVote = currentData?.child("total_votes")?.getValue(Int::class.java) ?: 0
                                    binding.tvRating.text = String.format("%.1f", newAverageRating)
                                    binding.ratingBar.rating = newAverageRating / 2
                                    binding.tvReviewer.text = "($movieQuantityVote reviews)"
                                    dialog.dismiss()
                                    bottomSheetRateSuccess()
                                }.addOnFailureListener {
                                }
                            } else {
                            }
                        }
                    })
                }
            }
        }.addOnFailureListener {
        }
    }
    private fun calculateTotalRating(ratingBar1: RatingBar?, ratingBar2: RatingBar?): Int {
        val rating1 = ratingBar1?.rating?.toInt() ?: 0
        val rating2 = ratingBar2?.rating?.toInt() ?: 0
        return if (rating2 > 0) 5 + rating2 else rating1
    }

    private fun bottomSheetRateSuccess(){
        val dialog = BottomSheetDialog(this)
        dialog.setContentView(R.layout.bottom_sheet_rate_success)
        dialog.show()

        dialog.findViewById<LottieAnimationView>(R.id.lottieAnimationView)?.playAnimation()

        val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
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
                }
            })
        }
    }

    private fun bottomSheetAlreadyRated(){
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_already_rated, null)
        dialog.setContentView(view)
        dialog.show()

        val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
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
                }
            })
        }

        val btnOkay = dialog.findViewById<Button>(R.id.btnOkay)
        btnOkay?.setOnClickListener {
            dialog.dismiss()
        }
    }

    private fun bottomSheetCanNotRate() {
        val dialog = BottomSheetDialog(this)
        dialog.setContentView(R.layout.bottom_sheet_can_not_rate)
        dialog.show()

        val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
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
                }
            })
        }

        val btnOkay = dialog.findViewById<Button>(R.id.btnOkay)
        btnOkay?.setOnClickListener {
            dialog.dismiss()
        }
    }

    private fun setupRecycleView() {
        binding.rcvCast.setHasFixedSize(true)
        binding.rcvCast.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rcvCast.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                val spacing = resources.getDimensionPixelSize(R.dimen.item_spacing)
                outRect.right = spacing
                outRect.left = if (parent.getChildAdapterPosition(view) == 0) spacing else 0
            }
        })
    }

    fun sendNotificationWithFCMv1(context: Context, recipientToken: String, senderName: String, messageText: String) {
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
}