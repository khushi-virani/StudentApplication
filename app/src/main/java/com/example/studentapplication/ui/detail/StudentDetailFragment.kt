package com.example.studentapplication.ui.detail

import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.studentapplication.data.local.StudentEntity
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.studentapplication.R
import com.example.studentapplication.StudentApp
import com.example.studentapplication.data.local.StudentDatabase
import com.example.studentapplication.data.repository.StudentRepository
import com.example.studentapplication.databinding.FragmentStudentDetailBinding
import com.example.studentapplication.utils.AuthPreferences
import com.example.studentapplication.viewmodel.StudentViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class StudentDetailFragment : Fragment(R.layout.fragment_student_detail) {

    private var _binding: FragmentStudentDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: StudentViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentStudentDetailBinding.bind(view)

        val studentId = arguments?.getInt("studentId") ?: -1
        viewModel.setStudentId(studentId)

        // Collect studentDetail StateFlow
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.studentDetail.collect { student ->
                    student ?: return@collect

                    binding.tvName.text   = student.name
                    binding.tvEmail.text  = student.email
                    binding.tvCourse.text = student.course
                    binding.tvPhone.text  = student.phone
                    if (student.photoUri.isNotEmpty()) {
                        Glide.with(this@StudentDetailFragment)
                            .load(Uri.parse(student.photoUri))
                            .placeholder(R.drawable.ic_person_placeholder)
                            .error(R.drawable.ic_person_placeholder)
                            .circleCrop()
                            .into(binding.imgStudentPhoto)
                    } else {
                        binding.imgStudentPhoto.setImageResource(R.drawable.ic_person_placeholder)
                    }

                    binding.btnEdit.setOnClickListener {
                        val bundle = Bundle().apply {
                            putInt("id", student.id)
                            putString("name", student.name)
                            putString("email", student.email)
                            putString("course", student.course)
                            putString("phone", student.phone)
                            putString("photoUri", student.photoUri)
                        }
                        findNavController().navigate(R.id.editStudentFragment, bundle)
                    }

                    binding.btnDelete.setOnClickListener {
                        AlertDialog.Builder(requireContext())
                            .setTitle("Delete Student")
                            .setMessage("Are you sure?")
                            .setPositiveButton("Yes") { _, _ ->
                                viewModel.deleteStudent(student)
                                findNavController().popBackStack()
                            }
                            .setNegativeButton("No", null)
                            .show()
                    }
                    // Inside studentDetail.collect { student -> ... }
                    binding.btnViewAttendance.setOnClickListener {
                        val bundle = Bundle().apply {
                            putInt("studentId", student.id)
                            putString("studentName", student.name)
                            putString("studentCourse", student.course)
                        }
                        findNavController().navigate(R.id.studentAttendanceFragment, bundle)
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