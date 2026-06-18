package com.example.studentapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.studentapplication.databinding.ActivitySignupBinding
import com.example.studentapplication.utils.AnalyticsHelper
import com.example.studentapplication.utils.AuthPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private lateinit var authPrefs: AuthPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authPrefs = AuthPreferences(this)

        binding.btnSignup.setOnClickListener {
            handleSignup()
        }

        binding.tvGoToLogin.setOnClickListener {
            finish()
        }
    }

private fun handleSignup() {
    val fullName        = binding.etFullName.text.toString().trim()
    val email           = binding.etUsername.text.toString().trim()  // field reused as email
    val password        = binding.etPassword.text.toString().trim()
    val confirmPassword = binding.etConfirmPassword.text.toString().trim()

    if (fullName.isEmpty() || email.isEmpty() ||
        password.isEmpty() || confirmPassword.isEmpty()) {
        Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
        return
    }

    if (password.length < 6) {
        Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
        return
    }

    if (password != confirmPassword) {
        Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
        return
    }

    FirebaseAuth.getInstance()
        .createUserWithEmailAndPassword(email, password)
        .addOnSuccessListener { result ->
            // Save display name to Firebase profile
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(fullName)
                .build()
            result.user?.updateProfile(profileUpdates)
            AnalyticsHelper.logSignup()
            Toast.makeText(this, "Account created! Welcome, $fullName!", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
        }
        .addOnFailureListener { e ->
            Toast.makeText(this, e.message ?: "Signup failed", Toast.LENGTH_SHORT).show()
        }
}
}