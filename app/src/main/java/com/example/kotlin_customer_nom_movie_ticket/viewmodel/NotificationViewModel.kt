package com.example.kotlin_customer_nom_movie_ticket.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.kotlin_customer_nom_movie_ticket.data.model.Notification
import com.example.kotlin_customer_nom_movie_ticket.data.repository.NotificationRepository

class NotificationViewModel : ViewModel() {
    private val notificationRepository = NotificationRepository()
    private val _notification = MutableLiveData<List<Notification>>()
    val notification: MutableLiveData<List<Notification>> get() = _notification

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: MutableLiveData<Boolean> get() = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: MutableLiveData<String?> get() = _error

    fun fetchAllNotification(userId: String) {
        _isLoading.value = true
        _error.value = null
        notificationRepository.getAllNotification(userId) { notificationList, error ->
            _isLoading.value = false
            if (error != null) {
                _error.value = error
                _notification.value = emptyList()
            } else {
                _notification.value = notificationList
            }
        }
    }
}