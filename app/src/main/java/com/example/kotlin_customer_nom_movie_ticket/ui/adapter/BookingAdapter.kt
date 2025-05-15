package com.example.kotlin_customer_nom_movie_ticket.ui.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.kotlin_customer_nom_movie_ticket.data.model.Booking
import com.example.kotlin_customer_nom_movie_ticket.databinding.UpcomingTicketItemBinding
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

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
            binding.tvShowtimeTime.text = booking.showtime_time?.let { convertShowtimeTime(it) }
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

    fun convertShowtimeTime(showtimeTime: String): String {
        try {
            // Parse chuỗi ISO 8601 không có múi giờ
            val localDateTime = LocalDateTime.parse(showtimeTime)
            // Chuyển sang múi giờ Asia/Ho_Chi_Minh
            val zonedDateTime = localDateTime.atZone(ZoneId.of("Asia/Ho_Chi_Minh"))
            // Chuyển thành Date để sử dụng với SimpleDateFormat
            val date = Date.from(zonedDateTime.toInstant())

            // Kiểm tra xem có phải ngày hôm nay không
            val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh")).apply {
                time = date
            }
            val todayCalendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"))
            val isToday = calendar.get(Calendar.YEAR) == todayCalendar.get(Calendar.YEAR) &&
                    calendar.get(Calendar.MONTH) == todayCalendar.get(Calendar.MONTH) &&
                    calendar.get(Calendar.DAY_OF_MONTH) == todayCalendar.get(Calendar.DAY_OF_MONTH)

            // Định dạng đầu ra
            val dateFormat = SimpleDateFormat("dd 'thg' M, yyyy HH:mm", Locale("vi", "VN")).apply {
                timeZone = TimeZone.getTimeZone("Asia/Ho_Chi_Minh")
            }
            val dayOfWeekFormat = SimpleDateFormat("EEEE", Locale("vi", "VN")).apply {
                timeZone = TimeZone.getTimeZone("Asia/Ho_Chi_Minh")
            }

            val formattedDate = dateFormat.format(date)
            return if (isToday) {
                "Hôm nay, $formattedDate"
            } else {
                "${dayOfWeekFormat.format(date)}, $formattedDate"
            }
        } catch (e: Exception) {
            Log.e("ConvertShowtimeTime", "Error parsing showtime_time: $showtimeTime", e)
            return showtimeTime // Trả về giá trị gốc nếu parse thất bại
        }
    }
}