package com.example.kotlin_customer_nom_movie_ticket.ui.adapter

import android.content.res.TypedArray
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.kotlin_customer_nom_movie_ticket.R
import com.example.kotlin_customer_nom_movie_ticket.databinding.PointHistoryItemBinding
import com.example.kotlin_customer_nom_movie_ticket.data.model.PointHistory

class PointHistoryAdapter(private val listPoint: List<PointHistory>) : RecyclerView.Adapter<PointHistoryAdapter.pointViewHolder>() {

    inner class pointViewHolder(private val binding: PointHistoryItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun onBind(pointHistory: PointHistory) {
            binding.tvDescription.text = "#${pointHistory.description}"
            binding.tvDateTime.text = pointHistory.created_at.toString()

            if (pointHistory.type == "earned") {
                // Green for earned points
                binding.tvPoint.setTextColor(ContextCompat.getColor(binding.root.context, R.color.light_green))
                binding.tvPoint.text = "+${pointHistory.points}"
            } else {
                // Get text color from TextColor style for redeemed or other types
                val typedArray: TypedArray = binding.root.context.obtainStyledAttributes(
                    R.style.TextColor, intArrayOf(android.R.attr.textColor)
                )
                val textColor = typedArray.getColor(0, ContextCompat.getColor(binding.root.context, R.color.black))
                typedArray.recycle()

                binding.tvPoint.setTextColor(textColor)
                binding.tvPoint.text = "-${pointHistory.points}"
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): pointViewHolder {
        val binding = PointHistoryItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return pointViewHolder(binding)
    }

    override fun onBindViewHolder(holder: pointViewHolder, position: Int) {
        holder.onBind(listPoint[position])
    }

    override fun getItemCount(): Int {
        return listPoint.size
    }
}