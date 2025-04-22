package com.example.kotlin_customer_nom_movie_ticket.ui.view.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.example.kotlin_customer_nom_movie_ticket.databinding.ActivityLoginBinding
import com.example.kotlin_customer_nom_movie_ticket.viewmodel.LoginViewModel

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var loginViewModel: LoginViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loginViewModel = LoginViewModel()

        binding.progressBar.visibility = View.GONE
        binding.btnStart.visibility = View.VISIBLE

        binding.countryCodePicker.registerCarrierNumberEditText(binding.etPhoneNumber)

        binding.btnStart.setOnClickListener {
            val isValid = binding.countryCodePicker.isValidFullNumber
            loginViewModel.validatePhoneNumber(binding.countryCodePicker.fullNumberWithPlus, isValid)
            if (!isValid) {
                binding.etPhoneNumber.error = "Phone number not valid"
                return@setOnClickListener
            }
            loginViewModel.startLoading()
            Handler().postDelayed({
                val intent = Intent(this@LoginActivity, VerifyActivity::class.java)
                intent.putExtra("phone_number", binding.countryCodePicker.fullNumberWithPlus)
                startActivity(intent)
                finish()
                hideKeyboard()
                Log.d("LoginActivity", "Phone number: ${binding.countryCodePicker.fullNumberWithPlus}")
            }, 2000)
        }

//        binding.btnBack.setOnClickListener {
//            val intent = Intent(this, IntroActivity::class.java)
//            startActivity(intent)
//            finish()
//        }

        loginViewModel.isLoading.observe(this, Observer { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnStart.visibility = if (isLoading) View.GONE else View.VISIBLE
        })
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.root.windowToken, 0)
    }
}