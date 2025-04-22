package com.example.kotlin_customer_nom_movie_ticket.ui.view.activity

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kotlin_customer_nom_movie_ticket.R
import com.example.kotlin_customer_nom_movie_ticket.data.model.Cart
import com.example.kotlin_customer_nom_movie_ticket.data.model.PointHistory
import com.example.kotlin_customer_nom_movie_ticket.databinding.ActivityMemberInformationBinding
import com.example.kotlin_customer_nom_movie_ticket.ui.adapter.PointHistoryAdapter
import com.example.kotlin_customer_nom_movie_ticket.util.SessionManager
import com.example.kotlin_customer_nom_movie_ticket.viewmodel.PointHistoryViewModel

class MemberInformationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMemberInformationBinding
    private lateinit var pointHistoryViewModel: PointHistoryViewModel
    private lateinit var pointHistoryAdapter: PointHistoryAdapter
    private var listPointHistory = mutableListOf<PointHistory>()

    private var customerId: String = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMemberInformationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val point = intent.getStringExtra("point")
        binding.tvPoint.text = point

        customerId = SessionManager.getUserId(this).toString()
        pointHistoryViewModel = PointHistoryViewModel()

        setupRecycleView()

        pointHistoryViewModel.fetchPointHistoryByCustomerId(customerId)
        pointHistoryViewModel.pointHistory.observe(this) { pointHistoryList ->
            listPointHistory.clear()
            listPointHistory.addAll(pointHistoryList)
            pointHistoryAdapter = PointHistoryAdapter(listPointHistory)
            binding.rcvPointHistory.adapter = pointHistoryAdapter

            Log.d("PointHistory", "Fetched point history: $listPointHistory")
        }

        binding.btnBack.setOnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }


    }

    private fun setupRecycleView() {
        binding.rcvPointHistory.layoutManager = LinearLayoutManager(this)

    }
}