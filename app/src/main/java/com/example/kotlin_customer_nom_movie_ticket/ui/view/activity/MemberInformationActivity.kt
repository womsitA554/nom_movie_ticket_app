package com.example.kotlin_customer_nom_movie_ticket.ui.view.activity

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kotlin_customer_nom_movie_ticket.R
import com.example.kotlin_customer_nom_movie_ticket.databinding.ActivityMemberInformationBinding
import com.example.kotlin_customer_nom_movie_ticket.ui.adapter.PointHistoryAdapter
import com.example.kotlin_customer_nom_movie_ticket.util.SessionManager
import com.example.kotlin_customer_nom_movie_ticket.viewmodel.PointHistoryViewModel
import com.example.kotlin_customer_nom_movie_ticket.data.model.PointHistory

class MemberInformationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMemberInformationBinding
    private lateinit var pointHistoryViewModel: PointHistoryViewModel
    private lateinit var pointHistoryAdapter: PointHistoryAdapter
    private val listPointHistory = mutableListOf<PointHistory>()
    private var customerId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMemberInformationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        customerId = SessionManager.getUserId(this) ?: run {
            finish()
            return
        }

        val point = intent.getStringExtra("point")
        binding.tvPoint.text = point

        pointHistoryViewModel = PointHistoryViewModel()
        setupRecyclerView()
        setupObservers()

        binding.btnBack.setOnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        // Start fetching point history
        pointHistoryViewModel.fetchPointHistoryByCustomerId(customerId)
    }

    private fun setupRecyclerView() {
        binding.rcvPointHistory.setHasFixedSize(true)
        binding.rcvPointHistory.layoutManager = LinearLayoutManager(this)
    }

    private fun setupObservers() {
        // Observe point history
        pointHistoryViewModel.pointHistory.observe(this) { pointHistoryList ->
            listPointHistory.clear()
            listPointHistory.addAll(pointHistoryList)
            pointHistoryAdapter = PointHistoryAdapter(listPointHistory)
            binding.rcvPointHistory.adapter = pointHistoryAdapter
            if (pointHistoryList.isNotEmpty()) {
                binding.rcvPointHistory.visibility = View.VISIBLE
                binding.tvEmpty.visibility = View.GONE
            } else {
                binding.rcvPointHistory.visibility = View.GONE
                binding.tvEmpty.visibility = View.VISIBLE
                binding.tvEmpty.text = "Không có lịch sử điểm"
            }
        }

        // Observe loading state
        pointHistoryViewModel.isLoading.observe(this) { isLoading ->
            if (isLoading) {
                binding.progressBar.visibility = View.VISIBLE
                binding.progressBar.playAnimation()
                binding.rcvPointHistory.visibility = View.GONE
                binding.tvEmpty.visibility = View.GONE
            } else {
                binding.progressBar.visibility = View.GONE
                binding.progressBar.cancelAnimation()
            }
        }

        // Observe errors
        pointHistoryViewModel.error.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                binding.rcvPointHistory.visibility = View.GONE
                binding.tvEmpty.visibility = View.VISIBLE
                binding.tvEmpty.text = "Lỗi khi tải lịch sử điểm"
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.progressBar.cancelAnimation()
    }
}