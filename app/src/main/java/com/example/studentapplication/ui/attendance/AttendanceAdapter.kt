package com.example.studentapplication.ui.attendance

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.studentapplication.R
import com.example.studentapplication.data.local.AttendanceEntity
import com.example.studentapplication.data.local.StudentEntity
import com.example.studentapplication.databinding.ItemAttendanceBinding

class AttendanceAdapter(
    private val onMark: (studentId: Int, status: String) -> Unit
) : RecyclerView.Adapter<AttendanceAdapter.ViewHolder>() {

    // Combined data class — merges student + attendance status into one item
    data class AttendanceItem(
        val student: StudentEntity,
        val status: String?
    )

    private var items: List<AttendanceItem> = emptyList()

    class ViewHolder(val binding: ItemAttendanceBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAttendanceBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.binding.tvStudentName.text   = item.student.name
        holder.binding.tvStudentCourse.text = item.student.course

        // Highlight the active button based on current status
        updateButtonStyles(holder, item.status)

        holder.binding.btnPresent.setOnClickListener {
            onMark(item.student.id, "Present")
        }
        holder.binding.btnAbsent.setOnClickListener {
            onMark(item.student.id, "Absent")
        }
    }

    // Visually highlight selected status — green for Present, red for Absent
    private fun updateButtonStyles(holder: ViewHolder, status: String?) {
        val context = holder.binding.root.context

        val presentBg   = ContextCompat.getColorStateList(context, R.color.presentBtnBg)
        val presentText = ContextCompat.getColor(context, R.color.presentBtnText)
        val absentBg    = ContextCompat.getColorStateList(context, R.color.absentBtnBg)
        val absentText  = ContextCompat.getColor(context, R.color.absentBtnTxt)
        val presentActiveBg = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.presentBtnText))
        val absentActiveBg  = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.absentBtnTxt))
        val white = ContextCompat.getColor(context, R.color.white)

        when (status) {
            "Present" -> {
                holder.binding.btnPresent.backgroundTintList = presentActiveBg
                holder.binding.btnPresent.setTextColor(white)
                holder.binding.btnAbsent.backgroundTintList = absentBg
                holder.binding.btnAbsent.setTextColor(absentText)
            }
            "Absent" -> {
                holder.binding.btnAbsent.backgroundTintList = absentActiveBg
                holder.binding.btnAbsent.setTextColor(white)
                holder.binding.btnPresent.backgroundTintList = presentBg
                holder.binding.btnPresent.setTextColor(presentText)
            }
            else -> {
                holder.binding.btnPresent.backgroundTintList = presentBg
                holder.binding.btnPresent.setTextColor(presentText)
                holder.binding.btnAbsent.backgroundTintList = absentBg
                holder.binding.btnAbsent.setTextColor(absentText)
            }
        }
    }
private class AttendanceDiffCallback(
    private val oldList: List<AttendanceItem>,
    private val newList: List<AttendanceItem>
) : DiffUtil.Callback() {

    override fun getOldListSize() = oldList.size
    override fun getNewListSize() = newList.size

    // Same item = same student id
    override fun areItemsTheSame(oldPos: Int, newPos: Int) =
        oldList[oldPos].student.id == newList[newPos].student.id

    // Same content = same student data AND same attendance status
    override fun areContentsTheSame(oldPos: Int, newPos: Int) =
        oldList[oldPos] == newList[newPos]
}

    // Replace notifyDataSetChanged() with DiffUtil dispatch
    fun updateData(newStudents: List<StudentEntity>, newAttendanceMap: Map<Int, String>) {
        val newItems = newStudents.map { student ->
            AttendanceItem(
                student = student,
                status  = newAttendanceMap[student.id]
            )
        }
        val diff = DiffUtil.calculateDiff(AttendanceDiffCallback(items, newItems))
        items = newItems
        diff.dispatchUpdatesTo(this)
    }
}