package com.example.kotlin_customer_nom_movie_ticket.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.kotlin_customer_nom_movie_ticket.data.model.Banner
import com.example.kotlin_customer_nom_movie_ticket.data.repository.HomeRepository

class BannerViewModel : ViewModel() {
    private val homeRepository = HomeRepository()
    private val _banners = MutableLiveData<List<Banner>>()
    val banners: LiveData<List<Banner>> get() = _banners

    fun fetchBanners(){
        homeRepository.getBanners { bannerList ->
            _banners.value = bannerList
        }
    }
}