package com.example.kotlin_customer_nom_movie_ticket.data.repository

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class RoomRepository {
    private val dbRooms = FirebaseDatabase.getInstance().getReference("Rooms")

    fun getCinemaIdByRoomId(roomId: String, callback: (String?) -> Unit) {
        dbRooms.child(roomId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val cinemaId = snapshot.child("cinema_id").getValue(String::class.java)
                callback(cinemaId)
            }

            override fun onCancelled(error: DatabaseError) {
                callback(null)
            }
        })
    }
}