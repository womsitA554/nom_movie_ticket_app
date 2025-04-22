package com.example.kotlin_customer_nom_movie_ticket.ui.view.fragment

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kotlin_customer_nom_movie_ticket.databinding.FragmentFavoritesCinemaBinding
import com.example.kotlin_customer_nom_movie_ticket.ui.adapter.CinemaAdapter
import com.example.kotlin_customer_nom_movie_ticket.ui.view.activity.CinemaDetailActivity
import com.example.kotlin_customer_nom_movie_ticket.ui.view.activity.CinemaDetailFromBookNowActivity
import com.example.kotlin_customer_nom_movie_ticket.util.SessionManager
import com.example.kotlin_customer_nom_movie_ticket.viewmodel.CinemaViewModel

class FavoritesCinemaFragment : Fragment() {
    private var _binding: FragmentFavoritesCinemaBinding? = null
    private val binding get() = _binding!!
    private lateinit var cinemaViewModel: CinemaViewModel
    private lateinit var cinemaAdapter: CinemaAdapter
    private lateinit var userId: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFavoritesCinemaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userId = SessionManager.getUserId(requireContext()) ?: run {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            requireActivity().finish()
            return
        }

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
        cinemaViewModel.fetchCinemas()
        cinemaViewModel.fetchFavoriteCinemas(userId)

        // Observe favoriteCinemaList to display favorite cinemas
        cinemaViewModel.favoriteCinemaList.observe(viewLifecycleOwner) { favoriteCinemas ->
            cinemaViewModel.favoriteCinemas.observe(viewLifecycleOwner) { favoriteCinemaIds ->
                cinemaAdapter = CinemaAdapter(favoriteCinemas, favoriteCinemaIds, cinemaViewModel, userId)
                binding.rcvFavoritesCinema.adapter = cinemaAdapter
                binding.rcvFavoritesCinema.visibility = if (favoriteCinemas.isNotEmpty()) View.VISIBLE else View.GONE
                binding.progressBar.visibility = View.GONE

                Log.d("FavoritesCinemaFragment", "Favorite Cinemas: $favoriteCinemas")
                Log.d("FavoritesCinemaFragment", "Favorite Cinema IDs: $favoriteCinemaIds")

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
                    }
                }
            }
        }
    }

    private fun setupRecycleView() {
        binding.rcvFavoritesCinema.setHasFixedSize(true)
        binding.rcvFavoritesCinema.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}