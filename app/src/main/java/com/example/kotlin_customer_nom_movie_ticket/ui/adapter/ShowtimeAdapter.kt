package com.example.kotlin_customer_nom_movie_ticket.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.kotlin_customer_nom_movie_ticket.R
import com.example.kotlin_customer_nom_movie_ticket.data.model.Showtime
import com.example.kotlin_customer_nom_movie_ticket.databinding.TimeItemBinding
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ShowtimeAdapter(
    private var showtimeList: List<Showtime>,
    private val onShowtimeClick: (Showtime) -> Unit
) : RecyclerView.Adapter<ShowtimeAdapter.ViewHolder>() {

    private var selectedShowtimeId: String? = null

    fun setSelectedShowtimeId(showtimeId: String?) {
        this.selectedShowtimeId = showtimeId
        notifyDataSetChanged()
    }

    inner class ViewHolder(private val binding: TimeItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(showtime: Showtime, isSelected: Boolean) {
            val dateTime = LocalDateTime.parse(showtime.showtime_time)
            val formatter = DateTimeFormatter.ofPattern("HH:mm")
            binding.tvTime.text = dateTime.format(formatter) ?: "N/A"

            binding.root.setOnClickListener {
                onShowtimeClick(showtime)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = TimeItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val showtime = showtimeList[position]
        holder.bind(showtime, showtime.showtime_id == selectedShowtimeId)
    }

    override fun getItemCount(): Int = showtimeList.size

    fun updateData(newShowtimes: List<Showtime>) {
        showtimeList = newShowtimes
        notifyDataSetChanged()
    }
}