package com.example.kotlin_customer_nom_movie_ticket.data.repository

import android.util.Log
import com.example.kotlin_customer_nom_movie_ticket.data.model.Cinema
import com.example.kotlin_customer_nom_movie_ticket.data.model.Food
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class FoodAndDrinkRepository {
    private val dbPopular = FirebaseDatabase.getInstance().getReference("PopularFoods")
    private val dbFood = FirebaseDatabase.getInstance().getReference("Foods")
    private val dbDrink = FirebaseDatabase.getInstance().getReference("Drinks")
    private val dbCombo = FirebaseDatabase.getInstance().getReference("ComboFoodAndDrink")

    fun getAllPopularFood(callback: (List<Food>) -> Unit) {
        dbPopular.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val foods = mutableListOf<Food>()
                for (data in snapshot.children) {
                    val food = data.getValue(Food::class.java)
                    food?.let { foods.add(it) }
                }
                callback(foods)
                Log.d("FoodAndDrinkRepository", "Fetched popular foods: $foods")
            }

            override fun onCancelled(error: DatabaseError) {
                callback(emptyList())
            }
        })
    }

    fun getAllFood(callback: (List<Food>) -> Unit) {
        dbFood.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val foods = mutableListOf<Food>()
                for (data in snapshot.children) {
                    val food = data.getValue(Food::class.java)
                    food?.let { foods.add(it) }
                }
                callback(foods)
            }

            override fun onCancelled(error: DatabaseError) {
                callback(emptyList())
            }
        })
    }

    fun getAllDrink(callback: (List<Food>) -> Unit) {
        dbDrink.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val drinks = mutableListOf<Food>()
                for (data in snapshot.children) {
                    val drink = data.getValue(Food::class.java)
                    drink?.let { drinks.add(it) }
                }
                callback(drinks)
            }

            override fun onCancelled(error: DatabaseError) {
                callback(emptyList())
            }
        })
    }

    fun getAllCombo(callback: (List<Food>) -> Unit) {
        dbCombo.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val combos = mutableListOf<Food>()
                for (data in snapshot.children) {
                    val combo = data.getValue(Food::class.java)
                    combo?.let { combos.add(it) }
                }
                callback(combos)
            }

            override fun onCancelled(error: DatabaseError) {
                callback(emptyList())
            }
        })
    }
}