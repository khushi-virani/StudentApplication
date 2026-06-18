package com.example.studentapplication.utils

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.studentapplication.MainActivity
import com.example.studentapplication.R
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    private val context: Context
) {

    companion object {
        const val CHANNEL_ID_REMINDER  = "student_reminder_channel"
        const val CHANNEL_ID_WELCOME   = "student_welcome_channel"
        const val NOTIFICATION_ID_DAILY   = 1001    //android need to identify notification
        const val NOTIFICATION_ID_WELCOME = 1002
    }

    // Create notification channels
    // Must be called before showing any notification
    fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {   //notification channel only on exists android 8+ api 26+
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE)
                        as NotificationManager

            // Daily reminder channel
            val reminderChannel = NotificationChannel(
                CHANNEL_ID_REMINDER,
                "Daily Reminder",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily student count reminder"
            }

            // Welcome channel
            val welcomeChannel = NotificationChannel(
                CHANNEL_ID_WELCOME,
                "Welcome Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Login welcome notifications"
            }

            notificationManager.createNotificationChannel(reminderChannel)
            notificationManager.createNotificationChannel(welcomeChannel)
        }
    }

    // Show daily reminder notification
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun showDailyReminder(studentCount: Int) {
        // Tapping notification opens MainActivity
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val message = when {
            studentCount == 0 -> "No students enrolled yet. Add your first student!"
            studentCount == 1 -> "You have 1 student enrolled."
            else -> "You have $studentCount students enrolled."
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_REMINDER)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("📚 Student Manager")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context)
            .notify(NOTIFICATION_ID_DAILY, notification)
    }

    // Show welcome notification on login
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun showWelcomeNotification(userName: String, studentCount: Int) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val message = "Welcome back $userName! " +
                "You have $studentCount students enrolled."

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_WELCOME)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("👋 Welcome Back!")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context)
            .notify(NOTIFICATION_ID_WELCOME, notification)
    }
}