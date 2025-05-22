package com.example.kotlin_customer_nom_movie_ticket.ui.view.fragment

import android.Manifest
import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.kotlin_customer_nom_movie_ticket.R
import com.example.kotlin_customer_nom_movie_ticket.data.model.Booking
import com.example.kotlin_customer_nom_movie_ticket.databinding.FragmentUpcomingTicketBinding
import com.example.kotlin_customer_nom_movie_ticket.service.Notification.NotificationWorker
import com.example.kotlin_customer_nom_movie_ticket.ui.adapter.BookingAdapter
import com.example.kotlin_customer_nom_movie_ticket.ui.adapter.FoodBookingAdapter
import com.example.kotlin_customer_nom_movie_ticket.ui.view.activity.BookingDetailActivity
import com.example.kotlin_customer_nom_movie_ticket.ui.view.activity.MainActivity
import com.example.kotlin_customer_nom_movie_ticket.ui.view.activity.OrderFoodDetailActivity
import com.example.kotlin_customer_nom_movie_ticket.util.SessionManager
import com.example.kotlin_customer_nom_movie_ticket.viewmodel.TicketViewModel
import com.google.firebase.messaging.FirebaseMessaging
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

class UpcomingTicketFragment : Fragment() {

    private var _binding: FragmentUpcomingTicketBinding? = null
    private val binding get() = _binding!!
    private lateinit var ticketViewModel: TicketViewModel
    private lateinit var adapter: BookingAdapter
    private lateinit var foodAdapter: FoodBookingAdapter
    private var isMovieTabSelected = true
    private lateinit var customerId: String
    private var isFetching = true
    private var isFetchingBookings = false
    private var isFetchingFoodBookings = false

    private val paymentSuccessReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            isFetching = true
            fetchInitialData()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUpcomingTicketBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val isDarkMode = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        binding.rcvUpcomingTicket.visibility = View.GONE
        binding.rcvUpcomingFood.visibility = View.GONE
        binding.progressBar.visibility = View.VISIBLE
        binding.progressBar.playAnimation()
        binding.layoutNotFoundTicket.visibility = View.GONE
        isMovieTabSelected = true
        isFetching = true

        customerId = SessionManager.getUserId(requireContext()) ?: run {
            requireActivity().finish()
            return
        }

        LocalBroadcastManager.getInstance(requireContext())
            .registerReceiver(paymentSuccessReceiver, IntentFilter("PAYMENT_SUCCESS"))

        ticketViewModel = TicketViewModel()
        setupRecyclerViews()
        setupTabButtons(isDarkMode)
        fetchInitialData()
        setupObservers()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
            }
        }
    }

    private fun setupRecyclerViews() {
        binding.rcvUpcomingTicket.layoutManager = LinearLayoutManager(requireContext())
        binding.rcvUpcomingFood.layoutManager = LinearLayoutManager(requireContext())
        adapter = BookingAdapter(mutableListOf()).apply {
            onClickItem = { booking, _ ->
                val intent = Intent(requireContext(), BookingDetailActivity::class.java)
                intent.putExtra("BOOKING", booking)
                startActivity(intent)
                requireActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }
            onClickReminder = { booking, position, isChecked ->
                val prefs = requireContext().getSharedPreferences("reminder_prefs", Context.MODE_PRIVATE)
                prefs.edit().putBoolean("reminder_${booking.bill_id}", isChecked).apply()

                if (isChecked) {
                    val appPrefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                    if (appPrefs.getBoolean("notifications_enabled", true)) {
                        scheduleNotification(booking)
                    } else {
                        showOpenProfileDialog(booking, position)
                    }
                } else {
                    cancelNotification(booking)
                }
            }
        }
        foodAdapter = FoodBookingAdapter(mutableListOf()).apply {
            onClickItem = { foodBooking, _ ->
                val intent = Intent(requireContext(), OrderFoodDetailActivity::class.java)
                intent.putExtra("FOOD_BOOKING", foodBooking)
                startActivity(intent)
                requireActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }
        }
        binding.rcvUpcomingTicket.adapter = adapter
        binding.rcvUpcomingFood.adapter = foodAdapter
    }

    private fun setupObservers() {
        ticketViewModel.upcomingBookings.observe(viewLifecycleOwner) { bookings ->
            val prefs = requireContext().getSharedPreferences("reminder_prefs", Context.MODE_PRIVATE)
            bookings.forEach { booking ->
                booking.isReminderEnabled = prefs.getBoolean("reminder_${booking.bill_id}", false)
            }
            adapter.updateData(bookings.toMutableList())
            isFetchingBookings = false
            if (!isFetchingBookings && !isFetchingFoodBookings) {
                isFetching = false
                updateUI()
            }
        }

        ticketViewModel.upComingFoodBookings.observe(viewLifecycleOwner) { foodBookings ->
            foodAdapter.updateData(foodBookings)
            isFetchingFoodBookings = false
            if (!isFetchingBookings && !isFetchingFoodBookings) {
                isFetching = false
                updateUI()
            }
        }

        ticketViewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                isFetchingBookings = false
                isFetchingFoodBookings = false
                isFetching = false
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                stopAnimation()
                binding.layoutNotFoundTicket.visibility = View.VISIBLE
            }
        }

        ticketViewModel.isLoading.observe(viewLifecycleOwner) { updateUI() }
    }

    private fun stopAnimation() {
        binding.progressBar.cancelAnimation()
        binding.progressBar.visibility = View.GONE
    }

    private fun setupTabButtons(isDarkMode: Boolean) {
        if (!isDarkMode) {
            binding.btnMovie.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            binding.btnFood.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
        } else {
            binding.btnMovie.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            binding.btnFood.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        }

        binding.btnMovie.setOnClickListener {
            isMovieTabSelected = true
            binding.btnMovie.setBackgroundResource(R.drawable.button_book_now_background)
            binding.btnFood.setBackgroundResource(R.drawable.button_grey_stroke_background)
            if (!isDarkMode) {
                binding.btnMovie.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                binding.btnFood.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
            }
            updateUI()
        }

        binding.btnFood.setOnClickListener {
            isMovieTabSelected = false
            binding.btnMovie.setBackgroundResource(R.drawable.button_grey_stroke_background)
            binding.btnFood.setBackgroundResource(R.drawable.button_book_now_background)
            if (!isDarkMode) {
                binding.btnMovie.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                binding.btnFood.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            }
            updateUI()
        }
    }

    private fun fetchInitialData() {
        isFetchingBookings = true
        isFetchingFoodBookings = true
        binding.rcvUpcomingTicket.visibility = View.GONE
        binding.rcvUpcomingFood.visibility = View.GONE
        binding.progressBar.visibility = View.VISIBLE
        binding.progressBar.playAnimation()
        ticketViewModel.fetchBookings(customerId)
        ticketViewModel.fetchFoodBookings(customerId)
    }

    private fun updateUI() {
        val isLoading = ticketViewModel.isLoading.value
        if (isFetching || isLoading == true) {
            binding.rcvUpcomingTicket.visibility = View.GONE
            binding.rcvUpcomingFood.visibility = View.GONE
            binding.progressBar.visibility = View.VISIBLE
            binding.progressBar.playAnimation()
            binding.layoutNotFoundTicket.visibility = View.GONE
            return
        }

        stopAnimation()
        if (isMovieTabSelected) {
            val bookings = ticketViewModel.upcomingBookings.value ?: emptyList()
            binding.rcvUpcomingTicket.visibility = if (bookings.isNotEmpty()) View.VISIBLE else View.GONE
            binding.rcvUpcomingFood.visibility = View.GONE
            binding.layoutNotFoundTicket.visibility = if (bookings.isEmpty()) View.VISIBLE else View.GONE
        } else {
            val foodBookings = ticketViewModel.upComingFoodBookings.value ?: emptyList()
            binding.rcvUpcomingTicket.visibility = View.GONE
            binding.rcvUpcomingFood.visibility = if (foodBookings.isNotEmpty()) View.VISIBLE else View.GONE
            binding.layoutNotFoundTicket.visibility = if (foodBookings.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun showOpenProfileDialog(booking: Booking, position: Int) {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_open_profile_fragment)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCancelable(false)

        val btnCancel = dialog.findViewById<Button>(R.id.btnCancel)
        val btnTurnOn = dialog.findViewById<Button>(R.id.btnTurnOn)

        btnCancel.setOnClickListener {
            dialog.dismiss()
            adapter.resetReminder(position)
        }

        btnTurnOn.setOnClickListener {
            dialog.dismiss()
            adapter.resetReminder(position)
            val prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("from_reminder_dialog", true).apply()
            (activity as? MainActivity)?.let { mainActivity ->
                mainActivity.binding.bottomBar.selectTabById(R.id.tab_profile, true)
            }
        }

        dialog.show()
    }

    private fun showCustomToast(message: String) {
        val inflater = LayoutInflater.from(requireContext())
        val layout = inflater.inflate(R.layout.toast_reminder, null)
        val textView = layout.findViewById<TextView>(R.id.tv_reminder)
        textView.text = message
        val toast = Toast(requireContext())
        toast.duration = Toast.LENGTH_SHORT
        val density = resources.displayMetrics.density
        val yOffset = (70 * density).toInt()
        toast.setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, yOffset)
        toast.setView(layout)
        toast.show()
    }

    private fun scheduleNotification(booking: Booking) {
        val prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("notifications_enabled", true)) {
            Log.d("UpcomingTicketFragment", "Notifications disabled, skipping schedule")
            return
        }

        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault())
        val showtime = sdf.parse(booking.showtime_time) ?: return
        val calendar = Calendar.getInstance().apply {
            time = showtime
            add(Calendar.MINUTE, -30)
        }
        val notifyTime = calendar.timeInMillis
        val currentTime = System.currentTimeMillis()
        if (notifyTime <= currentTime) {
            showCustomToast("Showtime has passed or is too close!")
            booking.isReminderEnabled = false
            return
        }
        val delay = notifyTime - currentTime

        val title = "Sắp Đến Giờ Chiếu ${booking.title}!"
        val message = "${booking.title} bắt đầu sau 30 phút tại ${booking.cinema_name ?: "rạp chiếu"}. Đừng đến muộn!"

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                val data = Data.Builder()
                    .putString("title", title)
                    .putString("message", message)
                    .putString("token", token)
                    .putString("movie_id", booking.movie_id)
                    .putString("bill_id", booking.bill_id)
                    .putBoolean("isShowtime", true)
                    .putBoolean("isReview", false)
                    .build()
                val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
                    .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                    .setInputData(data)
                    .addTag(booking.bill_id)
                    .build()
                WorkManager.getInstance(requireContext()).enqueue(workRequest)
                booking.isReminderEnabled = true
                Log.d("FCM_Token", "Current device token: $token")
            } else {
                booking.isReminderEnabled = false
                Log.e("FCM_Token", "Failed to get token: ${task.exception?.message}")
            }
        }
    }

    private fun cancelNotification(booking: Booking) {
        booking.isReminderEnabled = false
        WorkManager.getInstance(requireContext()).cancelAllWorkByTag(booking.bill_id)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopAnimation()
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(paymentSuccessReceiver)
        _binding = null
    }
}