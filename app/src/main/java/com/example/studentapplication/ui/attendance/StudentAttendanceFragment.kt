package com.example.studentapplication.ui.attendance

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.studentapplication.R
import com.example.studentapplication.data.local.AttendanceEntity
import com.example.studentapplication.databinding.FragmentStudentAttendanceBinding
import com.example.studentapplication.databinding.ItemAttendanceHistoryBinding
import com.example.studentapplication.utils.AuthPreferences
import com.example.studentapplication.viewmodel.AttendanceViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@AndroidEntryPoint
class StudentAttendanceFragment : Fragment(R.layout.fragment_student_attendance) {

    private var _binding: FragmentStudentAttendanceBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AttendanceViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentStudentAttendanceBinding.bind(view)

        val userId    = AuthPreferences(requireContext()).getCurrentUserId()
        viewModel.setUserId(userId)

        val studentId   = arguments?.getInt("studentId") ?: -1
        val studentName = arguments?.getString("studentName") ?: ""
        val studentCourse = arguments?.getString("studentCourse") ?: ""

        binding.tvStudentName.text   = studentName
        binding.tvStudentCourse.text = studentCourse

        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        // History RecyclerView
        val historyAdapter = AttendanceHistoryAdapter(emptyList())
        binding.rvHistory.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHistory.adapter = historyAdapter

        // Collect history + counts together
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                // Collector 1 — attendance history list
                launch {
                    viewModel.getAttendanceForStudent(studentId).collect { history ->
                        historyAdapter.updateList(history)
                    }
                }

                // Collector 2 — present % summary
                launch {
                    combine(
                        viewModel.getPresentCount(studentId),
                        viewModel.getTotalCount(studentId)
                    ) { present, total -> Pair(present, total) }
                        .collect { (present, total) ->
                            val absent  = total - present
                            val percent = if (total > 0) (present * 100) / total else 0

                            binding.tvAttendancePercent.text = "$percent%"
                            binding.tvPresentCount.text      = present.toString()
                            binding.tvAbsentCount.text       = absent.toString()

                            binding.tvAttendancePercent.setTextColor(
                                when {
                                    percent >= 75 -> Color.parseColor("#2E7D32")
                                    percent >= 50 -> Color.parseColor("#FF9800")
                                    else          -> Color.parseColor("#C62828")
                                }
                            )
                        }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// Inline adapter for attendance history list
class AttendanceHistoryAdapter(
    private var list: List<AttendanceEntity>
) : RecyclerView.Adapter<AttendanceHistoryAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemAttendanceHistoryBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAttendanceHistoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount() = list.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val record = list[position]
        holder.binding.tvDate.text   = record.date
        holder.binding.tvStatus.text = record.status

        // Badge color — green for Present, red for Absent
        val bgColor   = if (record.status == "Present") "#E8F5E9" else "#FFEBEE"
        val textColor = if (record.status == "Present") "#2E7D32" else "#C62828"

        holder.binding.tvStatus.backgroundTintList =
            android.content.res.ColorStateList.valueOf(Color.parseColor(bgColor))
        holder.binding.tvStatus.setTextColor(Color.parseColor(textColor))
    }

    fun updateList(newList: List<AttendanceEntity>) {
        list = newList
        notifyDataSetChanged()
    }
}