package com.example.kotlin_customer_nom_movie_ticket.data.repository

import android.util.Log
import com.example.kotlin_customer_nom_movie_ticket.data.model.PointHistory
import com.google.firebase.database.FirebaseDatabase

class PointHistoryRepository {
    private val dbPointHistory = FirebaseDatabase.getInstance().getReference("PointTransactions")

    fun getPointHistoryByCustomerId(customerId: String, callback: (List<PointHistory>, String?) -> Unit) {
        dbPointHistory.orderByChild("customer_id").equalTo(customerId)
            .get()
            .addOnSuccessListener { snapshot ->
                val pointHistoryList = mutableListOf<PointHistory>()
                for (childSnapshot in snapshot.children) {
                    val pointTransaction = childSnapshot.getValue(PointHistory::class.java)
                    pointTransaction?.let { pointHistoryList.add(it) }
                }
                Log.d("PointHistoryRepository", "Fetched point history: $pointHistoryList")
                callback(pointHistoryList, null)
            }
            .addOnFailureListener { exception ->
                Log.e("PointHistoryRepository", "Error getting point history", exception)
                callback(emptyList(), exception.message ?: "Lỗi khi tải lịch sử điểm")
            }
            .addOnCanceledListener {
                Log.d("PointHistoryRepository", "Fetch point history canceled")
                callback(emptyList(), "Hủy tải lịch sử điểm")
            }
    }
}