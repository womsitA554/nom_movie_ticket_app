package com.example.kotlin_customer_nom_movie_ticket.ui.view.fragment

import android.Manifest
import android.app.Dialog
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.kotlin_customer_nom_movie_ticket.R
import com.example.kotlin_customer_nom_movie_ticket.databinding.FragmentProfileBinding
import com.example.kotlin_customer_nom_movie_ticket.helper.ThemePreferences
import com.example.kotlin_customer_nom_movie_ticket.ui.view.activity.LoginActivity
import com.example.kotlin_customer_nom_movie_ticket.ui.view.activity.MainActivity
import com.example.kotlin_customer_nom_movie_ticket.ui.view.activity.MemberInformationActivity
import com.example.kotlin_customer_nom_movie_ticket.ui.view.activity.PaymentActivity
import com.example.kotlin_customer_nom_movie_ticket.ui.view.activity.SettingProfileActivity
import com.example.kotlin_customer_nom_movie_ticket.util.SessionManager
import com.example.kotlin_customer_nom_movie_ticket.viewmodel.ProfileViewModel
import com.google.firebase.auth.FirebaseAuth

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var userId: String
    private lateinit var profileViewModel: ProfileViewModel

    // ActivityResultLauncher for SettingProfileActivity
    private val settingProfileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == AppCompatActivity.RESULT_OK) {
            // Refresh customer data when profile is updated
            profileViewModel.fetchCustomerById(userId)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        firebaseAuth = FirebaseAuth.getInstance()
        profileViewModel = ProfileViewModel()

        sharedPreferences = requireContext().getSharedPreferences("loginSaved", Context.MODE_PRIVATE)
        userId = SessionManager.getUserId(requireContext()) ?: run {
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            requireActivity().finish()
            return
        }

        profileViewModel.fetchCustomerById(userId)
        profileViewModel.customer.observe(viewLifecycleOwner) { customer ->
            Glide.with(requireContext())
                .load(customer.avatar)
                .into(binding.avatar)
            binding.tvName.text = customer.full_name
            binding.tvPoint.text = customer.point.toString()

            binding.btnSettingProfile.setOnClickListener {
                val intent = Intent(requireContext(), SettingProfileActivity::class.java).apply {
                    putExtra("avatar", customer.avatar)
                    putExtra("name", customer.full_name)
                    putExtra("phoneNumber", customer.phone_number)
                }
                settingProfileLauncher.launch(intent) // Use launcher instead of startActivity
                activity?.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }
        }

        binding.btnMemberInfo.setOnClickListener {
            val intent = Intent(requireContext(), MemberInformationActivity::class.java)
            intent.putExtra("point", binding.tvPoint.text)
            startActivity(intent)
            activity?.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        binding.btnNightMode.setOnCheckedChangeListener(null)
        binding.btnNightMode.isChecked = ThemePreferences.isDarkMode(requireContext())
        binding.btnNightMode.setOnCheckedChangeListener { _, isChecked ->
            ThemePreferences.saveTheme(requireContext(), isChecked)
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            )
            requireActivity().recreate()
        }

        updateNotificationSwitchState()

        val prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean("from_reminder_dialog", false)) {
            val isSystemNotificationEnabled =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                } else {
                    val notificationManager =
                        requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.areNotificationsEnabled()
                }

            if (!isSystemNotificationEnabled && !binding.btnNotificationMode.isChecked) {
                showOpenSettingDevice()
            } else {
                prefs.edit().putBoolean("notifications_enabled", isSystemNotificationEnabled).apply()
                binding.btnNotificationMode.isChecked = isSystemNotificationEnabled
            }
            prefs.edit().putBoolean("from_reminder_dialog", false).apply()
        }

        binding.btnLogout.setOnClickListener {
            showLogoutDialog()
        }

//        binding.btnPayment.setOnClickListener {
//            val intent = Intent(requireContext(), PaymentActivity::class.java)
//            startActivity(intent)
//            activity?.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
//        }
    }

    private fun showLogoutDialog() {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_logout)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCancelable(false)

        val btnNo = dialog.findViewById<Button>(R.id.btnNo)
        val btnSure = dialog.findViewById<Button>(R.id.btnSure)

        btnNo.setOnClickListener {
            dialog.dismiss()
        }

        btnSure.setOnClickListener {
            signOutAndStartSignInActivity()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showOpenSettingDevice() {
        val isSystemNotificationEnabled =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else {
                val notificationManager =
                    requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.areNotificationsEnabled()
            }

        if (isSystemNotificationEnabled) {
            val prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("notifications_enabled", true).apply()
            binding.btnNotificationMode.isChecked = true
            return
        }

        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_allow_open_setting_device)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCancelable(false)

        val btnCancel = dialog.findViewById<Button>(R.id.btnCancel)
        val btnGoSetting = dialog.findViewById<Button>(R.id.btnGoSetting)

        btnCancel.setOnClickListener {
            dialog.dismiss()
            binding.btnNotificationMode.isChecked = false
            val prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("notifications_enabled", false).apply()
        }

        val prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

        btnGoSetting.setOnClickListener {
            dialog.dismiss()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                ) {
                    (activity as? MainActivity)?.requestNotificationPermission { granted ->
                        if (granted) {
                            prefs.edit().putBoolean("notifications_enabled", true).apply()
                            binding.btnNotificationMode.isChecked = true
                        } else {
                            binding.btnNotificationMode.isChecked = false
                            prefs.edit().putBoolean("notifications_enabled", false).apply()
                        }
                        updateNotificationSwitchState()
                    }
                } else {
                    prefs.edit().putBoolean("notifications_enabled", true).apply()
                    binding.btnNotificationMode.isChecked = true
                }
            } else {
                openNotificationSettings()
            }
        }

        dialog.show()
    }

    private fun updateNotificationSwitchState() {
        binding.btnNotificationMode.setOnCheckedChangeListener(null)

        val prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val isSystemNotificationEnabled =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else {
                val notificationManager =
                    requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.areNotificationsEnabled()
            }

        prefs.edit().putBoolean("notifications_enabled", isSystemNotificationEnabled).apply()

        binding.btnNotificationMode.isChecked = isSystemNotificationEnabled

        binding.btnNotificationMode.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (!isSystemNotificationEnabled) {
                    showOpenSettingDevice()
                } else {
                    prefs.edit().putBoolean("notifications_enabled", true).apply()
                }
            } else {
                prefs.edit().putBoolean("notifications_enabled", false).apply()
            }
        }
    }

    private fun openNotificationSettings() {
        val intent = Intent().apply {
            when {
                true -> {
                    action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                    putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().packageName)
                }
                else -> {
                    action = "android.settings.APP_NOTIFICATION_SETTINGS"
                    putExtra("app_package", requireContext().packageName)
                    putExtra("app_uid", requireContext().applicationInfo.uid)
                }
            }
        }
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        updateNotificationSwitchState()
    }

    private fun signOutAndStartSignInActivity() {
        firebaseAuth.signOut()
        val editor = sharedPreferences.edit()
        editor.clear()
        editor.apply()
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}