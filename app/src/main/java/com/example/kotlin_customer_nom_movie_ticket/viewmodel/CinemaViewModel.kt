package com.example.kotlin_customer_nom_movie_ticket.viewmodel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.kotlin_customer_nom_movie_ticket.data.model.Cinema
import com.example.kotlin_customer_nom_movie_ticket.data.model.Showtime
import com.example.kotlin_customer_nom_movie_ticket.data.repository.CinemaRepository
import com.example.kotlin_customer_nom_movie_ticket.data.repository.ShowtimeRepository
import com.example.kotlin_customer_nom_movie_ticket.util.LocationUtil
import com.example.kotlin_customer_nom_movie_ticket.util.UserLocation
import java.time.LocalDate

class CinemaViewModel : ViewModel() {
    private val cinemaRepository = CinemaRepository()
    private val showtimeRepository = ShowtimeRepository()
    private val _cinemas = MutableLiveData<List<Cinema>>()
    val cinemas: MutableLiveData<List<Cinema>> = _cinemas
    private val _cinemaName = MutableLiveData<String>()
    val cinemaName: MutableLiveData<String> = _cinemaName
    private val _showtimes = MutableLiveData<List<Showtime>>()
    val showtimes: MutableLiveData<List<Showtime>> = _showtimes
    private val _favoriteCinemas = MutableLiveData<List<String>>()
    val favoriteCinemas: MutableLiveData<List<String>> = _favoriteCinemas
    private val _favoriteCinemaList = MutableLiveData<List<Cinema>>()
    val favoriteCinemaList: MutableLiveData<List<Cinema>> = _favoriteCinemaList
    private val _suggestedCinemas = MutableLiveData<List<Cinema>>()
    val suggestedCinemas: MutableLiveData<List<Cinema>> = _suggestedCinemas

    fun fetchCinemas() {
        cinemaRepository.getAllCinemas { cinemaList ->
            _cinemas.value = cinemaList
        }
    }

    fun fetchFavoriteCinemas(userId: String) {
        cinemaRepository.getFavoriteCinemas(userId) { favorites ->
            _favoriteCinemas.value = favorites
            updateFavoriteCinemaList(favorites)
        }
    }

    private fun updateFavoriteCinemaList(favoriteCinemaIds: List<String>) {
        cinemaRepository.getAllCinemas { cinemaList ->
            val favoriteList = cinemaList.filter { favoriteCinemaIds.contains(it.cinema_id) }
            _favoriteCinemaList.value = favoriteList
        }
    }

    fun toggleFavoriteCinema(userId: String, cinemaId: String, isFavorite: Boolean, callback: (Boolean) -> Unit) {
        cinemaRepository.toggleFavoriteCinema(userId, cinemaId, isFavorite) { success ->
            if (success) {
                fetchFavoriteCinemas(userId)
            }
            callback(success)
        }
    }

    fun fetchAllShowtimesByCinema(cinemaId: String) {
        cinemaRepository.getAllShowtimesByCinema(cinemaId) { showtimes ->
            _showtimes.value = showtimes
        }
    }

    fun fetchShowtimesByFilmAndCinema(filmId: String, cinemaId: String, date: LocalDate) {
        showtimeRepository.getShowtimesByMovieCinemaAndDate(filmId, cinemaId, date) { showtimes ->
            _showtimes.value = showtimes
        }
    }

    fun fetchCinemaNameById(cinemaId: String) {
        cinemaRepository.getCinemaNameById(cinemaId) { name ->
            _cinemaName.value = name
        }
    }

    fun fetchSuggestedCinemas(userLocation: UserLocation?, maxSuggestions: Int = 3) {
        cinemaRepository.getAllCinemas { cinemaList ->
            if (userLocation == null || cinemaList.isEmpty()) {
                _suggestedCinemas.value = emptyList()
                return@getAllCinemas
            }
            val sortedCinemas = cinemaList
                .map { cinema ->
                    val distance = LocationUtil.calculateDistance(
                        userLocation.latitude, userLocation.longitude,
                        cinema.latitude, cinema.longitude
                    )
                    cinema to distance
                }
                .sortedBy { it.second }
                .take(maxSuggestions)
                .map { it.first }
            _suggestedCinemas.value = sortedCinemas
        }
    }
}