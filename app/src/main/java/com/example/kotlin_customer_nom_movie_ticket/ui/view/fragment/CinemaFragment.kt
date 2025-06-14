package com.example.kotlin_customer_nom_movie_ticket.ui.view.fragment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kotlin_customer_nom_movie_ticket.R
import com.example.kotlin_customer_nom_movie_ticket.databinding.FragmentCinemaBinding
import com.example.kotlin_customer_nom_movie_ticket.ui.adapter.CinemaAdapter
import com.example.kotlin_customer_nom_movie_ticket.ui.view.activity.CinemaDetailActivity
import com.example.kotlin_customer_nom_movie_ticket.ui.view.activity.CinemaDetailFromBookNowActivity
import com.example.kotlin_customer_nom_movie_ticket.util.SessionManager
import com.example.kotlin_customer_nom_movie_ticket.viewmodel.CinemaViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CinemaFragment : Fragment() {
    private var _binding: FragmentCinemaBinding? = null
    private val binding get() = _binding!!

    private lateinit var cinemaViewModel: CinemaViewModel
    private lateinit var cinemaAdapter: CinemaAdapter
    private var isCinemasLoaded = false
    private var isFavoriteCinemasLoaded = false
    private lateinit var userId: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCinemaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.progressBar.visibility = View.VISIBLE
        binding.progressBar.playAnimation()
        binding.rcvAllCinema.visibility = View.GONE
        binding.tvChooseArena.visibility = View.GONE

        userId = SessionManager.getUserId(requireContext()).toString()
        val movieId = arguments?.getString("movie_id")
        val movieCountry = arguments?.getString("country")
        val moviePosterUrl = arguments?.getString("poster_url")
        val movieTitle = arguments?.getString("title")
        val movieReleaseYear = arguments?.getString("release_year")
        val movieLanguage = arguments?.getString("language")
        val movieDuration = arguments?.getInt("duration", 0)
        val movieGenre = arguments?.getString("genre")
        val movieSynopsis = arguments?.getString("synopsis")
        val movieDirectorId = arguments?.getString("director_id")
        val movieStatus = arguments?.getString("status")
        val movieTrailerUrl = arguments?.getString("trailer_url")
        val movieActorIds = arguments?.getStringArrayList("actor_ids")?.toList()
        val movieAgeRating = arguments?.getString("age_rating")
        val movieRating = arguments?.getFloat("rating", 0f)
        val movieBanner = arguments?.getString("banner")

        setupRecycleView()
        cinemaViewModel = CinemaViewModel()

        cinemaAdapter = CinemaAdapter(cinemaViewModel, cinemaViewModel.favoriteCinemas.value ?: emptyList(), userId)
        binding.rcvAllCinema.adapter = cinemaAdapter

        cinemaViewModel.fetchCinemas()
        cinemaViewModel.fetchFavoriteCinemas(userId)

        setupObservers(
            movieId, movieCountry, moviePosterUrl, movieTitle, movieReleaseYear,
            movieLanguage, movieDuration, movieGenre, movieSynopsis, movieDirectorId,
            movieStatus, movieTrailerUrl, movieActorIds, movieAgeRating, movieRating, movieBanner
        )
    }

    private fun setupObservers(
        movieId: String?, movieCountry: String?, moviePosterUrl: String?, movieTitle: String?,
        movieReleaseYear: String?, movieLanguage: String?, movieDuration: Int?, movieGenre: String?,
        movieSynopsis: String?, movieDirectorId: String?, movieStatus: String?,
        movieTrailerUrl: String?, movieActorIds: List<String>?, movieAgeRating: String?,
        movieRating: Float?, movieBanner: String?
    ) {
        cinemaViewModel.cinemas.observe(viewLifecycleOwner) { cinemas ->
            isCinemasLoaded = true
            if (cinemas.isEmpty()) {
                Toast.makeText(requireContext(), "No cinemas found", Toast.LENGTH_SHORT).show()
            }
            setupCinemaAdapter(cinemas, movieId, movieCountry, moviePosterUrl, movieTitle,
                movieReleaseYear, movieLanguage, movieDuration, movieGenre, movieSynopsis,
                movieDirectorId, movieStatus, movieTrailerUrl, movieActorIds, movieAgeRating,
                movieRating, movieBanner)
            checkAllDataLoaded()
        }

        cinemaViewModel.favoriteCinemas.observe(viewLifecycleOwner) { favoriteCinemas ->
            isFavoriteCinemasLoaded = true
            if (favoriteCinemas.isEmpty()) {
                Log.d("CinemaFragment", "No favorite cinemas found")
            }
            // Cập nhật favoriteCinemas trong adapter
            cinemaAdapter = CinemaAdapter(cinemaViewModel, favoriteCinemas, userId)
            binding.rcvAllCinema.adapter = cinemaAdapter
            setupCinemaAdapter(cinemaViewModel.cinemas.value ?: emptyList(), movieId, movieCountry,
                moviePosterUrl, movieTitle, movieReleaseYear, movieLanguage, movieDuration,
                movieGenre, movieSynopsis, movieDirectorId, movieStatus, movieTrailerUrl,
                movieActorIds, movieAgeRating, movieRating, movieBanner)
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
                val intent = Intent(requireContext(), CinemaDetailActivity::class.java)
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
                activity?.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            } else {
                val intent = Intent(requireContext(), CinemaDetailFromBookNowActivity::class.java)
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
                activity?.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }
        }
    }

    private fun checkAllDataLoaded() {
        if (isCinemasLoaded && isFavoriteCinemasLoaded) {
            stopAnimation()
            binding.rcvAllCinema.visibility = View.VISIBLE
            binding.tvChooseArena.visibility = View.VISIBLE
            Log.d("CinemaFragment", "All data loaded, showing rcvAllCinema")
        }
    }

    private fun stopAnimation() {
        binding.progressBar.cancelAnimation()
        binding.progressBar.visibility = View.GONE
    }

    private fun setupRecycleView() {
        binding.rcvAllCinema.setHasFixedSize(true)
        binding.rcvAllCinema.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopAnimation()
        _binding = null
    }
}