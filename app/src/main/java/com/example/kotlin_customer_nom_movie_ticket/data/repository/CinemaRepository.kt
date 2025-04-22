package com.example.kotlin_customer_nom_movie_ticket.data.repository

import com.example.kotlin_customer_nom_movie_ticket.data.model.Cinema
import com.example.kotlin_customer_nom_movie_ticket.data.model.Showtime
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class CinemaRepository {
    private val dbCinema = FirebaseDatabase.getInstance().getReference("Cinemas")
    private val dbShowtimes = FirebaseDatabase.getInstance().getReference("Showtimes")
    private val dbFavoriteCinemas = FirebaseDatabase.getInstance().getReference("FavoriteCinemas")

    fun getAllCinemas(callback: (List<Cinema>) -> Unit) {
        dbCinema.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val cinemas = mutableListOf<Cinema>()
                for (data in snapshot.children) {
                    val cinema = data.getValue(Cinema::class.java)
                    cinema?.let { cinemas.add(it) }
                }
                callback(cinemas)
            }

            override fun onCancelled(error: DatabaseError) {
                callback(emptyList())
            }
        })
    }

    fun getAllShowtimesByCinema(cinemaId: String, callback: (List<Showtime>) -> Unit) {
        dbShowtimes.orderByChild("cinema_id").equalTo(cinemaId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val showtimes = mutableListOf<Showtime>()
                    for (data in snapshot.children) {
                        val showtime = data.getValue(Showtime::class.java)?.apply { showtime_id = data.key!! }
                        showtime?.let { showtimes.add(it) }
                    }
                    callback(showtimes)
                }

                override fun onCancelled(error: DatabaseError) {
                    callback(emptyList())
                }
            })
    }

    fun getShowtimesByFilmAndCinema(filmId: String, cinemaId: String, callback: (List<Showtime>) -> Unit) {
        dbShowtimes.orderByChild("cinema_id").equalTo(cinemaId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val showtimes = mutableListOf<Showtime>()
                    for (data in snapshot.children) {
                        val showtime = data.getValue(Showtime::class.java)?.apply { showtime_id = data.key!! }
                        showtime?.let {
                            if (it.movie_id == filmId) showtimes.add(it)
                        }
                    }
                    callback(showtimes)
                }

                override fun onCancelled(error: DatabaseError) {
                    callback(emptyList())
                }
            })
    }

    fun getCinemaNameById(cinemaId: String, callback: (String) -> Unit) {
        dbCinema.child(cinemaId).get().addOnSuccessListener {
            callback(it.child("cinema_name").value.toString())
        }.addOnFailureListener {
            callback("Unknown")
        }
    }

    fun getFavoriteCinemas(userId: String, callback: (List<String>) -> Unit) {
        dbFavoriteCinemas.child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val favoriteCinemas = snapshot.children
                    .filter { it.getValue(Boolean::class.java) == true }
                    .mapNotNull { it.key }
                callback(favoriteCinemas)
            }

            override fun onCancelled(error: DatabaseError) {
                callback(emptyList())
            }
        })
    }

    // Thêm hàm để thêm/xóa rạp yêu thích
    fun toggleFavoriteCinema(userId: String, cinemaId: String, isFavorite: Boolean, callback: (Boolean) -> Unit) {
        if (isFavorite) {
            // Xóa khỏi danh sách yêu thích
            dbFavoriteCinemas.child(userId).child(cinemaId).removeValue()
                .addOnSuccessListener { callback(true) }
                .addOnFailureListener { callback(false) }
        } else {
            // Thêm vào danh sách yêu thích
            dbFavoriteCinemas.child(userId).child(cinemaId).setValue(true)
                .addOnSuccessListener { callback(true) }
                .addOnFailureListener { callback(false) }
        }
    }
}