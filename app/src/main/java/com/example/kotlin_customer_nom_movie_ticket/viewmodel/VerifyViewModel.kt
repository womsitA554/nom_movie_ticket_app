package com.example.chatapp.ViewModel

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.database.FirebaseDatabase
import java.util.concurrent.TimeUnit

class VerifyViewModel : ViewModel() {
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading
    private val _verificationCode = MutableLiveData<String>()
    val verificationCode: LiveData<String> get() = _verificationCode
    private val _verificationStatus = MutableLiveData<Boolean>()
    val verificationStatus: LiveData<Boolean> get() = _verificationStatus
    var resendingToken: PhoneAuthProvider.ForceResendingToken? = null

    fun sendOtp(context: Context, phoneNumber: String, isResend: Boolean) {
        val builder = PhoneAuthOptions.newBuilder(firebaseAuth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(context as AppCompatActivity)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(phoneAuthCredential: PhoneAuthCredential) {
                    Log.d("OTPActivity", "Verification completed")
                    signUpPassword(context, phoneAuthCredential)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    Log.e("OTPActivity", "Verification failed: ${e.message}")
                    Toast.makeText(context, "OTP send failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }

                override fun onCodeSent(s: String, forceResendingToken: PhoneAuthProvider.ForceResendingToken) {
                    _verificationCode.value = s
                    resendingToken = forceResendingToken
                    Log.d("OTPActivity", "OTP code sent successfully")
                    Toast.makeText(context, "OTP sent successfully", Toast.LENGTH_SHORT).show()
                }
            })

        if (isResend) {
            resendingToken?.let {
                builder.setForceResendingToken(it)
            }
        }

        PhoneAuthProvider.verifyPhoneNumber(builder.build())
    }

    fun verifyOtp(context: Context, otp: String) {
        val phoneAuthCredential = PhoneAuthProvider.getCredential(_verificationCode.value!!, otp)
        signUpPassword(context, phoneAuthCredential)
    }

    private fun signUpPassword(context: Context, phoneAuthCredential: PhoneAuthCredential) {
        _isLoading.value = true
        firebaseAuth.signInWithCredential(phoneAuthCredential).addOnCompleteListener { task ->
            _isLoading.value = false
            if (task.isSuccessful) {
                firebaseAuth.currentUser?.let { currentUser ->
                    val userId = currentUser.uid
                    val userRef = FirebaseDatabase.getInstance().getReference("Customers").child(userId)

                    userRef.get().addOnSuccessListener { snapshot ->
                        if (snapshot.exists()) {
                            saveUserIdToSharedPreferences(context, userId)
                            _verificationStatus.value = true
                        } else {
                            saveUserIdToSharedPreferences(context, userId)
                            _verificationStatus.value = false
                        }
                    }
                }
            } else {
                Log.e("OTPActivity", "OTP verification failed: ${task.exception?.message}")
                Toast.makeText(context, "OTP verification failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveUserIdToSharedPreferences(context: Context, userId: String) {
        val sharedPreferences = context.getSharedPreferences("loginSaved", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("customer_id", userId)
        editor.putBoolean("isLogin", true)
        editor.apply()
    }
}