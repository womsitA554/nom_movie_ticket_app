package com.example.kotlin_customer_nom_movie_ticket.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.kotlin_customer_nom_movie_ticket.R
import com.example.kotlin_customer_nom_movie_ticket.data.model.Booking
import com.example.kotlin_customer_nom_movie_ticket.data.model.FoodBooking
import com.example.kotlin_customer_nom_movie_ticket.databinding.UpcomingFoodItemBinding
import com.example.kotlin_customer_nom_movie_ticket.databinding.UpcomingTicketItemBinding

class FoodBookingAdapter (private val listFoodBooking: MutableList<FoodBooking>) : RecyclerView.Adapter<FoodBookingAdapter.BookingViewHolder>() {
    var onClickItem: (FoodBooking, Int) -> Unit = {_,_->}
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): FoodBookingAdapter.BookingViewHolder {
        val binding = UpcomingFoodItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BookingViewHolder(binding)
    }

    inner class BookingViewHolder (private val binding: UpcomingFoodItemBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.linearOnclick.setOnClickListener{
                onClickItem.invoke(listFoodBooking[adapterPosition], adapterPosition)
            }

        }
        fun onBind(foodBooking: FoodBooking) {
            binding.tvTimePickUp.text = foodBooking.pick_up_time
            if (foodBooking.food_items.size > 1) {
                binding.picFood.setBackgroundResource(R.drawable.seat_selected_background)
                binding.picFood.setImageResource(R.drawable.popcorn_and_drink)
                val foodNames = foodBooking.food_items.map { it.title }
                binding.tvTitle.text = when {
                    foodNames.size <= 2 -> foodNames.joinToString(" + ")
                    else -> "${foodNames.take(2).joinToString(" + ")} ..."
                }
            } else if (foodBooking.food_items.isNotEmpty()) {
                val foodItem = foodBooking.food_items[0]
                Glide.with(itemView.context)
                    .load(foodItem.picUrl)
                    .into(binding.picFood)
                binding.tvTitle.text = foodItem.title
            }
        }
    }

    override fun onBindViewHolder(holder: FoodBookingAdapter.BookingViewHolder, position: Int) {
        holder.onBind(listFoodBooking[position])
    }

    override fun getItemCount(): Int {
        return listFoodBooking.size
    }

    fun updateData(newFoodBookings: List<FoodBooking>) {
        listFoodBooking.clear()
        listFoodBooking.addAll(newFoodBookings)
        notifyDataSetChanged()
    }
}