package com.example.kotlin_customer_nom_movie_ticket.ui.view.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import com.example.kotlin_customer_nom_movie_ticket.R
import com.example.kotlin_customer_nom_movie_ticket.databinding.ActivityNowPlayingBinding
import com.example.kotlin_customer_nom_movie_ticket.helper.GridSpaceItemDecoration
import com.example.kotlin_customer_nom_movie_ticket.helper.HorizontalSpaceItemDecoration
import com.example.kotlin_customer_nom_movie_ticket.ui.adapter.MovieIsComingAdapter
import com.example.kotlin_customer_nom_movie_ticket.ui.adapter.MovieIsShowingAdapter
import com.example.kotlin_customer_nom_movie_ticket.ui.adapter.NowPlayingItemAdapter
import com.example.kotlin_customer_nom_movie_ticket.viewmodel.MovieViewModel

class NowPlayingActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNowPlayingBinding
    private lateinit var movieIsShowingAdapter: NowPlayingItemAdapter
    private lateinit var movieViewModel: MovieViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNowPlayingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()

        movieViewModel = ViewModelProvider(this)[MovieViewModel::class.java]

        movieViewModel.movieIsShowing.observe(this){ movies ->
            movieIsShowingAdapter = NowPlayingItemAdapter(movies, isGrid = true)
            binding.rcvNowPlaying.adapter = movieIsShowingAdapter

            binding.progressBar.visibility = View.GONE
            binding.rcvNowPlaying.visibility = View.VISIBLE

            movieIsShowingAdapter.onBookClick = { movie, position ->
                val intent = Intent(this, ChooseCinemaActivity::class.java)
                intent.putExtra("movie_id", movie.movie_id)
                intent.putExtra("title", movie.title)
                intent.putExtra("poster_url", movie.poster_url)
                intent.putExtra("language", movie.language)
                intent.putExtra("release_year", movie.release_year)
                intent.putExtra("duration", movie.duration)
                intent.putExtra("genre", movie.genre)
                intent.putExtra("synopsis", movie.synopsis)
                intent.putExtra("director_id", movie.director_id)
                intent.putExtra("status", movie.status.name)
                intent.putExtra("trailer_url", movie.trailer_url)
                intent.putExtra("banner", movie.banner)
                intent.putExtra("age_rating", movie.age_rating)
                intent.putExtra("rating", movie.ratings.average_rating)
                intent.putExtra("quantity_vote", movie.ratings.total_votes)
                intent.putStringArrayListExtra("actor_ids", ArrayList(movie.actor_ids))
                startActivity(intent)
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }

            movieIsShowingAdapter.onClickItem = { movie, position ->
                val intent = Intent(this, NowPlayingDetailActivity::class.java)
                intent.putExtra("movie_id", movie.movie_id)
                intent.putExtra("title", movie.title)
                intent.putExtra("poster_url", movie.poster_url)
                intent.putExtra("language", movie.language)
                intent.putExtra("release_year", movie.release_year)
                intent.putExtra("duration", movie.duration)
                intent.putExtra("genre", movie.genre)
                intent.putExtra("synopsis", movie.synopsis)
                intent.putExtra("director_id", movie.director_id)
                intent.putExtra("status", movie.status.name)
                intent.putExtra("trailer_url", movie.trailer_url)
                intent.putExtra("banner", movie.banner)
                intent.putExtra("age_rating", movie.age_rating)
                intent.putExtra("rating", movie.ratings.average_rating)
                intent.putExtra("quantity_vote", movie.ratings.total_votes)
                intent.putStringArrayListExtra("actor_ids", ArrayList(movie.actor_ids))
                startActivity(intent)
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }

        }

        if (movieViewModel.movieIsShowing.value == null) {
            movieViewModel.fetchMoviesIsShowing()
        }

        binding.btnBack.setOnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

    }

    private fun setupRecyclerView() {

        binding.rcvNowPlaying.setHasFixedSize(true)
        binding.rcvNowPlaying.layoutManager = GridLayoutManager(this, 2)

        val spaceInPixels = resources.getDimensionPixelSize(R.dimen.item_spacing)
        val spaceVertical = resources.getDimensionPixelSize(R.dimen.item_spacing_vertical_15)
        binding.rcvNowPlaying.addItemDecoration(GridSpaceItemDecoration(spaceInPixels))
    }
}