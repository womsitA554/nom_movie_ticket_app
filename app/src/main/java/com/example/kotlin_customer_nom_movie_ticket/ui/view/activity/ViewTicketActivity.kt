package com.example.kotlin_customer_nom_movie_ticket.ui.view.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.example.kotlin_customer_nom_movie_ticket.R
import com.example.kotlin_customer_nom_movie_ticket.databinding.ActivityViewTicketBinding
import com.example.kotlin_customer_nom_movie_ticket.viewmodel.TicketViewModel
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ViewTicketActivity : AppCompatActivity() {
    private lateinit var binding: ActivityViewTicketBinding
    private val ticketViewModel: TicketViewModel by viewModels()
    private val STORAGE_PERMISSION_CODE = 100
    private val CORNER_RADIUS = 40f // Adjust to match your @style/pic_item_top corner radius

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewTicketBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.statusBarColor = getColor(R.color.orange)
            window.decorView.systemUiVisibility = 0
        }

        val billId = intent.getStringExtra("bill_id")
        val cinemaName = intent.getStringExtra("cinema_name")
        val showtimeTime = intent.getStringExtra("showtime_time")
        val movieTitle = intent.getStringExtra("title")
        val movieDuration = intent.getIntExtra("duration", 0)
        val movieAgeRating = intent.getStringExtra("age_rating")
        val seatName = intent.getStringExtra("seat_name")
        val totalPrice = intent.getDoubleExtra("total_price", 0.0)
        val moviePoster = intent.getStringExtra("poster_url")
        val movieDirector = intent.getStringExtra("director")
        val movieGenre = intent.getStringExtra("genre")
        val movieRoomName = intent.getStringExtra("room_name")

        Glide.with(this)
            .load(moviePoster)
            .into(binding.picMovie)

        binding.tvDirectorName.text = movieDirector
        binding.tvGenre.text = movieGenre
        binding.tvPrice.text = "$${String.format("%.2f", totalPrice)}"
        binding.tvBillId.text = billId
        binding.tvAgeRate.text = movieAgeRating
        binding.tvCinemaName.text = cinemaName
        binding.tvMovieTitle.text = movieTitle
        binding.tvSeats.text = seatName
        binding.tvDuration.text = "$movieDuration minutes"
        binding.tvRoomName.text = movieRoomName

        if (showtimeTime != null) {
            val parts = showtimeTime.split("T")
            if (parts.size == 2) {
                val date = parts[0]
                val time = parts[1].substring(0, 5)
                binding.tvDate.text = date
                binding.tvTime.text = time
            }
        }

        binding.btnShare.setOnClickListener {
            if (checkStoragePermission()) {
                captureAndShareTicket()
            } else {
                requestStoragePermission()
            }
        }

        binding.btnBack.setOnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        binding.btnDownLoad.setOnClickListener {
            if (checkStoragePermission()) {
                captureAndSaveTicket()
            } else {
                requestStoragePermission()
            }
        }
    }

    private fun checkStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            true // Scoped storage doesn't require WRITE_EXTERNAL_STORAGE
        } else {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                STORAGE_PERMISSION_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (binding.btnDownLoad.isPressed) {
                    captureAndSaveTicket()
                } else if (binding.btnShare.isPressed) {
                    captureAndShareTicket()
                }
            } else {
                Toast.makeText(
                    this,
                    "Storage permission denied. Cannot perform action.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun captureAndSaveTicket() {
        captureTicketBitmap { bitmap ->
            if (bitmap != null) {
                saveTicketBitmap(bitmap)
            } else {
                Toast.makeText(this, "Failed to capture ticket", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun captureAndShareTicket() {
        captureTicketBitmap { bitmap ->
            if (bitmap != null) {
                shareTicketBitmap(bitmap)
            } else {
                Toast.makeText(this, "Failed to capture ticket", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun captureTicketBitmap(callback: (Bitmap?) -> Unit) {
        val view = binding.ticketLayout
        view.post {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Use PixelCopy for API 26+
                val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
                val location = IntArray(2)
                view.getLocationInWindow(location)
                PixelCopy.request(
                    window,
                    android.graphics.Rect(
                        location[0],
                        location[1],
                        location[0] + view.width,
                        location[1] + view.height
                    ),
                    bitmap,
                    { result ->
                        if (result == PixelCopy.SUCCESS) {
                            callback(bitmap)
                        } else {
                            callback(null)
                        }
                    },
                    Handler(Looper.getMainLooper())
                )
            } else {
                // Fallback for older devices: Manual bitmap with rounded corners for poster
                try {
                    val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    view.draw(canvas)

                    // Manually apply rounded corners to the poster
                    val posterBitmap = getPosterBitmapWithRoundedCorners()
                    if (posterBitmap != null) {
                        val posterView = binding.picMovie
                        val posterLocation = IntArray(2)
                        posterView.getLocationInWindow(posterLocation)

                        // Adjust for ticketLayout's position
                        val ticketLocation = IntArray(2)
                        view.getLocationInWindow(ticketLocation)
                        val relativeX = posterLocation[0] - ticketLocation[0]
                        val relativeY = posterLocation[1] - ticketLocation[1]

                        canvas.drawBitmap(
                            posterBitmap,
                            relativeX.toFloat(),
                            relativeY.toFloat(),
                            null
                        )
                    }

                    callback(bitmap)
                } catch (e: Exception) {
                    e.printStackTrace()
                    callback(null)
                }
            }
        }
    }

    private fun getPosterBitmapWithRoundedCorners(): Bitmap? {
        try {
            val posterView = binding.picMovie
            posterView.isDrawingCacheEnabled = true
            posterView.buildDrawingCache()
            val originalBitmap = Bitmap.createBitmap(posterView.drawingCache)
            posterView.isDrawingCacheEnabled = false

            val roundedBitmap = Bitmap.createBitmap(
                originalBitmap.width,
                originalBitmap.height,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(roundedBitmap)
            val path = Path()
            val rect = RectF(0f, 0f, originalBitmap.width.toFloat(), originalBitmap.height.toFloat())
            path.addRoundRect(rect, CORNER_RADIUS, CORNER_RADIUS, Path.Direction.CW)
            canvas.clipPath(path)
            canvas.drawBitmap(originalBitmap, 0f, 0f, null)

            originalBitmap.recycle()
            return roundedBitmap
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun saveTicketBitmap(bitmap: Bitmap) {
        try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "Ticket_$timeStamp.png"
            val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val file = File(picturesDir, fileName)

            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                out.flush()
            }

            sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)))
            Toast.makeText(this, "Ticket downloaded to Pictures/$fileName", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to download ticket: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareTicketBitmap(bitmap: Bitmap) {
        try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "Ticket_$timeStamp.png"
            val cacheDir = cacheDir
            val file = File(cacheDir, fileName)

            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                out.flush()
            }

            val fileUri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                putExtra(Intent.EXTRA_SUBJECT, "My Movie Ticket")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(shareIntent, "Share Ticket Image via"))
            Toast.makeText(this, "Sharing ticket image...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to share ticket: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}