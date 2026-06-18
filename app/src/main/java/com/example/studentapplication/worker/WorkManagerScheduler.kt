package com.example.studentapplication.worker

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkManagerScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        const val DAILY_REMINDER_WORK = "daily_reminder_work"
    }

    // Schedule daily reminder — runs once every 24 hours
//    fun scheduleDailyReminder() {
//        val constraints = Constraints.Builder()
//            .setRequiresBatteryNotLow(true) // don't run on low battery
//            .build()
//
//        val dailyWorkRequest =
//            PeriodicWorkRequestBuilder<DailyReminderWorker>(    //mean -> this request will run DailyReminderWorker
//                24, TimeUnit.HOURS  // repeat every 24 hours
//            )
//                .setConstraints(constraints)    //attach rules
//                .build()    //create final workRequest
//
//        // KEEP existing work if already scheduled
//        // so it doesn't reset the timer on every app open
//        WorkManager.getInstance(context).enqueueUniquePeriodicWork( //schedule repeating worker
//            DAILY_REMINDER_WORK,    //worker name
//            ExistingPeriodicWorkPolicy.KEEP,    //if already exists then keep old worker
//            dailyWorkRequest
//        )
//    }
    fun scheduleDailyReminder() {
        // Remove battery constraint — it blocks worker on most devices
        val dailyWorkRequest =
            PeriodicWorkRequestBuilder<DailyReminderWorker>(
                24, TimeUnit.HOURS
            )
                .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            DAILY_REMINDER_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,  // ← UPDATE forces reschedule immediately
            dailyWorkRequest
        )
    }

    // Cancel daily reminder — call on logout
    fun cancelDailyReminder() {
        WorkManager.getInstance(context)
            .cancelUniqueWork(DAILY_REMINDER_WORK)
    }
}