package com.example.kotlin_customer_nom_movie_ticket.ui.view.activity

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kotlin_customer_nom_movie_ticket.R
import com.example.kotlin_customer_nom_movie_ticket.databinding.ActivityNotificationBinding
import com.example.kotlin_customer_nom_movie_ticket.ui.adapter.NotificationAdapter
import com.example.kotlin_customer_nom_movie_ticket.util.SessionManager
import com.example.kotlin_customer_nom_movie_ticket.viewmodel.NotificationViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class NotificationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNotificationBinding
    private lateinit var notificationAdapter: NotificationAdapter
    private lateinit var notificationViewModel: NotificationViewModel
    private lateinit var userId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userId = SessionManager.getUserId(this) ?: run {
            finish()
            return
        }

        notificationViewModel = NotificationViewModel()
        setupRecyclerView()
        setupObservers()
        binding.btnBack.setOnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        notificationViewModel.fetchAllNotification(userId)
    }

    private fun setupRecyclerView() {
        binding.rcvNotification.setHasFixedSize(true)
        binding.rcvNotification.layoutManager = LinearLayoutManager(this)
    }

    private fun setupObservers() {
        notificationViewModel.notification.observe(this) { notificationList ->
            if (notificationList.isNotEmpty()) {
                notificationAdapter = NotificationAdapter(notificationList.sortedByDescending { it.timestamp })
                binding.rcvNotification.adapter = notificationAdapter
                binding.rcvNotification.visibility = View.VISIBLE
                binding.tvEmpty.visibility = View.GONE
            } else {
                binding.rcvNotification.visibility = View.GONE
                binding.tvEmpty.visibility = View.VISIBLE
                binding.tvEmpty.text = "Không có thông báo nào"
            }
        }

        // Observe loading state
        notificationViewModel.isLoading.observe(this) { isLoading ->
            if (isLoading) {
                binding.progressBar.visibility = View.VISIBLE
                binding.progressBar.playAnimation()
                binding.rcvNotification.visibility = View.GONE
                binding.tvEmpty.visibility = View.GONE
            } else {
                binding.progressBar.visibility = View.GONE
                binding.progressBar.cancelAnimation()
            }
        }

        // Observe errors
        notificationViewModel.error.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                binding.rcvNotification.visibility = View.GONE
                binding.tvEmpty.visibility = View.VISIBLE
                binding.tvEmpty.text = "Lỗi khi tải thông báo"
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.progressBar.cancelAnimation()
    }
}