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
import com.example.kotlin_customer_nom_movie_ticket.databinding.FragmentMyTicketBinding
import com.example.kotlin_customer_nom_movie_ticket.util.SessionManager

class MyTicketFragment : Fragment() {
    private var _binding: FragmentMyTicketBinding? = null
    private val binding get() = _binding!!

    private var upcomingTicketFragment = UpcomingTicketFragment()
    private var passedTicketFragment = PassedTicketFragment()
    private var activeFragment: Fragment? = null
    private lateinit var userId: String


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyTicketBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userId = SessionManager.getUserId(requireContext()).toString()

        if(savedInstanceState == null){
            initializeFragments()
            showFragment(upcomingTicketFragment)
            updateColor(1)
        }

        binding.btnUpcoming.setOnClickListener {
            showFragment(upcomingTicketFragment)
            updateColor(1)
        }

        binding.btnPassed.setOnClickListener {
            showFragment(passedTicketFragment)
            updateColor(2)
        }
    }
    private fun initializeFragments() {
        val fragmentTransaction = childFragmentManager.beginTransaction()

        if (childFragmentManager.findFragmentByTag("UPCOMINGTICKET") == null) {
            fragmentTransaction.add(R.id.fragment_container_my_ticket, upcomingTicketFragment, "UPCOMINGTICKET").hide(upcomingTicketFragment)
        }
        if (childFragmentManager.findFragmentByTag("PASSEDTICKET") == null) {
            fragmentTransaction.add(R.id.fragment_container_my_ticket, passedTicketFragment, "PASSEDTICKET").hide(passedTicketFragment)
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
        binding.btnUpcoming.setTextColor(if (selected == 1) ContextCompat.getColor(requireContext(), R.color.orange) else ContextCompat.getColor(requireContext(), R.color.grey))
        binding.btnUpcoming.setBackgroundResource(if (selected == 1) R.drawable.my_ticket_orange_background else R.drawable.my_ticket_grey_background)

        binding.btnPassed.setTextColor(if (selected == 2) ContextCompat.getColor(requireContext(), R.color.orange) else ContextCompat.getColor(requireContext(), R.color.grey))
        binding.btnPassed.setBackgroundResource(if (selected == 2) R.drawable.my_ticket_orange_background else R.drawable.my_ticket_grey_background)
    }

}