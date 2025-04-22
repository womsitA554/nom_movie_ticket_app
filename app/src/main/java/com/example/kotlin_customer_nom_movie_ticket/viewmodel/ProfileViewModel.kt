package com.example.kotlin_customer_nom_movie_ticket.viewmodel

import androidx.lifecycle.MutableLiveData
import com.example.kotlin_customer_nom_movie_ticket.data.model.Customer
import com.example.kotlin_customer_nom_movie_ticket.data.repository.ProfileRepository

class ProfileViewModel {
    private val profileRepository = ProfileRepository()
    private val _customer = MutableLiveData<Customer>()
    val customer: MutableLiveData<Customer> get() = _customer

    fun fetchCustomerById(customerId: String) {
        profileRepository.getCustomerById(customerId) { customer ->
            _customer.value = customer
        }
    }
}