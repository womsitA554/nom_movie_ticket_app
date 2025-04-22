package com.example.kotlin_customer_nom_movie_ticket.ui.adapter

import android.content.res.Configuration
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.kotlin_customer_nom_movie_ticket.R
import com.example.kotlin_customer_nom_movie_ticket.data.model.Day
import com.example.kotlin_customer_nom_movie_ticket.databinding.DayItemBinding

class DayAdapter(private val days: List<Day>, private val isDarkMode: Boolean) : RecyclerView.Adapter<DayAdapter.DayViewHolder>() {
    var onClickItem: ((List<Day>, Int) -> Unit)? = null
    private var selectedPosition = days.indexOfFirst { it.isSelected }

    inner class DayViewHolder(private val binding: DayItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun onBind(day: Day, isSelected: Boolean) {
            binding.tvDayNumber.text = day.dayNumber
            binding.tvDayName.text = day.dayName

            if (!isDarkMode){
                if (isSelected) {
                    binding.root.background = ContextCompat.getDrawable(binding.root.context, R.drawable.item_orange_background)
                    binding.tvDayNumber.setTextColor(ContextCompat.getColor(binding.root.context, android.R.color.white))
                    binding.tvDayName.setTextColor(ContextCompat.getColor(binding.root.context, android.R.color.white))
                } else {
                    binding.root.background = ContextCompat.getDrawable(binding.root.context, R.drawable.item_white_stroke_backgound)
                    binding.tvDayNumber.setTextColor(ContextCompat.getColor(binding.root.context, android.R.color.black))
                    binding.tvDayName.setTextColor(ContextCompat.getColor(binding.root.context, android.R.color.black))
                }
            } else {
                if (isSelected) {
                    binding.root.background = ContextCompat.getDrawable(binding.root.context, R.drawable.item_orange_background)
                    binding.tvDayNumber.setTextColor(ContextCompat.getColor(binding.root.context, android.R.color.white))
                    binding.tvDayName.setTextColor(ContextCompat.getColor(binding.root.context, android.R.color.white))
                } else {
                    binding.root.background = ContextCompat.getDrawable(binding.root.context, R.drawable.item_white_stroke_backgound)
                    binding.tvDayNumber.setTextColor(ContextCompat.getColor(binding.root.context, android.R.color.white))
                    binding.tvDayName.setTextColor(ContextCompat.getColor(binding.root.context, android.R.color.white))
                }
            }


            binding.root.setOnClickListener {
                val previousPosition = selectedPosition
                selectedPosition = adapterPosition
                notifyItemChanged(previousPosition)
                notifyItemChanged(selectedPosition)
                onClickItem?.invoke(days, adapterPosition)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val binding = DayItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DayViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        holder.onBind(days[position], position == selectedPosition)
    }

    override fun getItemCount(): Int = days.size
}