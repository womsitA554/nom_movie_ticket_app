package com.example.kotlin_customer_nom_movie_ticket.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.DiffUtil
import com.example.kotlin_customer_nom_movie_ticket.R
import com.example.kotlin_customer_nom_movie_ticket.data.model.Cinema
import com.example.kotlin_customer_nom_movie_ticket.databinding.AllCinemaItemBinding
import com.example.kotlin_customer_nom_movie_ticket.viewmodel.CinemaViewModel

class SuggestCinemaAdapter(
    private val cinemaViewModel: CinemaViewModel,
    private val favoriteCinemas: List<String>,
    private val userId: String
) : ListAdapter<Cinema, SuggestCinemaAdapter.CinemaViewHolder>(CinemaDiffCallback()) {

    var onClickItem: (Cinema, Int) -> Unit = { _, _ -> }

    class CinemaDiffCallback : DiffUtil.ItemCallback<Cinema>() {
        override fun areItemsTheSame(oldItem: Cinema, newItem: Cinema): Boolean {
            return oldItem.cinema_id == newItem.cinema_id
        }

        override fun areContentsTheSame(oldItem: Cinema, newItem: Cinema): Boolean {
            return oldItem == newItem
        }
    }

    inner class CinemaViewHolder(private val binding: AllCinemaItemBinding) :
        androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root) {
        fun bind(cinema: Cinema, favoriteCinemas: List<String>) {
            binding.tvCinemaName.text = cinema.cinema_name
            val isFavorite = favoriteCinemas.contains(cinema.cinema_id)
            updateFavoriteIcon(isFavorite)

            binding.btnCinema.setOnClickListener {
                onClickItem.invoke(cinema, adapterPosition)
            }

            binding.btnFavorite.setOnClickListener {
                cinemaViewModel.toggleFavoriteCinema(userId, cinema.cinema_id, isFavorite) { success ->
                    if (success) {
                        updateFavoriteIcon(!isFavorite)
                    }
                }
            }
        }

        private fun updateFavoriteIcon(isFavorite: Boolean) {
            binding.btnFavorite.setImageResource(
                if (isFavorite) R.drawable.star_filled_icon else R.drawable.star_outline_icon
            )
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CinemaViewHolder {
        val binding = AllCinemaItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CinemaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CinemaViewHolder, position: Int) {
        holder.bind(getItem(position), favoriteCinemas)
    }
}