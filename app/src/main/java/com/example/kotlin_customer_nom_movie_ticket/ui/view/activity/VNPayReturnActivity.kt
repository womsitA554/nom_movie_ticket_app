package com.example.kotlin_customer_nom_movie_ticket.ui.view.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.kotlin_customer_nom_movie_ticket.service.vnpay.VNPayUtils

class VNPayReturnActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("VNPayReturn", "VNPayReturnActivity onCreate")

        // Xử lý return URL
        intent.data?.let { uri ->
            val returnUrl = uri.toString()
            Log.d("VNPayReturn", "Processing return URL: $returnUrl")

            try {
                val (isValid, responseCode) = VNPayUtils.verifyReturnUrl(returnUrl)
                Log.d("VNPayReturn", "URL validation result - isValid: $isValid, responseCode: $responseCode")

                // Determine which activity to return to based on the URL path
                val returnActivity = when {
                    returnUrl.contains("/food/return") -> {
                        Log.d("VNPayReturn", "Detected food payment return")
                        FoodPaymentDetailActivity::class.java
                    }
                    returnUrl.contains("/ticket/return") -> {
                        Log.d("VNPayReturn", "Detected ticket payment return")
                        PaymentDetailActivity::class.java
                    }
                    else -> {
                        Log.e("VNPayReturn", "Unknown return URL path: $returnUrl")
                        null
                    }
                }

                returnActivity?.let { activityClass ->
                    Log.d("VNPayReturn", "Starting return activity: ${activityClass.simpleName}")
                    
                    // Get stored data from SharedPreferences
                    val sharedPref = getSharedPreferences("vnpay_orders", Context.MODE_PRIVATE)
                    
                    val returnIntent = Intent(this, activityClass).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        putExtra("vnpay_return", true)
                        putExtra("vnpay_valid", isValid)
                        putExtra("vnpay_response_code", responseCode)
                        putExtra("vnpay_return_url", returnUrl)
                        
                        // Add all necessary data from SharedPreferences
                        putExtra("cinema_id", sharedPref.getString("current_cinema_id", ""))
                        putExtra("cinema_name", sharedPref.getString("current_cinema_name", ""))
                        putExtra("showtime_id", sharedPref.getString("current_showtime_id", ""))
                        putExtra("showtime_time", sharedPref.getString("current_showtime_time", ""))
                        putExtra("room_id", sharedPref.getString("current_room_id", ""))
                        putExtra("movie_id", sharedPref.getString("current_movie_id", ""))
                        putExtra("seat_name", sharedPref.getString("current_seat_name", ""))
                        putExtra("seat_price", sharedPref.getString("current_seat_price", ""))
                        putExtra("time_left", sharedPref.getLong("current_time_left", 0L))
                        
                        // Add movie information
                        putExtra("title", sharedPref.getString("current_movie_title", ""))
                        putExtra("poster_url", sharedPref.getString("current_movie_poster_url", ""))
                        putExtra("country", sharedPref.getString("current_movie_country", ""))
                        putExtra("release_year", sharedPref.getString("current_movie_release_year", ""))
                        putExtra("language", sharedPref.getString("current_movie_language", ""))
                        putExtra("duration", sharedPref.getInt("current_movie_duration", 0))
                        putExtra("genre", sharedPref.getString("current_movie_genre", ""))
                        putExtra("synopsis", sharedPref.getString("current_movie_synopsis", ""))
                        putExtra("director_id", sharedPref.getString("current_movie_director_id", ""))
                        putExtra("status", sharedPref.getString("current_movie_status", ""))
                        putExtra("trailer_url", sharedPref.getString("current_movie_trailer_url", ""))
                        putExtra("age_rating", sharedPref.getString("current_movie_age_rating", ""))
                        putExtra("rating", sharedPref.getFloat("current_movie_rating", 0f))
                        putExtra("banner", sharedPref.getString("current_movie_banner", ""))
                        
                        // Add ArrayList data
                        putStringArrayListExtra("ticket_ids", ArrayList(sharedPref.getStringSet("current_ticket_ids", emptySet()) ?: emptySet()))
                        putStringArrayListExtra("selected_seat_ids", ArrayList(sharedPref.getStringSet("current_selected_seat_ids", emptySet()) ?: emptySet()))
                        putStringArrayListExtra("actor_ids", ArrayList(sharedPref.getStringSet("current_actor_ids", emptySet()) ?: emptySet()))
                        
                        // Add price data
                        putExtra("total_price_seats", sharedPref.getFloat("current_total_price_seats", 0f).toDouble())
                        putExtra("total_price_food", sharedPref.getFloat("current_total_price_food", 0f).toDouble())

                        // Add food cart information if it exists
                        val cartSize = sharedPref.getInt("current_cart_size", 0)
                        if (cartSize > 0) {
                            putExtra("has_cart", true)
                            putExtra("cart_size", cartSize)
                            
                            // Add each cart item
                            for (i in 0 until cartSize) {
                                putExtra("cart_item_id_$i", sharedPref.getString("current_cart_item_id_$i", ""))
                                putExtra("cart_item_name_$i", sharedPref.getString("current_cart_item_name_$i", ""))
                                putExtra("cart_item_image_$i", sharedPref.getString("current_cart_item_image_$i", ""))
                                putExtra("cart_item_price_$i", sharedPref.getFloat("current_cart_item_price_$i", 0f).toDouble())
                                putExtra("cart_item_quantity_$i", sharedPref.getInt("current_cart_item_quantity_$i", 1))
                            }
                        } else {
                            putExtra("has_cart", false)
                        }
                    }
                    startActivity(returnIntent)
                } ?: run {
                    Log.e("VNPayReturn", "No valid return activity found for URL: $returnUrl")
                    handleError("Invalid return URL path")
                }
            } catch (e: Exception) {
                Log.e("VNPayReturn", "Error processing return URL: ${e.message}", e)
                handleError("Error processing payment return: ${e.message}")
            }
            finish() // Đóng VNPayReturnActivity
        } ?: run {
            Log.e("VNPayReturn", "No return URL found in intent")
            handleError("No return URL found")
        }
    }

    private fun handleError(errorMessage: String) {
        val mainIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("vnpay_error", errorMessage)
        }
        startActivity(mainIntent)
        finish()
    }
}