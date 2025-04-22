package com.example.kotlin_customer_nom_movie_ticket.data.repository

import com.example.kotlin_customer_nom_movie_ticket.data.model.Seat
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class SeatRepository {
    private val dbSeat = FirebaseDatabase.getInstance().getReference("Seats")
    private val dbSeatsBooking = FirebaseDatabase.getInstance().getReference("SeatBookings")
    private var bookingListener: ValueEventListener? = null

    // Lấy danh sách ghế với trạng thái từ SeatBookings
    fun getSeatByRoomId(roomId: String, showtimeId: String?, callback: (List<Seat>, Map<String, String?>) -> Unit) {
        dbSeat.orderByChild("room_id").equalTo(roomId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(seatsSnapshot: DataSnapshot) {
                    val seats = mutableListOf<Seat>()
                    val seatMap = mutableMapOf<String, Seat>()

                    for (data in seatsSnapshot.children) {
                        val seat = data.getValue(Seat::class.java)?.copy(status = "available")
                        seat?.let {
                            seats.add(it)
                            seatMap[seat.seat_id] = it
                        }
                    }

                    if (showtimeId == null || seats.isEmpty()) {
                        val sortedSeats = seats.sortedWith(compareBy({ it.row_number }, { it.seat_number.toInt() }))
                        callback(sortedSeats, emptyMap())
                        return
                    }

                    // Lắng nghe thay đổi thời gian thực trong SeatBookings
                    bookingListener = object : ValueEventListener {
                        override fun onDataChange(bookingSnapshot: DataSnapshot) {
                            val userIds = mutableMapOf<String, String?>()
                            for (bookingData in bookingSnapshot.children) {
                                val seatId = bookingData.child("seat_id").getValue(String::class.java)
                                val status = bookingData.child("status").getValue(String::class.java)
                                val userId = bookingData.child("customer_id").getValue(String::class.java)
                                seatId?.let {
                                    seatMap[seatId]?.status = status ?: "available"
                                    if (status == "reserved") {
                                        userIds[seatId] = userId
                                    }
                                }
                            }
                            val sortedSeats = seats.sortedWith(compareBy({ it.row_number }, { it.seat_number.toInt() }))
                            callback(sortedSeats, userIds)
                        }

                        override fun onCancelled(bookingError: DatabaseError) {
                            val sortedSeats = seats.sortedWith(compareBy({ it.row_number }, { it.seat_number.toInt() }))
                            callback(sortedSeats, emptyMap())
                        }
                    }
                    dbSeatsBooking.orderByChild("showtime_id").equalTo(showtimeId)
                        .addValueEventListener(bookingListener!!)
                }

                override fun onCancelled(seatsError: DatabaseError) {
                    callback(emptyList(), emptyMap())
                }
            })
    }

    fun removeSeatListener(showtimeId: String?) {
        bookingListener?.let {
            if (showtimeId != null) {
                dbSeatsBooking.orderByChild("showtime_id").equalTo(showtimeId).removeEventListener(it)
            }
            bookingListener = null
        }
    }

    // Cập nhật trạng thái ghế
    fun updateSeatStatus(showtimeId: String, seatId: String, status: String, userId: String, callback: (Boolean) -> Unit) {
        dbSeatsBooking.orderByChild("showtime_id").equalTo(showtimeId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val booking = snapshot.children.find {
                        it.child("seat_id").getValue(String::class.java) == seatId
                    }
                    if (booking != null) {
                        val updates = if (status == "available") {
                            mapOf("status" to status, "customer_id" to null)
                        } else {
                            mapOf("status" to status, "customer_id" to userId)
                        }
                        booking.ref.updateChildren(updates)
                            .addOnSuccessListener { callback(true) }
                            .addOnFailureListener { callback(false) }
                    } else if (status != "available") {
                        val newBookingRef = dbSeatsBooking.push()
                        val bookingData = mapOf(
                            "seat_id" to seatId,
                            "showtime_id" to showtimeId,
                            "status" to status,
                            "customer_id" to userId
                        )
                        newBookingRef.setValue(bookingData)
                            .addOnSuccessListener { callback(true) }
                            .addOnFailureListener { callback(false) }
                    } else {
                        callback(true)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    callback(false)
                }
            })
    }

    fun updateSeatBookedStatus(showtimeId: String, seatId: String, status: String, userId: String, callback: (Boolean) -> Unit) {
        dbSeatsBooking.orderByChild("showtime_id").equalTo(showtimeId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val booking = snapshot.children.find {
                        it.child("seat_id").getValue(String::class.java) == seatId
                    }
                    if (booking != null) {
                        val updates = mapOf("status" to status, "customer_id" to userId)
                        booking.ref.updateChildren(updates)
                            .addOnSuccessListener { callback(true) }
                            .addOnFailureListener { callback(false) }
                    } else {
                        callback(false)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    callback(false)
                }
            })
    }

    // Lấy userId của ghế với index
    fun getSeatUserId(showtimeId: String, seatId: String, callback: (String?) -> Unit) {
        dbSeatsBooking.orderByChild("showtime_id").equalTo(showtimeId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val booking = snapshot.children.find {
                        it.child("seat_id").getValue(String::class.java) == seatId
                    }
                    val userId = booking?.child("customer_id")?.getValue(String::class.java)
                    callback(userId)
                }

                override fun onCancelled(error: DatabaseError) {
                    callback(null)
                }
            })
    }

    fun resetSeatsByUserId(showtimeId: String, userId: String, callback: (Boolean) -> Unit) {
        dbSeatsBooking.orderByChild("showtime_id").equalTo(showtimeId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val updates = mutableMapOf<String, Any>()
                    for (bookingData in snapshot.children) {
                        val customerId = bookingData.child("customer_id").getValue(String::class.java)
                        val status = bookingData.child("status").getValue(String::class.java)
                        if (customerId == userId && status == "reserved") {
                            updates["${bookingData.key}/status"] = "available"
                            updates["${bookingData.key}/customer_id"] = null.toString()
                        }
                    }
                    if (updates.isNotEmpty()) {
                        dbSeatsBooking.updateChildren(updates)
                            .addOnSuccessListener { callback(true) }
                            .addOnFailureListener { callback(false) }
                    } else {
                        callback(true) // Không có ghế nào cần reset
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    callback(false)
                }
            })
    }
}