package com.example.kotlin_customer_nom_movie_ticket.data.repository

import Food
import android.util.Log
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
                    food?.let {
                        Log.d("FoodAndDrinkRepository", "PopularFood: ${it.title}, itemId: ${it.itemId}, isAvailable: ${it.isAvailable}")
                        foods.add(it)
                    }
                }
                callback(foods)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FoodAndDrinkRepository", "Failed to fetch popular foods: ${error.message}")
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
                    food?.let {
                        val isAvailable = data.child("isAvailable").getValue(Boolean::class.java)
                        Log.d("FoodAndDrinkRepository", "Food: ${it.title}, itemId: ${it.itemId}, isAvailable: $isAvailable")
                        foods.add(it.copy(isAvailable = isAvailable))
                    }
                }
                callback(foods)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FoodAndDrinkRepository", "Failed to fetch foods: ${error.message}")
                callback(emptyList())
            }
        })
    }

    fun getAllDrink(callback: (List<Food>) -> Unit) {
        dbDrink.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val foods = mutableListOf<Food>()
                for (data in snapshot.children) {
                    val food = data.getValue(Food::class.java)
                    food?.let {
                        val isAvailable = data.child("isAvailable").getValue(Boolean::class.java)
                        Log.d("FoodAndDrinkRepository", "Food: ${it.title}, itemId: ${it.itemId}, isAvailable: $isAvailable")
                        foods.add(it.copy(isAvailable = isAvailable))
                    }
                }
                callback(foods)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FoodAndDrinkRepository", "Failed to fetch foods: ${error.message}")
                callback(emptyList())
            }
        })
    }

    fun getAllCombo(callback: (List<Food>) -> Unit) {
        dbCombo.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val foods = mutableListOf<Food>()
                for (data in snapshot.children) {
                    val food = data.getValue(Food::class.java)
                    food?.let {
                        val isAvailable = data.child("isAvailable").getValue(Boolean::class.java)
                        Log.d("FoodAndDrinkRepository", "Food: ${it.title}, itemId: ${it.itemId}, isAvailable: $isAvailable")
                        foods.add(it.copy(isAvailable = isAvailable))
                    }
                }
                callback(foods)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FoodAndDrinkRepository", "Failed to fetch foods: ${error.message}")
                callback(emptyList())
            }
        })
    }
}