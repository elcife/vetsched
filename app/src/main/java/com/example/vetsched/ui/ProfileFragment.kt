package com.example.vetsched.ui

import android.content.Context
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.vetsched.R
import com.example.vetsched.api.RetrofitClient
import com.example.vetsched.api.models.AuthResponse
import com.example.vetsched.databinding.FragmentProfileBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

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

        binding.btnChangePassword.setOnClickListener {
            showChangePasswordDialog()
        }

        binding.btnChangeYearLevel.setOnClickListener {
            showChangeYearLevelWarning()
        }
    }

    private fun showChangeYearLevelWarning() {
        AlertDialog.Builder(requireContext())
            .setTitle("Change Year Level")
            .setMessage("Are you sure, Changing year level may affect your schedule and erase all your schedule data.")
            .setPositiveButton("Yes") { _, _ ->
                showYearLevelSelectionDialog()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun showYearLevelSelectionDialog() {
        val yearLevels = arrayOf("1st Year", "2nd Year", "3rd Year", "4th Year", "5th Year")
        AlertDialog.Builder(requireContext())
            .setTitle("Select New Year Level")
            .setItems(yearLevels) { _, which ->
                val yearLevelInt = which + 1
                updateYearLevelOnServer(yearLevelInt.toString())
            }
            .show()
    }

    private fun updateYearLevelOnServer(yearLevel: String) {
        val sharedPref = requireActivity().getSharedPreferences("VETSCHED_PREFS", Context.MODE_PRIVATE)
        val userEmail = sharedPref.getString("userEmail", null)

        if (userEmail == null) {
            Toast.makeText(requireContext(), "Error: User email not found", Toast.LENGTH_SHORT).show()
            return
        }

        val params = mapOf(
            "email" to userEmail,
            "year_level" to yearLevel
        )

        RetrofitClient.instance.updateYearLevel(params).enqueue(object : Callback<AuthResponse> {
            override fun onResponse(call: Call<AuthResponse>, response: Response<AuthResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(requireContext(), "Year level updated successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    val msg = response.body()?.message ?: "Update failed"
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<AuthResponse>, t: Throwable) {
                Toast.makeText(requireContext(), "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showChangePasswordDialog() {
        val sharedPref = requireActivity().getSharedPreferences("VETSCHED_PREFS", Context.MODE_PRIVATE)
        val userEmail = sharedPref.getString("userEmail", null)

        if (userEmail == null) {
            Toast.makeText(requireContext(), "Error: User email not found", Toast.LENGTH_SHORT).show()
            return
        }

        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Change Password")
        
        val layout = LinearLayout(requireContext())
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(48, 24, 48, 24)

        val etOldPassword = EditText(requireContext())
        etOldPassword.hint = "Old Password"
        etOldPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        layout.addView(etOldPassword)

        val etNewPassword = EditText(requireContext())
        etNewPassword.hint = "New Password"
        etNewPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        layout.addView(etNewPassword)

        builder.setView(layout)

        builder.setPositiveButton("Update") { dialog, _ ->
            val oldPass = etOldPassword.text.toString()
            val newPass = etNewPassword.text.toString()

            if (oldPass.isEmpty() || newPass.isEmpty()) {
                Toast.makeText(requireContext(), "Both passwords are required", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            if (newPass.length < 10) {
                Toast.makeText(requireContext(), "New password must be at least 10 characters", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            val params = mapOf(
                "email" to userEmail,
                "old_password" to oldPass,
                "new_password" to newPass
            )

            RetrofitClient.instance.changePassword(params).enqueue(object : Callback<AuthResponse> {
                override fun onResponse(call: Call<AuthResponse>, response: Response<AuthResponse>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(requireContext(), "Password updated successfully!", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    } else {
                        val msg = response.body()?.message ?: "Update failed"
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<AuthResponse>, t: Throwable) {
                    Toast.makeText(requireContext(), "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}