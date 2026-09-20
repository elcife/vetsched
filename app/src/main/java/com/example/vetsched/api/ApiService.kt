package com.example.vetsched.api

import com.example.vetsched.api.models.AuthResponse
import com.example.vetsched.api.models.EnrollmentRequest
import com.example.vetsched.api.models.Section
import com.example.vetsched.api.models.Subject
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {
    @POST("login.php")
    fun login(@Body params: Map<String, String>): Call<AuthResponse>

    @POST("add_account.php")
    fun register(@Body params: Map<String, String>): Call<AuthResponse>

    @POST("change_password.php")
    fun changePassword(@Body params: Map<String, String>): Call<AuthResponse>

    @POST("delete_account.php")
    fun deleteAccount(@Body params: Map<String, String>): Call<AuthResponse>

    @POST("update_year_level.php")
    fun updateYearLevel(@Body params: Map<String, String>): Call<AuthResponse>

    @GET("api.php?resource=subjects")
    fun getSubjects(): Call<List<Subject>>

    @GET("api.php?resource=sections")
    fun getSections(): Call<List<Section>>

    @GET("enrollment.php?resource=enrolled_courses")
    fun getEnrolledCourses(@Query("studentId") studentId: String): Call<List<com.example.vetsched.data.EnrolledCourse>>

    @POST("enrollment.php?resource=enroll")
    fun enroll(@Body request: EnrollmentRequest): Call<AuthResponse>

    @POST("enrollment.php?resource=unenroll")
    fun unenroll(@Body request: EnrollmentRequest): Call<AuthResponse>
}
