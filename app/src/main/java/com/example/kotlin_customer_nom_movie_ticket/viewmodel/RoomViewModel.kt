package com.example.kotlin_customer_nom_movie_ticket.viewmodel

import androidx.lifecycle.ViewModel
import com.example.kotlin_customer_nom_movie_ticket.data.repository.RoomRepository

class RoomViewModel : ViewModel() {
    private val roomRepository = RoomRepository()

    fun fetchCinemaIdByRoomId(roomId: String, callback: (String?) -> Unit) {
        roomRepository.getCinemaIdByRoomId(roomId) { cinemaId ->
            callback(cinemaId)
        }
    }
}