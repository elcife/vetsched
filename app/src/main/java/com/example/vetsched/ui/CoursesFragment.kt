package com.example.vetsched.ui

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.vetsched.R
import com.example.vetsched.api.RetrofitClient
import com.example.vetsched.api.models.Subject
import com.example.vetsched.api.models.Section
import com.example.vetsched.data.CourseRepository
import com.example.vetsched.data.EnrolledCourse
import com.example.vetsched.databinding.FragmentCoursesBinding
import com.example.vetsched.databinding.ItemCourseBinding
import com.example.vetsched.databinding.ItemCourseSectionBinding
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CoursesFragment : Fragment() {

    private var _binding: FragmentCoursesBinding? = null
    private val binding get() = _binding!!

    private var currentQuery = ""
    private var expandedPosition = -1
    
    private var allSubjects = listOf<Subject>()
    private val displayedSubjects = mutableListOf<Subject>()
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

        binding.rvCourses.layoutManager = LinearLayoutManager(requireContext())
        adapter = CoursesAdapter()
        binding.rvCourses.adapter = adapter

        fetchSubjects()
        setupSearch()

        binding.btnSchedule.setOnClickListener {
            findNavController().navigate(R.id.action_coursesFragment_to_scheduleFragment)
        }

        binding.btnProfile.setOnClickListener {
            findNavController().navigate(R.id.action_coursesFragment_to_profileFragment)
        }
    }

    private fun fetchSubjects() {
        val sharedPref = requireActivity().getSharedPreferences("VETSCHED_PREFS", Context.MODE_PRIVATE)
        val userEmail = sharedPref.getString("userEmail", null) ?: return

        val params = mapOf("email" to userEmail)
        RetrofitClient.instance.getSubjects(params).enqueue(object : Callback<List<Subject>> {
            override fun onResponse(call: Call<List<Subject>>, response: Response<List<Subject>>) {
                if (!isAdded || _binding == null) return

                if (response.isSuccessful) {
                    allSubjects = response.body() ?: emptyList()
                    Log.d("VETSCHED_API", "Received JSON: ${Gson().toJson(allSubjects)}")
                    updateList()
                    
                    val targetCode = arguments?.getString("targetCourseCode")
                    if (targetCode != null) {
                        val index = displayedSubjects.indexOfFirst { it.code == targetCode }
                        if (index != -1) {
                            expandedPosition = index
                            adapter.notifyItemChanged(index)
                            binding.rvCourses.scrollToPosition(index)
                        }
                    }
                } else {
                    val code = response.code()
                    Toast.makeText(context, "Error $code: Failed to load subjects", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<Subject>>, t: Throwable) {
                if (!isAdded || _binding == null) return
                Toast.makeText(context, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentQuery = s?.toString() ?: ""
                updateList()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun updateList() {
        displayedSubjects.clear()
        
        val sharedPref = requireActivity().getSharedPreferences("VETSCHED_PREFS", Context.MODE_PRIVATE)
        val userYearLevel = sharedPref.getInt("userYearLevel", 1)

        val query = currentQuery.trim().lowercase()
        val filtered = allSubjects.filter { subject ->
            val matchesYear = subject.yearLevel == userYearLevel
            val matchesQuery = if (query.isEmpty()) {
                true
            } else {
                subject.code.lowercase().contains(query) || subject.name.lowercase().contains(query)
            }
            matchesYear && matchesQuery
        }
        
        displayedSubjects.addAll(filtered)
        binding.tvFoundCount.text = "${filtered.size} Found"
        
        if (displayedSubjects.isEmpty()) {
            binding.tvEmptyState.visibility = View.VISIBLE
            binding.rvCourses.visibility = View.GONE
        } else {
            binding.tvEmptyState.visibility = View.GONE
            binding.rvCourses.visibility = View.VISIBLE
        }
        
        adapter.notifyDataSetChanged()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class CoursesAdapter : RecyclerView.Adapter<CoursesAdapter.ViewHolder>() {

        inner class ViewHolder(val itemBinding: ItemCourseBinding) : RecyclerView.ViewHolder(itemBinding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return ViewHolder(ItemCourseBinding.inflate(inflater, parent, false))
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val subject = displayedSubjects[position]
            val isExpanded = position == expandedPosition

            holder.itemBinding.tvCourseCode.text = subject.code
            holder.itemBinding.tvCourseNameMasked.text = subject.name
            
            val sectionCount = subject.sections.size
            holder.itemBinding.tvSectionCount.text = if (sectionCount == 1) "1 section" else "$sectionCount sections"
            
            holder.itemBinding.layoutSections.visibility = if (isExpanded) View.VISIBLE else View.GONE
            
            holder.itemBinding.containerSections.removeAllViews()
            if (isExpanded) {
                val inflater = LayoutInflater.from(holder.itemView.context)
                subject.sections.forEach { section ->
                    val sectionBinding = ItemCourseSectionBinding.inflate(inflater, holder.itemBinding.containerSections, true)
                    sectionBinding.tvSectionName.text = section.sectionName ?: section.type ?: "Lecture"

                    val formattedDays = getSafeDayString(section)
                    val formattedTime = "${formatTime(section.start)} - ${formatTime(section.end)}"
                    val fullTimeRange = formattedTime
                    
                    sectionBinding.tvSectionDays.text = formattedDays
                    sectionBinding.tvSectionTime.text = formattedTime
                    sectionBinding.tvSectionRoom.text = section.room
                    sectionBinding.tvSectionInstructor.text = section.instructor

                    val isEnrolled = CourseRepository.isEnrolled(subject.code, sectionBinding.tvSectionName.text.toString(), requireContext())
                    val isConflicting = !isEnrolled && CourseRepository.isConflicting(formattedDays, fullTimeRange, requireContext())

                    if (isEnrolled) {
                        sectionBinding.btnEnroll.text = "Enrolled"
                        sectionBinding.btnEnroll.isEnabled = false
                        sectionBinding.btnEnroll.setBackgroundColor(Color.LTGRAY)
                    } else if (isConflicting) {
                        sectionBinding.btnEnroll.text = "Conflict"
                        sectionBinding.btnEnroll.isEnabled = false
                        sectionBinding.btnEnroll.setBackgroundColor(Color.parseColor("#BDBDBD"))
                    } else {
                        sectionBinding.btnEnroll.text = "Enroll"
                        sectionBinding.btnEnroll.isEnabled = true
                    }

                    sectionBinding.btnEnroll.setOnClickListener {
                        val enrolled = EnrolledCourse(
                            formattedDays,
                            fullTimeRange,
                            sectionBinding.tvSectionName.text.toString(),
                            subject.code,
                            subject.name,
                            section.room,
                            section.instructor
                        )
                        CourseRepository.enroll(enrolled, it.context)
                        Toast.makeText(it.context, "Enrolled in ${enrolled.section}", Toast.LENGTH_SHORT).show()
                        notifyItemChanged(position)
                    }
                }
            }

            holder.itemBinding.courseHeader.setOnClickListener {
                val prev = expandedPosition
                expandedPosition = if (isExpanded) -1 else position
                notifyItemChanged(prev)
                notifyItemChanged(expandedPosition)
            }
        }

        private fun getSafeDayString(section: Section): String {
            val possibleFields = mutableListOf<String?>()
            
            section.day?.let { if (it.isNotBlank()) possibleFields.add(it) }
            section.dayOfWeek?.let { if (it.isNotBlank()) possibleFields.add(it) }
            section.scheduleDay?.let { if (it.isNotBlank()) possibleFields.add(it) }
            
            section.days?.let { d ->
                if (d is List<*>) {
                    if (d.isNotEmpty()) possibleFields.add(d.joinToString(", "))
                } else if (d is String && d.isNotBlank() && d != "[]" && d != "{}") {
                    possibleFields.add(d)
                }
            }

            val valid = possibleFields.filterNotNull().map { it.replace("{", "").replace("}", "").replace("[", "").replace("]", "").replace("\"", "").trim() }
                .filter { it.isNotBlank() && !it.equals("null", true) }

            if (valid.isEmpty()) return "Mon"

            return valid.first()
        }

        private fun formatTime(rawTime: String): String {
            return try {
                val parts = rawTime.split(":")
                val hour = parts[0].toInt()
                val min = parts[1].toInt()
                val suffix = if (hour >= 12) "PM" else "AM"
                val h12 = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
                String.format("%d:%02d %s", h12, min, suffix)
            } catch (e: Exception) {
                rawTime
            }
        }

        override fun getItemCount(): Int = displayedSubjects.size
    }
}
