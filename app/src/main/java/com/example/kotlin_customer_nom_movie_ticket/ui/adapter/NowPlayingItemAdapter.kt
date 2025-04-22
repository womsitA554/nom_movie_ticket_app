package com.example.kotlin_customer_nom_movie_ticket.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.kotlin_customer_nom_movie_ticket.R
import com.example.kotlin_customer_nom_movie_ticket.data.model.Movie
import com.example.kotlin_customer_nom_movie_ticket.databinding.NowPlayingItemListBinding

class NowPlayingItemAdapter(private val list: List<Movie>, private val isGrid: Boolean = false) : RecyclerView.Adapter<NowPlayingItemAdapter.MovieViewHolder>() {
    var onClickItem: (Movie, Int) -> Unit = { _, _ -> }
    var onBookClick: (Movie, Int) -> Unit = { _, _ -> }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NowPlayingItemAdapter.MovieViewHolder {
        val binding = NowPlayingItemListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
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

    inner class MovieViewHolder(private val binding: NowPlayingItemListBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.picMovie.setOnClickListener {
                onClickItem.invoke(list[adapterPosition], adapterPosition)
            }
            binding.tvTitle.setOnClickListener {
                onClickItem.invoke(list[adapterPosition], adapterPosition)
            }
            binding.btnBookNow.setOnClickListener {
                onBookClick.invoke(list[adapterPosition], adapterPosition)
            }
        }

        fun onBind(movie: Movie) {
            binding.tvTitle.text = movie.title
            Glide.with(binding.picMovie.context).load(movie.poster_url).into(binding.picMovie)
        }
    }

    override fun onBindViewHolder(holder: NowPlayingItemAdapter.MovieViewHolder, position: Int) {
        holder.onBind(list[position])
    }

    override fun getItemCount(): Int {
        return list.size
    }
}