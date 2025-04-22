package com.example.kotlin_customer_nom_movie_ticket.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.kotlin_customer_nom_movie_ticket.R
import com.example.kotlin_customer_nom_movie_ticket.data.model.Cinema
import com.example.kotlin_customer_nom_movie_ticket.databinding.AllCinemaItemBinding
import com.example.kotlin_customer_nom_movie_ticket.viewmodel.CinemaViewModel

class CinemaAdapter(
    private val listCinema: List<Cinema>,
    private val favoriteCinemas: List<String>,
    private val cinemaViewModel: CinemaViewModel,
    private val userId: String
) : RecyclerView.Adapter<CinemaAdapter.CinemaViewHolder>() {
    var onClickItem: (Cinema, Int) -> Unit = { _, _ -> }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CinemaViewHolder {
        val binding = AllCinemaItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CinemaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CinemaViewHolder, position: Int) {
        holder.onBind(listCinema[position])
    }

    override fun getItemCount(): Int = listCinema.size

    inner class CinemaViewHolder(private val binding: AllCinemaItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun onBind(cinema: Cinema) {
            binding.tvCinemaName.text = cinema.cinema_name

            val isFavorite = favoriteCinemas.contains(cinema.cinema_id)
            updateFavoriteIcon(isFavorite)

            binding.btnCinema.setOnClickListener {
                onClickItem.invoke(listCinema[adapterPosition], adapterPosition)
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
            if (isFavorite) {
                binding.btnFavorite.setImageResource(R.drawable.star_filled_icon)
            } else {
                binding.btnFavorite.setImageResource(R.drawable.star_outline_icon)
            }
        }
    }
}