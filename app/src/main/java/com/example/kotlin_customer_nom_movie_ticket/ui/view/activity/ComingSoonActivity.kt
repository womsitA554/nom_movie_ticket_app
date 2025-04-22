package com.example.kotlin_customer_nom_movie_ticket.ui.view.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.example.kotlin_customer_nom_movie_ticket.R
import com.example.kotlin_customer_nom_movie_ticket.databinding.ActivityComingSoonBinding
import com.example.kotlin_customer_nom_movie_ticket.helper.GridSpaceItemDecoration
import com.example.kotlin_customer_nom_movie_ticket.ui.adapter.MovieIsComingAdapter
import com.example.kotlin_customer_nom_movie_ticket.viewmodel.MovieViewModel

class ComingSoonActivity : AppCompatActivity() {
    private lateinit var binding: ActivityComingSoonBinding
    private lateinit var movieIsComingAdapter: MovieIsComingAdapter
    private lateinit var movieViewModel: MovieViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityComingSoonBinding.inflate(layoutInflater)
        setContentView(binding.root)

        movieViewModel = ViewModelProvider(this)[MovieViewModel::class.java]

        setupRecyclerView()

        movieViewModel.movieIsComing.observe(this){ movies ->
            movieIsComingAdapter = MovieIsComingAdapter(movies, isGrid = true)
            binding.rcvComingSoon.adapter = movieIsComingAdapter

            binding.progressBar.visibility = android.view.View.GONE
            binding.rcvComingSoon.visibility = android.view.View.VISIBLE
        }

        if (movieViewModel.movieIsComing.value == null) {
            movieViewModel.fetchMoviesIsComing()
        }

        binding.btnBack.setOnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
    }

    private fun setupRecyclerView() {
        val spaceInPixels = resources.getDimensionPixelSize(R.dimen.item_spacing)

        binding.rcvComingSoon.setHasFixedSize(true)
        binding.rcvComingSoon.layoutManager = GridLayoutManager(this, 2)
        binding.rcvComingSoon.addItemDecoration(GridSpaceItemDecoration(spaceInPixels))
    }
}