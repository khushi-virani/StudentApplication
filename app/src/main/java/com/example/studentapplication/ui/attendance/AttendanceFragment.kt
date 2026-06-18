package com.example.studentapplication.ui.attendance

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.studentapplication.R
import com.example.studentapplication.databinding.FragmentAttendanceBinding
import com.example.studentapplication.utils.AnalyticsHelper
import com.example.studentapplication.utils.AuthPreferences
import com.example.studentapplication.viewmodel.AttendanceViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Calendar

@AndroidEntryPoint
class AttendanceFragment : Fragment(R.layout.fragment_attendance) {

    private var _binding: FragmentAttendanceBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AttendanceViewModel by viewModels()
    private lateinit var adapter: AttendanceAdapter

    // date range state
    private var startDate: String? = null
    private var endDate: String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAttendanceBinding.bind(view)

        val userId = AuthPreferences(requireContext()).getCurrentUserId()
        viewModel.setUserId(userId)

        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        AnalyticsHelper.logScreenView("AttendanceScreen")
        adapter = AttendanceAdapter { studentId, status ->
            viewModel.markAttendance(studentId, status)
        }
        binding.rvAttendance.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAttendance.adapter = adapter

        binding.tvSelectedDate.text = viewModel.selectedDate.value
        binding.btnPickDate.setOnClickListener { showDatePicker() }

        // ── Date Range Filter ─────────────────────────────

        // Pick start date
        binding.tvStartDate.setOnClickListener {
            showRangeDatePicker { date ->
                startDate = date
                binding.tvStartDate.text = "From: $date"
            }
        }

        // Pick end date
        binding.tvEndDate.setOnClickListener {
            showRangeDatePicker { date ->
                endDate = date
                binding.tvEndDate.text = "To: $date"
            }
        }

        binding.btnFilterRange.setOnClickListener {
            val start = startDate
            val end = endDate

            if (start == null || end == null) {
                Toast.makeText(requireContext(), "Please select both dates", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (start > end) {
                Toast.makeText(requireContext(), "Start date must be before end date", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewLifecycleOwner.lifecycleScope.launch {
                val results = viewModel.getAttendanceByDateRange(start, end)
                AnalyticsHelper.logDateRangeFiltered(start, end, results.size)
                if (results.isEmpty()) {
                    Toast.makeText(requireContext(), "No records found", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // Build list of (student, date, status) rows
                val allStudents = viewModel.allStudents.value
                val studentMap = allStudents.associateBy { it.id }

                // Create one fake "student" row per attendance record using date in name
                val displayStudents = results.mapNotNull { attendance ->
                    studentMap[attendance.studentId]?.copy(
                        name = "${studentMap[attendance.studentId]?.name} (${attendance.date})"
                    )
                }

                val attendanceMap = results.associate { it.studentId to it.status }

                adapter.updateData(displayStudents, attendanceMap)

                val presentCount = results.count { it.status == "Present" }
                val absentCount  = results.count { it.status == "Absent" }
                binding.chipPresentCount.text = "Present: $presentCount"
                binding.chipAbsentCount.text  = "Absent: $absentCount"

                Toast.makeText(requireContext(), "${results.size} records found", Toast.LENGTH_SHORT).show()
            }
        }

        // Clear filter — go back to normal view
        binding.btnClearFilter.setOnClickListener {
            startDate = null
            endDate = null
            binding.tvStartDate.text = "From: --"
            binding.tvEndDate.text = "To: --"

            // Restore today's attendance view
            val currentStudents = viewModel.allStudents.value
            val currentAttendance = viewModel.attendanceForDate.value
            val attendanceMap = currentAttendance.associate { it.studentId to it.status }

            adapter.updateData(currentStudents, attendanceMap)

            val presentCount = attendanceMap.values.count { it == "Present" }
            val absentCount  = attendanceMap.values.count { it == "Absent" }
            binding.chipPresentCount.text = "Present: $presentCount"
            binding.chipAbsentCount.text  = "Absent: $absentCount"
        }

        // ── Normal attendance flow (unchanged) ───────────

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    viewModel.allStudents,
                    viewModel.attendanceForDate
                ) { students, attendanceList ->
                    val attendanceMap = attendanceList.associate { it.studentId to it.status }
                    Pair(students, attendanceMap)
                }.collect { (students, attendanceMap) ->
                    adapter.updateData(students, attendanceMap)
                    val presentCount = attendanceMap.values.count { it == "Present" }
                    val absentCount  = attendanceMap.values.count { it == "Absent" }
                    binding.chipPresentCount.text = "Present: $presentCount"
                    binding.chipAbsentCount.text  = "Absent: $absentCount"
                }
            }
        }
    }

    // Date picker for range selection
    private fun showRangeDatePicker(onDateSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                val formatted = "%04d-%02d-%02d".format(year, month + 1, day)
                onDateSelected(formatted)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                val formatted = "%04d-%02d-%02d".format(year, month + 1, day)
                viewModel.setDate(formatted)
                binding.tvSelectedDate.text = formatted
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}