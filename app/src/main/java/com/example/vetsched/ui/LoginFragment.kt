package com.example.vetsched.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.vetsched.R
import com.example.vetsched.api.RetrofitClient
import com.example.vetsched.api.models.AuthResponse
import com.example.vetsched.databinding.FragmentLoginBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvRegister.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val params = mapOf(
                "email" to email,
                "password" to password
            )

            RetrofitClient.instance.login(params).enqueue(object : Callback<AuthResponse> {
                override fun onResponse(call: Call<AuthResponse>, response: Response<AuthResponse>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(requireContext(), "Login successful!", Toast.LENGTH_SHORT).show()
                        
                        val user = response.body()?.user
                        val bundle = Bundle().apply {
                            putString("userName", "${user?.firstName} ${user?.lastName}")
                        }
                        findNavController().navigate(R.id.action_loginFragment_to_demoFragment, bundle)
                    } else {
                        val errorBody = response.errorBody()?.string()
                        val authResponse = if (errorBody != null) {
                            com.google.gson.Gson().fromJson(errorBody, AuthResponse::class.java)
                        } else {
                            response.body()
                        }
                        
                        val errorMsg = authResponse?.message ?: "Login failed"
                        Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<AuthResponse>, t: Throwable) {
                    Toast.makeText(requireContext(), "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
