package com.example.kotlin_customer_nom_movie_ticket.ui.view.activity

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.example.kotlin_customer_nom_movie_ticket.R
import com.example.kotlin_customer_nom_movie_ticket.databinding.ActivityVideoPlayerBinding

class VideoPlayerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityVideoPlayerBinding
    private lateinit var player: ExoPlayer

    var isFullScreen = false


    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)


        val trailerUrl = intent.getStringExtra("trailer_url")
        if (trailerUrl.isNullOrEmpty()) {
            finish()
            return
        }

        player =
            ExoPlayer.Builder(this).setSeekBackIncrementMs(5000).setSeekForwardIncrementMs(5000)
                .build()
        binding.videoPlayer.player = player
        binding.videoPlayer.keepScreenOn = true
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY -> {
                        binding.progressBar.visibility = View.GONE
                    }

                    Player.STATE_BUFFERING -> {
                        binding.progressBar.visibility = View.VISIBLE
                    }

                    Player.STATE_ENDED -> {
                        player.seekTo(0)
                        player.play()
                    }

                    Player.STATE_IDLE -> {
                    }
                }

            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                binding.videoPlayer.findViewById<View>(androidx.media3.ui.R.id.exo_play)?.visibility =
                    if (isPlaying) View.GONE else View.VISIBLE
                binding.videoPlayer.findViewById<View>(androidx.media3.ui.R.id.exo_pause)?.visibility =
                    if (isPlaying) View.VISIBLE else View.GONE
            }

        })

        val mediaItem = MediaItem.fromUri(trailerUrl)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()

        binding.videoPlayer.findViewById<View>(androidx.media3.ui.R.id.exo_play)
            ?.setOnClickListener {
                player.play()
            }
        binding.videoPlayer.findViewById<View>(androidx.media3.ui.R.id.exo_pause)
            ?.setOnClickListener {
                player.pause()
            }

        binding.videoPlayer.findViewById<View>(R.id.exo_close)?.setOnClickListener {
            onBackPressed()
        }
        val btnFullScreen = binding.videoPlayer.findViewById<ImageView>(R.id.btnFullScreen)
        btnFullScreen?.setOnClickListener {
            if (!isFullScreen) {
                btnFullScreen.setImageResource(R.drawable.fullscreen_exit_icon)
                requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            } else {
                btnFullScreen.setImageResource(R.drawable.fullscreen_icon)
                requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }

            isFullScreen = !isFullScreen
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        player.release()
    }
}