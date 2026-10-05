package com.example.vetsched.util

import android.util.Patterns
import java.util.Locale

object InputValidation {
    fun normalizeEmail(value: String): String = value.trim().lowercase(Locale.ROOT)

    fun isValidEmail(value: String): Boolean {
        val email = normalizeEmail(value)
        return email.length <= 254 && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun normalizeName(value: String): String =
        value.trim().replace(Regex("\\s+"), " ")

    fun isValidName(value: String): Boolean {
        if (value.any(Char::isISOControl)) return false
        val name = normalizeName(value)
        return name.isNotEmpty() &&
            name.codePointCount(0, name.length) <= 80
    }

    fun normalizeStudentId(value: String): String? {
        val raw = value.trim()
        if (!raw.matches(Regex("(?:[0-9]{9,12}|[0-9]{2}-[0-9]{4}-[0-9]{3,6})"))) return null
        return raw.replace("-", "").takeIf { it.length in 9..12 }
    }

    fun isValidPassword(value: String): Boolean {
        val length = value.codePointCount(0, value.length)
        return length in 10..128
    }

    fun isPasswordWithinLimit(value: String): Boolean =
        value.isNotEmpty() && value.codePointCount(0, value.length) <= 128

    fun isValidOtp(value: String): Boolean =
        value.length == 6 && value.all { it in '0'..'9' }
}
