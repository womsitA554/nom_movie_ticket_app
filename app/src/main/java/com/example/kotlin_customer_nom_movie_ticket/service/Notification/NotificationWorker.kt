package com.example.kotlin_customer_nom_movie_ticket.service.Notification

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class NotificationWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val title = inputData.getString("title") ?: "Movie Reminder"
        val message = inputData.getString("message") ?: "Your movie starts soon!"
        val token = inputData.getString("token") ?: return Result.failure()

        NotificationUtils.sendNotificationWithFCMv1(applicationContext, token, title, message)
        return Result.success()
    }
}