package com.example.studentapplication.utils

import android.content.Context
import com.google.firebase.auth.FirebaseAuth

class AuthPreferences(context: Context) {

    private val auth = FirebaseAuth.getInstance()
    fun isLoggedIn(): Boolean = auth.currentUser != null

    fun getCurrentUserId(): String = auth.currentUser?.uid ?: ""
    fun getUsername(): String = auth.currentUser?.email ?: ""
    fun getFullName(): String = auth.currentUser?.displayName ?: "User"

    fun logout() = auth.signOut()

    // These are now no-ops — Firebase handles internally
    fun saveAccount(fullName: String, username: String, password: String) {}
    fun setLoggedIn(username: String) {}
    fun accountExists(username: String): Boolean = false
    fun getPassword(username: String): String = ""
}