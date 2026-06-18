package com.example.studentapplication.ui.home

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.studentapplication.R
import com.example.studentapplication.StudentApp
import com.example.studentapplication.databinding.FragmentHomeBinding
import com.example.studentapplication.utils.AnalyticsHelper
import com.example.studentapplication.utils.AuthPreferences
import com.example.studentapplication.viewmodel.StudentViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : Fragment(R.layout.fragment_home) {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StudentViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentHomeBinding.bind(view)
        binding.etSearchHome.setText("")

        val userId = AuthPreferences(requireContext()).getCurrentUserId()

        viewModel.setUserId(userId)

        // Show logged in user's name
        val authPrefs = AuthPreferences(requireContext())
        binding.tvWelcome.text = "Hello ${authPrefs.getFullName()} 👋"

        // Home search → navigate to Students with query
//        binding.etSearchHome.addTextChangedListener { editable ->
//            val query = editable?.toString()?.trim() ?: ""
//            if (query.isNotEmpty()) {
//                val bundle = Bundle().apply { putString("searchQuery", query) }
//                findNavController().navigate(R.id.studentsFragment, bundle)
//            }
//        }

        binding.etSearchHome.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                actionId == EditorInfo.IME_ACTION_DONE) {
                val query = binding.etSearchHome.text?.toString()?.trim() ?: ""
                if (query.isNotEmpty()) {
                    val bundle = Bundle().apply { putString("searchQuery", query) }
                    val navOptions = androidx.navigation.NavOptions.Builder()
                        .setPopUpTo(R.id.homeFragment, false) // keep homeFragment in stack
                        .build()
                    findNavController().navigate(R.id.studentsFragment, bundle, navOptions)
                }
                true
            } else false
        }

        // Collect studentCount StateFlow
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.studentCount.collect { count ->
                    binding.tvTotalStudents.text = count.toString()
                }
            }
        }

        // Collect latestStudent StateFlow
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.latestStudent.collect { student ->
                    if (student != null) {
                        binding.tvLatestStudent.text = student.name
                        binding.tvLatestCourse.text = student.course
                    } else {
                        binding.tvLatestStudent.text = "No students yet"
                        binding.tvLatestCourse.text = ""
                    }
                }
            }
        }


        // Card navigation
        binding.cardStudents.setOnClickListener {
            findNavController().navigate(R.id.studentsFragment)
        }
        binding.cardAddStudent.setOnClickListener {
            findNavController().navigate(R.id.addStudentFragment)
        }
        binding.cardDashboard.setOnClickListener {
            findNavController().navigate(R.id.homeFragment)
        }
        binding.cardProfile.setOnClickListener {
            findNavController().navigate(R.id.profileFragment)
        }

        AnalyticsHelper.logScreenView("HomeScreen")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}