package com.example.vetsched.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.vetsched.R
import com.example.vetsched.databinding.FragmentCoursesBinding
import com.example.vetsched.databinding.ItemCourseBinding

class CoursesFragment : Fragment() {

    private var _binding: FragmentCoursesBinding? = null
    private val binding get() = _binding!!

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
        binding.rvCourses.adapter = CoursesAdapter(List(7) { "Course ${it + 1}" })

        binding.btnSchedule.setOnClickListener {
            findNavController().navigate(R.id.action_coursesFragment_to_scheduleFragment)
        }

        binding.btnProfile.setOnClickListener {
            findNavController().navigate(R.id.action_coursesFragment_to_profileFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class CoursesAdapter(private val courses: List<String>) :
        RecyclerView.Adapter<CoursesAdapter.ViewHolder>() {

        private var expandedPosition = -1

        inner class ViewHolder(val binding: ItemCourseBinding) :
            RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemCourseBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val isExpanded = position == expandedPosition
            holder.binding.layoutSections.visibility = if (isExpanded) View.VISIBLE else View.GONE
            
            if (position == 0) {
                holder.binding.tvCourseCode.text = "******** 101"
                holder.binding.tvSectionCount.text = "2 sections"
                holder.binding.layoutExpandedContent.visibility = View.VISIBLE
            } else {
                holder.binding.tvCourseCode.text = if (position == 1) "****** 201" else "******** ${101 + position * 100}"
                holder.binding.tvSectionCount.text = "0 sections"
                holder.binding.layoutExpandedContent.visibility = View.GONE
            }

            holder.binding.courseHeader.setOnClickListener {
                val prevExpanded = expandedPosition
                expandedPosition = if (isExpanded) -1 else position
                
                notifyItemChanged(prevExpanded)
                notifyItemChanged(expandedPosition)
            }

            holder.binding.btnEnrollA.setOnClickListener(null)
            holder.binding.btnEnrollB.setOnClickListener(null)
        }

        override fun getItemCount(): Int = courses.size
    }
}