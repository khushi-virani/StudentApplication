package com.example.studentapplication

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.studentapplication.databinding.ActivityForgotPasswordBinding
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint  //tells that this activity can receive dependency
class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForgotPasswordBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSendReset.setOnClickListener {
            handlePasswordReset()
        }

        binding.tvBackToLogin.setOnClickListener {
            finish()
        }
    }

    private fun handlePasswordReset() {
        val email = binding.etEmail.text.toString().trim()

        if (email.isEmpty()) {
            Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show()
            return
        }

        // Disable button to prevent double tap
        binding.btnSendReset.isEnabled = false
        binding.btnSendReset.text = "Sending..."

        FirebaseAuth.getInstance()
            .sendPasswordResetEmail(email)
            .addOnSuccessListener {
                Toast.makeText(
                    this,
                    "Reset link sent! Check your email.",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
            .addOnFailureListener { e ->
                binding.btnSendReset.isEnabled = true
                binding.btnSendReset.text = "Send Reset Link"
                Toast.makeText(this, e.message ?: "Failed to send reset email", Toast.LENGTH_SHORT).show()
            }
    }
}