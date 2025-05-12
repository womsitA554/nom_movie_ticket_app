package com.example.kotlin_customer_nom_movie_ticket.ui.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.kotlin_customer_nom_movie_ticket.R
import com.example.kotlin_customer_nom_movie_ticket.data.model.ArenaItem
import com.example.kotlin_customer_nom_movie_ticket.data.model.Cinema
import com.example.kotlin_customer_nom_movie_ticket.databinding.AllCinemaItemBinding
import com.example.kotlin_customer_nom_movie_ticket.viewmodel.CinemaViewModel

class CinemaAdapter(
    private val cinemaViewModel: CinemaViewModel,
    private val favoriteCinemas: List<String>,
    private val userId: String
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = mutableListOf<Any>()
    var onClickItem: (Cinema, Int) -> Unit = { _, _ -> }
    fun updateData(cinemas: List<Cinema>) {
        items.clear()
        val groupedByArena = cinemas.groupBy { it.arena }
        val arenaItems = groupedByArena.map { (arenaName, cinemaList) ->
            ArenaItem(arenaName, cinemaList)
        }.sortedBy { it.arenaName }
        items.addAll(arenaItems)
        notifyDataSetChanged()
    }

    companion object {
        private const val TYPE_ARENA = 0
        private const val TYPE_CINEMA = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is ArenaItem -> TYPE_ARENA
            is Cinema -> TYPE_CINEMA
            else -> throw IllegalArgumentException("Invalid item type")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_ARENA -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.arena_item, parent, false)
                ArenaViewHolder(view)
            }
            TYPE_CINEMA -> {
                val binding = AllCinemaItemBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                CinemaViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ArenaViewHolder -> {
                val arenaItem = items[position] as ArenaItem
                holder.bind(arenaItem, position)
            }
            is CinemaViewHolder -> {
                val cinema = items[position] as Cinema
                holder.bind(cinema, favoriteCinemas)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    inner class ArenaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvArenaName: TextView = itemView.findViewById(R.id.tvArenaName)
        private val tvQuantityArena: TextView = itemView.findViewById(R.id.tvQuantityArena)
        private val ivArenaUp: ImageView = itemView.findViewById(R.id.ivArenaUp)
        private val ivArenaDown: ImageView = itemView.findViewById(R.id.ivArenaDown)
        fun bind(arenaItem: ArenaItem, position: Int) {
            tvArenaName.text = arenaItem.arenaName
            tvQuantityArena.text = arenaItem.cinemas.size.toString()
            itemView.setOnClickListener {
                toggleCinemas(arenaItem)
                if (arenaItem.isExpanded) {
                    ivArenaUp.visibility = View.VISIBLE
                    ivArenaDown.visibility = View.GONE
                } else {
                    ivArenaUp.visibility = View.GONE
                    ivArenaDown.visibility = View.VISIBLE
                }
            }
        }
    }

    inner class CinemaViewHolder(private val binding: AllCinemaItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
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
            if (isFavorite) {
                binding.btnFavorite.setImageResource(R.drawable.star_filled_icon)
            } else {
                binding.btnFavorite.setImageResource(R.drawable.star_outline_icon)
            }
        }
    }

    private fun toggleCinemas(arenaItem: ArenaItem) {
        val position = items.indexOf(arenaItem)
        if (position == -1) return

        val startIndex = position + 1
        arenaItem.isExpanded = !arenaItem.isExpanded

        if (arenaItem.isExpanded) {
            items.addAll(startIndex, arenaItem.cinemas)
            notifyItemRangeInserted(startIndex, arenaItem.cinemas.size)
        } else {
            val cinemaCount = arenaItem.cinemas.size
            for (i in 0 until cinemaCount) {
                if (startIndex < items.size && items[startIndex] is Cinema) {
                    items.removeAt(startIndex)
                }
            }
            notifyItemRangeRemoved(startIndex, cinemaCount)
        }
    }
}
