package com.example.kotlin_customer_nom_movie_ticket.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.kotlin_customer_nom_movie_ticket.data.model.Movie
import com.example.kotlin_customer_nom_movie_ticket.data.model.Showtime
import com.example.kotlin_customer_nom_movie_ticket.databinding.ChooseListMovieItemBinding
import com.example.kotlin_customer_nom_movie_ticket.viewmodel.DirectorViewModel

class ChooseListMovieAdapter(
    private val listMovie: List<Movie>,
    private val directorViewModel: DirectorViewModel,
    private val lifecycleOwner: LifecycleOwner,
    private val showtimeMap: Map<String, List<Showtime>>,
    private val onShowtimeClick: (Showtime) -> Unit
) : RecyclerView.Adapter<ChooseListMovieAdapter.ViewHolder>() {

    private var selectedShowtimeId: String? = null

    inner class ViewHolder(private val binding: ChooseListMovieItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun onBind(
            movie: Movie,
            showtimeList: List<Showtime>,
            directorViewModel: DirectorViewModel,
            lifecycleOwner: LifecycleOwner,
            onShowtimeClick: (Showtime) -> Unit
        ) {
            binding.tvTitle.text = movie.title
            binding.tvDirectorName.text = movie.director_id
            binding.tvAgeRate.text = movie.age_rating
            binding.tvGenre.text = movie.genre
            binding.tvDuration.text = "${movie.duration} minutes"
            Glide.with(binding.root.context)
                .load(movie.poster_url)
                .into(binding.picMovie)

            directorViewModel.fetchDirectorNameById(movie.director_id)
            directorViewModel.directorName.observe(lifecycleOwner) { directorName ->
                binding.tvDirectorName.text = directorName ?: "Unknown"
            }

            val showtimeAdapter = ShowtimeAdapter(showtimeList) { showtime ->
                val previousSelectedShowtimeId = selectedShowtimeId
                selectedShowtimeId = showtime.showtime_id

                if (previousSelectedShowtimeId != selectedShowtimeId) {
                    notifyDataSetChanged()
                }
                onShowtimeClick(showtime)
            }
            showtimeAdapter.setSelectedShowtimeId(selectedShowtimeId)

            binding.recyclerViewTimes.apply {
                layoutManager = GridLayoutManager(binding.root.context, 4)
                adapter = showtimeAdapter
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ChooseListMovieItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val movie = listMovie[position]
        val showtimes = showtimeMap[movie.movie_id] ?: emptyList()
        holder.onBind(movie, showtimes, directorViewModel, lifecycleOwner, onShowtimeClick)
    }

    override fun getItemCount(): Int = listMovie.size

    fun getSelectedShowtime(): Showtime? {
        return showtimeMap.values.flatten().find { it.showtime_id == selectedShowtimeId }
    }

}