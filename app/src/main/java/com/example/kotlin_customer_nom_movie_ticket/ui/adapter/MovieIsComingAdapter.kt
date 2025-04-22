package com.example.kotlin_customer_nom_movie_ticket.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.kotlin_customer_nom_movie_ticket.R
import com.example.kotlin_customer_nom_movie_ticket.data.model.Movie
import com.example.kotlin_customer_nom_movie_ticket.databinding.ComingSoonItemBinding
import com.example.kotlin_customer_nom_movie_ticket.util.SessionManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MovieIsComingAdapter(private val list: List<Movie>, private val isGrid: Boolean = false) :
    RecyclerView.Adapter<MovieIsComingAdapter.MovieViewHolder>() {
    var onClickItem: (Movie, Int, Boolean) -> Unit = { _, _, _ -> }
    var onFavoriteClick: (Movie, Int, Boolean) -> Unit = { _, _, _ -> }
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MovieIsComingAdapter.MovieViewHolder {
        val binding = ComingSoonItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        val layoutParams = binding.root.layoutParams
        if (!isGrid) {
            layoutParams.width = parent.context.resources.getDimensionPixelSize(R.dimen.item_width)
            layoutParams.height =
                parent.context.resources.getDimensionPixelSize(R.dimen.item_height_375)
        } else {
            layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
            layoutParams.height =
                parent.context.resources.getDimensionPixelSize(R.dimen.item_height_440)
        }

        return MovieViewHolder(binding)
    }

    inner class MovieViewHolder(private val binding: ComingSoonItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.picMovie.setOnClickListener {
                val isFavorite = binding.btnAddWatchlist.tag as? Boolean ?: false
                onClickItem.invoke(list[adapterPosition], adapterPosition, isFavorite)
            }
            binding.tvTitle.setOnClickListener {
                val isFavorite = binding.btnAddWatchlist.tag as? Boolean ?: false
                onClickItem.invoke(list[adapterPosition], adapterPosition, isFavorite)
            }
            binding.btnAddWatchlist.setOnClickListener {
                val movie = list[adapterPosition]
                val isCurrentlyFavorite = binding.btnAddWatchlist.tag as? Boolean ?: false
                val newFavoriteStatus = !isCurrentlyFavorite
                updateFavoriteUI(newFavoriteStatus)
                onFavoriteClick(movie, adapterPosition, newFavoriteStatus)
            }
        }

        fun onBind(movie: Movie) {
            binding.tvTitle.text = movie.title
            Glide.with(binding.picMovie.context).load(movie.poster_url).into(binding.picMovie)

            checkFavoriteStatus(movie.movie_id) { isFavorite ->
                updateFavoriteUI(isFavorite)
            }

        }

        private fun updateFavoriteUI(isFavorite: Boolean) {
            if (isFavorite) {
                binding.btnAddWatchlist.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.heart_fill_icon, 0, 0, 0
                )
            } else {
                binding.btnAddWatchlist.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.heart_outline_icon, 0, 0, 0
                )
            }
            binding.btnAddWatchlist.tag = isFavorite
        }

        private fun checkFavoriteStatus(movieId: String, callback: (Boolean) -> Unit) {
            val userId = SessionManager.getUserId(binding.root.context) ?: return
            FirebaseDatabase.getInstance().getReference("FavoriteMovies")
                .child(userId)
                .child(movieId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        callback(snapshot.getValue(Boolean::class.java) == true)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        callback(false)
                    }
                })
        }
    }

    override fun onBindViewHolder(holder: MovieIsComingAdapter.MovieViewHolder, position: Int) {
        holder.onBind(list[position])
    }

    override fun getItemCount(): Int {
        return list.size
    }
}