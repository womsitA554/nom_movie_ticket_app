package com.example.kotlin_customer_nom_movie_ticket.ui.view.fragment

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.example.kotlin_customer_nom_movie_ticket.R
import com.example.kotlin_customer_nom_movie_ticket.data.model.Movie
import com.example.kotlin_customer_nom_movie_ticket.databinding.FragmentHomeBinding
import com.example.kotlin_customer_nom_movie_ticket.helper.HorizontalSpaceItemDecoration
import com.example.kotlin_customer_nom_movie_ticket.ui.adapter.BannerAdapter
import com.example.kotlin_customer_nom_movie_ticket.ui.adapter.MovieIsComingAdapter
import com.example.kotlin_customer_nom_movie_ticket.ui.adapter.MovieIsShowingAdapter
import com.example.kotlin_customer_nom_movie_ticket.ui.view.activity.ChooseCinemaActivity
import com.example.kotlin_customer_nom_movie_ticket.ui.view.activity.ComingSoonActivity
import com.example.kotlin_customer_nom_movie_ticket.ui.view.activity.ComingSoonDetailActivity
import com.example.kotlin_customer_nom_movie_ticket.ui.view.activity.NotificationActivity
import com.example.kotlin_customer_nom_movie_ticket.ui.view.activity.NowPlayingActivity
import com.example.kotlin_customer_nom_movie_ticket.ui.view.activity.NowPlayingDetailActivity
import com.example.kotlin_customer_nom_movie_ticket.util.SessionManager
import com.example.kotlin_customer_nom_movie_ticket.viewmodel.BannerViewModel
import com.example.kotlin_customer_nom_movie_ticket.viewmodel.MovieViewModel
import com.google.firebase.database.FirebaseDatabase
import kotlin.math.abs

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var bannerViewModel: BannerViewModel
    private lateinit var bannerAdapter: BannerAdapter
    private lateinit var movieIsComingAdapter: MovieIsComingAdapter
    private lateinit var movieIsShowingAdapter: MovieIsShowingAdapter
    private lateinit var movieViewModel: MovieViewModel
    private lateinit var userId: String
    private val handler = Handler(Looper.getMainLooper())
    private var currentPage = 0

    private var totalBanners = 0
    private val dotViews = mutableListOf<View>()

    private lateinit var listViewPageMovie: ArrayList<Movie>
    private var realItemCount = 0
    private var initialPosition = 0

    // Biến để theo dõi trạng thái load của từng danh mục
    private var isBannersLoaded = false
    private var isMovieIsShowingLoaded = false
    private var isMovieIsComingLoaded = false
    private var isAvatarCustomerLoaded = false

    private val favoriteUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val movieId = intent?.getStringExtra("movie_id") ?: return
            val isFavorite = intent.getBooleanExtra("is_favorite", false)
            movieViewModel.updateFavoriteStatus(movieId, isFavorite)
            movieIsComingAdapter.notifyDataSetChanged()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Khởi tạo UI ban đầu: hiển thị progressBar, ẩn scrollView
        binding.progressBar.visibility = View.VISIBLE
        binding.progressBar.playAnimation() // Bắt đầu animation
        binding.scrollView.visibility = View.GONE

        userId = SessionManager.getUserId(requireContext()) ?: run {
            requireActivity().finish()
            return
        }

        bannerViewModel = ViewModelProvider(this)[BannerViewModel::class.java]
        movieViewModel = ViewModelProvider(this)[MovieViewModel::class.java]

        setupRecycleView()

        // Fetch dữ liệu
        bannerViewModel.fetchBanners()
        movieViewModel.fetchMoviesIsShowing()
        movieViewModel.fetchMoviesIsComing()
        movieViewModel.fetchAvatarCustomer(userId)

        // Thiết lập observers
        setupObservers()

        LocalBroadcastManager.getInstance(requireContext())
            .registerReceiver(favoriteUpdateReceiver, IntentFilter("com.example.FAVORITE_UPDATED"))

        binding.btnViewAllNowPlaying.setOnClickListener {
            val intent = Intent(requireContext(), NowPlayingActivity::class.java)
            startActivity(intent)
            activity?.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        binding.btnViewAllComingSoon.setOnClickListener {
            val intent = Intent(requireContext(), ComingSoonActivity::class.java)
            startActivity(intent)
            activity?.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        binding.btnNotification.setOnClickListener {
            val intent = Intent(requireContext(), NotificationActivity::class.java)
            startActivity(intent)
            activity?.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
    }

    private fun setupObservers() {
        bannerViewModel.banners.observe(viewLifecycleOwner) { banners ->
            isBannersLoaded = true
            totalBanners = banners.size
            bannerAdapter = BannerAdapter(banners)
            binding.viewPager2.adapter = bannerAdapter
            binding.cardViewBanner.visibility = View.VISIBLE
            startAutoSlide(banners.size)
            checkAllDataLoaded()
        }

        setUpTransformer()
        movieViewModel.movieIsShowing.observe(viewLifecycleOwner) { movies ->
            isMovieIsShowingLoaded = true
            realItemCount = movies.size
            listViewPageMovie = ArrayList()
            repeat(20) {
                listViewPageMovie.addAll(movies)
            }

            movieIsShowingAdapter = MovieIsShowingAdapter(listViewPageMovie, binding.viewPagerMovie)
            binding.viewPagerMovie.adapter = movieIsShowingAdapter
            binding.viewPagerMovie.offscreenPageLimit = 3
            binding.viewPagerMovie.clipToPadding = false
            binding.viewPagerMovie.clipChildren = false
            binding.viewPagerMovie.getChildAt(0).overScrollMode = RecyclerView.OVER_SCROLL_NEVER

            initialPosition = (listViewPageMovie.size / 2) - ((listViewPageMovie.size / 2) % realItemCount)
            binding.viewPagerMovie.setCurrentItem(initialPosition, false)

            val initialRealPosition = initialPosition % realItemCount
            val initialMovie = listViewPageMovie[initialRealPosition]
            binding.tvTitle.text = initialMovie.title ?: "Unknown Title"
            binding.tvGenre.text = initialMovie.genre ?: "Unknown Genre"
            binding.tvAgeRate.text = initialMovie.age_rating ?: "N/A"
            binding.btnBookNow.setOnClickListener {
                val intent = Intent(requireContext(), ChooseCinemaActivity::class.java)
                intent.putExtra("movie_id", initialMovie.movie_id)
                intent.putExtra("country", initialMovie.country)
                intent.putExtra("title", initialMovie.title)
                intent.putExtra("poster_url", initialMovie.poster_url)
                intent.putExtra("language", initialMovie.language)
                intent.putExtra("release_year", initialMovie.release_year)
                intent.putExtra("duration", initialMovie.duration)
                intent.putExtra("genre", initialMovie.genre)
                intent.putExtra("synopsis", initialMovie.synopsis)
                intent.putExtra("director_id", initialMovie.director_id)
                intent.putExtra("status", initialMovie.status)
                intent.putExtra("trailer_url", initialMovie.trailer_url)
                intent.putExtra("banner", initialMovie.banner)
                intent.putExtra("age_rating", initialMovie.age_rating)
                intent.putExtra("rating", initialMovie.ratings.average_rating)
                intent.putStringArrayListExtra("actor_ids", ArrayList(initialMovie.actor_ids))
                startActivity(intent)
                Log.d("movie_id", initialMovie.movie_id)
            }

            binding.viewPagerMovie.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    val realPosition = position % realItemCount
                    val currentMovie = listViewPageMovie[realPosition]
                    binding.tvTitle.text = currentMovie.title
                    binding.tvGenre.text = currentMovie.genre
                    binding.tvAgeRate.text = currentMovie.age_rating
                    binding.btnBookNow.setOnClickListener {
                        val intent = Intent(requireContext(), ChooseCinemaActivity::class.java)
                        intent.putExtra("movie_id", currentMovie.movie_id)
                        intent.putExtra("country", currentMovie.country)
                        intent.putExtra("title", currentMovie.title)
                        intent.putExtra("poster_url", currentMovie.poster_url)
                        intent.putExtra("language", currentMovie.language)
                        intent.putExtra("release_year", currentMovie.release_year)
                        intent.putExtra("duration", currentMovie.duration)
                        intent.putExtra("genre", currentMovie.genre)
                        intent.putExtra("synopsis", currentMovie.synopsis)
                        intent.putExtra("director_id", currentMovie.director_id)
                        intent.putExtra("status", currentMovie.status)
                        intent.putExtra("trailer_url", currentMovie.trailer_url)
                        intent.putExtra("banner", currentMovie.banner)
                        intent.putExtra("age_rating", currentMovie.age_rating)
                        intent.putExtra("rating", currentMovie.ratings.average_rating)
                        intent.putStringArrayListExtra("actor_ids", ArrayList(currentMovie.actor_ids))
                        startActivity(intent)
                    }
                    if (position <= realItemCount || position >= listViewPageMovie.size - realItemCount) {
                        binding.viewPagerMovie.setCurrentItem(initialPosition, false)
                    }
                }
            })

            movieIsShowingAdapter.onClickItem = { movie, position ->
                val intent = Intent(requireContext(), NowPlayingDetailActivity::class.java)
                intent.putExtra("customer_id", userId)
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
                activity?.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }

            binding.viewPagerMovie.visibility = View.VISIBLE
            checkAllDataLoaded()
        }

        movieViewModel.movieIsComing.observe(viewLifecycleOwner) { movies ->
            isMovieIsComingLoaded = true
            movieIsComingAdapter = MovieIsComingAdapter(movies, isGrid = false)
            binding.rcvMovieIsComing.adapter = movieIsComingAdapter
            binding.rcvMovieIsComing.visibility = View.VISIBLE

            movieIsComingAdapter.onClickItem = { movie, _, isFavorite ->
                val intent = Intent(requireContext(), ComingSoonDetailActivity::class.java)
                intent.putExtra("customer_id", userId)
                intent.putExtra("movie_id", movie.movie_id)
                intent.putExtra("country", movie.country)
                intent.putExtra("title", movie.title)
                intent.putExtra("poster_url", movie.poster_url)
                intent.putExtra("language", movie.language)
                intent.putExtra("release_year", movie.release_year)
                intent.putExtra("duration", movie.duration)
                intent.putExtra("genre", movie.genre)
                intent.putExtra("synopsis", movie.synopsis)
                intent.putExtra("director_id", movie.director_id)
                intent.putExtra("status", movie.status)
                intent.putExtra("trailer_url", movie.trailer_url)
                intent.putExtra("banner", movie.banner)
                intent.putExtra("age_rating", movie.age_rating)
                intent.putExtra("rating", movie.ratings.average_rating)
                intent.putExtra("is_favorite", isFavorite)
                intent.putStringArrayListExtra("actor_ids", ArrayList(movie.actor_ids))
                startActivity(intent)
            }

            movieIsComingAdapter.onFavoriteClick = { movie, _, isFavorite ->
                toggleFavoriteMovie(movie.movie_id, isFavorite)
                movieViewModel.updateFavoriteStatus(movie.movie_id, isFavorite)
            }
            checkAllDataLoaded()
        }

        movieViewModel.avatarCustomer.observe(viewLifecycleOwner) { customer ->
            isAvatarCustomerLoaded = true
            Glide.with(requireContext())
                .load(customer.avatar)
                .into(binding.avatar)
            checkAllDataLoaded()
        }

        movieViewModel.favoriteStatus.observe(viewLifecycleOwner) { favoriteMap ->
            movieIsComingAdapter.notifyDataSetChanged()
        }

        // Xử lý lỗi
//        bannerViewModel.error.observe(viewLifecycleOwner) { error ->
//            error?.let {
//                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
//                Log.e("HomeFragment", "Banner error: $it")
//                isBannersLoaded = true // Đánh dấu là đã load để không chặn UI
//                checkAllDataLoaded()
//            }
//        }
//
//        movieViewModel.error.observe(viewLifecycleOwner) { error ->
//            error?.let {
//                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
//                Log.e("HomeFragment", "Movie error: $it")
//                // Đánh dấu các danh mục liên quan là đã load để không chặn UI
//                isMovieIsShowingLoaded = true
//                isMovieIsComingLoaded = true
//                isAvatarCustomerLoaded = true
//                checkAllDataLoaded()
//            }
//        }
    }

    private fun checkAllDataLoaded() {
        if (isBannersLoaded && isMovieIsShowingLoaded && isMovieIsComingLoaded && isAvatarCustomerLoaded) {
            // Tất cả dữ liệu đã load, cập nhật UI
            stopAnimation()
            binding.scrollView.visibility = View.VISIBLE
            Log.d("HomeFragment", "All data loaded, showing scrollView")
        }
    }

    private fun stopAnimation() {
        binding.progressBar.cancelAnimation() // Dừng animation
        binding.progressBar.visibility = View.GONE // Ẩn progressBar
    }

    private fun setUpTransformer() {
        val transformer = CompositePageTransformer().apply {
            addTransformer(MarginPageTransformer(40))
            addTransformer { page, position ->
                val scale = 0.90f + (1 - abs(position)) * 0.14f
                page.scaleY = scale
            }
        }
        binding.viewPagerMovie.setPageTransformer(transformer)
    }

    private fun toggleFavoriteMovie(movieId: String, isFavorite: Boolean) {
        val dbRef = FirebaseDatabase.getInstance().getReference("FavoriteMovies")
            .child(userId)
            .child(movieId)

        if (isFavorite) {
            dbRef.setValue(true)
                .addOnSuccessListener {
                    Log.d("HomeFragment", "Added $movieId to favorites for user $userId")
                }
                .addOnFailureListener { e ->
                    showCustomToast("Failed to add to favorites")
                    Log.e("HomeFragment", "Error adding favorite: ${e.message}")
                }
        } else {
            dbRef.removeValue()
                .addOnSuccessListener {
                    Log.d("HomeFragment", "Removed $movieId from favorites for user $userId")
                }
                .addOnFailureListener { e ->
                    Log.e("HomeFragment", "Error removing favorite: ${e.message}")
                }
        }
    }

    private fun showCustomToast(message: String) {
        val inflater = LayoutInflater.from(requireContext())
        val layout = inflater.inflate(R.layout.toast_reminder, null)
        val textView = layout.findViewById<TextView>(R.id.tv_reminder)
        textView.text = message

        val toast = Toast(requireContext())
        toast.duration = Toast.LENGTH_SHORT
        toast.setView(layout)
        val yOffset = (30 * resources.displayMetrics.density).toInt()
        toast.setGravity(Gravity.TOP or Gravity.CENTER_HORIZONTAL, 0, yOffset)
        toast.show()
    }

    private fun setupRecycleView() {
        val spaceInPixels = resources.getDimensionPixelSize(R.dimen.item_spacing)

        binding.rcvMovieIsComing.setHasFixedSize(true)
        binding.rcvMovieIsComing.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        binding.rcvMovieIsComing.addItemDecoration(HorizontalSpaceItemDecoration(spaceInPixels))
    }

    private fun startAutoSlide(itemCount: Int) {
        val fakePosition = Integer.MAX_VALUE / 2
        binding.viewPager2.setCurrentItem(fakePosition, false)

        val runnable = object : Runnable {
            override fun run() {
                val nextPage = binding.viewPager2.currentItem + 1
                if (nextPage >= Integer.MAX_VALUE - 10) {
                    binding.viewPager2.setCurrentItem(fakePosition, false)
                } else {
                    binding.viewPager2.setCurrentItem(nextPage, true)
                }
                handler.postDelayed(this, 3000)
            }
        }
        handler.postDelayed(runnable, 3000)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopAnimation() // Dừng và ẩn progressBar khi fragment bị hủy
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(favoriteUpdateReceiver)
        handler.removeCallbacksAndMessages(null)
        _binding = null
    }
}