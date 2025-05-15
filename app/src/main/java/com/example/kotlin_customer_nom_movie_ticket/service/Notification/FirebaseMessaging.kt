package com.example.kotlin_customer_nom_movie_ticket.service.Notification

import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.kotlin_customer_nom_movie_ticket.R
import com.example.kotlin_customer_nom_movie_ticket.ui.view.activity.NowPlayingDetailActivity
import com.example.kotlin_customer_nom_movie_ticket.ui.view.activity.ViewTicketActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d("FCM", "onMessageReceived started")
        if (remoteMessage.data.isNotEmpty()) {
            val title = remoteMessage.data["title"]
            val message = remoteMessage.data["message"]
            val cinemaName = remoteMessage.data["cinema_name"]
            val showtimeTime = remoteMessage.data["showtime_time"]
            val movieTitle = remoteMessage.data["movie_title"]
            val movieDuration = remoteMessage.data["duration"]?.toIntOrNull() ?: 0
            val movieAgeRating = remoteMessage.data["age_rating"]
            val seatName = remoteMessage.data["seat_name"]
            val billId = remoteMessage.data["bill_id"]
            val movieId = remoteMessage.data["movie_id"]

            Log.d("FCM", "Data extracted: title=$title, message=$message")

            // Determine notification type based on title or other data
            val isRatingNotification = title?.contains("Đánh Giá Thành Công") == true

            showNotification(
                title,
                message,
                cinemaName,
                showtimeTime,
                movieTitle,
                movieDuration,
                movieAgeRating,
                seatName,
                billId,
                movieId,
                isRatingNotification
            )
        } else if (remoteMessage.notification != null) {
            val title = remoteMessage.notification?.title
            val message = remoteMessage.notification?.body
            Log.d("FCM", "Notification extracted: title=$title, message=$message")
            showNotification(title, message, null, null, null, 0, null, null, null, null, false)
        } else {
            Log.d("MyFirebaseMessagingService", "No data or notification payload")
        }
        Log.d("FCM", "onMessageReceived finished")
    }

    private fun showNotification(
        title: String?,
        message: String?,
        cinemaName: String?,
        showtimeTime: String?,
        movieTitle: String?,
        movieDuration: Int,
        movieAgeRating: String?,
        seatName: String?,
        billId: String?,
        movieId: String?,
        isRatingNotification: Boolean
    ) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "ticket_channel",
                "Ticket Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        // Choose the target activity based on notification type
        val intent = if (isRatingNotification) {
            Intent(this, NowPlayingDetailActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("movie_id", movieId)
                putExtra("title", movieTitle)
            }
        } else {
            Intent(this, ViewTicketActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("cinema_name", cinemaName)
                putExtra("showtime_time", showtimeTime)
                putExtra("title", movieTitle)
                putExtra("duration", movieDuration)
                putExtra("age_rating", movieAgeRating)
                putExtra("seat_name", seatName)
                putExtra("bill_id", billId)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        Thread {
            val notification = NotificationCompat.Builder(this, "ticket_channel")
                .setContentTitle(title)
                .setContentText(message)
                .setColor(getColor(R.color.orange))
                .setSmallIcon(R.drawable.bell_icon)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()
            notificationManager.notify(System.currentTimeMillis().toInt(), notification)
        }.start()
    }

    private fun isAppInForeground(): Boolean {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningAppProcesses = activityManager.runningAppProcesses ?: return false
        for (processInfo in runningAppProcesses) {
            if (processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND && processInfo.processName == packageName) {
                return true
            }
        }
        return false
    }
}