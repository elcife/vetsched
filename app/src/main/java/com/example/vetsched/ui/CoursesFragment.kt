package com.example.vetsched.ui

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
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
import com.example.vetsched.notifications.NotificationCenter
import com.example.vetsched.notifications.NotificationInbox
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CoursesFragment : Fragment() {

    private var _binding: FragmentCoursesBinding? = null
    private val binding get() = _binding!!

    private var currentQuery = ""
    private var expandedPosition = -1
    private var selectedYearLevel = 1
    
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

        setupYearLevelSelector()
        fetchSubjects()
        setupSearch()
        setupSwipeRefresh()

        binding.btnSchedule.setOnClickListener {
            findNavController().navigate(R.id.action_coursesFragment_to_scheduleFragment)
        }

        binding.btnProfile.setOnClickListener {
            findNavController().navigate(R.id.action_coursesFragment_to_profileFragment)
        }

        binding.ivNotification.setOnClickListener {
            NotificationCenter.loadUpdates(
                requireContext(),
                selectedYearLevel,
                markAsRead = true
            ) { updates, error ->
                if (!isAdded || _binding == null) return@loadUpdates
                if (error != null) {
                    Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
                } else {
                    checkAndMergeData()
                    binding.viewNotificationIndicator.visibility = View.GONE
                    NotificationInbox.show(requireContext(), updates.orEmpty())
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null) refreshNotificationIndicator()
    }

    private fun refreshNotificationIndicator() {
        val checkedYearLevel = selectedYearLevel
        binding.viewNotificationIndicator.visibility =
            if (NotificationCenter.hasUnread(requireContext(), checkedYearLevel)) View.VISIBLE else View.GONE
        NotificationCenter.loadUpdates(requireContext(), checkedYearLevel) { _, error ->
            if (!isAdded || _binding == null) return@loadUpdates
            if (error == null) {
                if (selectedYearLevel == checkedYearLevel) checkAndMergeData()
                binding.viewNotificationIndicator.visibility =
                    if (NotificationCenter.hasUnread(requireContext(), selectedYearLevel)) View.VISIBLE else View.GONE
            }
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            fetchSubjects()
        }
        // Optional: Customize colors
        binding.swipeRefresh.setColorSchemeResources(R.color.vetsched_primary)
    }

    private fun setupYearLevelSelector() {
        val sharedPref = requireActivity().getSharedPreferences("VETSCHED_PREFS", Context.MODE_PRIVATE)
        val savedSelection = sharedPref.getInt(
            "coursesYearLevel",
            sharedPref.getInt("userYearLevel", 1)
        )
        selectedYearLevel = savedSelection.coerceIn(1, 4)

        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.year_levels,
            android.R.layout.simple_spinner_item
        ).also { spinnerAdapter ->
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerYearLevel.adapter = spinnerAdapter
        }
        binding.spinnerYearLevel.setSelection(selectedYearLevel - 1, false)
        binding.spinnerYearLevel.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedYearLevel = position + 1
                sharedPref.edit().putInt("coursesYearLevel", selectedYearLevel).apply()
                expandedPosition = -1
                if (fetchedSubjects != null && fetchedSections != null) {
                    NotificationCenter.initializeBaselineIfMissing(
                        requireContext(),
                        selectedYearLevel,
                        fetchedSubjects.orEmpty(),
                        fetchedSections.orEmpty()
                    )
                }
                refreshNotificationIndicator()
                updateList()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
    }

    private var fetchedSubjects: List<Subject>? = null
    private var fetchedSections: List<Section>? = null

    private fun fetchSubjects() {
        binding.swipeRefresh.isRefreshing = true
        fetchedSubjects = null
        fetchedSections = null

        RetrofitClient.instance.getSubjects().enqueue(object : Callback<List<Subject>> {
            override fun onResponse(call: Call<List<Subject>>, response: Response<List<Subject>>) {
                if (!isAdded || _binding == null) return
                if (response.isSuccessful) {
                    fetchedSubjects = response.body() ?: emptyList()
                    checkAndMergeData()
                } else {
                    handleFetchError("Subjects: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<List<Subject>>, t: Throwable) {
                if (!isAdded || _binding == null) return
                handleFetchError("Network: ${t.message}")
            }
        })

        RetrofitClient.instance.getSections().enqueue(object : Callback<List<Section>> {
            override fun onResponse(call: Call<List<Section>>, response: Response<List<Section>>) {
                if (!isAdded || _binding == null) return
                if (response.isSuccessful) {
                    fetchedSections = response.body() ?: emptyList()
                    checkAndMergeData()
                } else {
                    handleFetchError("Sections: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<List<Section>>, t: Throwable) {
                if (!isAdded || _binding == null) return
                handleFetchError("Network: ${t.message}")
            }
        })
    }

    private fun handleFetchError(message: String) {
        binding.swipeRefresh.isRefreshing = false
        // Prevent partial data from being stuck
        fetchedSubjects = fetchedSubjects ?: emptyList()
        fetchedSections = fetchedSections ?: emptyList()
        Toast.makeText(context, "Error: $message", Toast.LENGTH_LONG).show()
    }

    private fun checkAndMergeData() {
        val subjects = fetchedSubjects
        val allSections = fetchedSections

        if (subjects != null && allSections != null) {
            binding.swipeRefresh.isRefreshing = false
            
            // Group sections by subjectId
            val hiddenSectionIds = NotificationCenter.getHiddenSectionIds(
                requireContext(),
                selectedYearLevel
            )
            val sectionsMap = allSections
                .filterNot { it.id in hiddenSectionIds }
                .groupBy { it.subjectId }
            
            // Assign sections to subjects
            subjects.forEach { subject ->
                subject.sections = sectionsMap[subject.id] ?: emptyList()
            }
            
            allSubjects = subjects
            NotificationCenter.initializeBaselineIfMissing(
                requireContext(),
                selectedYearLevel,
                subjects,
                allSections
            )
            updateList()
            
            // Handle scrolling to target course if specified
            val targetCode = arguments?.getString("targetCourseCode")
            if (targetCode != null) {
                val index = displayedSubjects.indexOfFirst { it.code == targetCode }
                if (index != -1) {
                    expandedPosition = index
                    adapter.notifyItemChanged(index)
                    binding.rvCourses.scrollToPosition(index)
                }
            }
        }
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
        
        val query = currentQuery.trim().lowercase()
        val filtered = allSubjects.filter { subject ->
            val matchesYear = subject.yearLevel == selectedYearLevel
            val matchesQuery = if (query.isEmpty()) {
                true
            } else {
                subject.code.lowercase().contains(query) || subject.name.lowercase().contains(query)
            }
            matchesYear && matchesQuery
        }
        
        displayedSubjects.addAll(filtered)
        binding.tvFoundCount.text = resources.getQuantityString(
            R.plurals.course_count,
            filtered.size,
            filtered.size,
            selectedYearLevel
        )
        binding.tvEmptyState.text = if (query.isEmpty()) {
            "No courses available for Year $selectedYearLevel"
        } else {
            "No courses match your search"
        }
        
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
            holder.itemBinding.tvSectionCount.text = if (sectionCount == 0) {
                getString(R.string.no_sections_short)
            } else {
                resources.getQuantityString(R.plurals.section_count, sectionCount, sectionCount)
            }
            
            holder.itemBinding.layoutSections.visibility = if (isExpanded) View.VISIBLE else View.GONE
            holder.itemBinding.tvNoSections.visibility =
                if (isExpanded && sectionCount == 0) View.VISIBLE else View.GONE
            holder.itemBinding.containerSections.visibility =
                if (sectionCount == 0) View.GONE else View.VISIBLE
            
            holder.itemBinding.containerSections.removeAllViews()
            if (isExpanded) {
                val inflater = LayoutInflater.from(holder.itemView.context)
                subject.sections.forEach { section ->
                    val sectionBinding = ItemCourseSectionBinding.inflate(inflater, holder.itemBinding.containerSections, true)
                    sectionBinding.tvSectionName.text = section.type.uppercase()

                    sectionBinding.containerClassBoxes.removeAllViews()
                    val allDays = mutableSetOf<String>()
                    val instructors = mutableSetOf<String>()

                    section.classes?.forEach { comp ->
                        val type = comp.classType.uppercase()
                        val days = comp.days.joinToString(", ")
                        val time = "${formatTime(comp.start)} - ${formatTime(comp.end)}"
                        val instructor = comp.instructor?.uppercase() ?: "TBA"
                        
                        val detailText = "$type $days $time $instructor"
                        
                        // Add a separate box for each class component
                        val boxView = inflater.inflate(R.layout.item_class_box, sectionBinding.containerClassBoxes, false) as android.widget.TextView
                        boxView.text = detailText
                        sectionBinding.containerClassBoxes.addView(boxView)
                        
                        allDays.addAll(comp.days)
                        instructors.add(instructor)
                    }

                    // Fallback for old data
                    if (sectionBinding.containerClassBoxes.childCount == 0) {
                        val formattedDays = getSafeDayString(section)
                        val formattedTime = "${formatTime(section.start)} - ${formatTime(section.end)}"
                        val instructor = section.instructor?.uppercase() ?: "TBA"
                        
                        val detailText = "${section.type.uppercase()} $formattedDays $formattedTime $instructor"
                        val boxView = inflater.inflate(R.layout.item_class_box, sectionBinding.containerClassBoxes, false) as android.widget.TextView
                        boxView.text = detailText
                        sectionBinding.containerClassBoxes.addView(boxView)
                        
                        allDays.addAll(section.days ?: emptyList())
                        instructors.add(instructor)
                    }
                    
                    val remaining = section.remainingSeats ?: section.maxCapacity
                    sectionBinding.tvRemainingSeats.text = "$remaining seats left"

                    val combinedDaysStr = allDays.joinToString(", ")
                    val firstTimeRange = if (section.classes?.isNotEmpty() == true) {
                         "${formatTime(section.classes[0].start)} - ${formatTime(section.classes[0].end)}"
                    } else {
                         "${formatTime(section.start)} - ${formatTime(section.end)}"
                    }

                    val isEnrolled = CourseRepository.isEnrolled(subject.code, sectionBinding.tvSectionName.text.toString(), requireContext())
                    val isConflicting = !isEnrolled && CourseRepository.isConflicting(combinedDaysStr, firstTimeRange, requireContext())
                    val isFull = (section.remainingSeats ?: 1) <= 0

                    if (isEnrolled) {
                        sectionBinding.btnEnroll.text = "Enrolled"
                        sectionBinding.btnEnroll.isEnabled = false
                        sectionBinding.btnEnroll.setBackgroundColor(Color.LTGRAY)
                    } else if (!section.isOpen) {
                        sectionBinding.btnEnroll.text = "Closed"
                        sectionBinding.btnEnroll.isEnabled = false
                        sectionBinding.btnEnroll.setBackgroundColor(Color.LTGRAY)
                    } else if (isFull) {
                        sectionBinding.btnEnroll.text = "Full"
                        sectionBinding.btnEnroll.isEnabled = false
                        sectionBinding.btnEnroll.setBackgroundColor(Color.parseColor("#BDBDBD"))
                    } else if (isConflicting) {
                        sectionBinding.btnEnroll.text = "Conflict"
                        sectionBinding.btnEnroll.isEnabled = false
                        sectionBinding.btnEnroll.setBackgroundColor(Color.parseColor("#BDBDBD"))
                    } else {
                        sectionBinding.btnEnroll.text = "Enroll"
                        sectionBinding.btnEnroll.isEnabled = true
                        sectionBinding.btnEnroll.setBackgroundColor(Color.parseColor("#5C6D4F"))
                    }

                    sectionBinding.btnEnroll.setOnClickListener {
                        val previewSlots = mutableListOf<EnrolledCourse>()
                        
                        section.classes?.forEach { comp ->
                            comp.days.forEach { day ->
                                previewSlots.add(EnrolledCourse(
                                    day = day,
                                    timeRange = "${formatTime(comp.start)} - ${formatTime(comp.end)}",
                                    section = sectionBinding.tvSectionName.text.toString(),
                                    courseCode = subject.code,
                                    courseName = subject.name,
                                    room = comp.room ?: section.room ?: "TBA",
                                    instructor = comp.instructor ?: "TBA",
                                    offeringIds = section.offeringIds,
                                    type = comp.classType
                                ))
                            }
                        }

                        // Fallback for old data structure
                        if (previewSlots.isEmpty()) {
                            val formattedDays = getSafeDayString(section)
                            val formattedTime = "${formatTime(section.start)} - ${formatTime(section.end)}"
                            val dayList = formattedDays.split(",").map { it.trim() }
                            dayList.forEach { day ->
                                previewSlots.add(EnrolledCourse(
                                    day = day,
                                    timeRange = formattedTime,
                                    section = sectionBinding.tvSectionName.text.toString(),
                                    courseCode = subject.code,
                                    courseName = subject.name,
                                    room = section.room ?: "TBA",
                                    instructor = section.instructor ?: "TBA",
                                    offeringIds = section.offeringIds,
                                    type = section.type
                                ))
                            }
                        }
                        
                        val bundle = Bundle().apply {
                            putString("preview_courses_json", Gson().toJson(previewSlots))
                        }
                        findNavController().navigate(R.id.action_coursesFragment_to_confirmScheduleFragment, bundle)
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
            val days = section.days
            if (days.isNullOrEmpty()) return "Mon"
            return days.joinToString(", ")
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
