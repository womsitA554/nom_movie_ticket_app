package com.example.kotlin_customer_nom_movie_ticket.ui.view.fragment

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kotlin_customer_nom_movie_ticket.databinding.FragmentFavoritesCinemaBinding
import com.example.kotlin_customer_nom_movie_ticket.ui.adapter.CinemaAdapter
import com.example.kotlin_customer_nom_movie_ticket.ui.view.activity.CinemaDetailActivity
import com.example.kotlin_customer_nom_movie_ticket.ui.view.activity.CinemaDetailFromBookNowActivity
import com.example.kotlin_customer_nom_movie_ticket.util.SessionManager
import com.example.kotlin_customer_nom_movie_ticket.viewmodel.CinemaViewModel

class FavoritesCinemaFragment : Fragment() {
    private var _binding: FragmentFavoritesCinemaBinding? = null
    private val binding get() = _binding!!
    private lateinit var cinemaViewModel: CinemaViewModel
    private lateinit var cinemaAdapter: CinemaAdapter
    private lateinit var userId: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFavoritesCinemaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


    }
}