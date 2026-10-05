package com.example.vetsched.ui

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.vetsched.R
import com.example.vetsched.data.CourseRepository
import com.example.vetsched.data.EnrolledCourse
import com.example.vetsched.databinding.FragmentConfirmScheduleBinding
import com.example.vetsched.databinding.ItemDayScheduleBinding
import com.example.vetsched.databinding.ItemScheduleCardBinding
import com.google.gson.Gson

class ConfirmScheduleFragment : Fragment() {

    private var _binding: FragmentConfirmScheduleBinding? = null
    private val binding get() = _binding!!

    private var previewCourses: List<EnrolledCourse> = emptyList()

    private val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConfirmScheduleBinding.inflate(inflater, container, false)
        
        val json = arguments?.getString("preview_courses_json")
        if (json != null) {
            val type = object : com.google.gson.reflect.TypeToken<List<EnrolledCourse>>() {}.type
            previewCourses = Gson().fromJson(json, type)
        }
        
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViewPager()
        setupTabs()

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        if (previewCourses.isNotEmpty()) {
            // Mode A: Section Enrollment preview (from CoursesFragment)
            binding.btnExportPdf.visibility = View.GONE
            binding.btnYesSubmit.isEnabled = true
            binding.btnYesSubmit.setBackgroundColor(Color.parseColor("#77C647"))
            binding.btnYesSubmit.text = "Confirm Enrollment"

            binding.btnYesSubmit.setOnClickListener {
                enrollAllPreviews(0)
            }
        } else {
            // Mode B: Schedule Submission & Review (from ScheduleFragment)
            binding.btnExportPdf.visibility = View.VISIBLE
            binding.btnExportPdf.setOnClickListener {
                com.example.vetsched.util.SchedulePdfExporter.exportToPdf(requireContext())
            }

            binding.btnYesSubmit.isEnabled = true
            binding.btnYesSubmit.setBackgroundColor(Color.parseColor("#5C6D4F"))
            binding.btnYesSubmit.text = "Submit Schedule to Admin"

            binding.btnYesSubmit.setOnClickListener {
                Toast.makeText(context, "Schedule successfully submitted to Admin!", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_confirmScheduleFragment_to_scheduleFragment)
            }
        }
    }

    private fun enrollAllPreviews(index: Int) {
        if (index >= previewCourses.size) {
            Toast.makeText(context, "Enrollment completed!", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.action_confirmScheduleFragment_to_scheduleFragment)
            return
        }

        CourseRepository.enroll(previewCourses[index], requireContext()) { success ->
            if (success) {
                Toast.makeText(context, "Enrolled in ${previewCourses[index].section}", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_confirmScheduleFragment_to_scheduleFragment)
            } else {
                Toast.makeText(context, "Enrollment failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.viewPager.adapter?.notifyDataSetChanged()
    }

    private fun setupViewPager() {
        val adapter = DailyScheduleAdapter(days)
        binding.viewPager.adapter = adapter

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                binding.tvHeaderTitle.text = days[position]
                updateTabsUI(position)
            }
        })
    }

    private fun setupTabs() {
        val tabLayouts = listOf(
            binding.tabMon, binding.tabTue, binding.tabWed,
            binding.tabThu, binding.tabFri, binding.tabSat, binding.tabSun
        )

        tabLayouts.forEachIndexed { index, layout ->
            layout.setOnClickListener {
                binding.viewPager.currentItem = index
            }
        }
    }

    private fun updateTabsUI(selectedPosition: Int) {
        val whiteColor = Color.WHITE
        val textPrimaryColor = ContextCompat.getColor(requireContext(), R.color.vetsched_text_primary)

        val tabLayouts = listOf(
            binding.tabMon, binding.tabTue, binding.tabWed,
            binding.tabThu, binding.tabFri, binding.tabSat, binding.tabSun
        )
        val labelViews = listOf(
            binding.tvMonLabel, binding.tvTueLabel, binding.tvWedLabel,
            binding.tvThuLabel, binding.tvFriLabel, binding.tvSatLabel, binding.tvSunLabel
        )
        val dateViews = listOf(
            binding.tvMonDate, binding.tvTueDate, binding.tvWedDate,
            binding.tvThuDate, binding.tvFriDate, binding.tvSatDate, binding.tvSunDate
        )

        tabLayouts.forEachIndexed { index, layout ->
            if (index < tabLayouts.size) {
                if (index == selectedPosition) {
                    layout.setBackgroundResource(R.drawable.bg_date_selected)
                    labelViews[index].setTextColor(whiteColor)
                    dateViews[index].setTextColor(whiteColor)
                } else {
                    layout.setBackgroundResource(R.drawable.bg_date_unselected)
                    labelViews[index].setTextColor(textPrimaryColor)
                    dateViews[index].setTextColor(textPrimaryColor)
                }
            }
        }

        if (selectedPosition < tabLayouts.size) {
            binding.dateSelector.post {
                val selectedTab = tabLayouts[selectedPosition]
                val scrollX = selectedTab.left - (binding.dateSelector.width - selectedTab.width) / 2
                binding.dateSelector.smoothScrollTo(scrollX, 0)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun getShortDay(fullDay: String): String {
        return when (fullDay) {
            "Monday" -> "Mon"
            "Tuesday" -> "Tue"
            "Wednesday" -> "Wed"
            "Thursday" -> "Thu"
            "Friday" -> "Fri"
            "Saturday" -> "Sat"
            "Sunday" -> "Sun"
            else -> fullDay.take(3)
        }
    }

    inner class DailyScheduleAdapter(private val days: List<String>) :
        RecyclerView.Adapter<DailyScheduleAdapter.ViewHolder>() {

        inner class ViewHolder(val itemBinding: ItemDayScheduleBinding) :
            RecyclerView.ViewHolder(itemBinding.root) {

            fun bind(dayTitle: String) {
                itemBinding.tvDayTitle.text = dayTitle
                itemBinding.cardsContainer.removeAllViews()

                val enrolled = CourseRepository.getCoursesForDay(dayTitle, itemBinding.root.context).toMutableList()
                
                // Add preview courses if they belong to this day
                val shortDay = getShortDay(dayTitle)
                previewCourses.forEach { pc ->
                    val pcDays = pc.day.split(",").map { it.trim() }
                    if (pcDays.any { it.equals(shortDay, ignoreCase = true) }) {
                        enrolled.add(pc)
                    }
                }

                itemBinding.tvActiveClasses.text = if (enrolled.size == 1) "1 Class" else "${enrolled.size} Classes"
                
                itemBinding.cardsContainer.visibility = if (enrolled.isNotEmpty()) View.VISIBLE else View.GONE

                enrolled.forEach { course ->
                    if (course.courseCode.isNotBlank()) {
                        val isPreview = previewCourses.contains(course)
                        addCard(itemBinding, course, isPreview)
                    }
                }

                setupTimeline(itemBinding)
            }

            private fun addCard(itemBinding: ItemDayScheduleBinding, course: EnrolledCourse, isPreview: Boolean = false) {
                val cardBinding = ItemScheduleCardBinding.inflate(
                    LayoutInflater.from(itemBinding.root.context),
                    itemBinding.cardsContainer,
                    false
                )

                cardBinding.tvTimeRange.text = ScheduleTimeRange.format(course.timeRange)
                cardBinding.tvSection.text = if (course.type != null) "${course.section} (${course.type})" else course.section
                cardBinding.tvCourseName.text = if (isPreview) "[PREVIEW] ${course.courseName}" else course.courseName
                cardBinding.tvLocation.text = course.room
                cardBinding.tvInstructor.text = course.instructor
                
                if (isPreview) {
                    cardBinding.root.setBackgroundResource(R.drawable.bg_schedule_card)
                }

                cardBinding.btnRemove.visibility = View.GONE

                val timeInfo = ScheduleTimeRange.parse(course.timeRange) ?: return
                val density = itemBinding.root.resources.displayMetrics.density
                val timelineStartMinutes = 7 * 60
                val timelineEndMinutes = 22 * 60
                val visibleStart = timeInfo.startMinutes.coerceAtLeast(timelineStartMinutes)
                val visibleEnd = timeInfo.endMinutes.coerceAtMost(timelineEndMinutes)
                if (visibleStart >= visibleEnd) return
                val hourHeight = itemBinding.root.resources.getDimension(R.dimen.schedule_timeline_hour_height)
                val labelWidth = itemBinding.root.resources.getDimension(R.dimen.schedule_timeline_label_width)
                val markerOffset = itemBinding.root.resources.getDimension(R.dimen.schedule_timeline_marker_offset)
                val lineOverlap = itemBinding.root.resources.getDimension(R.dimen.schedule_timeline_card_line_overlap)
                val topMargin = markerOffset - lineOverlap +
                    (visibleStart - timelineStartMinutes) * hourHeight / 60f
                val cardHeight = ((visibleEnd - visibleStart) * hourHeight / 60f + 2 * lineOverlap).coerceAtLeast(
                    itemBinding.root.resources.displayMetrics.density * 40f
                )

                val params = ConstraintLayout.LayoutParams(
                    ConstraintLayout.LayoutParams.MATCH_CONSTRAINT,
                    cardHeight.toInt()
                )
                params.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                params.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                params.marginStart = labelWidth.toInt()
                params.marginEnd = 0
                params.topMargin = topMargin.toInt()

                cardBinding.root.layoutParams = params
                cardBinding.root.elevation = 4 * density
                itemBinding.cardsContainer.addView(cardBinding.root)
            }

        }

        private fun setupTimeline(itemBinding: ItemDayScheduleBinding) {
            ScheduleTimelineLabels.bind(
                listOf(
                    itemBinding.row7, itemBinding.row8, itemBinding.row9,
                    itemBinding.row10, itemBinding.row11, itemBinding.row12,
                    itemBinding.row1, itemBinding.row2, itemBinding.row3,
                    itemBinding.row4, itemBinding.row5, itemBinding.row6,
                    itemBinding.row7pm, itemBinding.row8pm, itemBinding.row9pm
                )
            )
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(ItemDayScheduleBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(days[position])
        }

        override fun getItemCount(): Int = days.size
    }

}