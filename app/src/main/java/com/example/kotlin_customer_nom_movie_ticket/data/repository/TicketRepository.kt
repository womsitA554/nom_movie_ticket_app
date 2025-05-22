package com.example.kotlin_customer_nom_movie_ticket.data.repository

import android.util.Log
import com.example.kotlin_customer_nom_movie_ticket.data.model.Booking
import com.example.kotlin_customer_nom_movie_ticket.data.model.FoodBooking
import com.example.kotlin_customer_nom_movie_ticket.data.model.PointTransaction
import com.example.kotlin_customer_nom_movie_ticket.util.CartManager
import com.example.kotlin_customer_nom_movie_ticket.viewmodel.SeatViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

class TicketRepository @Inject constructor() {

    private val db = FirebaseDatabase.getInstance().reference
    private val dbFoodBooking = FirebaseDatabase.getInstance().getReference("FoodBookings")
    private val seatViewModel = SeatViewModel()

    suspend fun getBookingsByCustomerId(customerId: String): Result<List<Booking>> = withContext(Dispatchers.IO) {
        try {
            val billsSnapshot = db.child("Bills")
                .orderByChild("customer_id")
                .equalTo(customerId)
                .get()
                .await()

            val bookings = coroutineScope {
                billsSnapshot.children.mapNotNull { billSnapshot ->
                    async {
                        try {
                            buildBookingFromBillSnapshot(billSnapshot)
                        } catch (e: Exception) {
                            Log.e("TicketRepository", "Error processing bill: ${billSnapshot.key}", e)
                            null
                        }
                    }
                }.awaitAll().filterNotNull()
            }

            Result.success(bookings)
        } catch (e: Exception) {
            Log.e("TicketRepository", "Error fetching bookings for customerId: $customerId", e)
            Result.failure(e)
        }
    }

    private suspend fun buildBookingFromBillSnapshot(billSnapshot: DataSnapshot): Booking? = coroutineScope {
        val billId = billSnapshot.child("bill_id").getValue(String::class.java) ?: return@coroutineScope null
        val paymentStatus = billSnapshot.child("payment_status").getValue(String::class.java) ?: return@coroutineScope null
        if (paymentStatus != "Paid") return@coroutineScope null

        val paymentMethod = billSnapshot.child("payment_method").getValue(String::class.java)
        val seatPrice = billSnapshot.child("seat_price").getValue(Double::class.java) ?: 0.0
        val foodPrice = billSnapshot.child("food_price").getValue(Double::class.java) ?: 0.0
        val fee = billSnapshot.child("fee_price").getValue(Double::class.java) ?: 0.0
        val discount = billSnapshot.child("discount").getValue(Double::class.java) ?: 0.0
        val totalPrice = billSnapshot.child("total_amount").getValue(Double::class.java) ?: 0.0

        val billDetailsSnapshot = db.child("BillDetails")
            .orderByChild("bill_id")
            .equalTo(billId)
            .get()
            .await()

        val ticketIds = billDetailsSnapshot.children.mapNotNull { detail ->
            detail.child("ticket_id").getValue(String::class.java)
        }
        if (ticketIds.isEmpty()) return@coroutineScope null

        val firstTicketId = ticketIds.first()
        val ticketDeferred = async { db.child("Tickets").child(firstTicketId).get().await() }
        val seatsDeferred = async { fetchSeatNames(ticketIds) }

        val ticketSnapshot = ticketDeferred.await()
        val cinemaId = ticketSnapshot.child("cinema_id").getValue(String::class.java) ?: return@coroutineScope null
        val showtimeId = ticketSnapshot.child("showtime_id").getValue(String::class.java) ?: return@coroutineScope null

        val cinemaDeferred = async { db.child("Cinemas").child(cinemaId).get().await() }
        val showtimeDeferred = async { db.child("Showtimes").child(showtimeId).get().await() }

        val cinemaSnapshot = cinemaDeferred.await()
        val showtimeSnapshot = showtimeDeferred.await()

        val cinemaName = cinemaSnapshot.child("cinema_name").getValue(String::class.java) ?: "Unknown"
        val movieId = showtimeSnapshot.child("movie_id").getValue(String::class.java) ?: return@coroutineScope null
        val showtimeTime = showtimeSnapshot.child("showtime_time").getValue(String::class.java)
        val roomId = showtimeSnapshot.child("room_id").getValue(String::class.java)

        val movieDeferred = async { db.child("Movies").child(movieId).get().await() }
        val roomDeferred = async { roomId?.let { db.child("Rooms").child(it).get().await() } }

        val movieSnapshot = movieDeferred.await()
        val roomSnapshot = roomDeferred.await()

        val movieTitle = movieSnapshot.child("title").getValue(String::class.java) ?: "Unknown"
        val movieDuration = movieSnapshot.child("duration").getValue(Int::class.java) ?: 0
        val movieAgeRating = movieSnapshot.child("age_rating").getValue(String::class.java) ?: "N/A"
        val posterUrl = movieSnapshot.child("poster_url").getValue(String::class.java) ?: ""
        val directorId = movieSnapshot.child("director_id").getValue(String::class.java)
        val genre = movieSnapshot.child("genre").getValue(String::class.java)
        val roomName = roomSnapshot?.child("room_name")?.getValue(String::class.java) ?: "Unknown"

        val directorDeferred = async { directorId?.let { db.child("Directors").child(it).get().await() } }
        val directorSnapshot = directorDeferred.await()
        val directorName = directorSnapshot?.child("name")?.getValue(String::class.java) ?: "Unknown"

        val seatNames = seatsDeferred.await()

        return@coroutineScope Booking(
            bill_id = billId,
            movie_id = movieId,
            title = movieTitle,
            age_rating = movieAgeRating,
            cinema_name = cinemaName,
            showtime_time = showtimeTime,
            seat_ids = seatNames,
            duration = movieDuration,
            poster_url = posterUrl,
            director = directorName,
            genre = genre,
            room_name = roomName,
            seat_price = seatPrice,
            food_price = foodPrice,
            convenience_fee = fee,
            discount = discount,
            total_price = totalPrice,
            payment_method = paymentMethod,
            payment_status = paymentStatus
        )
    }

    private suspend fun fetchSeatNames(ticketIds: List<String>): List<String> = coroutineScope {
        ticketIds.mapNotNull { ticketId ->
            async {
                try {
                    val ticketSnapshot = db.child("Tickets").child(ticketId).get().await()
                    val seatId = ticketSnapshot.child("seat_id").getValue(String::class.java) ?: return@async null
                    val seatSnapshot = db.child("Seats").child(seatId).get().await()
                    val seatRow = seatSnapshot.child("row_number").getValue(String::class.java)
                    val seatNumber = seatSnapshot.child("seat_number").getValue(String::class.java)
                    if (seatRow != null && seatNumber != null) "$seatRow$seatNumber" else null
                } catch (e: Exception) {
                    Log.e("TicketRepository", "Error fetching seat for ticketId: $ticketId", e)
                    null
                }
            }
        }.awaitAll().filterNotNull()
    }

    suspend fun getFoodBookingByCustomerId(userId: String): List<FoodBooking> {
        return try {
            val snapshot = dbFoodBooking.orderByChild("customer_id").equalTo(userId).get().await()
            val foodBookings = mutableListOf<FoodBooking>()
            for (foodBookingSnapshot in snapshot.children) {
                val foodBooking = foodBookingSnapshot.getValue(FoodBooking::class.java)
                foodBooking?.let { foodBookings.add(it) }
            }
            Log.d("FoodBookings", "Food bookings retrieved: $foodBookings")
            foodBookings
        } catch (e: Exception) {
            Log.e("FoodBookings", "Error getting food bookings: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun saveFoodBooking(foodBookingData: HashMap<String, Any?>, foodBillId: String) {
        dbFoodBooking.child(foodBillId).setValue(foodBookingData).await()

        val customerId = foodBookingData["customer_id"] as? String ?: return
        val totalPrice = foodBookingData["total_price"] as? Double ?: return
        addPointsForPayment(customerId, foodBillId, totalPrice)
        Log.d("SaveFoodBooking", "FoodBooking $foodBillId saved successfully")
    }

    suspend fun savePaymentData(
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
    ): String? {
        return try {
            val pricePerSeat = 12.0
            val createdAt = System.currentTimeMillis()
            val billId = "B${createdAt}"

            ticketIds.forEach { ticketId ->
                val ticketUpdate = hashMapOf<String, Any>("payment_status" to "Paid")
                db.child("Tickets").child(ticketId)
                    .updateChildren(ticketUpdate)
                    .await()
                Log.d("UpdateTicket", "Ticket $ticketId updated to Paid")
            }

            val billData = hashMapOf(
                "bill_id" to billId,
                "customer_id" to userId,
                "seat_price" to totalPriceSeats,
                "food_price" to totalPriceFood,
                "fee_price" to fee,
                "discount" to discount,
                "total_amount" to actualPay,
                "payment_method" to "Visa",
                "payment_status" to "Đã thanh toán",
                "created_at" to createdAt,
                "promotion_id" to null
            )
            db.child("Bills").child(billId).setValue(billData).await()
            Log.d("SaveBill", "Bill $billId saved successfully")

            ticketIds.forEachIndexed { index, ticketId ->
                val detailId = "D${System.currentTimeMillis()}$index"
                val billDetailData = hashMapOf(
                    "detail_id" to detailId,
                    "bill_id" to billId,
                    "ticket_id" to ticketId,
                    "seat_id" to selectedSeatIds[index],
                    "price" to pricePerSeat
                )
                db.child("BillDetails").child(detailId).setValue(billDetailData).await()
                Log.d("SaveBillDetail", "BillDetail $detailId saved successfully")
            }

            // Update seat status
            selectedSeatIds.forEach { seatId ->
                seatViewModel.updateSeatBookedStatus(
                    showtimeId,
                    seatId,
                    "booked",
                    userId
                ) { success ->
                    if (success) {
                        Log.d("UpdateSeatStatus", "Seat $seatId updated to booked")
                    } else {
                        Log.e("UpdateSeatStatus", "Failed to update seat $seatId to booked")
                    }
                }
            }

            val cartItems = cartManager.getCart(userId)
            if (cartItems.isNotEmpty()) {
                val foodBillId = "FB${createdAt}"
                val foodBookingData: HashMap<String, Any?> = hashMapOf(
                    "food_bill_id" to foodBillId,
                    "bill_id" to billId,
                    "customer_id" to userId,
                    "food_items" to cartItems.map { it.toMap() },
                    "total_price" to totalPriceFood,
                    "payment_method" to "Visa",
                    "payment_status" to "Đã thanh toán",
                    "order_time" to createdAt,
                    "pick_up_time" to pickUpTime
                )
                saveFoodBooking(foodBookingData, foodBillId)
            }

            addPointsForPayment(userId, billId, actualPay)

            billId
        } catch (e: Exception) {
            Log.e("SavePaymentData", "Failed to save payment data: ${e.message}", e)
            null
        }
    }

    suspend fun addPointsForPayment(customerId: String, billId: String, totalAmount: Double) {
        try {
            val pointsEarned = (totalAmount * 0.05).toInt()

            val transactionId = "PT${System.currentTimeMillis()}"
            val pointTransaction = hashMapOf(
                "transaction_id" to transactionId,
                "customer_id" to customerId,
                "points" to pointsEarned,
                "type" to "earned",
                "amount" to totalAmount,
                "description" to "Points earned from bill $billId",
                "created_at" to System.currentTimeMillis()
            )

            db.child("PointTransactions")
                .child(transactionId).setValue(pointTransaction).await()

            val customerRef = db.child("Customers").child(customerId)
            val customerSnapshot = customerRef.get().await()
            val currentPoints = customerSnapshot.child("point").getValue(Int::class.java) ?: 0
            val newPoints = currentPoints + pointsEarned

            customerRef.child("point").setValue(newPoints).await()

            Log.d(
                "AddPoints",
                "Added $pointsEarned points for customer $customerId. New total: $newPoints"
            )
        } catch (e: Exception) {
            Log.e("AddPoints", "Failed to add points: ${e.message}", e)
            throw e
        }
    }

    suspend fun getPointTransactions(customerId: String): List<PointTransaction> {
        return try {
            val snapshot = db.child("PointTransactions")
                .orderByChild("customer_id")
                .equalTo(customerId)
                .get()
                .await()
            val transactions = mutableListOf<PointTransaction>()
            for (transactionSnapshot in snapshot.children) {
                val transaction = transactionSnapshot.getValue(PointTransaction::class.java)
                transaction?.let { transactions.add(it) }
            }
            Log.d(
                "PointTransactions",
                "Retrieved ${transactions.size} transactions for customer $customerId"
            )
            transactions
        } catch (e: Exception) {
            Log.e("PointTransactions", "Error getting point transactions: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun getCustomerPoints(customerId: String): Int {
        return try {
            val customerSnapshot = db.child("Customers").child(customerId).get().await()
            customerSnapshot.child("point").getValue(Int::class.java) ?: 0
        } catch (e: Exception) {
            Log.e("GetCustomerPoints", "Error getting customer points: ${e.message}", e)
            0
        }
    }

    suspend fun deductCustomerPoints(customerId: String, points: Int) {
        try {
            val customerRef = db.child("Customers").child(customerId)
            val customerSnapshot = customerRef.get().await()
            val currentPoints = customerSnapshot.child("point").getValue(Int::class.java) ?: 0
            val newPoints = maxOf(0, currentPoints - points)

            customerRef.child("point").setValue(newPoints).await()

            // Record point deduction transaction
            val transactionId = "PT${System.currentTimeMillis()}"
            val pointTransaction = hashMapOf(
                "transaction_id" to transactionId,
                "customer_id" to customerId,
                "points" to points,
                "type" to "redeemed",
                "amount" to (points * 0.01),
                "description" to "Points redeemed for discount",
                "created_at" to System.currentTimeMillis()
            )
            db.child("PointTransactions").child(transactionId).setValue(pointTransaction).await()

            Log.d(
                "DeductPoints",
                "Deducted $points points for customer $customerId. New total: $newPoints"
            )
        } catch (e: Exception) {
            Log.e("DeductPoints", "Failed to deduct points: ${e.message}", e)
            throw e
        }
    }
}