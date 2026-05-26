package com.vclass.app.data.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.vclass.app.MainActivity
import com.vclass.app.R
import com.vclass.app.data.api.RetrofitClient
import com.vclass.app.data.local.VClassDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class DeadlineCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val db = VClassDatabase.getInstance(applicationContext)
            val dao = db.dao()
            val api = RetrofitClient.apiService
            val token = "YOUR_MOODLE_TOKEN"
            val userId = 0

            // Get courses from API
            val coursesResponse = api.getUserCourses(token, userId = userId)
            if (coursesResponse.isSuccessful) {
                val courses = coursesResponse.body() ?: emptyList()
                val courseIds = courses.map { it.id }

                // Get assignments
                val assignmentsResult = api.getAssignments(
                    token = token,
                    courseIds = courseIds.mapIndexed { i, id -> "courseids[$i]" to id }.toMap()
                )

                if (assignmentsResult.isSuccessful) {
                    val allAssignments = assignmentsResult.body()
                        ?.courses?.flatMap { it.assignments ?: emptyList() } ?: emptyList()

                    val now = System.currentTimeMillis() / 1000
                    val oneHour = 3600L

                    // Find urgent deadlines (not submitted, due within 1 hour)
                    val urgentAssignments = allAssignments.filter { assignment ->
                        val duedate = assignment.duedate
                        duedate != null && duedate > 0 &&
                        duedate > now &&
                        (duedate - now) <= oneHour
                    }

                    // Send notifications for urgent deadlines
                    urgentAssignments.forEach { assignment ->
                        sendNotification(
                            title = "Deadline Mendekat!",
                            message = "${assignment.name} deadline dalam ${(assignment.duedate!! - now) / 60} menit!",
                            notificationId = assignment.id
                        )
                    }

                    // Also check deadlines within 24 hours
                    val nearAssignments = allAssignments.filter { assignment ->
                        val duedate = assignment.duedate
                        duedate != null && duedate > 0 &&
                        duedate > now &&
                        (duedate - now) > oneHour &&
                        (duedate - now) <= 86400
                    }

                    if (nearAssignments.isNotEmpty()) {
                        sendNotification(
                            title = "Deadline Besok",
                            message = "${nearAssignments.size} tugas deadline dalam 24 jam",
                            notificationId = 99999
                        )
                    }
                }
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun sendNotification(title: String, message: String, notificationId: Int) {
        val context = applicationContext
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create channel for Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Deadline Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi deadline tugas mendekat"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Intent to open app
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    companion object {
        const val CHANNEL_ID = "deadline_notifications"
        const val WORK_NAME = "deadline_check"

        fun schedule(context: Context, intervalMinutes: Int = 15) {
            val request = PeriodicWorkRequestBuilder<DeadlineCheckWorker>(
                intervalMinutes.toLong(), TimeUnit.MINUTES
            ).setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
