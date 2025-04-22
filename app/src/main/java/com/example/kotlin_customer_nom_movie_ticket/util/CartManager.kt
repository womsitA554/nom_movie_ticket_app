package com.example.kotlin_customer_nom_movie_ticket.util

import android.content.Context
import com.example.kotlin_customer_nom_movie_ticket.data.model.Cart
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class CartManager(context: Context) {

    private val sharedPreferences =
        context.getSharedPreferences("cartPreferences", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun getCart(userId: String): MutableList<Cart> {
        val json = sharedPreferences.getString("cart_$userId", null)
        val type = object : TypeToken<MutableList<Cart>>() {}.type
        return if (json != null) gson.fromJson(json, type) else mutableListOf()
    }

    private fun saveCart(userId: String, cart: MutableList<Cart>) {
        val editor = sharedPreferences.edit()
        val json = gson.toJson(cart)
        editor.putString("cart_$userId", json)
        editor.apply()
    }

    fun addItemToCart(userId: String, item: Cart, quantity: Int) {
        val cart = getCart(userId)
        val existingItem = cart.find { it.title == item.title }
        if (existingItem != null) {
            existingItem.quantity = existingItem.quantity!! + quantity!!
            val eachPrice = item.price!! / quantity!!
            existingItem.price = existingItem.price?.plus((eachPrice * quantity))
        } else {
            cart.add(item)
        }
        saveCart(userId, cart)
    }

    fun removeItemFromCart(userId: String, itemId: String) {
        val cart = getCart(userId)
        cart.removeAll { it.itemId == itemId }
        saveCart(userId, cart)
    }

    fun clearCart(userId: String) {
        saveCart(userId, mutableListOf())
    }

    fun updateQuantity(userId: String, itemId: String, newQuantity: Int) {
        val cart = getCart(userId)

        val item = cart.find { it.itemId == itemId }

        if (item != null) {
            item.quantity = newQuantity

            saveCart(userId, cart)
        }
    }

    fun updatePrice(userId: String, itemId: String, newPrice: Double) {
        val cart = getCart(userId)

        val item = cart.find { it.itemId == itemId }

        if (item != null) {
            item.price = newPrice

            saveCart(userId, cart)
        }
    }

    fun countItem(userId: String): Int {
        val cart = getCart(userId)
        return cart.size
    }

    fun totalPriceOfCart(userId: String): Double {
        val cart = getCart(userId)
        return cart.sumOf { it.price ?: 0.0 }
    }

}