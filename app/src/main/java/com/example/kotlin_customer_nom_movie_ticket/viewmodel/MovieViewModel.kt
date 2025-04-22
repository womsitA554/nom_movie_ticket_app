package com.example.kotlin_customer_nom_movie_ticket.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.kotlin_customer_nom_movie_ticket.data.model.Customer
import com.example.kotlin_customer_nom_movie_ticket.data.model.Movie
import com.example.kotlin_customer_nom_movie_ticket.data.repository.HomeRepository

class MovieViewModel : ViewModel() {
    private val homeRepository = HomeRepository()
    private val _movieIsShowing = MutableLiveData<List<Movie>>()
    private val _movieIsSComing = MutableLiveData<List<Movie>>()
    val movieIsShowing: MutableLiveData<List<Movie>> get() = _movieIsShowing
    val movieIsComing: MutableLiveData<List<Movie>> get() = _movieIsSComing
    private val _avatarCustomer = MutableLiveData<Customer>()
    val avatarCustomer: MutableLiveData<Customer> get() = _avatarCustomer
    private val _favoriteStatus = MutableLiveData<Map<String, Boolean>>()
    val favoriteStatus: LiveData<Map<String, Boolean>> get() = _favoriteStatus

    fun fetchMoviesIsShowing(){
        homeRepository.getMovieIsShowing { movieList ->
            _movieIsShowing.value = movieList
        }
    }

    fun fetchMoviesIsComing(){
        homeRepository.getMovieIsComing { movieList ->
            _movieIsSComing.value = movieList
        }
    }

    fun fetchAvatarCustomer(userId: String){
        homeRepository.getAvatarCustomer(userId) { avatar ->
            _avatarCustomer.value = avatar
        }
    }

    fun updateFavoriteStatus(movieId: String, isFavorite: Boolean) {
        val currentMap = _favoriteStatus.value?.toMutableMap() ?: mutableMapOf()
        currentMap[movieId] = isFavorite
        _favoriteStatus.value = currentMap
    }
}