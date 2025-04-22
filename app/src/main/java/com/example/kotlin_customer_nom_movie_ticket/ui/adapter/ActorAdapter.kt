package com.example.kotlin_customer_nom_movie_ticket.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.kotlin_customer_nom_movie_ticket.data.model.Actor
import com.example.kotlin_customer_nom_movie_ticket.databinding.CastItemBinding

class ActorAdapter(private val listActor: List<Actor>) : RecyclerView.Adapter<ActorAdapter.ActorViewHolder>() {
    class ActorViewHolder(private val binding: CastItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun onBind(actor: Actor) {
            binding.tvActorName.text = actor.name
            Glide.with(binding.root.context).load(actor.actor_image_url).into(binding.actorImg)
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActorAdapter.ActorViewHolder {
        val binding = CastItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ActorViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return listActor.size
    }

    override fun onBindViewHolder(holder: ActorViewHolder, position: Int) {
        holder.onBind(listActor[position])
    }
}
