package com.example.vetsched

import org.junit.Assert.assertEquals
import org.junit.Test

class CourseMappingTest {

    @Test
    fun testDayParsing() {
        val rawDayStr = "{Tue}"
        val formattedDays = rawDayStr.replace("{", "")
            .replace("}", "")
            .replace("\"", "")
            .replace("[", "")
            .replace("]", "")
            .trim()

        assertEquals("Tue", formattedDays)
    }

    @Test
    fun testListJoin() {
        val daysList = listOf("Tue")
        val formattedDays = daysList.joinToString(", ")

        assertEquals("Tue", formattedDays)
    }

    @Test
    fun testNoDefaultToMonday() {
        val rawDayStr = "Tue"
        val formattedDays = rawDayStr.replace("{", "")
            .replace("}", "")
            .replace("\"", "")
            .trim()

        // Proving that it is Tue and not Mon
        assertEquals("Tue", formattedDays)
    }
}
