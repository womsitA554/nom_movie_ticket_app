package com.example.kotlin_customer_nom_movie_ticket.viewmodel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.kotlin_customer_nom_movie_ticket.data.model.Cinema
import com.example.kotlin_customer_nom_movie_ticket.data.model.Showtime
import com.example.kotlin_customer_nom_movie_ticket.data.repository.CinemaRepository
import com.example.kotlin_customer_nom_movie_ticket.data.repository.ShowtimeRepository
import java.time.LocalDate

class CinemaViewModel : ViewModel() {
    private val cinemaRepository = CinemaRepository()
    private val showtimeRepository = ShowtimeRepository()
    private val _cinemas = MutableLiveData<List<Cinema>>()
    var cinemas: MutableLiveData<List<Cinema>> = _cinemas
    private val _cinemaName = MutableLiveData<String>()
    var cinemaName: MutableLiveData<String> = _cinemaName
    private val _showtimes = MutableLiveData<List<Showtime>>()
    val showtimes: MutableLiveData<List<Showtime>> = _showtimes
    private val _favoriteCinemas = MutableLiveData<List<String>>()
    val favoriteCinemas: MutableLiveData<List<String>> = _favoriteCinemas
    private val _favoriteCinemaList = MutableLiveData<List<Cinema>>()
    val favoriteCinemaList: MutableLiveData<List<Cinema>> = _favoriteCinemaList

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
            Log.d("CinemaViewModel", "All Cinemas: $cinemaList")
            Log.d("CinemaViewModel", "Favorite Cinema IDs: $favoriteCinemaIds")
            Log.d("CinemaViewModel", "Filtered Favorite Cinemas: $favoriteList")
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
}