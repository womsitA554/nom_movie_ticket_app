package com.example.kotlin_customer_nom_movie_ticket.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.kotlin_customer_nom_movie_ticket.data.model.Booking
import com.example.kotlin_customer_nom_movie_ticket.databinding.UpcomingTicketItemBinding

class BookingAdapter(private val listBooking: MutableList<Booking>) : RecyclerView.Adapter<BookingAdapter.BookingViewHolder>() {
    var onClickItem: (Booking, Int) -> Unit = { _, _ -> }
    var onClickReminder: (Booking, Int, Boolean) -> Unit = { _, _, _ -> }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val binding = UpcomingTicketItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BookingViewHolder(binding)
    }

    inner class BookingViewHolder(private val binding: UpcomingTicketItemBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.linearOnclick.setOnClickListener {
                onClickItem.invoke(listBooking[adapterPosition], adapterPosition)
            }

            binding.btnRemind.setOnCheckedChangeListener { _, isChecked ->
                listBooking[adapterPosition].isReminderEnabled = isChecked
                onClickReminder.invoke(listBooking[adapterPosition], adapterPosition, isChecked)
            }
        }

        fun onBind(booking: Booking) {
            binding.tvTitle.text = booking.title
            binding.tvShowtimeTime.text = booking.showtime_time
            Glide.with(itemView.context)
                .load(booking.poster_url)
                .into(binding.picMovie)

            if (booking.isUpcoming) {
                binding.linearReminder.visibility = android.view.View.VISIBLE
                binding.view.visibility = android.view.View.VISIBLE
            } else {
                binding.linearReminder.visibility = android.view.View.GONE
                binding.view.visibility = android.view.View.GONE
            }

            // Cập nhật trạng thái SwitchButton
            binding.btnRemind.isChecked = booking.isReminderEnabled
        }

        fun resetReminder() {
            listBooking[adapterPosition].isReminderEnabled = false
            binding.btnRemind.isChecked = false
        }
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        holder.onBind(listBooking[position])
    }

    override fun getItemCount(): Int {
        return listBooking.size
    }

    fun resetReminder(position: Int) {
        if (position in 0 until listBooking.size) {
            listBooking[position].isReminderEnabled = false
            // Truy cập ViewHolder để cập nhật trực tiếp SwitchButton
            val holder = (bindingRecyclerView?.findViewHolderForAdapterPosition(position) as? BookingViewHolder)
            holder?.resetReminder()
        }
    }

    // Lưu tham chiếu đến RecyclerView để truy cập ViewHolder
    private var bindingRecyclerView: RecyclerView? = null

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        bindingRecyclerView = recyclerView
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        bindingRecyclerView = null
    }

    fun updateData(newBookings: MutableList<Booking>) {
        listBooking.clear()
        listBooking.addAll(newBookings)
        notifyDataSetChanged()
    }
}