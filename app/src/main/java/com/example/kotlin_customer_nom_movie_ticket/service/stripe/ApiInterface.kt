package com.example.kotlin_customer_nom_movie_ticket.service.stripe

import com.example.kotlin_customer_nom_movie_ticket.viewmodel.CustomerModel
import com.example.kotlin_customer_nom_movie_ticket.viewmodel.EphemeralKeyModel
import com.example.kotlin_customer_nom_movie_ticket.viewmodel.PaymentIntentModel
import retrofit2.Call
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiInterface {
    @Headers("Authorization: Bearer ${Utils.SECRET_KEY}")
    @POST("v1/customers")
    fun getCustomer(): Call<CustomerModel>

    @Headers("Authorization: Bearer ${Utils.SECRET_KEY}", "stripe-version: 2024-06-20")
    @POST("v1/ephemeral_keys")
    fun getEphemeralKey(@Query("customer") customer: String, ): Call<EphemeralKeyModel>

    @Headers("Authorization: Bearer ${Utils.SECRET_KEY}")
    @POST("v1/payment_intents")
    fun getPaymentIntents(
        @Query("customer") customer: String,
        @Query("amount") amount: String,
        @Query("currency") currency: String = "usd",
        @Query("automatic_payment_methods[enabled]") automatePay: Boolean = true,
    ): Call<PaymentIntentModel>

}