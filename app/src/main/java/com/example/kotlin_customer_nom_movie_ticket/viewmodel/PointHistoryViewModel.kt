package com.example.kotlin_customer_nom_movie_ticket.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.kotlin_customer_nom_movie_ticket.data.model.PointHistory
import com.example.kotlin_customer_nom_movie_ticket.data.repository.PointHistoryRepository

class PointHistoryViewModel : ViewModel() {
    private val pointHistoryRepository = PointHistoryRepository()
    private val _pointHistory = MutableLiveData<List<PointHistory>>()
    val pointHistory: MutableLiveData<List<PointHistory>> get() = _pointHistory

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: MutableLiveData<Boolean> get() = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: MutableLiveData<String?> get() = _error

    fun fetchPointHistoryByCustomerId(customerId: String) {
        _isLoading.value = true
        _error.value = null
        pointHistoryRepository.getPointHistoryByCustomerId(customerId) { pointHistoryList, error ->
            _isLoading.value = false
            if (error != null) {
                _error.value = error
                _pointHistory.value = emptyList()
            } else {
                _pointHistory.value = pointHistoryList
            }
        }
    }
}