package com.example.vetsched.ui

import android.os.Bundle
import android.graphics.Paint
import android.graphics.Typeface
import android.os.CountDownTimer
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
import com.example.vetsched.databinding.FragmentPasswordResetBinding
import com.example.vetsched.util.InputValidation
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PasswordResetFragment : Fragment() {

    private var _binding: FragmentPasswordResetBinding? = null
    private val binding get() = _binding!!
    private var cooldownTimer: CountDownTimer? = null
    private var cooldownMillisRemaining = 0L
    private var resetForEmail: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPasswordResetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.tvBackToLogin.paintFlags =
            binding.tvBackToLogin.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        binding.tvBackToLogin.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)

        binding.tvBackToLogin.setOnClickListener {
            findNavController().popBackStack(R.id.loginFragment, false)
        }

        binding.btnRequestCode.setOnClickListener {
            requestCode()
        }

        binding.btnResendCode.setOnClickListener {
            requestCode()
        }

        binding.etResetEmail.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.etResetEmail.error = null
                val currentEmail = s?.toString()?.trim().orEmpty()
                val previousEmail = resetForEmail
                if (previousEmail != null && !currentEmail.equals(previousEmail, ignoreCase = true)) {
                    clearResetState()
                }
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })

        binding.btnResetPassword.setOnClickListener {
            resetPassword()
        }
    }

    private fun requestCode() {
        val email = InputValidation.normalizeEmail(binding.etResetEmail.text?.toString().orEmpty())
        if (!InputValidation.isValidEmail(email)) {
            binding.etResetEmail.error = "Enter a valid email address"
            return
        }

        setLoading(true)
        binding.etResetEmail.isEnabled = false
        RetrofitClient.instance.requestPasswordReset(mapOf("email" to email))
            .enqueue(object : Callback<AuthResponse> {
                override fun onResponse(call: Call<AuthResponse>, response: Response<AuthResponse>) {
                    if (!isAdded || _binding == null) return
                    binding.etResetEmail.isEnabled = true
                    setLoading(false)
                    val result = response.body() ?: parseError(response)
                    if (response.isSuccessful && result?.success == true) {
                        resetForEmail = email
                        binding.etResetEmail.setText(email)
                        binding.resetFields.visibility = View.VISIBLE
                        binding.btnRequestCode.visibility = View.GONE
                        binding.tvResetMessage.setText(R.string.reset_code_sent_instructions)
                        binding.etResetCode.text?.clear()
                        binding.etNewPassword.text?.clear()
                        binding.etConfirmNewPassword.text?.clear()
                        startCooldown()
                        setLoading(false)
                        Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                    } else {
                        showError(result?.message ?: "Could not send the verification code")
                    }
                }

                override fun onFailure(call: Call<AuthResponse>, t: Throwable) {
                    if (!isAdded || _binding == null) return
                    binding.etResetEmail.isEnabled = true
                    setLoading(false)
                    showError("Network error: ${t.message}")
                }
            })
    }

    private fun resetPassword() {
        val email = InputValidation.normalizeEmail(binding.etResetEmail.text?.toString().orEmpty())
        val code = binding.etResetCode.text?.toString()?.trim().orEmpty()
        val password = binding.etNewPassword.text?.toString().orEmpty()
        val confirmPassword = binding.etConfirmNewPassword.text?.toString().orEmpty()

        when {
            !InputValidation.isValidEmail(email) -> {
                binding.etResetEmail.error = "Enter a valid email address"
                return
            }
            resetForEmail == null || !email.equals(resetForEmail, ignoreCase = true) -> {
                showError("Request a verification code for this email first")
                return
            }
            !InputValidation.isValidOtp(code) -> {
                binding.etResetCode.error = "Enter the 6-digit code"
                return
            }
            !InputValidation.isValidPassword(password) -> {
                binding.etNewPassword.error = "Use 10-128 characters"
                return
            }
            password != confirmPassword -> {
                binding.etConfirmNewPassword.error = "Passwords do not match"
                return
            }
        }

        setLoading(true)
        RetrofitClient.instance.confirmPasswordReset(
            mapOf("email" to email, "code" to code, "password" to password)
        ).enqueue(object : Callback<AuthResponse> {
            override fun onResponse(call: Call<AuthResponse>, response: Response<AuthResponse>) {
                if (!isAdded || _binding == null) return
                setLoading(false)
                val result = response.body() ?: parseError(response)
                if (response.isSuccessful && result?.success == true) {
                    Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                    findNavController().popBackStack(R.id.loginFragment, false)
                } else {
                    showError(result?.message ?: "Could not reset the password")
                }
            }

            override fun onFailure(call: Call<AuthResponse>, t: Throwable) {
                if (!isAdded || _binding == null) return
                setLoading(false)
                showError("Network error: ${t.message}")
            }
        })
    }

    private fun parseError(response: Response<AuthResponse>): AuthResponse? {
        val body = response.errorBody()?.string() ?: return null
        return try {
            Gson().fromJson(body, AuthResponse::class.java)
        } catch (_: JsonSyntaxException) {
            null
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.etResetEmail.isEnabled = !loading
        binding.etResetCode.isEnabled = !loading
        binding.etNewPassword.isEnabled = !loading
        binding.etConfirmNewPassword.isEnabled = !loading
        binding.btnRequestCode.isEnabled = !loading
        binding.btnResendCode.isEnabled = !loading && cooldownMillisRemaining == 0L
        binding.btnResetPassword.isEnabled = !loading && resetForEmail != null
    }

    private fun startCooldown() {
        cooldownTimer?.cancel()
        cooldownMillisRemaining = 60_000L
        renderCooldown()
        cooldownTimer = object : CountDownTimer(cooldownMillisRemaining, 1_000L) {
            override fun onTick(millisUntilFinished: Long) {
                cooldownMillisRemaining = millisUntilFinished
                renderCooldown()
            }

            override fun onFinish() {
                cooldownMillisRemaining = 0L
                cooldownTimer = null
                renderCooldown()
            }
        }.start()
    }

    private fun renderCooldown() {
        if (_binding == null) return
        if (cooldownMillisRemaining > 0L) {
            val seconds = (cooldownMillisRemaining + 999L) / 1_000L
            binding.btnResendCode.text = getString(R.string.resend_otp_countdown, seconds)
            binding.btnResendCode.isEnabled = false
        } else {
            binding.btnResendCode.setText(R.string.resend_otp)
            binding.btnResendCode.isEnabled = true
        }
    }

    private fun clearResetState() {
        cooldownTimer?.cancel()
        cooldownTimer = null
        cooldownMillisRemaining = 0L
        resetForEmail = null
        binding.resetFields.visibility = View.GONE
        binding.btnRequestCode.visibility = View.VISIBLE
        binding.btnRequestCode.setText(R.string.send_otp)
        binding.etResetCode.text?.clear()
        binding.etNewPassword.text?.clear()
        binding.etConfirmNewPassword.text?.clear()
        binding.tvResetMessage.setText(R.string.reset_password_instructions)
        renderCooldown()
        setLoading(false)
    }

    private fun showError(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        cooldownTimer?.cancel()
        cooldownTimer = null
        super.onDestroyView()
        _binding = null
    }
}
