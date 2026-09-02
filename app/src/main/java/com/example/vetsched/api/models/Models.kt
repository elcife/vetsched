package com.example.vetsched.api.models

import com.google.gson.annotations.SerializedName

data class User(
    @SerializedName("student_id") val studentId: String,
    @SerializedName("first_name") val firstName: String,
    @SerializedName("last_name") val lastName: String,
    val email: String
)

data class AuthResponse(
    val success: Boolean,
    val message: String,
    @SerializedName("error_field") val errorField: String? = null,
    val user: User? = null
)
