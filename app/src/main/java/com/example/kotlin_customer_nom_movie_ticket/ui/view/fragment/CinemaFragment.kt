package com.example.kotlin_customer_nom_movie_ticket.ui.view.fragment

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.example.kotlin_customer_nom_movie_ticket.R
import com.example.kotlin_customer_nom_movie_ticket.databinding.FragmentCinemaBinding
import com.example.kotlin_customer_nom_movie_ticket.util.SessionManager

class CinemaFragment : Fragment() {
    private var _binding: FragmentCinemaBinding? = null
    private val binding get() = _binding!!

    private var allCinemaFragment = AllCinemaFragment()
    private var favoritesCinemaFragment = FavoritesCinemaFragment()
    private var activeFragment: Fragment? = null
    private lateinit var userId: String


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCinemaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userId = SessionManager.getUserId(requireContext()).toString()

        if(savedInstanceState == null){
            initializeFragments()
            showFragment(allCinemaFragment)
            updateColor(1)
        }

        binding.btnAllCinema.setOnClickListener {
            showFragment(allCinemaFragment)
            updateColor(1)
        }

        binding.btnFavoriteCinema.setOnClickListener {
            showFragment(favoritesCinemaFragment)
            updateColor(2)
        }
    }
    private fun initializeFragments() {
        val fragmentTransaction = childFragmentManager.beginTransaction()

        if (childFragmentManager.findFragmentByTag("ALLCINEMA") == null) {
            fragmentTransaction.add(R.id.fragment_container_cinema, allCinemaFragment, "ALLCINEMA").hide(allCinemaFragment)
        }
        if (childFragmentManager.findFragmentByTag("FAVORITECINEMA") == null) {
            fragmentTransaction.add(R.id.fragment_container_cinema, favoritesCinemaFragment, "FAVORITECINEMA").hide(favoritesCinemaFragment)
        }

        fragmentTransaction.commit()
    }

    private fun showFragment(fragment: Fragment) {
        val fragmentTransaction = childFragmentManager.beginTransaction()

        if (activeFragment != null && activeFragment != fragment) {
            fragmentTransaction.hide(activeFragment!!)
        }

        // Hiển thị fragment mới
        fragmentTransaction.show(fragment)

        activeFragment = fragment
        fragmentTransaction.commit()
    }

    private fun updateColor(selected : Int){
        binding.btnAllCinema.setTextColor(if (selected == 1) ContextCompat.getColor(requireContext(), R.color.orange) else ContextCompat.getColor(requireContext(), R.color.grey))
        binding.btnAllCinema.setBackgroundResource(if (selected == 1) R.drawable.my_ticket_orange_background else R.drawable.my_ticket_grey_background)

        binding.btnFavoriteCinema.setTextColor(if (selected == 2) ContextCompat.getColor(requireContext(), R.color.orange) else ContextCompat.getColor(requireContext(), R.color.grey))
        binding.btnFavoriteCinema.setBackgroundResource(if (selected == 2) R.drawable.my_ticket_orange_background else R.drawable.my_ticket_grey_background)
    }

}