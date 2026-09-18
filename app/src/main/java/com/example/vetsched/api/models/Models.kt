package com.example.vetsched.api.models

import com.google.gson.annotations.SerializedName

data class User(
    @SerializedName("student_id") val studentId: String,
    @SerializedName("first_name") val firstName: String,
    @SerializedName("last_name") val lastName: String,
    val email: String,
    @SerializedName("year_level") val yearLevel: Int? = null
)

data class Subject(
    val id: Int,
    val code: String,
    val name: String,
    val yearLevel: Int,
    val sections: List<Section>
)

data class Section(
    val id: Int,
    val type: String,
    val start: String,
    val end: String,
    val days: Any?,
    @SerializedName("day") val day: String? = null,
    @SerializedName("section_name") val sectionName: String? = null,
    @SerializedName("day_of_week") val dayOfWeek: String? = null,
    @SerializedName("schedule_day") val scheduleDay: String? = null,
    val instructor: String,
    val room: String,
    val maxCapacity: Int,
    val subjectId: Int
)

data class AuthResponse(
    val success: Boolean,
    val message: String,
    @SerializedName("error_field") val errorField: String? = null,
    val user: User? = null
)
