package com.example.studentapplication

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.studentapplication.data.repository.AttendanceRepository
import com.example.studentapplication.data.repository.StudentRepository
import com.example.studentapplication.databinding.ActivityLoginBinding
import com.example.studentapplication.utils.AnalyticsHelper
import com.example.studentapplication.utils.AuthPreferences
import com.example.studentapplication.utils.CrashlyticsHelper
import com.example.studentapplication.utils.NotificationHelper
import com.example.studentapplication.worker.WorkManagerScheduler
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var authPrefs: AuthPreferences

    @Inject lateinit var notificationHelper: NotificationHelper
    @Inject lateinit var workManagerScheduler: WorkManagerScheduler

    // need repository to get student count for welcome notification
    @Inject lateinit var repository: StudentRepository
    @Inject lateinit var attendanceRepository: AttendanceRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authPrefs = AuthPreferences(this)

        // Auto-login: already logged in → go straight to MainActivity
        if (authPrefs.isLoggedIn()) {
            goToMain()
            return
        }

        binding.btnLogin.setOnClickListener {
            handleLogin()
        }

        binding.tvGoToSignup.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }

        binding.tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }
    }
    @RequiresPermission(android.Manifest.permission.POST_NOTIFICATIONS)
    private fun handleLogin() {
        val email    = binding.etUsername.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
            return
        }

        FirebaseAuth.getInstance()
            .signInWithEmailAndPassword(email, password)    //run this code only if login succeeds
            .addOnSuccessListener {
                lifecycleScope.launch {
                    // Set Crashlytics user info
                    CrashlyticsHelper.setUser(
                        userId = authPrefs.getCurrentUserId(),
                        email  = authPrefs.getUsername()
                    )
                    CrashlyticsHelper.log("User logged in successfully")
                    AnalyticsHelper.logLogin()
                    AnalyticsHelper.setUser(authPrefs.getCurrentUserId())

                    repository.syncFromCloud(authPrefs.getCurrentUserId())
                    attendanceRepository.syncFromCloud()

                    notificationHelper.createNotificationChannels()
                    val count = repository.getStudentCount(authPrefs.getCurrentUserId()).first()
                    notificationHelper.showWelcomeNotification(authPrefs.getFullName(), count)

                    goToMain()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, e.message ?: "Login failed", Toast.LENGTH_SHORT).show()
            }
    }

    private fun goToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}