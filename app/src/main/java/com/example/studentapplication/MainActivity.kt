package com.example.studentapplication

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.studentapplication.data.repository.AttendanceRepository
import com.example.studentapplication.data.repository.StudentRepository
import com.example.studentapplication.databinding.ActivityMainBinding
import com.example.studentapplication.utils.AuthPreferences
import com.example.studentapplication.utils.NotificationHelper
import com.example.studentapplication.viewmodel.StudentViewModel
import com.example.studentapplication.worker.DailyReminderWorker
import com.example.studentapplication.worker.WorkManagerScheduler
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: StudentViewModel by viewModels()
    @Inject lateinit var notificationHelper: NotificationHelper
    @Inject lateinit var workManagerScheduler: WorkManagerScheduler

    @Inject lateinit var studentRepository: StudentRepository
    @Inject lateinit var attendanceRepository: AttendanceRepository

    private var studentListener: ListenerRegistration? = null
    private var attendanceListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

//        binding.bottomNav.setupWithNavController(navController)

        // Sync bottom nav highlight when destination changes
        navController.addOnDestinationChangedListener { _, destination, _ ->
            // Update selected bottom nav item without triggering navigation
            binding.bottomNav.menu.findItem(destination.id)?.isChecked = true

            // Hide bottom nav on detail screens
            binding.bottomNav.visibility = when (destination.id) {
                R.id.studentDetailFragment,
                R.id.editStudentFragment,
                R.id.studentAttendanceFragment -> View.GONE
                else -> View.VISIBLE
            }
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            val currentId = navController.currentDestination?.id
            if (currentId != item.itemId) {
                val navOptions = NavOptions.Builder()
                    .setPopUpTo(R.id.homeFragment, false)
                    .setLaunchSingleTop(true)
                    .build()
                navController.navigate(item.itemId, null, navOptions)
            }
            true
        }
        notificationHelper.createNotificationChannels()

        // Schedule daily reminder
        workManagerScheduler.scheduleDailyReminder()

        // Badge on Students tab

        val userId = AuthPreferences(this).getCurrentUserId()
        viewModel.setUserId(userId)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.studentCount.collect { count ->
                    val badge = binding.bottomNav.getOrCreateBadge(R.id.studentsFragment)
                    if (count > 0) {
                        badge.isVisible = true
                        badge.number = count
                    } else {
                        badge.isVisible = false
                    }
                }
            }
        }
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.studentDetailFragment,
                R.id.editStudentFragment -> {
                    binding.bottomNav.visibility = View.GONE
                }
                else -> {
                    binding.bottomNav.visibility = View.VISIBLE
                }
            }
        }
        //  Ask permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }

        studentListener = studentRepository.startRealtimeSync(lifecycleScope)
        attendanceListener = attendanceRepository.startRealtimeSync(lifecycleScope)

        // Get FCM token for this device
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            Log.d("FCM", "Token: $token")
            // Save to Firestore
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid != null) {
                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(uid)
                    .update("fcmToken", token)
            }
        }

        // Subscribe to topic — all users get notified
        FirebaseMessaging.getInstance().subscribeToTopic("students")    //every device joining students to become member
            .addOnSuccessListener {
                Log.d("FCM", "Subscribed to students topic")
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Stop listeners when app closes
        studentListener?.remove()
        attendanceListener?.remove()
    }
}
