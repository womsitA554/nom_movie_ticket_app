package com.example.kotlin_customer_nom_movie_ticket.data.repository

import com.example.kotlin_customer_nom_movie_ticket.data.model.Customer
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class RegisterRepository {
    private val dbCustomer = FirebaseDatabase.getInstance().reference.child("Customers")

    fun getAllUserId(callback: (List<String>) -> Unit) {
        dbCustomer.get()
            .addOnSuccessListener { snapshot ->
                val userIds = snapshot.children.mapNotNull { it.key }
                callback(userIds)
            }
            .addOnFailureListener { exception ->
                println("Error getting user IDs: ${exception.message}")
                callback(emptyList())
            }
    }

    fun currentUserId(): String? {
        val currentUser = FirebaseAuth.getInstance().currentUser
        return currentUser?.uid
    }

    fun addUser(customer: Customer) {
        val userId = currentUserId()
        if (userId != null) {
            dbCustomer.child(userId).setValue(customer)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        println("Customer added successfully")
                    } else {
                        println("Failed to add user: ${task.exception?.message}")
                    }
                }
        } else {
            println("User ID is null")
        }
    }
}