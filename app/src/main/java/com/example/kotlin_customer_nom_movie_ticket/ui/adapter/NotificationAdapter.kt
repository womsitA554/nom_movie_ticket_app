package com.example.kotlin_customer_nom_movie_ticket.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.kotlin_customer_nom_movie_ticket.R
import com.example.kotlin_customer_nom_movie_ticket.data.model.Notification
import com.example.kotlin_customer_nom_movie_ticket.databinding.NotificationItemBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class NotificationAdapter(val list: List<Notification>) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {
    inner class NotificationViewHolder(val binding: NotificationItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun onbind(notification: Notification) {
            if (notification.type == "review") {
                binding.img.setImageResource(R.drawable.review_icon)
            } else if (notification.type == "showtime") {
                binding.img.setImageResource(R.drawable.noun_clock_icon)
            } else {
                binding.img.setImageResource(R.drawable.notification_icon)
            }
            binding.tvTitle.text = notification.title
            binding.tvMessage.text = notification.message
            val formattedDate = formatTimestampToVietnamese(notification.timestamp.toString().toLong())
            binding.tvDateTime.text = formattedDate
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val binding = NotificationItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NotificationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.onbind(list[position])
        holder.binding.root.setOnClickListener {
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    fun formatTimestampToVietnamese(timestamp: Long): String {
        val vietnameseLocale = Locale("vi", "VN")
        val sdf = SimpleDateFormat("EEEE, d 'thg' M, yyyy HH:mm", vietnameseLocale)
        sdf.timeZone = TimeZone.getTimeZone("Asia/Ho_Chi_Minh")

        val calendar = Calendar.getInstance().apply {
            timeInMillis = timestamp
        }
        return sdf.format(calendar.time)
    }
}