package com.example.kotlin_customer_nom_movie_ticket.ui.view.activity

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