package com.example.vetsched.ui

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.vetsched.R
import com.example.vetsched.databinding.FragmentScheduleBinding
import com.example.vetsched.databinding.ItemDayScheduleBinding

class ScheduleFragment : Fragment() {

    private var _binding: FragmentScheduleBinding? = null
    private val binding get() = _binding!!

    private val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
    private val labels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    private val dates = listOf("26", "27", "28", "29", "30", "31")

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

        var userName = arguments?.getString("userName")
        if (userName == null) {
            val sharedPref = requireActivity().getSharedPreferences("VETSCHED_PREFS", Context.MODE_PRIVATE)
            userName = sharedPref.getString("userName", "Student")
        }
        binding.tvUserName.text = userName

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
        val primaryColor = ContextCompat.getColor(requireContext(), R.color.vetsched_primary)
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

        inner class ViewHolder(private val itemBinding: ItemDayScheduleBinding) : 
            RecyclerView.ViewHolder(itemBinding.root) {
            
            fun bind(dayTitle: String) {
                itemBinding.tvDayTitle.text = dayTitle
                itemBinding.tvActiveClasses.text = "0 Active Classes"
                
                itemBinding.cardsContainer.removeAllViews()
                itemBinding.cardsContainer.visibility = View.GONE

                setupTimeline(itemBinding)
            }
        }

        private fun setupTimeline(itemBinding: ItemDayScheduleBinding) {
            itemBinding.row7.tvTime.text = "7:00"
            itemBinding.row8.tvTime.text = "8:00"
            itemBinding.row9.tvTime.text = "9:00"
            itemBinding.row10.tvTime.text = "10:00"
            itemBinding.row11.tvTime.text = "11:00"
            itemBinding.row12.tvTime.text = "12:00"
            itemBinding.row1.tvTime.text = "1:00"
            itemBinding.row2.tvTime.text = "2:00"
            itemBinding.row3.tvTime.text = "3:00"
            itemBinding.row4.tvTime.text = "4:00"
            itemBinding.row5.tvTime.text = "5:00"
            itemBinding.row6.tvTime.text = "6:00"
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val itemBinding = ItemDayScheduleBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return ViewHolder(itemBinding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(days[position])
        }

        override fun getItemCount(): Int = days.size
    }
}