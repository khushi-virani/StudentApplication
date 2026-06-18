package com.example.studentapplication.utils

import com.google.firebase.crashlytics.FirebaseCrashlytics
object CrashlyticsHelper {

    private val crashlytics: FirebaseCrashlytics? by lazy {
        try {
            FirebaseCrashlytics.getInstance()
        } catch (e: Exception) {
            null
        }
    }

    fun setUser(userId: String, email: String) {
        crashlytics?.setUserId(userId)
        crashlytics?.setCustomKey("email", email)
    }

    fun log(message: String) {
        crashlytics?.log(message)
    }

    fun recordError(throwable: Throwable, message: String = "") {
        if (message.isNotEmpty()) crashlytics?.log(message)
        crashlytics?.recordException(throwable)
    }
}