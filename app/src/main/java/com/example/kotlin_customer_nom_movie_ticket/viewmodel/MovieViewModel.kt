package com.example.kotlin_customer_nom_movie_ticket.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.kotlin_customer_nom_movie_ticket.data.model.Customer
import com.example.kotlin_customer_nom_movie_ticket.data.model.Movie
import com.example.kotlin_customer_nom_movie_ticket.data.model.Review
import com.example.kotlin_customer_nom_movie_ticket.data.repository.HomeRepository
import com.google.firebase.database.Query
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class MovieViewModel : ViewModel() {
    private val homeRepository = HomeRepository()
    private val _movieIsShowing = MutableLiveData<List<Movie>>()
    private val _movieIsComing = MutableLiveData<List<Movie>>()
    private val _avatarCustomer = MutableLiveData<Customer>()
    private val _favoriteStatus = MutableLiveData<Map<String, Boolean>>()
    private val _textReviews = MutableLiveData<List<Review>>()
    private val _hasMoreReviews = MutableLiveData<Boolean>()
    private var lastReviewTimestamp: Long? = null
    private var currentMovieId: String? = null
    private val REVIEWS_PER_PAGE = 5

    val movieIsShowing: LiveData<List<Movie>> get() = _movieIsShowing
    val movieIsComing: LiveData<List<Movie>> get() = _movieIsComing
    val avatarCustomer: LiveData<Customer> get() = _avatarCustomer
    val favoriteStatus: LiveData<Map<String, Boolean>> get() = _favoriteStatus
    val textReviews: LiveData<List<Review>> get() = _textReviews
    val hasMoreReviews: LiveData<Boolean> get() = _hasMoreReviews

    fun fetchMoviesIsShowing() {
        homeRepository.getMovieIsShowing { movieList ->
            _movieIsShowing.value = movieList
        }
    }

    fun fetchMoviesIsComing() {
        homeRepository.getMovieIsComing { movieList ->
            _movieIsComing.value = movieList
        }
    }

    fun fetchAvatarCustomer(userId: String) {
        homeRepository.getAvatarCustomer(userId) { avatar ->
            _avatarCustomer.value = avatar
        }
    }

    fun updateFavoriteStatus(movieId: String, isFavorite: Boolean) {
        val currentMap = _favoriteStatus.value?.toMutableMap() ?: mutableMapOf()
        currentMap[movieId] = isFavorite
        _favoriteStatus.value = currentMap
    }

    fun fetchRateText(movieId: String, isLoadMore: Boolean = false) {
        if (!isLoadMore) {
            lastReviewTimestamp = null
            currentMovieId = movieId
            _textReviews.value = emptyList()
        }

        val database = Firebase.database.reference
        val reviewsRef = database.child("Movies").child(movieId).child("text_ratings")
        var query: Query = reviewsRef.orderByChild("timestamp")

        if (isLoadMore && _textReviews.value?.isNotEmpty() == true) {
            val minTimestamp = _textReviews.value?.minByOrNull { it.timestamp ?: Long.MAX_VALUE }?.timestamp
            if (minTimestamp != null) {
                query = query.endBefore(minTimestamp.toDouble())
            }
        }

        query.limitToLast(REVIEWS_PER_PAGE)
            .addListenerForSingleValueEvent(object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                    val reviewList = mutableListOf<Review>()
                    val customerRef = database.child("Customers")
                    var processedCount = 0
                    val childrenCount = snapshot.children.count()

                    if (childrenCount == 0) {
                        _hasMoreReviews.postValue(false)
                        return
                    }

                    snapshot.children.forEach { reviewSnapshot ->
                        val review = reviewSnapshot.getValue(Review::class.java)?.copy(rating_id = reviewSnapshot.key!!)
                        review?.let {
                            customerRef.child(it.customer_id!!).get().addOnSuccessListener { customerSnapshot ->
                                val username = customerSnapshot.child("full_name").getValue(String::class.java)
                                val avatarUrl = customerSnapshot.child("avatar").getValue(String::class.java).toString()
                                reviewList.add(it.copy(
                                    full_name = username ?: "Người dùng ẩn danh",
                                    avatar = avatarUrl
                                ))
                                processedCount++

                                if (processedCount == childrenCount) {
                                    val sortedReviews = reviewList.sortedByDescending { it.timestamp }
                                    if (!isLoadMore) {
                                        _textReviews.postValue(sortedReviews)
                                    } else {
                                        val existingIds = (_textReviews.value ?: emptyList()).map { it.rating_id }.toSet()
                                        val newReviews = sortedReviews.filter { it.rating_id !in existingIds }
                                        val merged = (_textReviews.value ?: emptyList()) + newReviews
                                        _textReviews.postValue(merged.sortedByDescending { it.timestamp })
                                    }
                                    _hasMoreReviews.postValue(childrenCount >= REVIEWS_PER_PAGE)
                                }
                            }.addOnFailureListener { error ->
                                android.util.Log.e("MovieViewModel", "Error fetching customer data: ${error.message}")
                                processedCount++
                            }
                        }
                    }
                }

                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                    android.util.Log.e("MovieViewModel", "Error fetching reviews: ${error.message}")
                }
            })
    }}