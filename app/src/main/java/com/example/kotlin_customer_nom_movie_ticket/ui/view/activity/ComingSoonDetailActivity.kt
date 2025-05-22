package com.example.kotlin_customer_nom_movie_ticket.ui.view.activity

import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.kotlin_customer_nom_movie_ticket.R
import com.example.kotlin_customer_nom_movie_ticket.databinding.ActivityComingSoonDetailBinding
import com.example.kotlin_customer_nom_movie_ticket.ui.adapter.ActorAdapter
import com.example.kotlin_customer_nom_movie_ticket.util.SessionManager
import com.example.kotlin_customer_nom_movie_ticket.viewmodel.ActorViewModel
import com.example.kotlin_customer_nom_movie_ticket.viewmodel.DirectorViewModel
import com.example.kotlin_customer_nom_movie_ticket.viewmodel.MovieViewModel
import com.google.firebase.database.FirebaseDatabase

class ComingSoonDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityComingSoonDetailBinding
    private lateinit var directorViewModel: DirectorViewModel
    private lateinit var actorAdapter: ActorAdapter
    private lateinit var actorViewModel: ActorViewModel
    private lateinit var movieViewModel: MovieViewModel
    private var customerId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityComingSoonDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        movieViewModel = ViewModelProvider(this)[MovieViewModel::class.java]

        customerId = SessionManager.getUserId(this).toString()
        val movieId = intent.getStringExtra("movie_id")
        val movieCountry = intent.getStringExtra("country")
        val moviePosterUrl = intent.getStringExtra("poster_url")
        val movieTitle = intent.getStringExtra("title")
        val movieReleaseYear = intent.getStringExtra("release_year")
        val movieLanguage = intent.getStringExtra("language")
        val movieDuration = intent.getIntExtra("duration", 0)
        val movieGenre = intent.getStringExtra("genre")
        val movieSynopsis = intent.getStringExtra("synopsis")
        val movieDirectorId = intent.getStringExtra("director_id")
        val movieStatus = intent.getStringExtra("status")
        val movieTrailerUrl = intent.getStringExtra("trailer_url")
        val movieActorIds = intent.getStringArrayListExtra("actor_ids")?.toList()
        val movieAgeRating = intent.getStringExtra("age_rating")
        val movieRating = intent.getFloatExtra("rating", 0f)
        val movieBanner = intent.getStringExtra("banner")
        val isFavorite = intent.getBooleanExtra("is_favorite", false)

        directorViewModel = DirectorViewModel()
        if (movieDirectorId != null) {
            directorViewModel.fetchDirectorNameById(movieDirectorId).toString()
        } else {
            Toast.makeText(this, "Không có ID đạo diễn", Toast.LENGTH_SHORT).show()
        }

        directorViewModel.directorName.observe(this) { name ->
            binding.tvDirectorName.text = name
        }

        binding.tvTitle.text = movieTitle
        binding.tvAgeRate.text = movieAgeRating
        binding.tvDuration.text = "$movieDuration phút"
        binding.tvGenre.text = movieGenre
        binding.tvSynopsis.text = movieSynopsis
        Glide.with(this).load(moviePosterUrl).into(binding.picMovie)
        Glide.with(this).load(movieBanner).into(binding.bannerMovie)

        binding.btnBack.setOnClickListener {
            onBackPressed()
        }

        if (movieTrailerUrl.isNullOrEmpty()) {
            Toast.makeText(this, "Không có URL trailer", Toast.LENGTH_SHORT).show()
        } else {
            binding.trailerVideo.setOnClickListener {
                val intent = Intent(this, VideoPlayerActivity::class.java)
                intent.putExtra("trailer_url", movieTrailerUrl)
                startActivity(intent)
            }
        }

        actorViewModel = ActorViewModel()
        setupRecycleView()
        actorAdapter = ActorAdapter(emptyList())
        binding.rcvCast.adapter = actorAdapter

        if (movieActorIds != null) {
            if (movieActorIds.isNotEmpty()) {
                actorViewModel.fetchActorsByIds(movieActorIds)
                actorViewModel.actors.observe(this) { actors ->
                    actorAdapter = ActorAdapter(actors)
                    binding.rcvCast.adapter = actorAdapter
                }
            } else {
                Toast.makeText(this, "Không có diễn viên nào", Toast.LENGTH_SHORT).show()
            }
        }

        updateFavoriteUI(isFavorite)
        binding.btnFavorite.setOnClickListener {
            val currentFavoriteStatus = binding.btnFavorite.tag as? Boolean ?: false
            val newFavoriteStatus = !currentFavoriteStatus
            toggleFavoriteMovie(movieId!!, customerId, newFavoriteStatus)
        }
    }

    private fun updateFavoriteUI(isFavorite: Boolean) {
        val icon = binding.btnFavorite.findViewById<ImageView>(R.id.favorite_icon)

        if (isFavorite) {
            icon.setImageResource(R.drawable.heart_fill_icon)
        } else {
            icon.setImageResource(R.drawable.heart_outline_icon)
        }
        binding.btnFavorite.tag = isFavorite
    }

    private fun toggleFavoriteMovie(movieId: String, customerId: String, isFavorite: Boolean) {
        val dbRef = FirebaseDatabase.getInstance().getReference("FavoriteMovies")
            .child(customerId)
            .child(movieId)

        if (isFavorite) {
            dbRef.setValue(true)
                .addOnSuccessListener {
                    updateFavoriteUI(true)
                    movieViewModel.updateFavoriteStatus(movieId, true) // Cập nhật LiveData
                    sendFavoriteUpdateBroadcast(movieId, true) // Gửi broadcast
                    Log.d("ComingSoonDetail", "Added $movieId to favorites for $customerId")
                }
                .addOnFailureListener { e ->
                    Log.e("ComingSoonDetail", "Error: ${e.message}")
                }
        } else {
            dbRef.removeValue()
                .addOnSuccessListener {
                    updateFavoriteUI(false)
                    movieViewModel.updateFavoriteStatus(movieId, false) // Cập nhật LiveData
                    sendFavoriteUpdateBroadcast(movieId, false) // Gửi broadcast
                    Log.d("ComingSoonDetail", "Removed $movieId from favorites for $customerId")
                }
                .addOnFailureListener { e ->
                    Log.e("ComingSoonDetail", "Error: ${e.message}")
                }
        }
    }

    private fun sendFavoriteUpdateBroadcast(movieId: String, isFavorite: Boolean) {
        val intent = Intent("com.example.FAVORITE_UPDATED")
        intent.putExtra("movie_id", movieId)
        intent.putExtra("is_favorite", isFavorite)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun setupRecycleView(){
        binding.rcvCast.setHasFixedSize(true)
        binding.rcvCast.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rcvCast.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                val spacing = resources.getDimensionPixelSize(R.dimen.item_spacing) // 8dp
                outRect.right = spacing
                outRect.left = if (parent.getChildAdapterPosition(view) == 0) spacing else 0
            }
        })
    }
}