package com.example.kotlin_customer_nom_movie_ticket.service.Notification

import android.content.Context
import android.util.Log
import com.google.auth.oauth2.GoogleCredentials
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

object NotificationUtils {
    fun sendNotificationWithFCMv1(context: Context, recipientToken: String, senderName: String, messageText: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val projectId = "shoponline-f6905"

            val googleCredentials = try {
                val inputStream = context.assets.open("service-account.json")
                GoogleCredentials.fromStream(inputStream)
                    .createScoped(listOf("https://www.googleapis.com/auth/firebase.messaging"))
            } catch (e: IOException) {
                Log.e("NotificationUtils", "Error reading service-account.json", e)
                return@launch
            }

            try {
                googleCredentials.refreshIfExpired()
            } catch (e: IOException) {
                Log.e("NotificationUtils", "Error refreshing Google credentials", e)
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
                    Log.d("NotificationUtils", "Notification sent successfully!")
                } else {
                    Log.e("NotificationUtils", "Error sending notification: ${response.message}")
                }
            } catch (e: IOException) {
                Log.e("NotificationUtils", "Error executing FCM request", e)
            }
        }
    }
}