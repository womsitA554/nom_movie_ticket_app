package com.example.kotlin_customer_nom_movie_ticket.ui.view.fragment

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kotlin_customer_nom_movie_ticket.R
import com.example.kotlin_customer_nom_movie_ticket.databinding.FragmentPassedTicketBinding
import com.example.kotlin_customer_nom_movie_ticket.ui.adapter.BookingAdapter
import com.example.kotlin_customer_nom_movie_ticket.ui.adapter.FoodBookingAdapter
import com.example.kotlin_customer_nom_movie_ticket.ui.view.activity.BookingDetailActivity
import com.example.kotlin_customer_nom_movie_ticket.ui.view.activity.OrderFoodDetailActivity
import com.example.kotlin_customer_nom_movie_ticket.util.SessionManager
import com.example.kotlin_customer_nom_movie_ticket.viewmodel.TicketViewModel

class PassedTicketFragment : Fragment() {

    private var _binding: FragmentPassedTicketBinding? = null
    private val binding get() = _binding!!
    private lateinit var ticketViewModel: TicketViewModel
    private lateinit var adapter: BookingAdapter
    private lateinit var foodAdapter: FoodBookingAdapter
    private var isMovieTabSelected = true
    private lateinit var customerId: String
    private var isFetching = true

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
        _binding = FragmentPassedTicketBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val isDarkMode = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        // Initialize UI with progressBar visible
        binding.rcvPassedTicket.visibility = View.GONE
        binding.rcvPassedFood.visibility = View.GONE
        binding.progressBar.visibility = View.VISIBLE
        binding.progressBar.playAnimation() // Bắt đầu animation
        binding.layoutNotFoundTicket.visibility = View.GONE
        isMovieTabSelected = true
        isFetching = true

        customerId = SessionManager.getUserId(requireContext()) ?: run {
            requireActivity().finish()
            return
        }

        ticketViewModel = TicketViewModel()
        setupRecyclerViews()
        setupObservers()
        setupTabButtons(isDarkMode)
        fetchInitialData()

        LocalBroadcastManager.getInstance(requireContext())
            .registerReceiver(paymentSuccessReceiver, IntentFilter("PAYMENT_SUCCESS"))

        // Request notification permission (if needed for other features)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
            }
        }
    }

    private fun setupRecyclerViews() {
        binding.rcvPassedTicket.layoutManager = LinearLayoutManager(requireContext())
        binding.rcvPassedFood.layoutManager = LinearLayoutManager(requireContext())
        adapter = BookingAdapter(mutableListOf()).apply {
            onClickItem = { booking, _ ->
                val intent = Intent(requireContext(), BookingDetailActivity::class.java)
                intent.putExtra("BOOKING", booking)
                startActivity(intent)
                activity?.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }
        }
        foodAdapter = FoodBookingAdapter(mutableListOf()).apply {
            onClickItem = { foodBooking, _ ->
                val intent = Intent(requireContext(), OrderFoodDetailActivity::class.java)
                intent.putExtra("FOOD_BOOKING", foodBooking)
                startActivity(intent)
                activity?.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }
        }
        binding.rcvPassedTicket.adapter = adapter
        binding.rcvPassedFood.adapter = foodAdapter
    }

    private fun setupObservers() {
        ticketViewModel.passedBookings.observe(viewLifecycleOwner) { bookings ->
            adapter.updateData(bookings.toMutableList())
            isFetching = false
            binding.layoutNotFoundTicket.visibility = if (bookings.isEmpty() && isMovieTabSelected) View.VISIBLE else View.GONE
            updateUI()
            Log.d("PassedTicketFragment", "Updated passed bookings: ${bookings.size}")
        }

        ticketViewModel.passedFoodBookings.observe(viewLifecycleOwner) { foodBookings ->
            foodAdapter.updateData(foodBookings)
            isFetching = false
            binding.layoutNotFoundTicket.visibility = if (foodBookings.isEmpty() && !isMovieTabSelected) View.VISIBLE else View.GONE
            updateUI()
            Log.d("PassedTicketFragmentFood", "Updated passed food bookings: ${foodBookings.size}")
        }

        ticketViewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                isFetching = false
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                Log.e("PassedTicketFragment", "Error: $it")
                stopAnimation() // Dừng và ẩn progressBar khi có lỗi
                binding.layoutNotFoundTicket.visibility = View.VISIBLE
            }
        }

        ticketViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            updateUI()
        }
    }

    private fun stopAnimation() {
        binding.progressBar.cancelAnimation() // Dừng animation
        binding.progressBar.visibility = View.GONE // Ẩn progressBar
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
            } else {
                binding.btnMovie.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                binding.btnFood.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
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
            } else {
                binding.btnMovie.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                binding.btnFood.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            }
            updateUI()
        }
    }

    private fun fetchInitialData() {
        isFetching = true
        binding.rcvPassedTicket.visibility = View.GONE
        binding.rcvPassedFood.visibility = View.GONE
        binding.progressBar.visibility = View.VISIBLE
        binding.progressBar.playAnimation() // Bắt đầu animation
        binding.layoutNotFoundTicket.visibility = View.GONE
        ticketViewModel.fetchBookings(customerId)
        ticketViewModel.fetchFoodBookings(customerId)
        Log.d("PassedTicketFragment", "Fetching passed data for customer: $customerId")
    }

    private fun updateUI() {
        Log.d("PassedTicketFragment", "Updating UI, isMovieTabSelected: $isMovieTabSelected, isFetching: $isFetching")
        if (isFetching || ticketViewModel.isLoading.value == true) {
            binding.rcvPassedTicket.visibility = View.GONE
            binding.rcvPassedFood.visibility = View.GONE
            binding.progressBar.visibility = View.VISIBLE
            binding.progressBar.playAnimation() // Đảm bảo animation chạy khi hiển thị
            binding.layoutNotFoundTicket.visibility = View.GONE
            return
        }

        stopAnimation()
        if (isMovieTabSelected) {
            val bookings = ticketViewModel.passedBookings.value ?: emptyList()
            Log.d("PassedTicketFragment", "Movie tab, bookings: ${bookings.size}")
            binding.rcvPassedTicket.visibility = if (bookings.isNotEmpty()) View.VISIBLE else View.GONE
            binding.rcvPassedFood.visibility = View.GONE
            binding.layoutNotFoundTicket.visibility = if (bookings.isEmpty()) View.VISIBLE else View.GONE
        } else {
            val foodBookings = ticketViewModel.passedFoodBookings.value ?: emptyList()
            Log.d("PassedTicketFragment", "Food tab, foodBookings: ${foodBookings.size}")
            binding.rcvPassedTicket.visibility = View.GONE
            binding.rcvPassedFood.visibility = if (foodBookings.isNotEmpty()) View.VISIBLE else View.GONE
            binding.layoutNotFoundTicket.visibility = if (foodBookings.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopAnimation()
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(paymentSuccessReceiver)
        _binding = null
    }
}