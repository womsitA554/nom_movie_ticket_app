package com.example.kotlin_customer_nom_movie_ticket.data.repository

import android.util.Log
import com.example.kotlin_customer_nom_movie_ticket.data.model.Banner
import com.example.kotlin_customer_nom_movie_ticket.data.model.Customer
import com.example.kotlin_customer_nom_movie_ticket.data.model.Movie
import com.example.kotlin_customer_nom_movie_ticket.data.model.MovieStatus
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class HomeRepository {
    private val dbBanner = FirebaseDatabase.getInstance().getReference("Banners")
    private val dbMovie = FirebaseDatabase.getInstance().getReference("Movies")
    private val dbCustomer = FirebaseDatabase.getInstance().getReference("Customers")

    fun getAvatarCustomer(userId: String, callback: (Customer) -> Unit) {
        dbCustomer.child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val customer = snapshot.getValue(Customer::class.java)
                customer?.let { callback(it) }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("HomeRepository", "Firebase Error: ${error.message}")
            }
        })
    }

    fun getBanners(callback: (List<Banner>) -> Unit) {
        dbBanner.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val bannerList = mutableListOf<Banner>()
                for (bannerSnapshot in snapshot.children) {
                    val banner = bannerSnapshot.getValue(Banner::class.java)
                    banner?.let { bannerList.add(it) }
                }
                callback(bannerList)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("BannerRepository", "Firebase Error: ${error.message}")
            }
        })
    }

    fun getMovieIsShowing(callback: (List<Movie>) -> Unit) {
        dbMovie.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val movieList = mutableListOf<Movie>()
                for (bannerSnapshot in snapshot.children) {
                    val movies = bannerSnapshot.getValue(Movie::class.java)
                    if (movies?.status == MovieStatus.isShowing) {
                        movies.let { movieList.add(it) }
                    }
                }
                callback(movieList)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("BannerRepository", "Firebase Error: ${error.message}")
            }
        })
    }

    fun getMovieIsComing(callback: (List<Movie>) -> Unit) {
        dbMovie.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val movieList = mutableListOf<Movie>()
                for (bannerSnapshot in snapshot.children) {
                    val movies = bannerSnapshot.getValue(Movie::class.java)
                    if (movies?.status == MovieStatus.isComing) {
                        movies.let { movieList.add(it) }
                    }
                }
                callback(movieList)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("BannerRepository", "Firebase Error: ${error.message}")
            }
        })
    }
}