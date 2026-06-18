package com.example.studentapplication.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.studentapplication.LoginActivity
import com.example.studentapplication.R
import com.example.studentapplication.databinding.FragmentProfileBinding
import com.example.studentapplication.utils.AnalyticsHelper
import com.example.studentapplication.utils.AuthPreferences
import com.example.studentapplication.worker.WorkManagerScheduler
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    // ADD injection in ProfileFragment
    @Inject lateinit var workManagerScheduler: WorkManagerScheduler

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentProfileBinding.bind(view)

        val authPrefs = AuthPreferences(requireContext())

        // Show the logged-in user's name dynamically
        binding.tvAdminName.text = authPrefs.getFullName()

        // Logout button with confirmation dialog
        binding.btnLogout.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes") { _, _ ->
                    // ADD in logout positive button
                    workManagerScheduler.cancelDailyReminder()
                    authPrefs.logout()
                    AnalyticsHelper.logLogout()
                    val intent = Intent(requireContext(), LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
//        binding.btnCrash.setOnClickListener {
//            FirebaseCrashlytics.getInstance().log("About to crash")
//            throw RuntimeException("Test crash from Student App")
//        }

        AnalyticsHelper.logScreenView("ProfileScreen")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}