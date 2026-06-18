package com.example.studentapplication.worker

import android.Manifest
import android.content.Context
import androidx.annotation.RequiresPermission
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.studentapplication.data.repository.StudentRepository
import com.example.studentapplication.utils.AuthPreferences
import com.example.studentapplication.utils.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

// HiltWorker — allows Hilt injection inside Worker
@HiltWorker
class DailyReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: StudentRepository,
    private val authPreferences: AuthPreferences,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(context, workerParams) {

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override suspend fun doWork(): Result {
        return try {
            val userId = authPreferences.getCurrentUserId()
            // Only show notification if user is logged in
            if (userId.isEmpty()) return Result.success()
            val count = repository.getStudentCount(userId).first()
            notificationHelper.createNotificationChannels()
            notificationHelper.showDailyReminder(count)
            Result.success()

        } catch (e: Exception) {
            Result.failure()
        }
    }
}