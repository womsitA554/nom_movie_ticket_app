package com.example.kotlin_customer_nom_movie_ticket.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage

class SettingProfileViewModel : ViewModel() {

    fun updateAvatar(userId: String, imageUri: Uri, callback: (Boolean) -> Unit) {
        val storageRef = FirebaseStorage.getInstance().reference.child("avatars/$userId.jpg")
        storageRef.putFile(imageUri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    FirebaseDatabase.getInstance().getReference("Customers").child(userId)
                        .child("avatar").setValue(uri.toString())
                        .addOnSuccessListener {
                            Log.d("SettingProfileViewModel", "Cập nhật ảnh đại diện thành công")
                            callback(true)
                        }
                        .addOnFailureListener { e ->
                            Log.e("SettingProfileViewModel", "Lỗi cập nhật ảnh đại diện", e)
                            callback(false)
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("SettingProfileViewModel", "Lỗi tải ảnh đại diện", e)
                callback(false)
            }
    }

    fun updateName(userId: String, name: String, callback: (Boolean) -> Unit) {
        FirebaseDatabase.getInstance().getReference("Customers").child(userId)
            .child("full_name").setValue(name)
            .addOnSuccessListener {
                Log.d("SettingProfileViewModel", "Cập nhật tên: $name")
                callback(true)
            }
            .addOnFailureListener { e ->
                Log.e("SettingProfileViewModel", "Lỗi cập nhật tên", e)
                callback(false)
            }
    }

    fun updatePhoneNumber(userId: String, phoneNumber: String, callback: (Boolean) -> Unit) {
        FirebaseDatabase.getInstance().getReference("Customers").child(userId)
            .child("phone_number").setValue(phoneNumber)
            .addOnSuccessListener {
                Log.d("SettingProfileViewModel", "Cập nhật số điện thoại: $phoneNumber")
                callback(true)
            }
            .addOnFailureListener { e ->
                Log.e("SettingProfileViewModel", "Lỗi cập nhật số điện thoại", e)
                callback(false)
            }
    }
}