package com.example.kotlin_customer_nom_movie_ticket.ui.view.activity

import android.app.Dialog
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kotlin_customer_nom_movie_ticket.R
import com.example.kotlin_customer_nom_movie_ticket.data.model.Day
import com.example.kotlin_customer_nom_movie_ticket.data.model.Showtime
import com.example.kotlin_customer_nom_movie_ticket.databinding.ActivityCinemaDetailFromBookNowBinding
import com.example.kotlin_customer_nom_movie_ticket.ui.adapter.DayAdapter
import com.example.kotlin_customer_nom_movie_ticket.ui.adapter.ShowtimeAdapter
import com.example.kotlin_customer_nom_movie_ticket.data.repository.ShowtimeRepository
import com.example.kotlin_customer_nom_movie_ticket.util.SessionManager
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.TextStyle
import java.util.Locale

class CinemaDetailFromBookNowActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var binding: ActivityCinemaDetailFromBookNowBinding
    private lateinit var googleMap: GoogleMap
    private lateinit var dayAdapter: DayAdapter
    private lateinit var showtimeAdapter: ShowtimeAdapter
    private lateinit var showtimeRepository: ShowtimeRepository
    private var selectedDate: LocalDate? = null
    private lateinit var userId: String
    private var isFavorite = false

    // Biến để theo dõi trạng thái load
    private var isMapLoaded = false
    private var isDaysLoaded = false
    private var isShowtimesLoaded = false
    private var isFavoriteStatusLoaded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCinemaDetailFromBookNowBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Khởi tạo UI ban đầu: hiển thị progressBar, ẩn nestedScrollView
        binding.progressBar.visibility = View.VISIBLE
        binding.progressBar.playAnimation() // Bắt đầu animation
        binding.nestedScrollView.visibility = View.GONE

        val isDarkMode = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        userId = SessionManager.getUserId(this) ?: run {
            finish()
            return
        }
        val movieId = intent.getStringExtra("movie_id") ?: return
        val cinemaId = intent.getStringExtra("cinema_id") ?: return
        val cinemaName = intent.getStringExtra("cinema_name")
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
        val phoneNumber = intent.getStringExtra("phone_number")
        val cinemaAddress = intent.getStringExtra("address")
        showtimeRepository = ShowtimeRepository()

        binding.tvCinemaName.text = cinemaName
        binding.tvPhoneNumber.text = phoneNumber
        binding.tvAddress.text = cinemaAddress

        // Khởi tạo danh sách ngày
        val days = generateDays(LocalDate.now(), 7)
        dayAdapter = DayAdapter(days, isDarkMode)
        setupRecycleView()
        binding.rcvDay.adapter = dayAdapter
        selectedDate = days.first().fullDate
        isDaysLoaded = true // Danh sách ngày được tạo ngay lập tức
        checkAllDataLoaded()

        showtimeAdapter = ShowtimeAdapter(emptyList()) { showtime ->
            showTimeLimitDialog(cinemaId, cinemaName, showtime.showtime_id, showtime.showtime_time, showtime.room_id, movieId, moviePosterUrl, movieTitle, movieLanguage, movieCountry, movieReleaseYear, movieDuration, movieGenre, movieSynopsis, movieDirectorId, movieStatus, movieTrailerUrl, movieActorIds, movieAgeRating, movieRating, movieBanner)
        }
        binding.recyclerViewTimes.adapter = showtimeAdapter

        // Load suất chiếu
        selectedDate?.let { date ->
            filterShowtimeByMovieIdAndDateAndCinema(movieId, cinemaId, date)
        }

        dayAdapter.onClickItem = { daysList, position ->
            selectedDate = daysList[position].fullDate
            selectedDate?.let { date ->
                filterShowtimeByMovieIdAndDateAndCinema(movieId, cinemaId, date)
            }
        }

        binding.btnBack.setOnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        binding.btnFavorite.setOnClickListener {
            toggleFavorite(cinemaId)
        }

        // Kiểm tra trạng thái yêu thích
        checkFavoriteStatus(cinemaId)

        // Khởi tạo bản đồ
        val mapFragment = supportFragmentManager.findFragmentById(binding.mapView.id) as SupportMapFragment?
            ?: SupportMapFragment.newInstance()
        supportFragmentManager.beginTransaction()
            .replace(binding.mapView.id, mapFragment)
            .commit()
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        isMapLoaded = true // Bản đồ đã load
        checkAllDataLoaded()

        val cinemaLatitude = intent.getDoubleExtra("latitude", 0.0)
        val cinemaLongitude = intent.getDoubleExtra("longitude", 0.0)
        val cinemaAddress = intent.getStringExtra("address")
        val cinemaName = intent.getStringExtra("cinema_name") ?: "Unknown"

        val cinemaLocation = LatLng(cinemaLatitude, cinemaLongitude)

        googleMap.addMarker(
            MarkerOptions().position(cinemaLocation).title(cinemaName).snippet(cinemaAddress)
        )
        googleMap.setOnMarkerClickListener { marker ->
            marker.showInfoWindow()
            true
        }
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(cinemaLocation, 15f))
        googleMap.uiSettings.isZoomControlsEnabled = false

        googleMap.setOnMarkerClickListener { marker ->
            val gmmIntentUri =
                Uri.parse("geo:$cinemaLatitude,$cinemaLongitude?q=$cinemaLatitude,$cinemaLongitude($cinemaName)")
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            mapIntent.setPackage("com.google.android.apps.maps")

            if (mapIntent.resolveActivity(packageManager) != null) {
                startActivity(mapIntent)
            } else {
                val webIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://maps.google.com/?q=$cinemaLatitude,$cinemaLongitude")
                )
                startActivity(webIntent)
            }
            true
        }
    }

    fun generateDays(startDate: LocalDate, numberOfDays: Int): List<Day> {
        val days = mutableListOf<Day>()
        for (i in 0 until numberOfDays) {
            val date = startDate.plusDays(i.toLong())
            val day = Day(
                dayNumber = String.format("%02d", date.dayOfMonth),
                dayName = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                fullDate = date,
                isSelected = i == 0
            )
            days.add(day)
        }
        return days
    }

    private fun setupRecycleView() {
        binding.rcvDay.setHasFixedSize(true)
        binding.rcvDay.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        binding.recyclerViewTimes.setHasFixedSize(true)
        binding.recyclerViewTimes.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
    }

    private fun filterShowtimeByMovieIdAndDateAndCinema(movieId: String, cinemaId: String, date: LocalDate) {
        showtimeRepository.getShowtimesByMovieCinemaAndDate(movieId, cinemaId, date) { showtimes ->
            val currentDateTime = LocalDateTime.now()
            val filteredShowtimes = showtimes.filter { showtime ->
                val showtimeDateTime = LocalDateTime.parse(showtime.showtime_time)
                showtimeDateTime.isAfter(currentDateTime)
            }

            showtimeAdapter.updateData(filteredShowtimes)
            isShowtimesLoaded = true // Suất chiếu đã load
            binding.recyclerViewTimes.visibility = if (filteredShowtimes.isNotEmpty()) View.VISIBLE else View.GONE
            binding.layoutNotFoundMovie.visibility = if (filteredShowtimes.isEmpty()) View.VISIBLE else View.GONE
            binding.tvPriceTitle.visibility = if (filteredShowtimes.isNotEmpty()) View.VISIBLE else View.GONE
            Log.d("ShowtimeByMovie", filteredShowtimes.toString())
            checkAllDataLoaded()
        }
    }

    private fun checkAllDataLoaded() {
        if (isMapLoaded && isDaysLoaded && isShowtimesLoaded && isFavoriteStatusLoaded) {
            // Tất cả dữ liệu đã load, cập nhật UI
            stopAnimation()
            binding.nestedScrollView.visibility = View.VISIBLE
            Log.d("CinemaDetailActivity", "All data loaded, showing nestedScrollView")
        }
    }

    private fun stopAnimation() {
        binding.progressBar.cancelAnimation() // Dừng animation
        binding.progressBar.visibility = View.GONE // Ẩn progressBar
    }

    private fun showTimeLimitDialog(
        cinemaId: String?, cinemaName: String?, showtimeId: String, showtimeTime: String, roomId: String,
        movieId: String?, moviePosterUrl: String?, movieTitle: String?, movieLanguage: String?,
        movieCountry: String?, movieReleaseYear: String?, movieDuration: Int?, movieGenre: String?,
        movieSynopsis: String?, movieDirectorId: String?, movieStatus: String?, movieTrailerUrl: String?,
        movieActorIds: List<String>?, movieAgeRating: String?, movieRating: Float?, movieBanner: String?
    ) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_time_limit)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val btnUnderstand = dialog.findViewById<Button>(R.id.btnUnderstand)
        btnUnderstand.setOnClickListener {
            dialog.dismiss()
            val intent = Intent(this, ChooseSeatActivity::class.java)
            intent.putExtra("cinema_id", cinemaId)
            intent.putExtra("showtime_id", showtimeId)
            intent.putExtra("cinema_name", cinemaName)
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
            intent.putStringArrayListExtra("actor_ids", ArrayList(movieActorIds))
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        dialog.setCancelable(false)
        dialog.show()
    }

    private fun checkFavoriteStatus(cinemaId: String) {
        val db = FirebaseDatabase.getInstance().reference
        db.child("FavoriteCinemas").child(userId).child(cinemaId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    isFavorite = snapshot.getValue(Boolean::class.java) == true
                    isFavoriteStatusLoaded = true // Trạng thái yêu thích đã load
                    updateFavoriteButton()
                    checkAllDataLoaded()
                    Log.d("CinemaDetailActivity", if (isFavorite) "Favorite: $cinemaId" else "Not Favorite: $cinemaId")
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@CinemaDetailFromBookNowActivity, "Error checking favorite: ${error.message}", Toast.LENGTH_SHORT).show()
                    isFavoriteStatusLoaded = true // Đánh dấu là đã load để không chặn UI
                    checkAllDataLoaded()
                }
            })
    }

    private fun toggleFavorite(cinemaId: String) {
        val db = FirebaseDatabase.getInstance().reference
        if (isFavorite) {
            db.child("FavoriteCinemas").child(userId).child(cinemaId).removeValue()
                .addOnSuccessListener {
                    isFavorite = false
                    updateFavoriteButton()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to remove favorite: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            db.child("FavoriteCinemas").child(userId).child(cinemaId).setValue(true)
                .addOnSuccessListener {
                    isFavorite = true
                    updateFavoriteButton()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to add favorite: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun updateFavoriteButton() {
        if (isFavorite) {
            binding.btnFavorite.setImageResource(R.drawable.star_filled_icon)
        } else {
            binding.btnFavorite.setImageResource(R.drawable.star_outline_icon)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAnimation() // Dừng và ẩn progressBar khi activity bị hủy
    }
}