package com.example.kotlin_customer_nom_movie_ticket.ui.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.kotlin_customer_nom_movie_ticket.R
import com.example.kotlin_customer_nom_movie_ticket.data.model.Seat
import com.example.kotlin_customer_nom_movie_ticket.databinding.SeatItemBinding
import com.example.kotlin_customer_nom_movie_ticket.viewmodel.SeatViewModel

class SeatAdapter(
    private var seatList: List<Seat>,
    private val currentUserId: String,
    private val seatViewModel: SeatViewModel,
    private val showtimeId: String
) : RecyclerView.Adapter<SeatAdapter.SeatViewHolder>() {
    var onClickItem: ((List<Seat>, Int) -> Unit)? = null
    private val selectedPositions = mutableSetOf<Int>()
    private val MAX_SEATS = 7

    inner class SeatViewHolder(private val binding: SeatItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun onBind(seat: Seat, isSelected: Boolean) {
            binding.tvSeat.text = "${seat.row_number}${seat.seat_number}"

            val userId = seatViewModel.getSeatBookingUserId(seat.seat_id)
            Log.e("SeatAdapter", "Seat: ${seat.seat_id}, userId: $userId, status: ${seat.status}, isSelected: $isSelected")

            when {
                isSelected -> {
                    binding.root.background = ContextCompat.getDrawable(binding.root.context, R.drawable.seat_taken_background)
                    binding.tvSeat.setTextColor(ContextCompat.getColor(binding.root.context, android.R.color.white))
                }
                seat.status == "reserved" && userId == currentUserId -> {
                    binding.root.background = ContextCompat.getDrawable(binding.root.context, R.drawable.seat_taken_background)
                    binding.tvSeat.setTextColor(ContextCompat.getColor(binding.root.context, android.R.color.white))
                }
                seat.status == "reserved" && userId != null && userId != currentUserId -> {
                    binding.root.background = ContextCompat.getDrawable(binding.root.context, R.drawable.seat_reserved_background)
                    binding.tvSeat.setTextColor(ContextCompat.getColor(binding.root.context, android.R.color.white))
                }
                seat.seat_type == "standard" && seat.status == "available" -> {
                    binding.root.background = ContextCompat.getDrawable(binding.root.context, R.drawable.seat_available_background)
                    binding.tvSeat.setTextColor(ContextCompat.getColor(binding.root.context, android.R.color.white))
                }
                seat.seat_type == "standard" && seat.status == "booked" -> {
                    binding.root.background = ContextCompat.getDrawable(binding.root.context, R.drawable.seat_selected_background)
                    binding.tvSeat.setTextColor(ContextCompat.getColor(binding.root.context, android.R.color.white))
                }
                seat.seat_type == "vip" && seat.status == "available" -> {
                    binding.root.background = ContextCompat.getDrawable(binding.root.context, R.drawable.seat_vip_background)
                    binding.tvSeat.setTextColor(ContextCompat.getColor(binding.root.context, android.R.color.white))
                }
                seat.seat_type == "vip" && seat.status == "booked" -> {
                    binding.root.background = ContextCompat.getDrawable(binding.root.context, R.drawable.seat_selected_background)
                    binding.tvSeat.setTextColor(ContextCompat.getColor(binding.root.context, android.R.color.white))
                }
                else -> {
                    binding.root.background = ContextCompat.getDrawable(binding.root.context, R.drawable.seat_available_background)
                    binding.tvSeat.setTextColor(ContextCompat.getColor(binding.root.context, android.R.color.white))
                }
            }

            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val seat = seatList[position]
                    val userId = seatViewModel.getSeatBookingUserId(seat.seat_id)
                    if (seat.status == "reserved" && userId == currentUserId && selectedPositions.contains(position)) {
                        // Hủy chọn ghế reserved của currentUserId
                        selectedPositions.remove(position)
                        onClickItem?.invoke(seatList, position)
                        notifyItemChanged(position)
                    } else if (seat.status == "available" && selectedPositions.size < MAX_SEATS) {
                        // Chọn ghế available
                        selectedPositions.add(position)
                        onClickItem?.invoke(seatList, position)
                        notifyItemChanged(position)
                    } else if (selectedPositions.size >= MAX_SEATS) {
                        Toast.makeText(binding.root.context, "You can only select up to 7 seats", Toast.LENGTH_SHORT).show()
                    } else if (seat.status == "reserved" && userId != null && userId != currentUserId) {
                        Toast.makeText(binding.root.context, "This seat is reserved by someone else", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SeatViewHolder {
        val binding = SeatItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SeatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SeatViewHolder, position: Int) {
        holder.onBind(seatList[position], selectedPositions.contains(position))
    }

    override fun getItemCount(): Int = seatList.size

    fun getSelectedSeats(): List<Seat> {
        val selectedSeats = selectedPositions.map { seatList[it] }
        Log.d("SeatAdapter", "Selected seats count: ${selectedSeats.size}, positions: $selectedPositions")
        return selectedSeats
    }

    fun getSelectedSeatsCount(): Int = selectedPositions.size

    fun clearSelection() {
        val oldSelections = selectedPositions.toSet()
        selectedPositions.clear()
        oldSelections.forEach { notifyItemChanged(it) }
    }

    fun getSelectedSeatIds(): List<String> {
        return getSelectedSeats().map { it.seat_id }
    }

    fun getSelectedSeatNames(): String {
        val selectedSeats = getSelectedSeats()
        return when (selectedSeats.size) {
            0 -> "0"
            1 -> "${selectedSeats[0].row_number}${selectedSeats[0].seat_number}"
            else -> selectedSeats.take(7).joinToString(", ") { "${it.row_number}${it.seat_number}" } +
                    if (selectedSeats.size > 7) "..." else ""
        }
    }

    fun updateSeats(newSeats: List<Seat>) {
        val oldSelectedSeats = getSelectedSeats().map { it.seat_id }.toSet()
        seatList = newSeats
        selectedPositions.clear()
        newSeats.forEachIndexed { index, seat ->
            if (seat.status == "reserved" && seatViewModel.getSeatBookingUserId(seat.seat_id) == currentUserId) {
                selectedPositions.add(index)
            } else if (seat.status == "available" && oldSelectedSeats.contains(seat.seat_id)) {
                selectedPositions.add(index)
            }
        }
        notifyDataSetChanged()
    }
}