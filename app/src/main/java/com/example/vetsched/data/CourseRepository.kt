package com.example.vetsched.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class EnrolledCourse(
    val day: String,
    val timeRange: String,
    val section: String,
    val courseCode: String,
    val courseName: String,
    val room: String
)

object CourseRepository {
    private val enrolledCourses = mutableListOf<EnrolledCourse>()

    fun enroll(course: EnrolledCourse, context: Context) {
        if (!enrolledCourses.any { it.day == course.day && it.timeRange == course.timeRange }) {
            enrolledCourses.add(course)
            saveToPrefs(context)
        }
    }

    fun remove(course: EnrolledCourse, context: Context) {
        enrolledCourses.removeAll { it.courseCode == course.courseCode && it.day == course.day && it.timeRange == course.timeRange }
        saveToPrefs(context)
    }

    fun getCoursesForDay(day: String, context: Context): List<EnrolledCourse> {
        loadFromPrefs(context)
        return enrolledCourses.filter { it.day == day }
    }

    fun getAllEnrolled(context: Context): List<EnrolledCourse> {
        loadFromPrefs(context)
        return enrolledCourses
    }

    private fun saveToPrefs(context: Context) {
        val sharedPref = context.getSharedPreferences("VETSCHED_PREFS", Context.MODE_PRIVATE)
        val json = Gson().toJson(enrolledCourses)
        sharedPref.edit().putString("enrolled_courses", json).apply()
    }

    private fun loadFromPrefs(context: Context) {
        val sharedPref = context.getSharedPreferences("VETSCHED_PREFS", Context.MODE_PRIVATE)
        val json = sharedPref.getString("enrolled_courses", null)
        if (json != null) {
            val type = object : TypeToken<List<EnrolledCourse>>() {}.type
            val list: List<EnrolledCourse> = Gson().fromJson(json, type)
            enrolledCourses.clear()
            enrolledCourses.addAll(list)
        }
    }
}