package com.example.vetsched.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.vetsched.data.CourseRepository
import com.example.vetsched.data.EnrolledCourse
import com.example.vetsched.ui.ScheduleTimeRange
import java.io.File
import java.io.FileOutputStream

object SchedulePdfExporter {

    private val PALETTE = listOf(
        "#B4C6E7", // Soft Light Blue
        "#FFD966", // Amber / Gold
        "#A6A6A6", // Grey
        "#A9D08E", // Light Green
        "#7030A0", // Purple
        "#00B0F0", // Cyan / Sky Blue
        "#FCE4D6", // Soft Peach
        "#E2EFDA", // Soft Sage
        "#F8CBAD"  // Soft Orange
    )

    fun exportToPdf(context: Context, extraPreviewCourses: List<EnrolledCourse> = emptyList()) {
        val sharedPref = context.getSharedPreferences("VETSCHED_PREFS", Context.MODE_PRIVATE)
        val studentName = sharedPref.getString("userName", "Student") ?: "Student"
        val studentId = sharedPref.getString("studentId", "N/A") ?: "N/A"
        val yearLevelInt = sharedPref.getInt("userYearLevel", 0)
        val yearLevelStr = if (yearLevelInt in 1..5) {
            when (yearLevelInt) {
                1 -> "1st Year"
                2 -> "2nd Year"
                3 -> "3rd Year"
                4 -> "4th Year"
                else -> "5th Year"
            }
        } else "N/A"

        val allEnrolled = (CourseRepository.getAllEnrolled(context) + extraPreviewCourses).distinctBy {
            "${it.courseCode}_${it.day}_${it.timeRange}"
        }

        if (allEnrolled.isEmpty()) {
            Toast.makeText(context, "No subjects in schedule to export.", Toast.LENGTH_SHORT).show()
            return
        }

        val pdfDocument = PdfDocument()
        // Page dimensions: Landscape A4 (842 x 595 pt)
        val pageWidth = 842
        val pageHeight = 595
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        // Paints
        val fillPaint = Paint().apply { style = Paint.Style.FILL }
        val strokePaint = Paint().apply {
            style = Paint.Style.STROKE
            color = Color.BLACK
            strokeWidth = 1f
        }
        val textPaint = Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        // 1. Top Banner (Orange)
        val topBannerHeight = 28f
        fillPaint.color = Color.parseColor("#ED7D31")
        canvas.drawRect(20f, 15f, pageWidth - 20f, 15f + topBannerHeight, fillPaint)

        textPaint.apply {
            color = Color.WHITE
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(
            "VETSCHED OFFICIAL CLASS SCHEDULE",
            pageWidth / 2f,
            15f + 19f,
            textPaint
        )

        // 2. Student Information Header (Name and ID, no section)
        val infoY = 58f
        textPaint.apply {
            color = Color.BLACK
            textSize = 10.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }
        canvas.drawText("NAME: $studentName", 25f, infoY, textPaint)
        canvas.drawText("STUDENT ID: $studentId", 350f, infoY, textPaint)
        canvas.drawText("YEAR LEVEL: $yearLevelStr", 620f, infoY, textPaint)

        // 3. Grid Table Setup
        val startX = 20f
        val startY = 68f
        val timeColWidth = 75f
        val dayColWidth = (pageWidth - 40f - timeColWidth) / 6f // 6 days: MON..SAT
        val headerRowHeight = 22f

        val days = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT")
        val daysFull = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")

        // Draw Table Header Row (Dark Green fill, Yellow/Gold text)
        fillPaint.color = Color.parseColor("#1E4620")
        canvas.drawRect(startX, startY, startX + timeColWidth + 6 * dayColWidth, startY + headerRowHeight, fillPaint)

        textPaint.apply {
            color = Color.parseColor("#FFE600")
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        // Header Labels
        canvas.drawText("TIME", startX + timeColWidth / 2f, startY + 15f, textPaint)
        days.forEachIndexed { i, day ->
            val colX = startX + timeColWidth + i * dayColWidth + dayColWidth / 2f
            canvas.drawText(day, colX, startY + 15f, textPaint)
        }

        // Time slots definition (7:00 AM to 10:00 PM in half-hour increments)
        val timeLabels = listOf(
            "7:00-7:30", "7:30-8:00", "8:00-8:30", "8:30-9:00",
            "9:00-9:30", "9:30-10:00", "10:00-10:30", "10:30-11:00",
            "11:00-11:30", "11:30-12:00", "12:00-12:30", "12:30-1:00",
            "1:00-1:30", "1:30-2:00", "2:00-2:30", "2:30-3:00",
            "3:00-3:30", "3:30-4:00", "4:00-4:30", "4:30-5:00",
            "5:00-5:30", "5:30-6:00", "6:00-6:30", "6:30-7:00",
            "7:00-7:30", "7:30-8:00", "8:00-8:30", "8:30-9:00",
            "9:00-9:30", "9:30-10:00"
        )

        val slotHeight = 15.5f
        val gridTopY = startY + headerRowHeight
        val gridBottomY = gridTopY + timeLabels.size * slotHeight

        // Draw Time Slot Rows & Lines
        timeLabels.forEachIndexed { idx, label ->
            val rowY = gridTopY + idx * slotHeight

            // Time column background
            fillPaint.color = Color.parseColor("#F2F2F2")
            canvas.drawRect(startX, rowY, startX + timeColWidth, rowY + slotHeight, fillPaint)

            // Time text
            textPaint.apply {
                color = Color.DKGRAY
                textSize = 7.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(label, startX + timeColWidth / 2f, rowY + 11f, textPaint)

            // Horizontal grid line
            canvas.drawLine(startX, rowY, startX + timeColWidth + 6 * dayColWidth, rowY, strokePaint)
        }

        // Bottom border of grid
        canvas.drawLine(startX, gridBottomY, startX + timeColWidth + 6 * dayColWidth, gridBottomY, strokePaint)

        // Vertical grid lines for columns
        var currentX = startX
        canvas.drawLine(currentX, startY, currentX, gridBottomY, strokePaint)
        currentX += timeColWidth
        canvas.drawLine(currentX, startY, currentX, gridBottomY, strokePaint)

        for (i in 0..5) {
            currentX += dayColWidth
            canvas.drawLine(currentX, startY, currentX, gridBottomY, strokePaint)
        }

        // Map course codes to colors
        val courseColorMap = mutableMapOf<String, String>()
        var colorIdx = 0

        // 4. Draw Course Blocks
        val timelineStartMin = 7 * 60 // 7:00 AM = 420 mins

        allEnrolled.forEach { course ->
            val colorHex = courseColorMap.getOrPut(course.courseCode) {
                PALETTE[colorIdx % PALETTE.size].also { colorIdx++ }
            }

            val range = ScheduleTimeRange.parse(course.timeRange) ?: return@forEach
            val startMin = range.startMinutes
            val endMin = range.startMinutes + range.durationMinutes

            val startSlotIdx = ((startMin - timelineStartMin) / 30f).coerceIn(0f, timeLabels.size.toFloat())
            val endSlotIdx = ((endMin - timelineStartMin) / 30f).coerceIn(0f, timeLabels.size.toFloat())

            if (endSlotIdx <= startSlotIdx) return@forEach

            val blockTop = gridTopY + startSlotIdx * slotHeight
            val blockBottom = gridTopY + endSlotIdx * slotHeight

            // Identify matching day columns
            val courseDays = parseDays(course.day)

            daysFull.forEachIndexed { dIdx, dayFull ->
                val shortDay = when (dayFull) {
                    "Monday" -> "Mon"
                    "Tuesday" -> "Tue"
                    "Wednesday" -> "Wed"
                    "Thursday" -> "Thu"
                    "Friday" -> "Fri"
                    "Saturday" -> "Sat"
                    else -> dayFull
                }

                if (courseDays.any { it.equals(shortDay, ignoreCase = true) }) {
                    val leftX = startX + timeColWidth + dIdx * dayColWidth
                    val rightX = leftX + dayColWidth

                    // Draw block background
                    fillPaint.color = Color.parseColor(colorHex)
                    canvas.drawRect(leftX + 0.5f, blockTop + 0.5f, rightX - 0.5f, blockBottom - 0.5f, fillPaint)

                    // Draw block border
                    canvas.drawRect(leftX + 0.5f, blockTop + 0.5f, rightX - 0.5f, blockBottom - 0.5f, strokePaint)

                    // Text color
                    val textColor = if (colorHex.equals("#7030A0", ignoreCase = true)) Color.WHITE else Color.BLACK

                    // Lines to draw inside box (no section)
                    val lines = mutableListOf<String>()
                    lines.add(course.courseCode)
                    if (!course.type.isNullOrBlank()) {
                        lines.add(course.type)
                    }
                    if (!course.instructor.isNullOrBlank()) {
                        lines.add(course.instructor)
                    }
                    if (!course.room.isNullOrBlank()) {
                        lines.add(course.room)
                    }

                    val boxHeight = blockBottom - blockTop
                    textPaint.apply {
                        color = textColor
                        textSize = if (boxHeight < 35f) 7f else 8f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        textAlign = Paint.Align.CENTER
                    }

                    val totalTextHeight = lines.size * (textPaint.textSize + 2f)
                    var textY = blockTop + (boxHeight - totalTextHeight) / 2f + textPaint.textSize

                    lines.forEach { line ->
                        if (textY < blockBottom - 2f) {
                            val maxChars = (dayColWidth / (textPaint.textSize * 0.55f)).toInt().coerceAtLeast(5)
                            val displayLine = if (line.length > maxChars) line.take(maxChars - 2) + ".." else line
                            canvas.drawText(displayLine, leftX + dayColWidth / 2f, textY, textPaint)
                            textY += textPaint.textSize + 2f
                        }
                    }
                }
            }
        }

        pdfDocument.finishPage(page)

        // Save PDF file
        val pdfDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
        val sanitizeName = studentName.replace(Regex("[^a-zA-Z0-9]"), "_")
        val pdfFile = File(pdfDir, "VETSCHED_Schedule_${sanitizeName}.pdf")

        try {
            FileOutputStream(pdfFile).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()

            Toast.makeText(context, "Exported PDF: ${pdfFile.name}", Toast.LENGTH_LONG).show()

            // View PDF Intent
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Open Schedule PDF"))
        } catch (e: Exception) {
            pdfDocument.close()
            Toast.makeText(context, "Failed to export PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
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
}
