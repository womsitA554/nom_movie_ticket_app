package com.example.kotlin_customer_nom_movie_ticket.data.repository

import android.util.Log
import com.example.kotlin_customer_nom_movie_ticket.data.model.Customer
import com.google.firebase.database.FirebaseDatabase

class ProfileRepository {
    private val dbCustomer = FirebaseDatabase.getInstance().getReference("Customers")

    fun getCustomerById(customerId: String, callback: (Customer) -> Unit) {
        dbCustomer.child(customerId).get().addOnSuccessListener { snapshot ->
            val customer = snapshot.getValue(Customer::class.java)
            customer?.let { callback(it) }
        }.addOnFailureListener { exception ->
            Log.e("ProfileRepository", "Error getting customer data", exception)
        }
    }
}