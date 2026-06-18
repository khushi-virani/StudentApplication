package com.example.studentapplication.utils

import android.os.Bundle
import androidx.room.Query
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
object AnalyticsHelper {

    private val analytics: FirebaseAnalytics? by lazy {
        try { Firebase.analytics } catch (e: Exception) { null }
    }

    fun logStudentAdded(studentName: String, course: String) {
        try {
            val bundle = Bundle().apply {
                putString("student_name", studentName)
                putString("course", course)
            }
            analytics?.logEvent("student_added", bundle)
        } catch (e: Exception) { /* ignore in unit tests */ }
    }

    fun logStudentDeleted(studentName: String) {
        try {
            analytics?.logEvent("student_deleted", Bundle().apply {
                putString("student_name", studentName)
            })
        } catch (e: Exception) { }
    }

    fun logStudentUpdated(studentName: String) {
        try {
            analytics?.logEvent("student_updated", Bundle().apply {
                putString("student_name", studentName)
            })
        } catch (e: Exception) { }
    }

    fun logAttendanceMarked(status: String, date: String) {
        try {
            analytics?.logEvent("attendance_marked", Bundle().apply {
                putString("status", status)
                putString("date", date)
            })
        } catch (e: Exception) { }
    }

    fun logDateRangeFiltered(startDate: String, endDate: String, resultCount: Int) {
        try {
            analytics?.logEvent("attendance_date_range_filter", Bundle().apply {
                putString("start_date", startDate)
                putString("end_date", endDate)
                putInt("result_count", resultCount)
            })
        } catch (e: Exception) { }
    }

    fun logLogin() {
        try {
            analytics?.logEvent(FirebaseAnalytics.Event.LOGIN, Bundle().apply {
                putString(FirebaseAnalytics.Param.METHOD, "email_password")
            })
        } catch (e: Exception) { }
    }

    fun logSignup() {
        try {
            analytics?.logEvent(FirebaseAnalytics.Event.SIGN_UP, Bundle().apply {
                putString(FirebaseAnalytics.Param.METHOD, "email_password")
            })
        } catch (e: Exception) { }
    }

    fun logLogout() {
        try { analytics?.logEvent("logout", null) } catch (e: Exception) { }
    }

    fun logSearch(query: String) {
        try {
            analytics?.logEvent(FirebaseAnalytics.Event.SEARCH, Bundle().apply {
                putString(FirebaseAnalytics.Param.SEARCH_TERM, query)
            })
        } catch (e: Exception) { }
    }

    fun logScreenView(screenName: String) {
        try {
            analytics?.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, Bundle().apply {
                putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
                putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenName)
            })
        } catch (e: Exception) { }
    }

    fun setUser(userId: String) {
        try { analytics?.setUserId(userId) } catch (e: Exception) { }
    }
}