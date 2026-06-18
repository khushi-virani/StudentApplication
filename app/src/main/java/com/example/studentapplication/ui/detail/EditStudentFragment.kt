package com.example.studentapplication.ui.detail

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.studentapplication.R
import com.example.studentapplication.data.local.StudentEntity
import com.example.studentapplication.databinding.FragmentEditStudentBinding
import com.example.studentapplication.utils.AuthPreferences
import com.example.studentapplication.utils.PhotoPickerHelper
import com.example.studentapplication.utils.ValidationUtils
import com.example.studentapplication.viewmodel.StudentViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class EditStudentFragment : Fragment(R.layout.fragment_edit_student) {

    private var _binding: FragmentEditStudentBinding? = null
    private val binding get() = _binding!!
    private val viewModel: StudentViewModel by viewModels()
    private var studentId: Int = -1

    // Holds the URI of the photo — either picked from gallery or taken by camera
    private var selectedPhotoUri: Uri? = null
    private lateinit var photoPicker: PhotoPickerHelper

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentEditStudentBinding.bind(view)

        val userId = AuthPreferences(requireContext()).getCurrentUserId()
        viewModel.setUserId(userId)

        studentId = arguments?.getInt("id") ?: -1

        binding.etName.setText(arguments?.getString("name"))
        binding.etEmail.setText(arguments?.getString("email"))
        binding.etCourse.setText(arguments?.getString("course"))
        binding.etPhone.setText(arguments?.getString("phone"))

        val existingPhoto = arguments?.getString("photoUri") ?: ""

        if (existingPhoto.isNotEmpty()) {

            selectedPhotoUri = Uri.parse(existingPhoto)

            Glide.with(this)
                .load(selectedPhotoUri)
                .placeholder(R.drawable.ic_person_placeholder)
                .circleCrop()
                .into(binding.imgStudentPhoto)
        }

        // Photo click → show bottom sheet chooser
        photoPicker = PhotoPickerHelper(this) { uri ->

            selectedPhotoUri = uri

            Glide.with(this)
                .load(uri)
                .placeholder(R.drawable.ic_person_placeholder)
                .circleCrop()
                .into(binding.imgStudentPhoto)
        }

        binding.imgStudentPhoto.setOnClickListener {
            photoPicker.showPhotoChooser()
        }

        binding.btnUpdate.setOnClickListener {
            updateStudent()
        }
    }

    // ─── Update ───────────────────────────────────────────────────────────────

    private fun updateStudent() {

        val name   = binding.etName.text.toString().trim()
        val email  = binding.etEmail.text.toString().trim()
        val course = binding.etCourse.text.toString().trim()
        val phone  = binding.etPhone.text.toString().trim()

        binding.tilName.error   = null
        binding.tilEmail.error  = null
        binding.tilCourse.error = null
        binding.tilPhone.error  = null

        var hasError = false

        if (name.isEmpty()) { binding.tilName.error = "Name is required"; hasError = true }
        else if (!ValidationUtils.isValidName(name)) { binding.tilName.error = "Name must be letters only (min 2 chars)"; hasError = true }

        if (email.isEmpty()) { binding.tilEmail.error = "Email is required"; hasError = true }
        else if (!ValidationUtils.isValidEmail(email)) { binding.tilEmail.error = "Enter a valid email (e.g. test@gmail.com)"; hasError = true }

        if (course.isEmpty()) { binding.tilCourse.error = "Course is required"; hasError = true }
        else if (!ValidationUtils.isValidCourse(course)) { binding.tilCourse.error = "Course must be at least 2 characters"; hasError = true }

        if (phone.isEmpty()) { binding.tilPhone.error = "Phone is required"; hasError = true }
        else if (!ValidationUtils.isValidPhone(phone)) { binding.tilPhone.error = "Enter a valid 10-digit phone number"; hasError = true }

        if (hasError) return

        val updatedStudent = StudentEntity(
            id       = studentId,
            name     = name,
            email    = email,
            course   = course,
            phone    = phone,
            userId   = AuthPreferences(requireContext()).getCurrentUserId(),
            photoUri = selectedPhotoUri?.toString() ?: ""
        )

        viewModel.updateStudent(updatedStudent)
        Toast.makeText(requireContext(), "Updated Successfully", Toast.LENGTH_SHORT).show()
        findNavController().popBackStack()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}