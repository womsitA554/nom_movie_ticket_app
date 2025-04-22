package com.example.kotlin_customer_nom_movie_ticket.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.example.kotlin_customer_nom_movie_ticket.R
import com.example.kotlin_customer_nom_movie_ticket.data.model.Movie
import com.example.kotlin_customer_nom_movie_ticket.databinding.NowPlayingItemBinding

class MovieIsShowingAdapter(
    private val list: ArrayList<Movie>,
    private val viewPager2: ViewPager2
) : RecyclerView.Adapter<MovieIsShowingAdapter.MovieViewHolder>() {
    var onClickItem: (Movie, Int) -> Unit = { _, _ -> }
    var onBookClick: (Movie, Int) -> Unit = { _, _ -> }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder {
        val binding = NowPlayingItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MovieViewHolder(binding)
    }

    inner class MovieViewHolder(private val binding: NowPlayingItemBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.picMovie.setOnClickListener {
                onClickItem.invoke(list[adapterPosition], adapterPosition)
            }
//            binding.tvTitle.setOnClickListener {
//                onClickItem.invoke(list[adapterPosition], adapterPosition)
//            }
//            binding.btnBookNow.setOnClickListener {
//                onBookClick.invoke(list[adapterPosition], adapterPosition)
//            }
        }

        fun onBind(movie: Movie) {
//            binding.tvTitle.text = movie.title
            Glide.with(binding.picMovie.context).load(movie.poster_url).into(binding.picMovie)
        }
    }

    override fun onBindViewHolder(holder: MovieViewHolder, position: Int) {
        holder.onBind(list[position % list.size]) // Sử dụng modulo để lặp lại danh sách
    }

    override fun getItemCount(): Int {
        return if (list.isEmpty()) 0 else Int.MAX_VALUE // Tạo hiệu ứng lướt vô hạn
    }
}