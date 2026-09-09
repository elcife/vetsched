package com.example.vetsched.api

import com.example.vetsched.api.models.AuthResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("login.php")
    fun login(@Body params: Map<String, String>): Call<AuthResponse>

    @POST("add_account.php")
    fun register(@Body params: Map<String, String>): Call<AuthResponse>
}
