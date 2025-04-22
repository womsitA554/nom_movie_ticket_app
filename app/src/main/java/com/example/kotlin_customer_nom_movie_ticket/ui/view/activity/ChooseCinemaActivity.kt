package com.example.kotlin_customer_nom_movie_ticket.ui.view.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.kotlin_customer_nom_movie_ticket.R
import com.example.kotlin_customer_nom_movie_ticket.databinding.ActivityChooseCinemaBinding
import com.example.kotlin_customer_nom_movie_ticket.ui.view.fragment.AllCinemaFragment
import com.example.kotlin_customer_nom_movie_ticket.ui.view.fragment.FavoritesCinemaFragment
import com.example.kotlin_customer_nom_movie_ticket.util.SessionManager

class ChooseCinemaActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChooseCinemaBinding
    private var allCinemaFragment = AllCinemaFragment()
    private var favoritesCinemaFragment = FavoritesCinemaFragment()
    private var activeFragment: Fragment? = null
    private lateinit var userId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChooseCinemaBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

        if(savedInstanceState == null){
            initializeFragments(movieId, moviePosterUrl, movieTitle, movieReleaseYear,movieLanguage, movieCountry, movieDuration, movieGenre, movieSynopsis, movieDirectorId, movieStatus, movieTrailerUrl, movieActorIds, movieAgeRating, movieRating, movieBanner)
            showFragment(allCinemaFragment)
            updateColor(1)
        }

        binding.btnAllCinema.setOnClickListener {
            showFragment(allCinemaFragment)
            updateColor(1)
        }

        binding.btnFavoriteCinema.setOnClickListener {
            showFragment(favoritesCinemaFragment)
            updateColor(2)
        }

        binding.btnBack.setOnClickListener {
            onBackPressed()
        }
    }

    private fun initializeFragments(movieId: String?,moviePosterUrl: String?, movieTitle: String?,movieLanguage: String?, movieCountry: String?,  movieReleaseYear: String?, movieDuration: Int?, movieGenre: String?, movieSynopsis: String?, movieDirectorId: String?, movieStatus: String?, movieTrailerUrl: String?, movieActorIds: List<String>?, movieAgeRating: String?, movieRating: Float?, movieBanner: String?) {
        val fragmentTransaction = supportFragmentManager.beginTransaction()

        val allCinemaBundle = Bundle().apply {
            putString("movie_id", movieId)
            putString("poster_url", moviePosterUrl)
            putString("title", movieTitle)
            putString("release_year", movieReleaseYear)
            putString("language", movieLanguage)
            putInt("duration", movieDuration!!)
            putString("genre", movieGenre)
            putString("synopsis", movieSynopsis)
            putString("director_id", movieDirectorId)
            putString("status", movieStatus)
            putString("trailer_url", movieTrailerUrl)
            putStringArrayList("actor_ids", movieActorIds?.let { ArrayList(it) })
            putString("age_rating", movieAgeRating)
            putFloat("rating", movieRating!!)
            putString("banner", movieBanner)
            putString("country", movieCountry)
        }
        allCinemaFragment.arguments = allCinemaBundle

        val favoritesCinemaBundle = Bundle().apply {
            putString("movie_id", movieId)
        }
        favoritesCinemaFragment.arguments = favoritesCinemaBundle

        if (supportFragmentManager.findFragmentByTag("ALLCINEMA") == null) {
            fragmentTransaction.add(R.id.fragment_container_cinema, allCinemaFragment, "ALLCINEMA").hide(allCinemaFragment)
        }
        if (supportFragmentManager.findFragmentByTag("FAVORITECINEMA") == null) {
            fragmentTransaction.add(R.id.fragment_container_cinema, favoritesCinemaFragment, "FAVORITECINEMA").hide(favoritesCinemaFragment)
        }

        fragmentTransaction.commit()
    }

    private fun showFragment(fragment: Fragment) {
        val fragmentTransaction = supportFragmentManager.beginTransaction()

        if (activeFragment != null && activeFragment != fragment) {
            fragmentTransaction.hide(activeFragment!!)
        }

        fragmentTransaction.show(fragment)

        activeFragment = fragment
        fragmentTransaction.commit()
    }

    private fun updateColor(selected : Int){
        binding.btnAllCinema.setTextColor(if (selected == 1) ContextCompat.getColor(this, R.color.orange) else ContextCompat.getColor(this, R.color.grey))
        binding.btnAllCinema.setBackgroundResource(if (selected == 1) R.drawable.my_ticket_orange_background else R.drawable.my_ticket_grey_background)

        binding.btnFavoriteCinema.setTextColor(if (selected == 2) ContextCompat.getColor(this, R.color.orange) else ContextCompat.getColor(this, R.color.grey))
        binding.btnFavoriteCinema.setBackgroundResource(if (selected == 2) R.drawable.my_ticket_orange_background else R.drawable.my_ticket_grey_background)
    }
}