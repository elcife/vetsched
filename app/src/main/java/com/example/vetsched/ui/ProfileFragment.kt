package com.example.vetsched.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.vetsched.R
import com.example.vetsched.databinding.FragmentProfileBinding

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return _binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sharedPref = requireActivity().getSharedPreferences("VETSCHED_PREFS", Context.MODE_PRIVATE)
        val userName = sharedPref.getString("userName", "User")
        binding.tvUserNameProfile.text = userName

        binding.btnLogout.setOnClickListener {
            with(sharedPref.edit()) {
                putBoolean("isLoggedIn", false)
                putString("userName", null)
                apply()
            }
            findNavController().navigate(R.id.action_profileFragment_to_startFragment)
        }

        binding.btnSchedule.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_scheduleFragment)
        }

        binding.btnCourses.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_coursesFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}