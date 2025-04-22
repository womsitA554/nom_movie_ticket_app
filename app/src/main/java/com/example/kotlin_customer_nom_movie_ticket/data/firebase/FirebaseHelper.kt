package com.example.kotlin_customer_nom_movie_ticket.data.firebase

import com.google.firebase.database.FirebaseDatabase

class FirebaseHelper {
    val dbBanner = FirebaseDatabase.getInstance().getReference("Banners")


}