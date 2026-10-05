package com.example.vetsched.notifications

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.example.vetsched.R

object NotificationInbox {
    fun show(context: Context, notifications: List<CourseUpdateNotification>) {
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(context, 20), dp(context, 12), dp(context, 20), dp(context, 12))
        }

        content.addView(TextView(context).apply {
            text = "Subject and schedule changes for your selected year level."
            textSize = 14f
            setTextColor(Color.DKGRAY)
            setPadding(dp(context, 2), 0, dp(context, 2), dp(context, 14))
        })

        if (notifications.isEmpty()) {
            content.addView(TextView(context).apply {
                text = "No updates yet. Changes to subjects, sections, and class times will appear here."
                textSize = 16f
                setTextColor(Color.DKGRAY)
                setPadding(0, dp(context, 16), 0, dp(context, 16))
            })
        } else {
            notifications.forEachIndexed { index, notification ->
                content.addView(createNotificationCard(context, notification))
                if (index != notifications.lastIndex) {
                    content.addView(LinearLayout(context).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            dp(context, 12)
                        )
                    })
                }
            }
        }

        val scrollView = ScrollView(context).apply {
            isFillViewport = true
            addView(content)
        }
        val maxHeight = (context.resources.displayMetrics.heightPixels * 0.65f).toInt()
        scrollView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            maxHeight
        )

        AlertDialog.Builder(context)
            .setTitle("Course updates")
            .setView(scrollView)
            .setPositiveButton("Done", null)
            .show()
    }

    private fun createNotificationCard(
        context: Context,
        notification: CourseUpdateNotification
    ): LinearLayout {
        val isConflict = notification.title.equals("Schedule conflict", ignoreCase = true)
        val isRemoved = notification.title.equals("Subject removed", ignoreCase = true) ||
            notification.title.equals("Section removed", ignoreCase = true)
        val isClosed = notification.title.equals("Section closed", ignoreCase = true)
        val isOpen = notification.title.equals("Section now open", ignoreCase = true)
        val accentColor = when {
            isConflict || isRemoved -> R.color.vetsched_warning
            isClosed -> R.color.vetsched_closed
            isOpen -> R.color.vetsched_open
            else -> R.color.vetsched_primary
        }
        val backgroundColor = when {
            isConflict || isRemoved -> R.color.vetsched_warning_background
            isClosed -> R.color.vetsched_closed_background
            isOpen -> R.color.vetsched_open_background
            else -> R.color.vetsched_card_bg
        }
        val borderColor = when {
            isConflict || isRemoved -> R.color.vetsched_warning_border
            isClosed -> R.color.vetsched_closed_border
            isOpen -> R.color.vetsched_open_border
            else -> R.color.vetsched_divider
        }
        val primary = ContextCompat.getColor(
            context,
            accentColor
        )
        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            clipToOutline = true
            background = GradientDrawable().apply {
                setColor(ContextCompat.getColor(context, backgroundColor))
                cornerRadius = dp(context, 12).toFloat()
                setStroke(dp(context, 1), ContextCompat.getColor(context, borderColor))
            }
        }

        card.addView(View(context).apply {
            setBackgroundColor(primary)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(context, 4)
            )
        })

        val details = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(context, 16), dp(context, 14), dp(context, 16), dp(context, 14))
        }
        details.addView(TextView(context).apply {
            text = when {
                isConflict -> "⚠  ${notification.title}"
                isRemoved -> "✕  ${notification.title}"
                isClosed -> "●  ${notification.title}"
                isOpen -> "✓  ${notification.title}"
                else -> notification.title
            }
            textSize = 18f
            setTextColor(primary)
            setTypeface(typeface, Typeface.BOLD)
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.END
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(context, 7)
            }
        })

        val lines = notification.message.split('\n')
        if (lines.size >= 3 &&
            lines[1].startsWith("Before: ") &&
            lines[2].startsWith("Now: ")
        ) {
            details.addView(TextView(context).apply {
                text = notification.emphasizedText(lines.first(), context)
                textSize = 16f
                setTextColor(Color.rgb(42, 45, 40))
                setLineSpacing(dp(context, 3).toFloat(), 1f)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dp(context, 12)
                }
            })
            addScheduleRow(context, details, "BEFORE", lines[1].removePrefix("Before: "), false, notification)
            addScheduleRow(context, details, "NOW", lines[2].removePrefix("Now: "), true, notification)
        } else {
            details.addView(TextView(context).apply {
                text = notification.emphasizedMessage(context)
                textSize = 16f
                setTextColor(Color.rgb(42, 45, 40))
                setLineSpacing(dp(context, 3).toFloat(), 1f)
                contentDescription = notification.message
            })
        }

        details.addView(TextView(context).apply {
            text = notification.displayDate()
            textSize = 13f
            setTextColor(Color.DKGRAY)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(context, 12)
            }
        })
        card.addView(details)
        return card
    }

    private fun addScheduleRow(
        context: Context,
        parent: LinearLayout,
        label: String,
        detail: String,
        isCurrent: Boolean,
        notification: CourseUpdateNotification
    ) {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.TOP
            setPadding(0, dp(context, 7), 0, dp(context, 7))
        }
        val labelColor = if (isCurrent) {
            ContextCompat.getColor(context, R.color.vetsched_primary)
        } else {
            Color.rgb(143, 66, 58)
        }
        row.addView(TextView(context).apply {
            text = label
            textSize = 12f
            setTextColor(labelColor)
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(if (isCurrent) Color.rgb(225, 236, 219) else Color.rgb(246, 229, 226))
                cornerRadius = dp(context, 6).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(dp(context, 68), dp(context, 28)).apply {
                marginEnd = dp(context, 10)
            }
        })
        row.addView(TextView(context).apply {
            text = notification.emphasizedText(detail, context)
            textSize = 15f
            setTextColor(Color.rgb(42, 45, 40))
            setLineSpacing(dp(context, 2).toFloat(), 1f)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        parent.addView(row)
    }

    private fun dp(context: Context, value: Int): Int =
        (value * context.resources.displayMetrics.density).toInt()
}
