package com.example.kotlin_customer_nom_movie_ticket.service.Notification

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.kotlin_customer_nom_movie_ticket.R
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class NotificationWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val title = inputData.getString("title") ?: "Movie Reminder"
        val message = inputData.getString("message") ?: "Your movie starts soon!"
        val token = inputData.getString("token") ?: return Result.failure()
        val movieId = inputData.getString("movie_id")
        val billId = inputData.getString("bill_id")
        val isShowtime = inputData.getBoolean("isShowtime", false)
        val isReview = inputData.getBoolean("isReview", false)

        // Store notification in Firebase
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            if (isShowtime){
                val database = FirebaseDatabase.getInstance().reference
                val notificationId = database.child("Notifications").child(userId).push().key
                if (notificationId != null) {
                    val notificationData = mutableMapOf(
                        "notification_id" to notificationId,
                        "title" to title,
                        "message" to message,
                        "timestamp" to System.currentTimeMillis(),
                        "type" to "showtime",
                    )
                    if (movieId != null) {
                        notificationData["movie_id"] = movieId
                    }
                    if (billId != null && isShowtime) {
                        notificationData["bill_id"] = billId
                    }
                    database.child("Notifications").child(userId).child(notificationId)
                        .setValue(notificationData)
                        .addOnSuccessListener {
                            Log.d("NotificationWorker", "Notification stored in Firebase: $notificationId")
                        }
                        .addOnFailureListener { e ->
                            Log.e("NotificationWorker", "Failed to store notification: ${e.message}")
                        }
                }
            } else {
                val database = FirebaseDatabase.getInstance().reference
                val notificationId = database.child("Notifications").child(userId).push().key
                if (notificationId != null) {
                    val notificationData = mutableMapOf(
                        "notification_id" to notificationId,
                        "title" to title,
                        "message" to message,
                        "timestamp" to System.currentTimeMillis(),
                        "type" to "review",
                    )
                    if (movieId != null) {
                        notificationData["movie_id"] = movieId
                    }
                    if (billId != null && isShowtime) {
                        notificationData["bill_id"] = billId
                    }
                    database.child("Notifications").child(userId).child(notificationId)
                        .setValue(notificationData)
                        .addOnSuccessListener {
                            Log.d("NotificationWorker", "Notification stored in Firebase: $notificationId")
                        }
                        .addOnFailureListener { e ->
                            Log.e("NotificationWorker", "Failed to store notification: ${e.message}")
                        }
                }
            }
        } else {
            Log.e("NotificationWorker", "User not logged in, cannot store notification")
        }

        sendNotificationWithFCMv1(applicationContext, token, title, message)

        return Result.success()
    }

    private fun sendNotificationWithFCMv1(context: Context, recipientToken: String, senderName: String, messageText: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val projectId = "shoponline-f6905"

            val googleCredentials = try {
                val inputStream = context.assets.open("service-account.json")
                GoogleCredentials.fromStream(inputStream)
                    .createScoped(listOf("https://www.googleapis.com/auth/firebase.messaging"))
            } catch (e: IOException) {
                Log.e("NotificationWorker", "Error reading service-account.json", e)
                return@launch
            }

            try {
                googleCredentials.refreshIfExpired()
            } catch (e: IOException) {
                Log.e("NotificationWorker", "Error refreshing Google credentials", e)
                return@launch
            }

            val accessToken = googleCredentials.accessToken.tokenValue

            val notificationJson = JSONObject().apply {
                put("title", senderName)
                put("body", messageText)
            }

            val dataJson = JSONObject().apply {
                put("title", senderName)
                put("message", messageText)
            }

            val messageJson = JSONObject().apply {
                put("token", recipientToken)
                put("notification", notificationJson)
                put("data", dataJson)
            }

            val requestBodyJson = JSONObject().apply {
                put("message", messageJson)
            }

            val client = OkHttpClient()
            val mediaType = "application/json; UTF-8".toMediaTypeOrNull()
            val requestBody = requestBodyJson.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url("https://fcm.googleapis.com/v1/projects/$projectId/messages:send")
                .addHeader("Authorization", "Bearer $accessToken")
                .addHeader("Content-Type", "application/json; UTF-8")
                .post(requestBody)
                .build()

            try {
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    Log.d("NotificationWorker", "Notification sent successfully!")
                } else {
                    Log.e("NotificationWorker", "Error sending notification: ${response.message}")
                }
            } catch (e: IOException) {
                Log.e("NotificationWorker", "Error executing FCM request", e)
            }
        }
    }
}