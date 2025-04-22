package com.example.kotlin_customer_nom_movie_ticket.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.kotlin_customer_nom_movie_ticket.data.model.Actor
import com.example.kotlin_customer_nom_movie_ticket.data.repository.MovieShowingDetailRepository

class ActorViewModel : ViewModel() {
    private val movieShowingDetailRepository = MovieShowingDetailRepository()
    val actors = MutableLiveData<List<Actor>>()

    fun fetchActorsByIds(actorIds: List<String>) {
        movieShowingDetailRepository.getActorsByIds(actorIds) { actorsList ->
            actors.postValue(actorsList)
        }
    }


}