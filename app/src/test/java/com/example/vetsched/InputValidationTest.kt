package com.example.vetsched

import com.example.vetsched.util.InputValidation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InputValidationTest {

    @Test
    fun normalizesAndValidatesStudentIds() {
        assertEquals("052526981919", InputValidation.normalizeStudentId(" 05-2526-981919 "))
        assertEquals("123456789", InputValidation.normalizeStudentId("123456789"))
        assertNull(InputValidation.normalizeStudentId("12<script>3456789"))
        assertNull(InputValidation.normalizeStudentId("12345678"))
    }

    @Test
    fun validatesNamesWithoutDeletingInvalidCharacters() {
        assertEquals("Mary Jane", InputValidation.normalizeName("  Mary   Jane "))
        assertTrue(InputValidation.isValidName("Mary-Jane O'Neil"))
        assertFalse(InputValidation.isValidName("Mary\nJane"))
        assertFalse(InputValidation.isValidName(" "))
    }

    @Test
    fun enforcesPasswordAndOtpBounds() {
        assertTrue(InputValidation.isValidPassword("a".repeat(10)))
        assertFalse(InputValidation.isValidPassword("short"))
        assertFalse(InputValidation.isValidPassword("a".repeat(129)))
        assertTrue(InputValidation.isValidOtp("012345"))
        assertFalse(InputValidation.isValidOtp("12345x"))
    }
}
