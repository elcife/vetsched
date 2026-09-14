package com.example.vetsched.ui

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.vetsched.R
import com.example.vetsched.data.CourseRepository
import com.example.vetsched.data.EnrolledCourse
import com.example.vetsched.databinding.FragmentScheduleBinding
import com.example.vetsched.databinding.ItemDayScheduleBinding
import com.example.vetsched.databinding.ItemScheduleCardBinding

class ScheduleFragment : Fragment() {

    private var _binding: FragmentScheduleBinding? = null
    private val binding get() = _binding!!

    private val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScheduleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sharedPref = requireActivity().getSharedPreferences("VETSCHED_PREFS", Context.MODE_PRIVATE)
        binding.tvUserName.text = sharedPref.getString("userName", "Student")

        setupViewPager()
        setupTabs()

        binding.btnProfile.setOnClickListener {
            findNavController().navigate(R.id.action_scheduleFragment_to_profileFragment)
        }

        binding.btnCourses.setOnClickListener {
            findNavController().navigate(R.id.action_scheduleFragment_to_coursesFragment)
        }

        binding.btnSubmit.setOnClickListener {
            findNavController().navigate(R.id.action_scheduleFragment_to_confirmScheduleFragment)
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
                updateTabsUI(position)
            }
        })
    }

    private fun setupTabs() {
        val tabLayouts = listOf(
            binding.tabMon, binding.tabTue, binding.tabWed, 
            binding.tabThu, binding.tabFri, binding.tabSat
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
            binding.tabThu, binding.tabFri, binding.tabSat
        )
        val labelViews = listOf(
            binding.tvMonLabel, binding.tvTueLabel, binding.tvWedLabel, 
            binding.tvThuLabel, binding.tvFriLabel, binding.tvSatLabel
        )
        val dateViews = listOf(
            binding.tvMonDate, binding.tvTueDate, binding.tvWedDate, 
            binding.tvThuDate, binding.tvFriDate, binding.tvSatDate
        )

        tabLayouts.forEachIndexed { index, layout ->
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

        binding.dateSelector.post {
            val selectedTab = tabLayouts[selectedPosition]
            val scrollX = selectedTab.left - (binding.dateSelector.width - selectedTab.width) / 2
            binding.dateSelector.smoothScrollTo(scrollX, 0)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class DailyScheduleAdapter(private val days: List<String>) : 
        RecyclerView.Adapter<DailyScheduleAdapter.ViewHolder>() {

        inner class ViewHolder(val itemBinding: ItemDayScheduleBinding) : 
            RecyclerView.ViewHolder(itemBinding.root) {
            
            fun bind(dayTitle: String) {
                itemBinding.tvDayTitle.text = dayTitle
                itemBinding.cardsContainer.removeAllViews()
                
                val enrolled = CourseRepository.getCoursesForDay(dayTitle, itemBinding.root.context)
                itemBinding.tvActiveClasses.text = if (enrolled.size == 1) "1 Active Class" else "${enrolled.size} Active Classes"
                
                itemBinding.cardsContainer.visibility = if (enrolled.isNotEmpty()) View.VISIBLE else View.GONE

                enrolled.forEach { course ->
                    if (course.courseCode.isNotBlank()) {
                        addCard(itemBinding, course)
                    }
                }

                setupTimeline(itemBinding)
            }

            private fun addCard(itemBinding: ItemDayScheduleBinding, course: EnrolledCourse) {
                val cardBinding = ItemScheduleCardBinding.inflate(
                    LayoutInflater.from(itemBinding.root.context),
                    itemBinding.cardsContainer,
                    false
                )
                
                cardBinding.tvTimeRange.text = course.timeRange
                cardBinding.tvSection.text = course.section
                cardBinding.tvCourseName.text = course.courseName
                cardBinding.tvLocation.text = course.room
                cardBinding.tvInstructorMasked.text = "• ************"
                
                cardBinding.btnRemove.setOnClickListener {
                    CourseRepository.remove(course, it.context)
                    bind(course.day)
                }

                val timeInfo = parseTimeRange(course.timeRange)
                val density = itemBinding.root.resources.displayMetrics.density
                
                val marginTopDp = (timeInfo.startMinutes - 7 * 60) * 80 / 60
                val heightDp = Math.max(80, timeInfo.durationMinutes * 80 / 60)

                val params = ConstraintLayout.LayoutParams(
                    ConstraintLayout.LayoutParams.MATCH_CONSTRAINT,
                    (heightDp * density).toInt()
                )
                params.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                params.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                params.marginStart = (64 * density).toInt()
                params.marginEnd = (16 * density).toInt()
                params.topMargin = (marginTopDp * density).toInt()
                
                cardBinding.root.layoutParams = params
                itemBinding.cardsContainer.addView(cardBinding.root)
            }

            private fun parseTimeRange(range: String): TimeInfo {
                val parts = range.split("-", "/")
                val startStr = parts[0].trim()
                val endStr = parts[1].trim()
                
                val endMin = timeToMinutes(endStr)
                var startMin = timeToMinutes(startStr)
                
                if (!startStr.contains("AM", true) && !startStr.contains("PM", true)) {
                    if (endMin >= 12 * 60) {
                        val startHour = startStr.split(":")[0].toInt()
                        if (startHour < 7 || startHour < (endMin / 60 % 12)) {
                            if (startMin + 12 * 60 <= endMin) {
                                startMin += 12 * 60
                            }
                        }
                    }
                }
                
                if (startMin >= endMin && startMin >= 12 * 60) {
                    startMin -= 12 * 60
                }
                
                return TimeInfo(startMin, Math.max(60, endMin - startMin))
            }

            private fun timeToMinutes(time: String): Int {
                val cleanTime = time.replace("AM", "", true).replace("PM", "", true).trim()
                val hm = cleanTime.split(":")
                var h = hm[0].toInt()
                val m = if (hm.size > 1) {
                    val mm = hm[1].split(" ")
                    if (mm.isNotEmpty()) mm[0].toInt() else 0
                } else 0
                
                if (time.contains("PM", true) && h < 12) h += 12
                if (time.contains("AM", true) && h == 12) h = 0
                
                return h * 60 + m
            }
        }

        private fun setupTimeline(itemBinding: ItemDayScheduleBinding) {
            val times = listOf("7:00", "8:00", "9:00", "10:00", "11:00", "12:00", "1:00", "2:00", "3:00", "4:00", "5:00", "6:00")
            val rows = listOf(itemBinding.row7, itemBinding.row8, itemBinding.row9, itemBinding.row10, itemBinding.row11, itemBinding.row12, itemBinding.row1, itemBinding.row2, itemBinding.row3, itemBinding.row4, itemBinding.row5, itemBinding.row6)
            
            times.forEachIndexed { i, time -> rows[i].tvTime.text = time }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(ItemDayScheduleBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(days[position])
        }

        override fun getItemCount(): Int = days.size
    }

    data class TimeInfo(val startMinutes: Int, val durationMinutes: Int)
}