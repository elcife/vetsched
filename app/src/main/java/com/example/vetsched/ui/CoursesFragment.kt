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

data class CourseModel(
    val code: String,
    val name: String,
    val day: String,
    val timeA: String,
    val timeB: String,
    val roomA: String,
    val roomB: String
)

class CoursesFragment : Fragment() {

    private var _binding: FragmentCoursesBinding? = null
    private val binding get() = _binding!!

    private var currentQuery = ""
    private var expandedPosition = -1

    private val allCourses = listOf(
        CourseModel("VETMED* 101", "**************************", "Monday", "8:00 - 9:30 AM", "1:00 - 2:30 PM", "VM-201", "VM-305"),
        CourseModel("****** 201", "**************************", "Tuesday", "10:00 - 12:00 PM", "3:00 - 5:00 PM", "VM-Lab 2", "VM-202"),
        CourseModel("******** 301", "**************************", "Wednesday", "9:00 - 11:00 AM", "2:00 - 4:00 PM", "VM-Lab 1", "VM-301"),
        CourseModel("******** 401", "**************************", "Thursday", "8:30 - 10:30 AM", "1:30 - 3:30 PM", "VM-203", "VM-Lab 3"),
        CourseModel("******** 501", "**************************", "Friday", "7:00 - 10:00 AM", "12:00 - 3:00 PM", "VM-OpRoom", "VM-305"),
        CourseModel("******** 601", "**************************", "Saturday", "8:00 - 12:00 PM", "1:00 - 5:00 PM", "VM-Hospital", "VM-Clinic"),
        CourseModel("VETMED* 102", "**************************", "Monday", "10:00 - 11:30 AM", "2:00 - 3:30 PM", "VM-202", "VM-301")
    )

    private val displayedCourses = mutableListOf<CourseModel>()
    private lateinit var adapter: CoursesAdapter

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

        updateList()

        binding.rvCourses.layoutManager = LinearLayoutManager(requireContext())
        adapter = CoursesAdapter()
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
        displayedCourses.clear()
        val query = currentQuery.trim().lowercase()
        val filtered = if (query.isEmpty()) {
            allCourses
        } else {
            allCourses.filter { it.code.lowercase().contains(query) || it.name.lowercase().contains(query) }
        }
        displayedCourses.addAll(filtered)
        binding.tvFoundCount.text = "${filtered.size} Found"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class CoursesAdapter : RecyclerView.Adapter<CoursesAdapter.ViewHolder>() {

        inner class ViewHolder(val binding: ItemCourseBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return ViewHolder(ItemCourseBinding.inflate(inflater, parent, false))
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val course = displayedCourses[position]
            val isExpanded = position == expandedPosition

            holder.binding.tvCourseCode.text = course.code
            holder.binding.tvCourseNameMasked.text = course.name
            holder.binding.tvSectionCount.text = "2 sections"
            
            holder.binding.layoutSections.visibility = if (isExpanded) View.VISIBLE else View.GONE
            holder.binding.layoutExpandedContent.visibility = View.VISIBLE

            holder.binding.courseHeader.setOnClickListener {
                val prev = expandedPosition
                expandedPosition = if (isExpanded) -1 else position
                notifyItemChanged(prev)
                notifyItemChanged(expandedPosition)
            }

            holder.binding.btnEnrollA.setOnClickListener {
                CourseRepository.enroll(
                    EnrolledCourse(course.day, course.timeA, "Section A", course.code, course.name, course.roomA),
                    requireContext()
                )
                Toast.makeText(requireContext(), "Enrolled in Section A", Toast.LENGTH_SHORT).show()
            }

            holder.binding.btnEnrollB.setOnClickListener {
                CourseRepository.enroll(
                    EnrolledCourse(course.day, course.timeB, "Section B", course.code, course.name, course.roomB),
                    requireContext()
                )
                Toast.makeText(requireContext(), "Enrolled in Section B", Toast.LENGTH_SHORT).show()
            }
        }

        override fun getItemCount(): Int = displayedCourses.size
    }
}