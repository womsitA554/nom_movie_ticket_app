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
import com.example.kotlin_customer_nom_movie_ticket.data.model.Movie
import com.example.kotlin_customer_nom_movie_ticket.data.model.Showtime
import com.example.kotlin_customer_nom_movie_ticket.data.repository.ShowtimeRepository
import com.example.kotlin_customer_nom_movie_ticket.databinding.ActivityCinemaDetailBinding
import com.example.kotlin_customer_nom_movie_ticket.ui.adapter.ChooseListMovieAdapter
import com.example.kotlin_customer_nom_movie_ticket.ui.adapter.DayAdapter
import com.example.kotlin_customer_nom_movie_ticket.util.SessionManager
import com.example.kotlin_customer_nom_movie_ticket.viewmodel.DirectorViewModel
import com.example.kotlin_customer_nom_movie_ticket.viewmodel.MovieViewModel
import com.example.kotlin_customer_nom_movie_ticket.viewmodel.RoomViewModel
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

class CinemaDetailActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var binding: ActivityCinemaDetailBinding
    private lateinit var googleMap: GoogleMap
    private lateinit var dayAdapter: DayAdapter
    private lateinit var chooseListMovieAdapter: ChooseListMovieAdapter
    private lateinit var movieViewModel: MovieViewModel
    private lateinit var directorViewModel: DirectorViewModel
    private lateinit var showtimeRepository: ShowtimeRepository
    private lateinit var roomViewModel: RoomViewModel
    private var selectedDate: LocalDate? = null
    private lateinit var userId: String
    private var isFavorite = false

    private var isMapLoaded = false
    private var isDaysLoaded = false
    private var isMoviesAndShowtimesLoaded = false
    private var isFavoriteStatusLoaded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCinemaDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.progressBar.visibility = View.VISIBLE
        binding.progressBar.playAnimation()
        binding.nestedScrollView.visibility = View.GONE

        val isDarkMode = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        userId = SessionManager.getUserId(this) ?: run {
            finish()
            return
        }
        val cinemaId = intent.getStringExtra("cinema_id") ?: run {
            finish()
            return
        }
        val cinemaName = intent.getStringExtra("cinema_name")
        val cinemaAddress = intent.getStringExtra("address")
        val cinemaPhoneNumber = intent.getStringExtra("phone_number")
        val cinemaCreatedAt = intent.getStringExtra("created_at")

        binding.tvCinemaName.text = cinemaName
        binding.tvAddress.text = cinemaAddress
        binding.tvPhoneNumber.text = cinemaPhoneNumber

        // Khởi tạo danh sách ngày
        val days = generateDays(LocalDate.now(), 7)
        dayAdapter = DayAdapter(days, isDarkMode)
        setupRecycleView()
        binding.rcvDay.adapter = dayAdapter
        selectedDate = days.first().fullDate
        isDaysLoaded = true // Danh sách ngày được tạo ngay lập tức
        checkAllDataLoaded()

        movieViewModel = MovieViewModel()
        directorViewModel = DirectorViewModel()
        showtimeRepository = ShowtimeRepository()
        roomViewModel = RoomViewModel()

        // Fetch dữ liệu phim
        movieViewModel.fetchMoviesIsShowing()

        // Kiểm tra trạng thái yêu thích
        checkFavoriteStatus(cinemaId)

        binding.btnFavorite.setOnClickListener {
            toggleFavorite(cinemaId)
        }

        binding.btnBack.setOnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        // Khởi tạo bản đồ
        val mapFragment = supportFragmentManager.findFragmentById(binding.mapView.id) as SupportMapFragment?
            ?: SupportMapFragment.newInstance()
        supportFragmentManager.beginTransaction()
            .replace(binding.mapView.id, mapFragment)
            .commit()
        mapFragment.getMapAsync(this)

        movieViewModel.movieIsShowing.observe(this) { movies ->
            filterMoviesByDateAndCinema(movies, cinemaId)
            Log.d("CinemaDetailActivityOfmovieisShowing", "Movies: $movies")
        }

        dayAdapter.onClickItem = { daysList, position ->
            selectedDate = daysList[position].fullDate
            filterMoviesByDateAndCinema(movies = movieViewModel.movieIsShowing.value ?: emptyList(), cinemaId = cinemaId)
        }
    }

    private fun filterMoviesByDateAndCinema(movies: List<Movie>, cinemaId: String) {
        val showtimesMap = mutableMapOf<String, List<Showtime>>()
        val filteredMovies = mutableListOf<Movie>()
        val allShowtimes = mutableMapOf<String, List<Showtime>>()
        var processedMovies = 0

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

        val currentDateTime = LocalDateTime.now()

        if (movies.isEmpty()) {
            isMoviesAndShowtimesLoaded = true
            checkAllDataLoaded()
            binding.rcvListMovieIsShowing.adapter = null
            binding.rcvListMovieIsShowing.visibility = View.GONE
            binding.layoutNotFoundMovie.visibility = View.VISIBLE
            return
        }

        movies.forEach { movie ->
            showtimeRepository.getShowtimeByMovieId(movie.movie_id) { showtimes ->
                allShowtimes[movie.movie_id] = showtimes
                processedMovies++

                if (processedMovies == movies.size) {
                    val uniqueRoomIds = allShowtimes.values.flatten().map { it.room_id }.distinct()
                    val roomCinemaMap = mutableMapOf<String, String?>()
                    var processedRooms = 0

                    if (uniqueRoomIds.isEmpty()) {
                        isMoviesAndShowtimesLoaded = true
                        checkAllDataLoaded()
                        binding.rcvListMovieIsShowing.adapter = null
                        binding.rcvListMovieIsShowing.visibility = View.GONE
                        binding.layoutNotFoundMovie.visibility = View.VISIBLE
                        return@getShowtimeByMovieId
                    }

                    uniqueRoomIds.forEach { roomId ->
                        roomViewModel.fetchCinemaIdByRoomId(roomId) { roomCinemaId ->
                            roomCinemaMap[roomId] = roomCinemaId
                            processedRooms++

                            if (processedRooms == uniqueRoomIds.size) {
                                movies.forEach { movie ->
                                    val showtimesForMovie = allShowtimes[movie.movie_id] ?: emptyList()
                                    val filteredShowtimes = showtimesForMovie.filter { showtime ->
                                        val showtimeDate = LocalDate.parse(showtime.showtime_time.split("T")[0])
                                        val showtimeDateTime = LocalDateTime.parse(showtime.showtime_time)
                                        roomCinemaMap[showtime.room_id] == cinemaId &&
                                                showtimeDate == selectedDate &&
                                                showtimeDateTime.isAfter(currentDateTime)
                                    }
                                    if (filteredShowtimes.isNotEmpty()) {
                                        filteredMovies.add(movie)
                                        showtimesMap[movie.movie_id] = filteredShowtimes
                                    }
                                }

                                if (filteredMovies.isNotEmpty()) {
                                    chooseListMovieAdapter = ChooseListMovieAdapter(
                                        filteredMovies,
                                        directorViewModel,
                                        this,
                                        showtimesMap,
                                        onShowtimeClick = { showtime ->
                                            val movieForShowtime = filteredMovies.find { m ->
                                                showtimesMap[m.movie_id]?.contains(showtime) == true
                                            }
                                            movieForShowtime?.let { movie ->
                                                showTimeLimitDialog(
                                                    cinemaId,
                                                    cinemaName!!,
                                                    showtime.showtime_id,
                                                    showtime.showtime_time,
                                                    showtime.room_id,
                                                    movie.movie_id,
                                                    movie.poster_url,
                                                    movie.title,
                                                    movie.language,
                                                    movie.country,
                                                    movie.release_year.toString(),
                                                    movie.duration,
                                                    movie.genre,
                                                    movie.synopsis,
                                                    movie.director_id,
                                                    movieStatus,
                                                    movie.trailer_url,
                                                    movie.actor_ids,
                                                    movie.age_rating,
                                                    movie.ratings.average_rating,
                                                    movie.banner
                                                )
                                            }
                                        }
                                    )
                                    binding.rcvListMovieIsShowing.adapter = chooseListMovieAdapter
                                    binding.rcvListMovieIsShowing.visibility = View.VISIBLE
                                    binding.layoutNotFoundMovie.visibility = View.GONE
                                    Log.d("CinemaDetailActivity>>>", "Filtered Movies: $filteredMovies")
                                } else {
                                    binding.rcvListMovieIsShowing.adapter = null
                                    binding.rcvListMovieIsShowing.visibility = View.GONE
                                    binding.layoutNotFoundMovie.visibility = View.VISIBLE
                                    Log.d("CinemaDetailActivity>>>", "No movies found for the selected date and cinema")
                                }
                                isMoviesAndShowtimesLoaded = true
                                checkAllDataLoaded()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun checkAllDataLoaded() {
        if (isMapLoaded && isDaysLoaded && isMoviesAndShowtimesLoaded && isFavoriteStatusLoaded) {
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
        val vietnameseLocale = Locale("vi", "VN")
        val today = LocalDate.now()
        for (i in 0 until numberOfDays) {
            val date = startDate.plusDays(i.toLong())
            val dayName = if (date.isEqual(today)) {
                "Hôm nay"
            } else {
                date.dayOfWeek.getDisplayName(TextStyle.FULL, vietnameseLocale)
            }
            val day = Day(
                dayNumber = String.format("%02d", date.dayOfMonth),
                dayName = dayName,
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

        binding.rcvListMovieIsShowing.setHasFixedSize(true)
        binding.rcvListMovieIsShowing.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
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
                    Toast.makeText(this@CinemaDetailActivity, "Error checking favorite: ${error.message}", Toast.LENGTH_SHORT).show()
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