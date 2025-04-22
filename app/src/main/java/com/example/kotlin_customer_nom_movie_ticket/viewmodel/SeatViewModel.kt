package com.example.kotlin_customer_nom_movie_ticket.viewmodel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.kotlin_customer_nom_movie_ticket.data.model.Seat
import com.example.kotlin_customer_nom_movie_ticket.data.repository.SeatRepository

class SeatViewModel : ViewModel() {
    private val seatRepository = SeatRepository()
    private val _seats = MutableLiveData<List<Seat>>()
    val seats: MutableLiveData<List<Seat>> = _seats
    private var userIds: Map<String, String?> = emptyMap()

    fun fetchSeat(roomId: String, showtimeId: String?) {
        seatRepository.getSeatByRoomId(roomId, showtimeId) { seatList, userIdMap ->
            _seats.value = seatList
            userIds = userIdMap
        }
    }

    fun getSeatBookingUserId(seatId: String): String? = userIds[seatId]

    fun updateSeatStatus(showtimeId: String, seatId: String, status: String, userId: String, callback: (Boolean) -> Unit) {
        seatRepository.updateSeatStatus(showtimeId, seatId, status, userId) { success ->
            if (success) {
                _seats.value?.let { currentSeats ->
                    val updatedSeats = currentSeats.map {
                        if (it.seat_id == seatId) it.copy(status = status) else it
                    }
                    _seats.postValue(updatedSeats)
                    val mutableUserIds = userIds.toMutableMap()
                    if (status == "reserved") {
                        mutableUserIds[seatId] = userId
                    } else {
                        mutableUserIds.remove(seatId)
                    }
                    userIds = mutableUserIds
                }
            }
            callback(success)
        }
    }

    fun updateSeatBookedStatus(
        showtimeId: String,
        seatId: String,
        status: String,
        userId: String,
        callback: (Boolean) -> Unit
    ) {
        seatRepository.updateSeatBookedStatus(showtimeId, seatId, status, userId) { success ->
            if (success) {
                Log.d("UpdateSeatBookedStatus", "Seat $seatId updated to $status for showtime $showtimeId")
            } else {
                Log.e("UpdateSeatBookedStatus", "Failed to update seat $seatId to $status")
            }
            callback(success)
        }
    }

    fun resetSeats(showtimeId: String, userId: String, callback: (Boolean) -> Unit) {
        seatRepository.resetSeatsByUserId(showtimeId, userId, callback)
    }

    fun stopListening(showtimeId: String?) {
        seatRepository.removeSeatListener(showtimeId)
    }

    override fun onCleared() {
        super.onCleared()
        stopListening(null)
    }
}