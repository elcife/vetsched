package com.example.vetsched.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.vetsched.R
import com.example.vetsched.api.RetrofitClient
import com.example.vetsched.api.models.AuthResponse
import com.example.vetsched.databinding.FragmentRegisterBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupStudentIDFormatting()

        binding.tvLogin.setOnClickListener {
            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }

        binding.btnCreateAccount.setOnClickListener {
            val firstName = binding.etFirstName.text.toString().trim()
            val lastName = binding.etLastName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val idNumber = binding.etIDNumber.text.toString().trim()
            val password = binding.etPassword.text.toString()
            val confirmPassword = binding.etConfirmPassword.text.toString()

            // Strict Validation
            if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || 
                idNumber.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(requireContext(), "All fields are required!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Check for complete ID format XX-XXXX-XXXXXX (14 chars)
            if (idNumber.length < 14) {
                Toast.makeText(requireContext(), "Please enter a valid Student ID (XX-XXXX-XXXXXX)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 10) {
                Toast.makeText(requireContext(), "Password must be at least 10 characters long!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(requireContext(), "Passwords do not match!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val params = mapOf(
                "first_name" to firstName,
                "last_name" to lastName,
                "email" to email,
                "student_id" to idNumber,
                "password" to password
            )

            RetrofitClient.instance.register(params).enqueue(object : Callback<AuthResponse> {
                override fun onResponse(call: Call<AuthResponse>, response: Response<AuthResponse>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(requireContext(), "Account Created!", Toast.LENGTH_SHORT).show()
                        findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
                    } else {
                        val errorMsg = response.body()?.message ?: "Registration failed"
                        Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<AuthResponse>, t: Throwable) {
                    Toast.makeText(requireContext(), "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun setupStudentIDFormatting() {
        binding.etIDNumber.addTextChangedListener(object : TextWatcher {
            private var isUpdating = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (isUpdating) return
                
                isUpdating = true
                
                val digits = s.toString().replace("-", "")
                val sb = StringBuilder()
                
                for (i in digits.indices) {
                    sb.append(digits[i])
                    // Add dash after 2nd and 6th digit
                    if ((i == 1 || i == 5) && i != digits.length - 1) {
                        sb.append("-")
                    }
                }
                
                val result = sb.toString()
                if (result != s.toString()) {
                    binding.etIDNumber.setText(result)
                    binding.etIDNumber.setSelection(result.length)
                }
                
                isUpdating = false
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
