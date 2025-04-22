package com.example.kotlin_customer_nom_movie_ticket.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.kotlin_customer_nom_movie_ticket.data.firebase.FirebaseHelper
import com.example.kotlin_customer_nom_movie_ticket.data.model.Customer
import com.example.kotlin_customer_nom_movie_ticket.data.repository.RegisterRepository
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class RegisterViewModel : ViewModel() {
    private var registerRepository = RegisterRepository()
    private val storageReference: StorageReference = FirebaseStorage.getInstance().reference
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading
    private val _imageUrl = MutableLiveData<String>()
    val imageUrl: LiveData<String> get() = _imageUrl
    private val _userSaved = MutableLiveData<Boolean>()
    val userSaved: LiveData<Boolean> get() = _userSaved

    fun saveUserData(context: Context, fullName: String, imageUrl: String, phoneNumber: String, email: String, fcmToken: String) {
        val userId = registerRepository.currentUserId()
        val user = Customer(userId, fullName, phoneNumber, email, imageUrl, fcmToken)

        registerRepository.getAllUserId { userIds ->
            if (userId != null && !userIds.contains(userId)) {
                registerRepository.addUser(user)
                saveUserIdToSharedPreferences(context, userId)
                _userSaved.value = true
            } else {
                _userSaved.value = false
            }
        }
    }

    private fun saveUserIdToSharedPreferences(context: Context, userId: String) {
        val sharedPreferences = context.getSharedPreferences("loginSaved", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("user_id", userId)
        editor.putBoolean("isLogin", true)
        editor.apply()
    }

    fun uploadImageToFirebaseStorage(uriAvatar: Uri) {
        _isLoading.value = true
        val imageReference = storageReference.child("avatar/${System.currentTimeMillis()}.jpg")
        imageReference.putFile(uriAvatar)
            .addOnSuccessListener {
                imageReference.downloadUrl.addOnSuccessListener { downloadUrl ->
                    _imageUrl.value = downloadUrl.toString()
                    _isLoading.value = false
                }
            }
            .addOnFailureListener { exception ->
                Log.e("ProfileAccountViewModel", "Image upload failed", exception)
                _isLoading.value = false
            }
    }
}