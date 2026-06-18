package com.example.studentapplication.utils

object ValidationUtils {

    // Email must have @ and a dot after it — e.g. test@gmail.com
    fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    // Phone must be 10 digits only
    fun isValidPhone(phone: String): Boolean {
        return phone.matches(Regex("^[0-9]{10}$"))
    }

    // Name must be at least 2 characters, letters and spaces only
    fun isValidName(name: String): Boolean {
        return name.length >= 2 && name.matches(Regex("^[a-zA-Z ]+$"))
    }

    // Course must be at least 2 characters
    fun isValidCourse(course: String): Boolean {
        return course.length >= 2
    }
}