package com.example.kotlin_customer_nom_movie_ticket.ui.view.activity

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.example.chatapp.ViewModel.VerifyViewModel
import com.example.kotlin_customer_nom_movie_ticket.R
import com.example.kotlin_customer_nom_movie_ticket.databinding.ActivityVerifyBinding

class VerifyActivity : AppCompatActivity() {
    private lateinit var binding: ActivityVerifyBinding
    private lateinit var verifyViewModel: VerifyViewModel
    private var phoneNumber: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVerifyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        verifyViewModel = VerifyViewModel()

        binding.progressBar.visibility = View.GONE
        binding.btnVertify.visibility = View.VISIBLE

        phoneNumber = intent.getStringExtra("phone_number").toString()
        if (phoneNumber.isNotEmpty()) {
            binding.tvPhoneNumber.text = phoneNumber
            Log.d("OTPActivity", "Phone number: $phoneNumber")
        } else {
            Log.d("OTPActivity", "Phone number is null or empty")
            Toast.makeText(this, "Phone number is not provided", Toast.LENGTH_SHORT).show()
            return
        }

        verifyViewModel.sendOtp(this, phoneNumber, false)

        setupTextWatchers()

        binding.btnVertify.setOnClickListener {
            val otp = getOtpFromEditTexts()
            if (otp.isEmpty()) {
                Toast.makeText(this@VerifyActivity, "OTP cannot be empty", Toast.LENGTH_SHORT).show()
            } else {
                binding.btnVertify.visibility = View.GONE
                binding.progressBar.visibility = View.VISIBLE
                verifyViewModel.verifyOtp(this, otp)
            }
        }

        binding.btnBack.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }

        verifyViewModel.isLoading.observe(this, Observer { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnVertify.visibility = if (isLoading) View.GONE else View.VISIBLE
        })

        verifyViewModel.verificationStatus.observe(this, Observer { isVerified ->
            if (isVerified) {
                val intent = Intent(this, MainActivity::class.java).apply {
                    putExtra("customer_id", verifyViewModel.verificationCode.value)
                }
                startActivity(intent)
                finish()
            } else {
                val intent = Intent(this, RegisterActivity::class.java).apply {
                    putExtra("customer_id", verifyViewModel.verificationCode.value)
                    putExtra("phone_number", phoneNumber)
                }
                startActivity(intent)
                finish()
                Toast.makeText(this, "OTP verification successful", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupTextWatchers() {
        val editTexts = listOf(binding.edText1, binding.edText2, binding.edText3, binding.edText4, binding.edText5, binding.edText6)
        for (i in 0 until editTexts.size - 1) {
            editTexts[i].addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    if (s?.length == 1) {
                        editTexts[i + 1].requestFocus()
                    }
                }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })
        }
    }

    private fun getOtpFromEditTexts(): String {
        return binding.edText1.text.toString() + binding.edText2.text.toString() + binding.edText3.text.toString() +
                binding.edText4.text.toString() + binding.edText5.text.toString() + binding.edText6.text.toString()
    }
}