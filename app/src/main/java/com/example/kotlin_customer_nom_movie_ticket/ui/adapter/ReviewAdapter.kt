package com.example.kotlin_customer_nom_movie_ticket.ui.adapter

import android.content.Context
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.kotlin_customer_nom_movie_ticket.R
import com.example.kotlin_customer_nom_movie_ticket.data.model.Review
import com.example.kotlin_customer_nom_movie_ticket.databinding.RateTextItemBinding
import com.example.kotlin_customer_nom_movie_ticket.util.SessionManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class ReviewAdapter(
    private val context: Context,
    private var reviews: List<Review>,
    private val movieId: String
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var onLikeClick: (Review, Int, Boolean) -> Unit = { _, _, _ -> }
    var onDislikeClick: (Review, Int, Boolean) -> Unit = { _, _, _ -> }
    var onLoadMoreClick: () -> Unit = { }
    
    private val VIEW_TYPE_REVIEW = 0
    private val VIEW_TYPE_LOAD_MORE = 1
    private var isLoadingMore = false
    private var hasMoreReviews = true

    inner class ReviewViewHolder(private val binding: RateTextItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun onBind(review: Review) {
            // Bind data
            binding.name.text = review.full_name ?: "Người dùng ẩn danh"
            binding.content.text = review.content
            binding.likeCount.text = review.likes.toString()
            binding.dislikeCount.text = review.dislikes.toString()

            // Format timestamp
            val relativeTime = DateUtils.getRelativeTimeSpanString(
                review.timestamp,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS
            )
            binding.time.text = relativeTime

            // Load avatar
            Glide.with(context)
                .load(review.avatar ?: R.drawable.beyonce)
                .placeholder(R.drawable.beyonce)
                .into(binding.avatar)

            // Get current user ID
            val currentUserId = SessionManager.getUserId(context) ?: return
            // Check if the current user has liked or disliked
            val isLiked = review.liked_by?.containsKey(currentUserId) == true
            val isDisliked = review.disliked_by?.containsKey(currentUserId) == true

            // Update UI for like and dislike buttons
            updateUi(isLiked, isDisliked)

            // Handle like button click
            binding.btnLike.setOnClickListener {
                if (currentUserId != null) {
                    handleLikeClick(review, adapterPosition, isLiked, isDisliked)
                } else {
                    Toast.makeText(context, "Vui lòng đăng nhập để thích bình luận", Toast.LENGTH_SHORT).show()
                }
            }

            // Handle dislike button click
            binding.btnDislike.setOnClickListener {
                if (currentUserId != null) {
                    handleDislikeClick(review, adapterPosition, isLiked, isDisliked)
                } else {
                    Toast.makeText(context, "Vui lòng đăng nhập để không thích bình luận", Toast.LENGTH_SHORT).show()
                }
            }
        }

        private fun updateUi(isLiked: Boolean, isDisliked: Boolean) {
            // Update like button UI
            binding.btnLike.setImageResource(
                if (isLiked) R.drawable.heart_fill_icon else R.drawable.heart_outline_icon
            )
            // Update dislike button UI
            binding.btnDislike.setImageResource(
                if (isDisliked) R.drawable.dislike_icon else R.drawable.dislike_outline_icon
            )
        }
    }

    inner class LoadMoreViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val btnLoadMore: TextView = itemView.findViewById(R.id.btnLoadMore)

        init {
            btnLoadMore.setOnClickListener {
                if (!isLoadingMore) {
                    isLoadingMore = true
                    onLoadMoreClick()
                }
            }
        }
    }

    private fun handleLikeClick(review: Review, position: Int, isLiked: Boolean, isDisliked: Boolean) {
        val currentUserId = SessionManager.getUserId(context) ?: return
        val reviewRef = Firebase.database.reference
            .child("Movies")
            .child(movieId)
            .child("text_ratings")
            .child(review.rating_id!!)

        if (isLiked) {
            // User already liked, remove like
            val updates = mapOf(
                "likes" to review.likes - 1,
                "liked_by/$currentUserId" to null
            )
            reviewRef.updateChildren(updates).addOnSuccessListener {
                val updatedReview = review.copy(
                    likes = review.likes - 1,
                    liked_by = review.liked_by?.toMutableMap()?.apply { remove(currentUserId) }
                )
                updateReviews(reviews.toMutableList().apply { set(position, updatedReview) }, position)
                onLikeClick(updatedReview, position, false)
            }.addOnFailureListener {
                Toast.makeText(context, "Không thể cập nhật lượt thích", Toast.LENGTH_SHORT).show()
            }
        } else {
            // User hasn't liked, add like
            val updates = mutableMapOf<String, Any?>(
                "likes" to review.likes + 1,
                "liked_by/$currentUserId" to true
            )
            // If user has disliked, remove dislike
            if (isDisliked) {
                updates["dislikes"] = review.dislikes - 1
                updates["disliked_by/$currentUserId"] = null
            }
            reviewRef.updateChildren(updates).addOnSuccessListener {
                val updatedReview = review.copy(
                    likes = review.likes + 1,
                    liked_by = review.liked_by?.toMutableMap()?.apply { put(currentUserId, true) }
                        ?: mapOf(currentUserId to true),
                    dislikes = if (isDisliked) review.dislikes - 1 else review.dislikes,
                    disliked_by = if (isDisliked) review.disliked_by?.toMutableMap()?.apply { remove(currentUserId) } else review.disliked_by
                )
                updateReviews(reviews.toMutableList().apply { set(position, updatedReview) }, position)
                onLikeClick(updatedReview, position, true)
            }.addOnFailureListener {
                Toast.makeText(context, "Không thể cập nhật lượt thích", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleDislikeClick(review: Review, position: Int, isLiked: Boolean, isDisliked: Boolean) {
        val currentUserId = SessionManager.getUserId(context) ?: return
        val ratingId = review.rating_id ?: return
        
        val reviewRef = Firebase.database.reference
            .child("Movies")
            .child(movieId)
            .child("text_ratings")
            .child(ratingId)

        if (isDisliked) {
            val updates = mapOf(
                "dislikes" to review.dislikes - 1,
                "disliked_by/$currentUserId" to null
            )
            reviewRef.updateChildren(updates).addOnSuccessListener {
                val updatedReview = review.copy(
                    dislikes = review.dislikes - 1,
                    disliked_by = review.disliked_by?.toMutableMap()?.apply { remove(currentUserId) }
                )
                updateReviews(reviews.toMutableList().apply { set(position, updatedReview) }, position)
                onDislikeClick(updatedReview, position, false)
            }.addOnFailureListener {
                Toast.makeText(context, "Không thể cập nhật lượt không thích", Toast.LENGTH_SHORT).show()
            }
        } else {
            // User hasn't disliked, add dislike
            val updates = mutableMapOf<String, Any?>(
                "dislikes" to review.dislikes + 1,
                "disliked_by/$currentUserId" to true
            )
            // If user has liked, remove like
            if (isLiked) {
                updates["likes"] = review.likes - 1
                updates["liked_by/$currentUserId"] = null
            }
            reviewRef.updateChildren(updates).addOnSuccessListener {
                val updatedReview = review.copy(
                    dislikes = review.dislikes + 1,
                    disliked_by = (review.disliked_by?.toMutableMap() ?: mutableMapOf()).apply { put(currentUserId, true) },
                    likes = if (isLiked) review.likes - 1 else review.likes,
                    liked_by = if (isLiked) review.liked_by?.toMutableMap()?.apply { remove(currentUserId) } else review.liked_by
                )
                updateReviews(reviews.toMutableList().apply { set(position, updatedReview) }, position)
                onDislikeClick(updatedReview, position, true)
            }.addOnFailureListener {
                Toast.makeText(context, "Không thể cập nhật lượt không thích", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_REVIEW -> {
                val binding = RateTextItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                ReviewViewHolder(binding)
            }
            VIEW_TYPE_LOAD_MORE -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_load_more, parent, false)
                LoadMoreViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ReviewViewHolder -> {
                val review = reviews[position]
                holder.onBind(review)
            }
            is LoadMoreViewHolder -> {
                // No need to bind anything for load more view holder
            }
        }
    }

    override fun getItemCount(): Int = reviews.size + if (hasMoreReviews) 1 else 0

    override fun getItemViewType(position: Int): Int {
        return if (position == reviews.size) VIEW_TYPE_LOAD_MORE else VIEW_TYPE_REVIEW
    }

    fun updateReviews(newReviews: List<Review>, changedPosition: Int? = null) {
        reviews = newReviews
        if (changedPosition != null && changedPosition in reviews.indices) {
            notifyItemChanged(changedPosition)
        } else {
            notifyDataSetChanged()
        }
    }

    fun setLoadingMore(loading: Boolean) {
        isLoadingMore = loading
        if (loading) {
            notifyItemChanged(reviews.size)
        }
    }

    fun setHasMoreReviews(hasMore: Boolean) {
        hasMoreReviews = hasMore
        notifyDataSetChanged()
    }
}