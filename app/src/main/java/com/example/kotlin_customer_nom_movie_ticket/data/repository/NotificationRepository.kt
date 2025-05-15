package com.example.kotlin_customer_nom_movie_ticket.data.repository

import com.example.kotlin_customer_nom_movie_ticket.data.model.Notification
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class NotificationRepository {
    private val dbNotification = FirebaseDatabase.getInstance().getReference("Notifications")

    fun getAllNotification(userId: String, callback: (List<Notification>, String?) -> Unit) {
        dbNotification.child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val notifications = mutableListOf<Notification>()
                    for (data in snapshot.children) {
                        val notification = data.getValue(Notification::class.java)
                        notification?.let { notifications.add(it) }
                    }
                    callback(notifications, null)
                }

                override fun onCancelled(error: DatabaseError) {
                    callback(emptyList(), error.message)
                }
            })
    }

}