package com.example.kotlin_customer_nom_movie_ticket.viewmodel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.example.kotlin_customer_nom_movie_ticket.data.model.PointHistory
import com.example.kotlin_customer_nom_movie_ticket.data.repository.PointHistoryRepository

class PointHistoryViewModel {
    private val pointHistoryRepository = PointHistoryRepository()
    private val _pointHistory = MutableLiveData<List<PointHistory>>()
    val pointHistory: MutableLiveData<List<PointHistory>> get() = _pointHistory

    fun fetchPointHistoryByCustomerId(customerId: String) {
        pointHistoryRepository.getPointHistoryByCustomerId(customerId) { pointHistory ->
            _pointHistory.value = pointHistory
            Log.d("PointHistoryViewModel", "Fetched point history: $pointHistory")
        }
    }
}