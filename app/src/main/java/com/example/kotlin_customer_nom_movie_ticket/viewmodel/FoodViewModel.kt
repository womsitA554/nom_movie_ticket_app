package com.example.kotlin_customer_nom_movie_ticket.viewmodel

import Food
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.kotlin_customer_nom_movie_ticket.data.repository.FoodAndDrinkRepository

class FoodViewModel : ViewModel() {
    private val foodAndDrinkRepository = FoodAndDrinkRepository()
    private val _food = MutableLiveData<List<Food>>()
    private val _drink = MutableLiveData<List<Food>>()
    private val _combo = MutableLiveData<List<Food>>()
    val food: MutableLiveData<List<Food>> get() = _food
    val drink: MutableLiveData<List<Food>> get() = _drink
    val combo: MutableLiveData<List<Food>> get() = _combo

    fun fetchAllFood() {
        foodAndDrinkRepository.getAllFood { foodList ->
            foodList.forEach {
                Log.d("FoodViewModel", "Food: ${it.title}, itemId: ${it.itemId}, isAvailable: ${it.isAvailable}")
            }
            _food.value = foodList
        }
    }

    fun fetchAllDrink() {
        foodAndDrinkRepository.getAllDrink { drinkList ->
            drinkList.forEach {
                Log.d("FoodViewModel", "Drink: ${it.title}, itemId: ${it.itemId}, isAvailable: ${it.isAvailable}")
            }
            _drink.value = drinkList
        }
    }

    fun fetchAllCombo() {
        foodAndDrinkRepository.getAllCombo { comboList ->
            comboList.forEach {
                Log.d("FoodViewModel", "Combo: ${it.title}, itemId: ${it.itemId}, isAvailable: ${it.isAvailable}")
            }
            _combo.value = comboList
        }
    }
}