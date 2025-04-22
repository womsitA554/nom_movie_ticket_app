package com.example.kotlin_customer_nom_movie_ticket

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.example.kotlin_customer_nom_movie_ticket.util.SessionManager
import com.google.firebase.FirebaseApp

class  MyApplication : Application() {
    private var userId: String = ""

    override fun onCreate() {
        super.onCreate()

        userId = SessionManager.getUserId(this).toString()
        FirebaseApp.initializeApp(this)

        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "ticket_channel"
            val channelName = "Thông báo đặt vé"
            val descriptionText = "Thông báo về trạng thái đặt vé phim"
            val importance = NotificationManager.IMPORTANCE_HIGH

            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = descriptionText
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}