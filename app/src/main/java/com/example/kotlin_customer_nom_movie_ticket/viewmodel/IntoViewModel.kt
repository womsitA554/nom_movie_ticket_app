package com.example.kotlin_customer_nom_movie_ticket.viewmodel

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class IntroViewModel : ViewModel() {
    private lateinit var sharedPreferences: SharedPreferences
    private val _isLogin = MutableLiveData<Boolean>()
    val isLogin: LiveData<Boolean> get() = _isLogin
    private val _userId = MutableLiveData<String>()
    val userId: LiveData<String> get() = _userId

    fun checkLoginStatus(context : Context){
        sharedPreferences = context.getSharedPreferences("loginSaved", Context.MODE_PRIVATE)
        _isLogin.value = sharedPreferences.getBoolean("isLogin", false)
        _userId.value = sharedPreferences.getString("customer_id", null)
    }
}