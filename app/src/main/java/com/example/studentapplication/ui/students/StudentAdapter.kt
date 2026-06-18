
package com.example.studentapplication.ui.students

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.studentapplication.data.local.StudentEntity
import com.example.studentapplication.databinding.ItemStudentBinding

class StudentAdapter(
    private var studentList: List<StudentEntity>,
    private val onClick: (StudentEntity) -> Unit
) : RecyclerView.Adapter<StudentAdapter.StudentViewHolder>() {

    class StudentViewHolder(val binding: ItemStudentBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudentViewHolder {
        val binding = ItemStudentBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return StudentViewHolder(binding)
    }

    override fun getItemCount() = studentList.size

    override fun onBindViewHolder(holder: StudentViewHolder, position: Int) {
        val student = studentList[position]
        holder.binding.txtName.text   = student.name
        holder.binding.txtCourse.text = student.course
        holder.binding.txtEmail.text  = student.email
        holder.itemView.setOnClickListener { onClick(student) }
    }
fun updateList(newList: List<StudentEntity>) {
    val oldList = studentList          // snapshot old reference
    val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
        override fun getOldListSize() = oldList.size
        override fun getNewListSize() = newList.size
        override fun areItemsTheSame(oldPos: Int, newPos: Int) =
            oldList[oldPos].id == newList[newPos].id
        override fun areContentsTheSame(oldPos: Int, newPos: Int) =
            oldList[oldPos] == newList[newPos]
    })
    studentList = newList
    diff.dispatchUpdatesTo(this)
}
    fun getStudentAt(position: Int): StudentEntity {
        return studentList[position]
    }
}

