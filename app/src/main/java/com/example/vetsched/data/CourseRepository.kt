package com.example.vetsched.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.example.vetsched.util.InputValidation

data class EnrolledCourse(
    val day: String,
    val timeRange: String,
    val section: String,
    val courseCode: String,
    val courseName: String,
    val room: String,
    val instructor: String,
    val offeringIds: List<Int>? = null,
    val type: String? = null
)

object CourseRepository {
    private val enrolledCourses = mutableListOf<EnrolledCourse>()
    private var isLoaded = false

    fun fetchEnrolled(context: Context, callback: (Boolean) -> Unit) {
        val sharedPref = context.getSharedPreferences("VETSCHED_PREFS", Context.MODE_PRIVATE)
        val studentId = sharedPref.getString("studentId", null)
            ?.let(InputValidation::normalizeStudentId) ?: return callback(false)

        com.example.vetsched.api.RetrofitClient.instance.getEnrolledCourses(studentId)
            .enqueue(object : retrofit2.Callback<List<EnrolledCourse>> {
                override fun onResponse(
                    call: retrofit2.Call<List<EnrolledCourse>>,
                    response: retrofit2.Response<List<EnrolledCourse>>
                ) {
                    if (response.isSuccessful) {
                        enrolledCourses.clear()
                        enrolledCourses.addAll(response.body() ?: emptyList())
                        isLoaded = true
                        saveToPrefs(context)
                        callback(true)
                    } else {
                        callback(false)
                    }
                }

                override fun onFailure(call: retrofit2.Call<List<EnrolledCourse>>, t: Throwable) {
                    callback(false)
                }
            })
    }

    fun enroll(course: EnrolledCourse, context: Context, callback: (Boolean) -> Unit) {
        val sharedPref = context.getSharedPreferences("VETSCHED_PREFS", Context.MODE_PRIVATE)
        val studentId = sharedPref.getString("studentId", null)
            ?.let(InputValidation::normalizeStudentId) ?: return callback(false)
        val offeringIds = course.offeringIds
            ?.takeIf { it.isNotEmpty() && it.all { id -> id > 0 } && it.distinct().size == it.size }
            ?: return callback(false)

        val request = com.example.vetsched.api.models.EnrollmentRequest(
            studentId = studentId,
            offeringIds = offeringIds
        )

        com.example.vetsched.api.RetrofitClient.instance.enroll(request)
            .enqueue(object : retrofit2.Callback<com.example.vetsched.api.models.AuthResponse> {
                override fun onResponse(
                    call: retrofit2.Call<com.example.vetsched.api.models.AuthResponse>,
                    response: retrofit2.Response<com.example.vetsched.api.models.AuthResponse>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        enrolledCourses.add(course)
                        saveToPrefs(context)
                        callback(true)
                    } else {
                        callback(false)
                    }
                }

                override fun onFailure(
                    call: retrofit2.Call<com.example.vetsched.api.models.AuthResponse>,
                    t: Throwable
                ) {
                    callback(false)
                }
            })
    }

    fun remove(course: EnrolledCourse, context: Context, callback: (Boolean) -> Unit) {
        val sharedPref = context.getSharedPreferences("VETSCHED_PREFS", Context.MODE_PRIVATE)
        val studentId = sharedPref.getString("studentId", null)
            ?.let(InputValidation::normalizeStudentId) ?: return callback(false)

        val ids = course.offeringIds
        if (ids.isNullOrEmpty()) {
            enrolledCourses.removeAll { it.courseCode == course.courseCode && it.section == course.section }
            saveToPrefs(context)
            return callback(true) 
        }

        val request = com.example.vetsched.api.models.EnrollmentRequest(
            studentId = studentId,
            offeringIds = ids
        )

        com.example.vetsched.api.RetrofitClient.instance.unenroll(request)
            .enqueue(object : retrofit2.Callback<com.example.vetsched.api.models.AuthResponse> {
                override fun onResponse(
                    call: retrofit2.Call<com.example.vetsched.api.models.AuthResponse>,
                    response: retrofit2.Response<com.example.vetsched.api.models.AuthResponse>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        enrolledCourses.removeAll { it.courseCode == course.courseCode && it.section == course.section }
                        saveToPrefs(context)
                        callback(true)
                    } else {
                        callback(false)
                    }
                }

                override fun onFailure(
                    call: retrofit2.Call<com.example.vetsched.api.models.AuthResponse>,
                    t: Throwable
                ) {
                    callback(false)
                }
            })
    }

    fun submitSchedule(context: Context, callback: (Boolean) -> Unit) {
        val sharedPref = context.getSharedPreferences("VETSCHED_PREFS", Context.MODE_PRIVATE)
        val studentId = sharedPref.getString("studentId", null)
            ?.let(InputValidation::normalizeStudentId) ?: return callback(false)
        val request = com.example.vetsched.api.models.ScheduleSubmissionRequest(studentId)

        com.example.vetsched.api.RetrofitClient.instance.submitSchedule(request)
            .enqueue(object : retrofit2.Callback<com.example.vetsched.api.models.AuthResponse> {
                override fun onResponse(
                    call: retrofit2.Call<com.example.vetsched.api.models.AuthResponse>,
                    response: retrofit2.Response<com.example.vetsched.api.models.AuthResponse>
                ) {
                    callback(response.isSuccessful && response.body()?.success == true)
                }

                override fun onFailure(
                    call: retrofit2.Call<com.example.vetsched.api.models.AuthResponse>,
                    t: Throwable
                ) {
                    callback(false)
                }
            })
    }

    fun clear(context: Context) {
        enrolledCourses.clear()
        saveToPrefs(context)
    }

    fun isEnrolled(courseCode: String, sectionName: String, context: Context): Boolean {
        if (!isLoaded) loadFromPrefs(context)
        return enrolledCourses.any { it.courseCode == courseCode && it.section == sectionName }
    }

    fun isConflicting(newDayStr: String, newTimeRange: String, context: Context): Boolean {
        if (newDayStr.isBlank() || newDayStr == "No days assigned") return false

        if (!isLoaded) loadFromPrefs(context)
        val newDays = parseDays(newDayStr)
        val newTime = parseRange(newTimeRange) ?: return false

        for (enrolled in enrolledCourses) {
            val enrolledDays = parseDays(enrolled.day)
            if (newDays.any { day -> enrolledDays.any { it.equals(day, ignoreCase = true) } }) {
                val enrolledTime = parseRange(enrolled.timeRange) ?: continue
                if (overlaps(newTime, enrolledTime)) {
                    return true
                }
            }
        }
        return false
    }

    private fun parseDays(dayStr: String): List<String> {
        return dayStr.replace("{", "")
            .replace("}", "")
            .replace("[", "")
            .replace("]", "")
            .replace("\"", "")
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    private data class TimeRange(val start: Int, val end: Int)

    private fun parseRange(range: String): TimeRange? {
        return try {
            val parts = range.split("-", "•")
            val cleanParts = parts.map { it.trim() }.filter { it.isNotEmpty() }
            val start = timeToMinutes(cleanParts[0])
            val end = if (cleanParts.size > 1) timeToMinutes(cleanParts[1]) else start + 60
            TimeRange(start, end)
        } catch (e: Exception) {
            null
        }
    }

    private fun timeToMinutes(time: String): Int {
        val clean = time.uppercase()
        val isPM = clean.contains("PM")
        val isAM = clean.contains("AM")
        val digits = clean.replace("AM", "").replace("PM", "").trim()
        val parts = digits.split(":")
        var h = parts[0].toInt()
        val m = if (parts.size > 1) parts[1].toInt() else 0

        if (isPM && h < 12) h += 12
        if (isAM && h == 12) h = 0
        return h * 60 + m
    }

    private fun overlaps(r1: TimeRange, r2: TimeRange): Boolean {
        return r1.start < r2.end && r2.start < r1.end
    }

    fun getCoursesForDay(dayFull: String, context: Context): List<EnrolledCourse> {
        if (!isLoaded) loadFromPrefs(context)
        val shortDay = when (dayFull) {
            "Monday" -> "Mon"
            "Tuesday" -> "Tue"
            "Wednesday" -> "Wed"
            "Thursday" -> "Thu"
            "Friday" -> "Fri"
            "Saturday" -> "Sat"
            "Sunday" -> "Sun"
            else -> dayFull
        }
        return enrolledCourses.filter { enrolled ->
            val enrolledDays = parseDays(enrolled.day)
            enrolledDays.any { it.equals(shortDay, ignoreCase = true) }
        }
    }

    fun getAllEnrolled(context: Context): List<EnrolledCourse> {
        if (!isLoaded) loadFromPrefs(context)
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
        isLoaded = true
    }
}
