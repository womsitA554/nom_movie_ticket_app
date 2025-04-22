package com.example.kotlin_customer_nom_movie_ticket.ui.view.activity

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.example.kotlin_customer_nom_movie_ticket.databinding.ActivityRegisterBinding
import com.example.kotlin_customer_nom_movie_ticket.viewmodel.RegisterViewModel

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private lateinit var registerViewModel: RegisterViewModel
    private var sharedPreferences: SharedPreferences? = null
    private var selectedImageUri: Uri? = null
    private val defaultImageUrl = "https://firebasestorage.googleapis.com/v0/b/nomchat-3b6f3.appspot.com/o/avatar%2Fperson_default_icon.jpg?alt=media&token=15e352e8-ce4a-4555-a3e5-0afb285eded1"
    private val IMAGE_PICKER_REQUEST_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        registerViewModel = RegisterViewModel()
        sharedPreferences = getSharedPreferences("loginSaved", MODE_PRIVATE)

        binding.btnStart.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        val phoneNumber = intent.getStringExtra("phone_number")

        binding.btnBack.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }

        binding.btnStart.visibility = View.VISIBLE
        binding.progressBar.visibility = View.GONE

        binding.btnStart.setOnClickListener {
            val firstName = binding.etFirstName.text.toString().trim()
            val lastName = binding.etLastName.text.toString().trim()
            val fullName = "$firstName $lastName"

            if (firstName.isNotEmpty() && lastName.isNotEmpty()) {
                if (selectedImageUri != null) {
                    registerViewModel.uploadImageToFirebaseStorage(selectedImageUri!!)
                } else {
                    registerViewModel.saveUserData(this, fullName, defaultImageUrl, phoneNumber.toString(), email = "", fcmToken = "")
                }
                binding.btnStart.visibility = View.GONE
                binding.progressBar.visibility = View.VISIBLE
            } else {
                Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnAddImage.setOnClickListener {
            openImagePicker()
        }

        registerViewModel.imageUrl.observe(this, Observer { imageUrl ->
            registerViewModel.saveUserData(this, binding.etFirstName.text.toString().trim() + " " + binding.etLastName.text.toString().trim(), imageUrl, phoneNumber.toString(), email = "", fcmToken = "")
        })

        registerViewModel.isLoading.observe(this, Observer { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnStart.visibility = if (isLoading) View.GONE else View.VISIBLE
        })

        registerViewModel.userSaved.observe(this, Observer { userSaved ->
            if (userSaved) {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "This phone number is already in use", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, IMAGE_PICKER_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == IMAGE_PICKER_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            selectedImageUri = data?.data
            binding.imgAvatar.setImageURI(selectedImageUri)
        }
    }
}