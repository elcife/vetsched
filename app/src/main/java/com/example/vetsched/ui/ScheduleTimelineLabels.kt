package com.example.vetsched.ui

import com.example.vetsched.databinding.ViewTimelineRowBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

internal object ScheduleTimelineLabels {
    fun bind(rows: List<ViewTimelineRowBinding>) {
        val formatter = SimpleDateFormat("h:mm a", Locale.getDefault())
        val time = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 7)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        rows.forEach { row ->
            row.tvTime.text = formatter.format(time.time)
            time.add(Calendar.MINUTE, 30)
            row.tvHalfHour.text = formatter.format(time.time)
            row.tvHalfHour.visibility = android.view.View.VISIBLE
            time.add(Calendar.MINUTE, 30)
        }
    }
}
