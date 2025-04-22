package com.example.kotlin_customer_nom_movie_ticket.data.repository

import com.example.kotlin_customer_nom_movie_ticket.data.model.Showtime
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ShowtimeRepository {
    private val dbShowtime = FirebaseDatabase.getInstance().getReference("Showtimes")
    private val dbRoom = FirebaseDatabase.getInstance().getReference("Rooms")

    fun getShowtimeByMovieId(movieId: String, callback: (List<Showtime>) -> Unit) {
        dbShowtime.orderByChild("movie_id").equalTo(movieId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val showtimes = mutableListOf<Showtime>()
                    for (data in snapshot.children) {
                        val showtime = data.getValue(Showtime::class.java)
                        showtime?.let { showtimes.add(it) }
                    }
                    callback(showtimes)
                }

                override fun onCancelled(error: DatabaseError) {
                    callback(emptyList())
                }
            })
    }

    fun getShowtimesByMovieCinemaAndDate(
        movieId: String,
        cinemaId: String,
        date: LocalDate,
        callback: (List<Showtime>) -> Unit
    ) {
        dbRoom.orderByChild("cinema_id").equalTo(cinemaId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(roomsSnapshot: DataSnapshot) {
                    val roomIds = mutableListOf<String>()
                    for (roomData in roomsSnapshot.children) {
                        val roomId = roomData.key
                        roomId?.let { roomIds.add(it) }
                    }

                    if (roomIds.isEmpty()) {
                        callback(emptyList())
                        return
                    }

                    dbShowtime.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(showtimesSnapshot: DataSnapshot) {
                            val showtimes = mutableListOf<Showtime>()
                            val formatter = DateTimeFormatter.ISO_LOCAL_DATE // "2025-03-25"

                            for (data in showtimesSnapshot.children) {
                                val showtime = data.getValue(Showtime::class.java)?.apply { showtime_id = data.key!! }
                                showtime?.let {
                                    if (it.movie_id == movieId && it.room_id in roomIds) {
                                        // Trích xuất phần ngày từ "2025-03-25T18:00:00"
                                        val showtimeDate = LocalDate.parse(it.showtime_time.split("T")[0], formatter)
                                        if (showtimeDate == date) {
                                            showtimes.add(it)
                                        }
                                    }
                                }
                            }
                            callback(showtimes)
                        }

                        override fun onCancelled(error: DatabaseError) {
                            callback(emptyList())
                        }
                    })
                }

                override fun onCancelled(error: DatabaseError) {
                    callback(emptyList())
                }
            })
    }
}