package com.example.kotlin_customer_nom_movie_ticket.data.repository

import com.example.kotlin_customer_nom_movie_ticket.data.model.Actor
import com.google.firebase.database.FirebaseDatabase

class MovieShowingDetailRepository {
    private val dbDirector = FirebaseDatabase.getInstance().getReference("Directors")
    private val dbActor = FirebaseDatabase.getInstance().getReference("Actors")

    fun getDirectorNameById(directorId: String, callback: (String) -> Unit) {
        dbDirector.child(directorId).get().addOnSuccessListener {
            callback(it.child("name").value.toString())
        }.addOnFailureListener {
            callback("Unknown")
        }
    }

    fun getActorById(actorId: String, callback: (Actor) -> Unit) {
        dbActor.child(actorId).get().addOnSuccessListener { snapshot ->
            val actor = snapshot.getValue(Actor::class.java) ?: Actor()
            callback(actor)
        }.addOnFailureListener {
            callback(Actor(name = "Unknown", actor_image_url = ""))
        }
    }

    fun getActorsByIds(actorIds: List<String>, callback: (List<Actor>) -> Unit) {
        val actorsList = mutableListOf<Actor>()
        var completedRequests = 0

        if (actorIds.isEmpty()) {
            callback(emptyList())
            return
        }

        for (actorId in actorIds) {
            getActorById(actorId) { actor ->
                actorsList.add(actor)
                completedRequests++
                if (completedRequests == actorIds.size) {
                    callback(actorsList)
                }
            }
        }
    }
}