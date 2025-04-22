package com.example.kotlin_customer_nom_movie_ticket.util

import android.content.Context

object SessionManager {
    private const val PREF_NAME = "loginSaved"
    private const val KEY_IS_LOGIN = "isLogin"
    private const val KEY_USER_ID = "customer_id"

    fun saveLoginStatus(context: Context, userId: String) {
        val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit()
            .putBoolean(KEY_IS_LOGIN, true)
            .putString(KEY_USER_ID, userId)
            .apply()
    }

    fun getUserId(context: Context): String? {
        val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getString(KEY_USER_ID, null)
    }

    fun isLoggedIn(context: Context): Boolean {
        val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean(KEY_IS_LOGIN, false)
    }

    fun logout(context: Context) {
        val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit()
            .clear()
            .apply()
    }
}