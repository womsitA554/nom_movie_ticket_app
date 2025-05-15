package com.example.kotlin_customer_nom_movie_ticket.ui.view.activity

import android.Manifest
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.kotlin_customer_nom_movie_ticket.R
import com.example.kotlin_customer_nom_movie_ticket.databinding.ActivityMainBinding
import com.example.kotlin_customer_nom_movie_ticket.helper.ThemePreferences
import com.example.kotlin_customer_nom_movie_ticket.ui.view.fragment.CinemaFragment
import com.example.kotlin_customer_nom_movie_ticket.ui.view.fragment.FoodAndDrinkFragment
import com.example.kotlin_customer_nom_movie_ticket.ui.view.fragment.HomeFragment
import com.example.kotlin_customer_nom_movie_ticket.ui.view.fragment.MyTicketFragment
import com.example.kotlin_customer_nom_movie_ticket.ui.view.fragment.ProfileFragment
import com.example.kotlin_customer_nom_movie_ticket.util.NetworkUtils
import com.example.kotlin_customer_nom_movie_ticket.util.SessionManager
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import nl.joery.animatedbottombar.AnimatedBottomBar

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    private var homeFragment = HomeFragment()
    private var cinemaFragment = CinemaFragment()
    private var foodAndDrinkFragment = FoodAndDrinkFragment()
    private var myTicketFragment = MyTicketFragment()
    private var profileFragment = ProfileFragment()
    private var activeFragment: Fragment? = null
    private var currentFragmentTag: String = TAG_HOME
    private var notificationDialog: Dialog? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            recreate()
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                !shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)
            ) {
                openAppSettings()
            }
        }
        notificationDialog?.dismiss()
        permissionCallback?.invoke(isGranted)
    }

    private var permissionCallback: ((Boolean) -> Unit)? = null

    companion object {
        private const val TAG_HOME = "HOME"
        private const val TAG_CINEMA = "CINEMA"
        private const val TAG_FOODANDDRINK = "FOODANDDRINK"
        private const val TAG_MY_TICKET = "MY_TICKET"
        private const val TAG_PROFILE = "PROFILE"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (ThemePreferences.isDarkMode(this)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Khởi tạo Fragment và giao diện
        currentFragmentTag = savedInstanceState?.getString("CURRENT_FRAGMENT_TAG") ?: TAG_HOME
        val currentFragment = supportFragmentManager.findFragmentByTag(currentFragmentTag)
        if (currentFragment != null) {
            showFragment(currentFragment)
        }
        setupFragments(savedInstanceState)
        setupBottomBar()

        // Kiểm tra mạng và lấy FCM token
        if (NetworkUtils.isNetworkAvailable(this)) {
            getFCMToken()
        } else {
            Toast.makeText(this, "Không có kết nối mạng. Vui lòng kiểm tra kết nối.", Toast.LENGTH_LONG).show()
        }

        // Theo dõi trạng thái mạng
        lifecycleScope.launch {
            NetworkUtils.networkStatusFlow(this@MainActivity).collectLatest { isConnected ->
                if (isConnected) {
                    getFCMToken()
                } else {
                    Toast.makeText(this@MainActivity, "Mất kết nối mạng.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        checkNotificationStatus()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("CURRENT_FRAGMENT_TAG", currentFragmentTag)
    }

    private fun setupFragments(savedInstanceState: Bundle?) {
        initializeFragments()
        if (savedInstanceState == null) {
            showFragment(homeFragment)
            binding.bottomBar.selectTabAt(0, true)
        } else {
            when (currentFragmentTag) {
                TAG_HOME -> {
                    showFragment(homeFragment)
                    binding.bottomBar.selectTabById(R.id.tab_home, true)
                }
                TAG_CINEMA -> {
                    showFragment(cinemaFragment)
                    binding.bottomBar.selectTabById(R.id.tab_theater, true)
                }
                TAG_FOODANDDRINK -> {
                    showFragment(foodAndDrinkFragment)
                    binding.bottomBar.selectTabById(R.id.tab_food, true)
                }
                TAG_MY_TICKET -> {
                    showFragment(myTicketFragment)
                    binding.bottomBar.selectTabById(R.id.tab_ticket, true)
                }
                TAG_PROFILE -> {
                    showFragment(profileFragment)
                    binding.bottomBar.selectTabById(R.id.tab_profile, true)
                }
            }
        }
    }

    private fun initializeFragments() {
        val transaction = supportFragmentManager.beginTransaction()

        // HOME
        homeFragment = supportFragmentManager.findFragmentByTag(TAG_HOME) as? HomeFragment
            ?: HomeFragment().also {
                transaction.add(R.id.fragment_container, it, TAG_HOME).hide(it)
            }

        // CINEMA
        cinemaFragment = supportFragmentManager.findFragmentByTag(TAG_CINEMA) as? CinemaFragment
            ?: CinemaFragment().also {
                transaction.add(R.id.fragment_container, it, TAG_CINEMA).hide(it)
            }

        // FOOD & DRINK
        foodAndDrinkFragment = supportFragmentManager.findFragmentByTag(TAG_FOODANDDRINK) as? FoodAndDrinkFragment
            ?: FoodAndDrinkFragment().also {
                transaction.add(R.id.fragment_container, it, TAG_FOODANDDRINK).hide(it)
            }

        // MY TICKET
        myTicketFragment = supportFragmentManager.findFragmentByTag(TAG_MY_TICKET) as? MyTicketFragment
            ?: MyTicketFragment().also {
                transaction.add(R.id.fragment_container, it, TAG_MY_TICKET).hide(it)
            }

        // PROFILE
        profileFragment = supportFragmentManager.findFragmentByTag(TAG_PROFILE) as? ProfileFragment
            ?: ProfileFragment().also {
                transaction.add(R.id.fragment_container, it, TAG_PROFILE).hide(it)
            }

        Log.d("FragmentCheck", "Fragments initialized:")
        Log.d("FragmentCheck", "Home=${homeFragment}, Cinema=${cinemaFragment}, Food=${foodAndDrinkFragment}")
        transaction.commit()
    }

    private fun showFragment(fragment: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
        Log.d("MainActivity", "Switching to fragment: ${fragment::class.java.simpleName}")

        activeFragment?.let {
            if (it != fragment) {
                transaction.hide(it)
            }
        }

        transaction.show(fragment)
        activeFragment = fragment
        transaction.commit()
    }

    private fun setupBottomBar() {
        binding.bottomBar.setOnTabSelectListener(object : AnimatedBottomBar.OnTabSelectListener {
            override fun onTabSelected(
                lastIndex: Int,
                lastTab: AnimatedBottomBar.Tab?,
                newIndex: Int,
                newTab: AnimatedBottomBar.Tab
            ) {
                when (newTab.id) {
                    R.id.tab_home -> {
                        showFragment(homeFragment)
                        currentFragmentTag = TAG_HOME
                    }
                    R.id.tab_theater -> {
                        showFragment(cinemaFragment)
                        currentFragmentTag = TAG_CINEMA
                    }
                    R.id.tab_food -> {
                        showFragment(foodAndDrinkFragment)
                        currentFragmentTag = TAG_FOODANDDRINK
                    }
                    R.id.tab_ticket -> {
                        showFragment(myTicketFragment)
                        currentFragmentTag = TAG_MY_TICKET
                    }
                    R.id.tab_profile -> {
                        showFragment(profileFragment)
                        currentFragmentTag = TAG_PROFILE
                    }
                }
            }

            override fun onTabReselected(index: Int, tab: AnimatedBottomBar.Tab) {
                // Optional
            }
        })
    }

    private fun getFCMToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                updateFCMToken(token)
                Log.d("MainActivity", "FCM token: $token")
            } else {
                Log.e("MainActivity", "Fetching FCM token failed", task.exception)
                Toast.makeText(this, "Không thể lấy FCM token.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateFCMToken(token: String) {
        val userId = SessionManager.getUserId(this)
        if (!userId.isNullOrEmpty()) {
            FirebaseDatabase.getInstance().reference
                .child("Customers")
                .child(userId)
                .child("fcm_token")
                .setValue(token)
                .addOnSuccessListener {
                    Log.d("MainActivity", "FCM token updated successfully!")
                }
                .addOnFailureListener { exception ->
                    Log.e("MainActivity", "Failed to update FCM token: ${exception.message}")
                    Toast.makeText(this, "Không thể cập nhật FCM token.", Toast.LENGTH_SHORT).show()
                }
        } else {
            Log.w("MainActivity", "User ID is null or empty, cannot update FCM token")
        }
    }

    private fun checkNotificationStatus() {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        if (shouldShowNotificationDialog() && prefs.getBoolean("notifications_enabled", true)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val hasPermission = ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                if (!hasPermission) {
                    showNotificationPermissionDialog()
                }
            } else {
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                if (!notificationManager.areNotificationsEnabled()) {
                    showNotificationPermissionDialog()
                }
            }
        }
    }

    private fun showNotificationPermissionDialog() {
        notificationDialog = Dialog(this).apply {
            setContentView(R.layout.dialog_notification_permisstion)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setCancelable(false)
        }

        val imageView = notificationDialog?.findViewById<ImageView>(R.id.imageView)
        val textView = notificationDialog?.findViewById<TextView>(R.id.textView9)
        val btnAllow = notificationDialog?.findViewById<Button>(R.id.btnAllow)
        val btnNotAllow = notificationDialog?.findViewById<Button>(R.id.btnNotAllow)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            textView?.text = getString(R.string.notification_permission_text)
        } else {
            textView?.text = HtmlCompat.fromHtml(
                getString(R.string.notification_permission_text),
                HtmlCompat.FROM_HTML_MODE_LEGACY
            )
        }

        btnAllow?.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                openNotificationSettings()
            }
        }

        btnNotAllow?.setOnClickListener {
            val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
            prefs.edit().putBoolean("notification_dialog_dismissed", true).apply()
            notificationDialog?.dismiss()
        }

        notificationDialog?.show()
    }

    private fun openNotificationSettings() {
        val intent = Intent().apply {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                    action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                    putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                }
                else -> {
                    action = "android.settings.APP_NOTIFICATION_SETTINGS"
                    putExtra("app_package", packageName)
                    putExtra("app_uid", applicationInfo.uid)
                }
            }
        }
        startActivity(intent)
        notificationDialog?.dismiss()
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", packageName, null)
        intent.data = uri
        startActivity(intent)
    }

    private fun shouldShowNotificationDialog(): Boolean {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        return !prefs.getBoolean("notification_dialog_dismissed", false)
    }

    fun requestNotificationPermission(callback: ((Boolean) -> Unit)? = null) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionCallback = callback
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            callback?.invoke(true)
        }
    }

    fun resetNotificationDialogPreference() {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        prefs.edit().remove("notification_dialog_dismissed").apply()
    }

    override fun onResume() {
        super.onResume()
        checkNotificationStatus()
        if (NetworkUtils.isNetworkAvailable(this)) {
            getFCMToken()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        notificationDialog?.dismiss()
    }
}