package com.example.kotlin_customer_nom_movie_ticket.ui.view.activity

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.kotlin_customer_nom_movie_ticket.databinding.ActivitySplashBinding
import com.example.kotlin_customer_nom_movie_ticket.helper.ThemePreferences
import com.example.kotlin_customer_nom_movie_ticket.ui.view.fragment.ProfileFragment

class SplashActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySplashBinding
    private lateinit var sharedPreferences: SharedPreferences
    companion object {
        private const val INTRO_DISPLAY_TIME = 1000
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (ThemePreferences.isDarkMode(this)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.lottieAnimationView.playAnimation()

        Handler(Looper.getMainLooper()).postDelayed({
            startNextActivity()
        }, INTRO_DISPLAY_TIME.toLong())
    }
    private fun startNextActivity() {
        val intent = Intent(this, IntroActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.lottieAnimationView.cancelAnimation()
    }

}