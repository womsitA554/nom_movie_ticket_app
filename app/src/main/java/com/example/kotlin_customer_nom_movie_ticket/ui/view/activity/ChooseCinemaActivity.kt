package com.example.kotlin_customer_nom_movie_ticket.ui.view.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kotlin_customer_nom_movie_ticket.R
import com.example.kotlin_customer_nom_movie_ticket.databinding.ActivityChooseCinemaBinding
import com.example.kotlin_customer_nom_movie_ticket.ui.adapter.CinemaAdapter
import com.example.kotlin_customer_nom_movie_ticket.ui.view.activity.CinemaDetailActivity
import com.example.kotlin_customer_nom_movie_ticket.ui.view.activity.CinemaDetailFromBookNowActivity
import com.example.kotlin_customer_nom_movie_ticket.util.SessionManager
import com.example.kotlin_customer_nom_movie_ticket.viewmodel.CinemaViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ChooseCinemaActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChooseCinemaBinding
    private lateinit var cinemaViewModel: CinemaViewModel
    private lateinit var cinemaAdapter: CinemaAdapter
    private var isCinemasLoaded = false
    private var isFavoriteCinemasLoaded = false
    private lateinit var userId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChooseCinemaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.progressBar.visibility = View.VISIBLE
        binding.progressBar.playAnimation()
        binding.rcvAllCinema.visibility = View.GONE
        binding.tvChooseArena.visibility = View.GONE

        userId = SessionManager.getUserId(this).toString()
        val intent = intent
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

        setupRecycleView()
        cinemaViewModel = CinemaViewModel()

        cinemaAdapter = CinemaAdapter(cinemaViewModel, emptyList(), userId)
        binding.rcvAllCinema.adapter = cinemaAdapter

        cinemaViewModel.fetchCinemas()
        cinemaViewModel.fetchFavoriteCinemas(userId)

        setupObservers(
            movieId, movieCountry, moviePosterUrl, movieTitle, movieReleaseYear,
            movieLanguage, movieDuration, movieGenre, movieSynopsis, movieDirectorId,
            movieStatus, movieTrailerUrl, movieActorIds, movieAgeRating, movieRating, movieBanner
        )

        lifecycleScope.launch {
            delay(10000)
            if (!isCinemasLoaded || !isFavoriteCinemasLoaded) {
                if (!isCinemasLoaded) {
                    Toast.makeText(this@ChooseCinemaActivity, "Timeout loading cinemas", Toast.LENGTH_SHORT).show()
                    isCinemasLoaded = true
                }
                if (!isFavoriteCinemasLoaded) {
                    Toast.makeText(this@ChooseCinemaActivity, "Timeout loading favorite cinemas", Toast.LENGTH_SHORT).show()
                    isFavoriteCinemasLoaded = true
                }
                checkAllDataLoaded()
            }
        }

        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun setupObservers(
        movieId: String?, movieCountry: String?, moviePosterUrl: String?, movieTitle: String?,
        movieReleaseYear: String?, movieLanguage: String?, movieDuration: Int?, movieGenre: String?,
        movieSynopsis: String?, movieDirectorId: String?, movieStatus: String?,
        movieTrailerUrl: String?, movieActorIds: List<String>?, movieAgeRating: String?,
        movieRating: Float?, movieBanner: String?
    ) {
        cinemaViewModel.cinemas.observe(this) { cinemas ->
            isCinemasLoaded = true
            if (cinemas.isEmpty()) {
                Toast.makeText(this, "No cinemas found", Toast.LENGTH_SHORT).show()
            }
            setupCinemaAdapter(
                cinemas, movieId, movieCountry, moviePosterUrl, movieTitle,
                movieReleaseYear, movieLanguage, movieDuration, movieGenre, movieSynopsis,
                movieDirectorId, movieStatus, movieTrailerUrl, movieActorIds, movieAgeRating,
                movieRating, movieBanner
            )
            checkAllDataLoaded()
        }

        cinemaViewModel.favoriteCinemas.observe(this) { favoriteCinemas ->
            isFavoriteCinemasLoaded = true
            if (favoriteCinemas.isEmpty()) {
                Log.d("ChooseCinemaActivity", "No favorite cinemas found")
            }
            cinemaAdapter = CinemaAdapter(cinemaViewModel, favoriteCinemas, userId)
            binding.rcvAllCinema.adapter = cinemaAdapter
            setupCinemaAdapter(
                cinemaViewModel.cinemas.value ?: emptyList(), movieId, movieCountry,
                moviePosterUrl, movieTitle, movieReleaseYear, movieLanguage, movieDuration,
                movieGenre, movieSynopsis, movieDirectorId, movieStatus, movieTrailerUrl,
                movieActorIds, movieAgeRating, movieRating, movieBanner
            )
            checkAllDataLoaded()
        }
    }

    private fun setupCinemaAdapter(
        cinemas: List<com.example.kotlin_customer_nom_movie_ticket.data.model.Cinema>,
        movieId: String?, movieCountry: String?, moviePosterUrl: String?, movieTitle: String?,
        movieReleaseYear: String?, movieLanguage: String?, movieDuration: Int?, movieGenre: String?,
        movieSynopsis: String?, movieDirectorId: String?, movieStatus: String?,
        movieTrailerUrl: String?, movieActorIds: List<String>?, movieAgeRating: String?,
        movieRating: Float?, movieBanner: String?
    ) {
        cinemaAdapter.updateData(cinemas)

        cinemaAdapter.onClickItem = { cinema, _ ->
            if (movieId == null) {
                val intent = Intent(this, CinemaDetailActivity::class.java)
                intent.putExtra("customer_id", userId)
                intent.putExtra("cinema_id", cinema.cinema_id)
                intent.putExtra("cinema_name", cinema.cinema_name)
                intent.putExtra("address", cinema.address)
                intent.putExtra("latitude", cinema.latitude)
                intent.putExtra("longitude", cinema.longitude)
                intent.putExtra("phone_number", cinema.phone_number)
                intent.putExtra("created_at", cinema.created_at)
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
                startActivity(intent)
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            } else {
                val intent = Intent(this, CinemaDetailFromBookNowActivity::class.java)
                intent.putExtra("movie_id", movieId)
                intent.putExtra("customer_id", userId)
                intent.putExtra("cinema_id", cinema.cinema_id)
                intent.putExtra("cinema_name", cinema.cinema_name)
                intent.putExtra("address", cinema.address)
                intent.putExtra("latitude", cinema.latitude)
                intent.putExtra("longitude", cinema.longitude)
                intent.putExtra("phone_number", cinema.phone_number)
                intent.putExtra("created_at", cinema.created_at)
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
                startActivity(intent)
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }
        }
    }

    private fun checkAllDataLoaded() {
        if (isCinemasLoaded && isFavoriteCinemasLoaded) {
            stopAnimation()
            binding.rcvAllCinema.visibility = View.VISIBLE
            binding.tvChooseArena.visibility = View.VISIBLE
            Log.d("ChooseCinemaActivity", "All data loaded, showing rcvAllCinema")
        }
    }

    private fun stopAnimation() {
        binding.progressBar.cancelAnimation()
        binding.progressBar.visibility = View.GONE
    }

    private fun setupRecycleView() {
        binding.rcvAllCinema.setHasFixedSize(false)
        binding.rcvAllCinema.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        binding.rcvAllCinema.isNestedScrollingEnabled = true
    }
}