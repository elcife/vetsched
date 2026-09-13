package com.example.vetsched.ui

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.vetsched.R
import com.example.vetsched.data.CourseRepository
import com.example.vetsched.data.EnrolledCourse
import com.example.vetsched.databinding.FragmentCoursesBinding
import com.example.vetsched.databinding.ItemCourseBinding
import com.example.vetsched.databinding.ItemDayHeaderBinding

data class CourseModel(
    val code: String,
    val name: String,
    val day: String,
    val timeA: String,
    val timeB: String,
    val roomA: String,
    val roomB: String
)

sealed class CoursesListItem {
    data class DayHeader(val day: String, val count: Int) : CoursesListItem()
    data class Course(val course: CourseModel) : CoursesListItem()
}

class CoursesFragment : Fragment() {

    private var _binding: FragmentCoursesBinding? = null
    private val binding get() = _binding!!

    private val expandedDays = mutableSetOf<String>()
    private var currentQuery = ""

    private val allCourses = listOf(
        CourseModel("******** 101", "**************************", "Monday", "8:00 - 9:30 AM", "1:00 - 2:30 PM", "VM-201", "VM-305"),
        CourseModel("****** 201", "**************************", "Tuesday", "10:00 - 12:00 PM", "3:00 - 5:00 PM", "VM-Lab 2", "VM-202"),
        CourseModel("******** 301", "**************************", "Wednesday", "9:00 - 11:00 AM", "2:00 - 4:00 PM", "VM-Lab 1", "VM-301"),
        CourseModel("******** 401", "**************************", "Thursday", "8:30 - 10:30 AM", "1:30 - 3:30 PM", "VM-203", "VM-Lab 3"),
        CourseModel("******** 501", "**************************", "Friday", "7:00 - 10:00 AM", "12:00 - 3:00 PM", "VM-OpRoom", "VM-305"),
        CourseModel("******** 601", "**************************", "Saturday", "8:00 - 12:00 PM", "1:00 - 5:00 PM", "VM-Hospital", "VM-Clinic")
    )

    private val displayedItems = mutableListOf<CoursesListItem>()
    private lateinit var adapter: MultiTypeCoursesAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCoursesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sharedPref = requireActivity().getSharedPreferences("VETSCHED_PREFS", Context.MODE_PRIVATE)
        binding.tvUserName.text = sharedPref.getString("userName", "Student")

        updateList()

        binding.rvCourses.layoutManager = LinearLayoutManager(requireContext())
        adapter = MultiTypeCoursesAdapter()
        binding.rvCourses.adapter = adapter

        setupSearch()

        binding.btnSchedule.setOnClickListener {
            findNavController().navigate(R.id.action_coursesFragment_to_scheduleFragment)
        }

        binding.btnProfile.setOnClickListener {
            findNavController().navigate(R.id.action_coursesFragment_to_profileFragment)
        }
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentQuery = s?.toString() ?: ""
                updateList()
                adapter.notifyDataSetChanged()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun updateList() {
        displayedItems.clear()

        val query = currentQuery.trim().lowercase()
        val filtered = if (query.isEmpty()) {
            allCourses
        } else {
            allCourses.filter { it.code.lowercase().contains(query) || it.name.lowercase().contains(query) }
        }

        val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
        days.forEach { day ->
            val dayCourses = filtered.filter { it.day == day }
            if (dayCourses.isNotEmpty()) {
                displayedItems.add(CoursesListItem.DayHeader(day, dayCourses.size))
                if (query.isNotEmpty() || expandedDays.contains(day)) {
                    dayCourses.forEach {
                        displayedItems.add(CoursesListItem.Course(it))
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class MultiTypeCoursesAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private val VIEW_TYPE_DAY = 0
        private val VIEW_TYPE_COURSE = 1

        private var expandedCoursePosition = -1

        override fun getItemViewType(position: Int): Int {
            return when (displayedItems[position]) {
                is CoursesListItem.DayHeader -> VIEW_TYPE_DAY
                is CoursesListItem.Course -> VIEW_TYPE_COURSE
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return if (viewType == VIEW_TYPE_DAY) {
                DayHeaderViewHolder(ItemDayHeaderBinding.inflate(inflater, parent, false))
            } else {
                CourseViewHolder(ItemCourseBinding.inflate(inflater, parent, false))
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (val item = displayedItems[position]) {
                is CoursesListItem.DayHeader -> {
                    val h = holder as DayHeaderViewHolder
                    val isExpanded = currentQuery.isNotEmpty() || expandedDays.contains(item.day)
                    
                    h.binding.tvDayTitle.text = item.day
                    h.binding.ivArrow.rotation = if (isExpanded) 180f else 0f
                    
                    h.binding.tvFoundCount.text = "${item.count} Found"
                    h.binding.tvFoundCount.visibility = if (isExpanded) View.VISIBLE else View.GONE
                    
                    h.itemView.setOnClickListener {
                        if (currentQuery.isNotEmpty()) return@setOnClickListener
                        
                        if (expandedDays.contains(item.day)) {
                            expandedDays.remove(item.day)
                        } else {
                            expandedDays.add(item.day)
                        }
                        updateList()
                        notifyDataSetChanged()
                    }
                }
                is CoursesListItem.Course -> {
                    val h = holder as CourseViewHolder
                    val course = item.course
                    val isExpanded = position == expandedCoursePosition

                    h.binding.tvCourseCode.text = course.code
                    h.binding.tvCourseNameMasked.text = course.name
                    h.binding.tvTimeRange.text = "${course.timeA} / ${course.timeB}"
                    h.binding.tvSectionCount.text = "2 sections"
                    
                    h.binding.layoutSections.visibility = if (isExpanded) View.VISIBLE else View.GONE
                    h.binding.layoutExpandedContent.visibility = View.VISIBLE

                    h.binding.courseHeader.setOnClickListener {
                        val prev = expandedCoursePosition
                        expandedCoursePosition = if (isExpanded) -1 else position
                        notifyItemChanged(prev)
                        notifyItemChanged(expandedCoursePosition)
                    }

                    h.binding.btnEnrollA.setOnClickListener {
                        CourseRepository.enroll(
                            EnrolledCourse(course.day, course.timeA, "Section A", course.code, course.name, course.roomA),
                            requireContext()
                        )
                        Toast.makeText(requireContext(), "Enrolled in Section A", Toast.LENGTH_SHORT).show()
                    }

                    h.binding.btnEnrollB.setOnClickListener {
                        CourseRepository.enroll(
                            EnrolledCourse(course.day, course.timeB, "Section B", course.code, course.name, course.roomB),
                            requireContext()
                        )
                        Toast.makeText(requireContext(), "Enrolled in Section B", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        override fun getItemCount(): Int = displayedItems.size

        inner class DayHeaderViewHolder(val binding: ItemDayHeaderBinding) : RecyclerView.ViewHolder(binding.root)
        inner class CourseViewHolder(val binding: ItemCourseBinding) : RecyclerView.ViewHolder(binding.root)
    }
}