package com.example.kotlin_customer_nom_movie_ticket.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.kotlin_customer_nom_movie_ticket.data.repository.MovieShowingDetailRepository

class DirectorViewModel: ViewModel() {
    private val movieShowingDetailRepository = MovieShowingDetailRepository()
    private val _directorName = MutableLiveData<String>()
    var directorName: MutableLiveData<String> = _directorName

    fun fetchDirectorNameById(directorId: String) {
        movieShowingDetailRepository.getDirectorNameById(directorId) { directorName ->
            _directorName.value = directorName
        }
    }

}