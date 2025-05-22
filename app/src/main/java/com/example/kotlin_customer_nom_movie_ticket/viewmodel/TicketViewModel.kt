package com.example.kotlin_customer_nom_movie_ticket.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kotlin_customer_nom_movie_ticket.data.model.Booking
import com.example.kotlin_customer_nom_movie_ticket.data.model.FoodBooking
import com.example.kotlin_customer_nom_movie_ticket.data.model.PointTransaction
import com.example.kotlin_customer_nom_movie_ticket.data.repository.TicketRepository
import com.example.kotlin_customer_nom_movie_ticket.util.CartManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class TicketViewModel : ViewModel() {
    private val ticketRepository = TicketRepository()
    private val _upcomingBookings = MutableLiveData<List<Booking>>()
    val upcomingBookings: LiveData<List<Booking>> get() = _upcomingBookings
    private val _upComingFoodBookings = MutableLiveData<List<FoodBooking>>()
    val upComingFoodBookings: LiveData<List<FoodBooking>> get() = _upComingFoodBookings
    private val _passedFoodBookings = MutableLiveData<List<FoodBooking>>()
    val passedFoodBookings: LiveData<List<FoodBooking>> get() = _passedFoodBookings
    private val _passedBookings = MutableLiveData<List<Booking>>()
    val passedBookings: LiveData<List<Booking>> get() = _passedBookings

    private val _isLoading = MutableLiveData<Boolean>(true) // Initialize to true
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error

    private val _foodBookingStatus = MutableLiveData<String>()
    val foodBookingStatus: LiveData<String> get() = _foodBookingStatus

    private val _paymentStatus = MutableLiveData<String>()
    val paymentStatus: LiveData<String> get() = _paymentStatus

    private val _pointTransactions = MutableLiveData<List<PointTransaction>>()
    val pointTransactions: LiveData<List<PointTransaction>> get() = _pointTransactions

    init {
        Log.d("TicketViewModel", "Initialized, isLoading: ${_isLoading.value}")
    }

    fun fetchPointTransactions(customerId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            Log.d("TicketViewModel", "Fetching point transactions, isLoading: ${_isLoading.value}")
            _error.value = null
            try {
                val transactions = ticketRepository.getPointTransactions(customerId)
                _pointTransactions.value = transactions
                Log.d("TicketViewModel", "Fetched ${transactions.size} point transactions")
            } catch (e: Exception) {
                _error.value = "Failed to load point transactions: ${e.message}"
                Log.e("TicketViewModel", "Failed to load point transactions", e)
            } finally {
                _isLoading.value = false
                Log.d("TicketViewModel", "Point transactions fetch complete, isLoading: ${_isLoading.value}")
            }
        }
    }

    fun fetchBookings(customerId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            Log.d("TicketViewModel", "Fetching bookings, isLoading: ${_isLoading.value}")
            _error.value = null
            val result = ticketRepository.getBookingsByCustomerId(customerId)
            if (result.isSuccess) {
                val bookings = result.getOrNull() ?: emptyList()
                _upcomingBookings.value = bookings.filter { it.isUpcoming }
                _passedBookings.value = bookings.filter { !it.isUpcoming }
                Log.d("TicketViewModel", "Fetched bookings: upcoming=${_upcomingBookings.value?.size}, passed=${_passedBookings.value?.size}")
            } else {
                val errorMessage = "Không thể tải danh sách vé: ${result.exceptionOrNull()?.message}"
                _error.value = errorMessage
                Log.e("TicketViewModel", "Failed to load bookings", result.exceptionOrNull())
            }
            _isLoading.value = false
            Log.d("TicketViewModel", "Bookings fetch complete, isLoading: ${_isLoading.value}")
        }
    }

        fun fetchFoodBookings(customerId: String) {
            viewModelScope.launch {
                _isLoading.value = true
                Log.d("TicketViewModel", "Fetching food bookings, isLoading: ${_isLoading.value}")
                _error.value = null
                try {
                    val allFoodBookings = ticketRepository.getFoodBookingByCustomerId(customerId)
                    if (allFoodBookings.isEmpty()) {
                        Log.d("TicketViewModel", "No food bookings found for customerId: $customerId")
                        _upComingFoodBookings.value = emptyList()
                        _passedFoodBookings.value = emptyList()
                    } else {
                        val currentTime = System.currentTimeMillis()
                        val dateFormat = SimpleDateFormat("EEEE, dd 'thg' M, yyyy HH:mm", Locale("vi", "VN")).apply {
                            timeZone = TimeZone.getTimeZone("Asia/Ho_Chi_Minh")
                        }

                        _upComingFoodBookings.value = allFoodBookings.filter { booking ->
                            try {
                                val pickUpTimeMillis = dateFormat.parse(booking.pick_up_time)?.time ?: 0L
                                pickUpTimeMillis > currentTime
                            } catch (e: Exception) {
                                Log.e("TicketViewModel", "Error parsing pick_up_time: ${booking.pick_up_time}", e)
                                false
                            }
                        }

                        _passedFoodBookings.value = allFoodBookings.filter { booking ->
                            try {
                                val pickUpTimeMillis = dateFormat.parse(booking.pick_up_time)?.time ?: 0L
                                pickUpTimeMillis <= currentTime
                            } catch (e: Exception) {
                                Log.e("TicketViewModel", "Error parsing pick_up_time: ${booking.pick_up_time}", e)
                                true
                            }
                        }

                        Log.d("TicketViewModel", "Food bookings fetched: upcoming=${_upComingFoodBookings.value?.size}, passed=${_passedFoodBookings.value?.size}")
                    }
                } catch (e: Exception) {
                    _error.value = "Failed to load food bookings: ${e.message}"
                    Log.e("TicketViewModel", "Failed to load food bookings", e)
                } finally {
                    _isLoading.value = false
                    Log.d("TicketViewModel", "Food bookings fetch complete, isLoading: ${_isLoading.value}")
                }
            }
        }

    fun saveFoodBooking(billId: String?, cartManager: CartManager, userId: String, totalPriceToPay: Double, fee: Double, discount: Double, pickUpTime: String) {
        viewModelScope.launch {
            val cartItems = cartManager.getCart(userId)
            if (cartItems.isEmpty()) {
                _foodBookingStatus.value = "Cart is empty"
                return@launch
            }

            val createdAt = System.currentTimeMillis()
            val foodBillId = "FB${createdAt}"
            val foodBookingData = hashMapOf(
                "food_bill_id" to foodBillId,
                "bill_id" to billId,
                "customer_id" to userId,
                "food_items" to cartItems.map { it.toMap() },
                "total_price" to totalPriceToPay,
                "fee" to fee,
                "total_price_to_pay" to totalPriceToPay + fee - discount,
                "discount" to discount,
                "payment_method" to "Stripe",
                "payment_status" to "Paid",
                "order_time" to createdAt,
                "pick_up_time" to pickUpTime
            )

            try {
                ticketRepository.saveFoodBooking(foodBookingData, foodBillId)
                _foodBookingStatus.value = "Order placed successfully for $foodBillId"
                fetchFoodBookings(userId)
            } catch (e: Exception) {
                _foodBookingStatus.value = "Failed to save order: ${e.message}"
                Log.e("SaveFoodBooking", "Failed to save FoodBooking $foodBillId: ${e.message}")
            }
        }
    }

    fun savePaymentData(
        ticketIds: List<String>,
        selectedSeatIds: List<String>,
        showtimeId: String,
        userId: String,
        totalPriceSeats: Double,
        totalPriceFood: Double,
        fee: Double,
        discount: Double,
        actualPay: Double,
        cartManager: CartManager,
        pickUpTime: String
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            Log.d("TicketViewModel", "Saving payment data, isLoading: ${_isLoading.value}")
            _error.value = null
            try {
                val billId = ticketRepository.savePaymentData(
                    ticketIds,
                    selectedSeatIds,
                    showtimeId,
                    userId,
                    totalPriceSeats,
                    totalPriceFood,
                    fee,
                    discount,
                    actualPay,
                    cartManager,
                    pickUpTime
                )
                if (billId != null) {
                    _paymentStatus.value = "Payment saved successfully for bill $billId"
                    fetchBookings(userId)
                    fetchFoodBookings(userId)
                    cartManager.clearCart(userId)
                } else {
                    _paymentStatus.value = "Failed to save payment data"
                    _error.value = "Failed to save payment data"
                }
            } catch (e: Exception) {
                _paymentStatus.value = "Failed to save payment data: ${e.message}"
                _error.value = "Failed to save payment data: ${e.message}"
                Log.e("SavePaymentData", "Error: ${e.message}", e)
            } finally {
                _isLoading.value = false
                Log.d("TicketViewModel", "Payment data save complete, isLoading: ${_isLoading.value}")
            }
        }
    }

    suspend fun getCustomerPoints(customerId: String): Int {
        return ticketRepository.getCustomerPoints(customerId)
    }

    fun deductCustomerPoints(customerId: String, points: Int) {
        viewModelScope.launch {
            try {
                ticketRepository.deductCustomerPoints(customerId, points)
                Log.d("TicketViewModel", "Deducted $points points for customer $customerId")
            } catch (e: Exception) {
                Log.e("TicketViewModel", "Failed to deduct points: ${e.message}", e)
            }
        }
    }
}