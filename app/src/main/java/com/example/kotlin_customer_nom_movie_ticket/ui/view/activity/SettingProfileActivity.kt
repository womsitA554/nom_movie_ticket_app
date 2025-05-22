package com.example.kotlin_customer_nom_movie_ticket.ui.view.activity

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.bumptech.glide.Glide
import com.example.kotlin_customer_nom_movie_ticket.R
import com.example.kotlin_customer_nom_movie_ticket.databinding.ActivitySettingProfileBinding
import com.example.kotlin_customer_nom_movie_ticket.util.SessionManager
import com.example.kotlin_customer_nom_movie_ticket.viewmodel.SettingProfileViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

class SettingProfileActivity : AppCompatActivity() {
    private lateinit var settingProfileViewModel: SettingProfileViewModel
    private lateinit var binding: ActivitySettingProfileBinding
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var firebaseAuth: FirebaseAuth
    private var userId = ""
    private var selectedAvatarImageUri: Uri? = null
    private var selectedBackgroundImageUri: Uri? = null
    private var verificationId: String? = null

    private val PERMISSION_CODE = 1000
    private val IMAGE_CAPTURE_AVATAR_CODE_OPEN_CAMERA = 1001
    private val IMAGE_CAPTURE_AVATAR_CODE_OPEN_DOCUMENT = 1003
    private val IMAGE_CAPTURE_BACKGROUND_CODE_OPEN_CAMERA = 1002
    private val IMAGE_CAPTURE_BACKGROUND_CODE_OPEN_DOCUMENT = 1004

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        settingProfileViewModel = SettingProfileViewModel()
        binding.btnSave.visibility = View.GONE

        // Lấy dữ liệu từ intent
        val fullName = intent.getStringExtra("name")
        val phoneNumber = intent.getStringExtra("phoneNumber")
        val avatar = intent.getStringExtra("avatar")

        // Đặt giá trị cho EditText với xử lý null
        binding.etName.setText(fullName ?: "")
        binding.etPhoneNumber.setText(phoneNumber ?: "")
        Glide.with(this)
            .load(avatar)
            .centerCrop()
            .into(binding.picAvatar)

        sharedPreferences = getSharedPreferences("loginSaved", MODE_PRIVATE)
        userId = SessionManager.getUserId(this) ?: run {
            Toast.makeText(this, "Không tìm thấy ID người dùng", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.btnBack.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        binding.btnSelectAvatar.setOnClickListener {
            val options = arrayOf("Camera", "Chọn ảnh")
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Chọn tùy chọn")
            builder.setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                        if (intent.resolveActivity(packageManager) != null) {
                            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), PERMISSION_CODE)
                            } else {
                                startActivityForResult(intent, IMAGE_CAPTURE_AVATAR_CODE_OPEN_CAMERA)
                            }
                        } else {
                            Toast.makeText(this, "Không tìm thấy ứng dụng camera", Toast.LENGTH_SHORT).show()
                        }
                    }
                    1 -> {
                        openImagePicker()
                    }
                }
            }
            builder.show()
        }

        binding.etName.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                window.decorView.rootView?.let {
                    it.isFocusableInTouchMode = true
                    it.requestFocus()
                }
            }
        }

        binding.etName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                binding.btnSave.isEnabled = !s.isNullOrEmpty()
                binding.btnSave.visibility = if (s.isNullOrEmpty()) View.GONE else View.VISIBLE
            }
        })

        binding.etPhoneNumber.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                binding.btnSave.isEnabled = !s.isNullOrEmpty()
                binding.btnSave.visibility = if (s.isNullOrEmpty()) View.GONE else View.VISIBLE
            }
        })

        binding.btnSave.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val newPhoneNumber = binding.etPhoneNumber.text.toString().trim()
            var updateCount = 0
            var expectedUpdates = 0

            // Cập nhật ảnh đại diện nếu có thay đổi
            if (selectedAvatarImageUri != null) {
                expectedUpdates++
                settingProfileViewModel.updateAvatar(userId, selectedAvatarImageUri!!) { success ->
                    if (success) {
                        Toast.makeText(this, "Cập nhật ảnh đại diện thành công", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Lỗi khi cập nhật ảnh đại diện", Toast.LENGTH_SHORT).show()
                    }
                    updateCount++
                    checkAndFinish(updateCount, expectedUpdates)
                }
            }

            // Cập nhật tên nếu có thay đổi
            if (name.isNotEmpty() && name != fullName) {
                expectedUpdates++
                settingProfileViewModel.updateName(userId, name) { success ->
                    if (success) {
                        Toast.makeText(this, "Cập nhật tên thành công", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Lỗi khi cập nhật tên", Toast.LENGTH_SHORT).show()
                    }
                    updateCount++
                    checkAndFinish(updateCount, expectedUpdates)
                }
            }

            // Cập nhật số điện thoại nếu có thay đổi
            if (newPhoneNumber.isNotEmpty() && newPhoneNumber != phoneNumber) {
                if (isValidPhoneNumber(newPhoneNumber)) {
                    expectedUpdates++
                    startPhoneNumberVerification(newPhoneNumber) { success ->
                        if (success) {
                            // Cập nhật cơ sở dữ liệu sau khi cập nhật auth thành công
                            settingProfileViewModel.updatePhoneNumber(userId, newPhoneNumber) { dbSuccess ->
                                if (dbSuccess) {
                                    Toast.makeText(this, "Cập nhật số điện thoại thành công", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(this, "Lỗi khi cập nhật số điện thoại trong cơ sở dữ liệu", Toast.LENGTH_SHORT).show()
                                }
                                updateCount++
                                checkAndFinish(updateCount, expectedUpdates)
                            }
                        } else {
                            Toast.makeText(this, "Lỗi khi xác minh số điện thoại", Toast.LENGTH_SHORT).show()
                            updateCount++
                            checkAndFinish(updateCount, expectedUpdates)
                        }
                    }
                } else {
                    Toast.makeText(this, "Số điện thoại không hợp lệ", Toast.LENGTH_SHORT).show()
                }
            }

            // Nếu không có cập nhật nào, thoát ngay
            if (expectedUpdates == 0) {
                setResult(RESULT_CANCELED)
                finish()
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }
        }
    }

    private fun isValidPhoneNumber(phoneNumber: String): Boolean {
        // Kiểm tra cơ bản: số điện thoại bắt đầu bằng "+" và có 10-15 chữ số
        val phonePattern = Regex("^\\+[1-9]\\d{9,14}\$")
        return phonePattern.matches(phoneNumber)
    }

    private fun startPhoneNumberVerification(phoneNumber: String, callback: (Boolean) -> Unit) {
        val options = PhoneAuthProvider.getInstance().verifyPhoneNumber(
            phoneNumber,
            60,
            TimeUnit.SECONDS,
            this,
            object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    // Xác minh tự động (ví dụ: trên cùng thiết bị)
                    updatePhoneNumberInAuth(credential, phoneNumber, callback)
                }

                override fun onVerificationFailed(e: com.google.firebase.FirebaseException) {
                    Log.e("SettingProfileActivity", "Xác minh thất bại", e)
                    Toast.makeText(this@SettingProfileActivity, "Xác minh thất bại: ${e.message}", Toast.LENGTH_SHORT).show()
                    callback(false)
                }

                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    this@SettingProfileActivity.verificationId = verificationId
                    showVerificationCodeDialog(phoneNumber, callback)
                }
            }
        )
    }

    private fun showVerificationCodeDialog(phoneNumber: String, callback: (Boolean) -> Unit) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_verify_phone)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCancelable(false)

        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val etCode = dialog.findViewById<EditText>(R.id.etVerificationCode)
        val btnVerify = dialog.findViewById<Button>(R.id.btnVerify)
        val btnCancel = dialog.findViewById<Button>(R.id.btnCancel)

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnVerify.setOnClickListener {
            val code = etCode.text.toString().trim()
            if (code.length == 6) {
                val credential = PhoneAuthProvider.getCredential(verificationId ?: return@setOnClickListener, code)
                updatePhoneNumberInAuth(credential, phoneNumber, callback)
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Mã xác minh phải có 6 chữ số", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun updatePhoneNumberInAuth(credential: PhoneAuthCredential, phoneNumber: String, callback: (Boolean) -> Unit) {
        val user = firebaseAuth.currentUser ?: run {
            Toast.makeText(this, "Người dùng chưa đăng nhập", Toast.LENGTH_SHORT).show()
            callback(false)
            return
        }

        user.updatePhoneNumber(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("SettingProfileActivity", "Cập nhật số điện thoại trong auth: $phoneNumber")
                    callback(true)
                } else {
                    Log.e("SettingProfileActivity", "Lỗi cập nhật số điện thoại trong auth", task.exception)
                    Toast.makeText(this, "Lỗi khi cập nhật số điện thoại: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    callback(false)
                }
            }
    }

    private fun checkAndFinish(updateCount: Int, expectedUpdates: Int) {
        if (updateCount == expectedUpdates) {
            setResult(RESULT_OK)
            finish()
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, IMAGE_CAPTURE_AVATAR_CODE_OPEN_DOCUMENT)
    }

    private fun openImagePickerForBackground() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, IMAGE_CAPTURE_BACKGROUND_CODE_OPEN_DOCUMENT)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                IMAGE_CAPTURE_AVATAR_CODE_OPEN_CAMERA -> {
                    val imageBitmap = data?.extras?.get("data") as Bitmap
                    binding.btnSave.visibility = View.VISIBLE
                    binding.picAvatar.setImageBitmap(imageBitmap)
                }
                IMAGE_CAPTURE_AVATAR_CODE_OPEN_DOCUMENT -> {
                    selectedAvatarImageUri = data?.data
                    binding.btnSave.visibility = View.VISIBLE
                    binding.picAvatar.setImageURI(selectedAvatarImageUri)
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(intent, IMAGE_CAPTURE_AVATAR_CODE_OPEN_CAMERA)
        } else {
            Toast.makeText(this, "Quyền camera bị từ chối", Toast.LENGTH_SHORT).show()
        }
    }
}