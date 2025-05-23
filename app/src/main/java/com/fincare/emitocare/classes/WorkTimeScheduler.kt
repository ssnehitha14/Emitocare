package com.fincare.emitocare

import android.content.Context
import android.util.Log
import androidx.work.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.TimeUnit
import java.util.Calendar

class WorkTimeScheduler(private val context: Context) {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun scheduleWorkTimeNotifications() {
        val userId = auth.currentUser?.uid ?: return

        // 1. Fetch the user's document
        firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) return@addOnSuccessListener

                // 2. Drill into the nested friend/profile data:
                val start = doc.getString("workStartTime") ?: return@addOnSuccessListener
                val end = doc.getString("workEndTime") ?: return@addOnSuccessListener

                // 3. Now that we have actual values, schedule WorkManager
                enqueueWorkRequests(start, end)
            }
            .addOnFailureListener {
                Log.e("WorkTimeScheduler", "Failed to load work times: ${it.message}")
            }
    }

    private fun enqueueWorkRequests(workStartTime: String, workEndTime: String) {
        val startData = workDataOf("workType" to "start")
        val endData = workDataOf("workType" to "end")

        val startRequest = PeriodicWorkRequestBuilder<WorkTimeNotificationWorker>(
            1, TimeUnit.DAYS
        )
            .setInputData(startData)
            .setInitialDelay(getDelayUntilTime(workStartTime), TimeUnit.MILLISECONDS)
            .addTag("work_start")
            .build()

        val endRequest = PeriodicWorkRequestBuilder<WorkTimeNotificationWorker>(
            1, TimeUnit.DAYS
        )
            .setInputData(endData)
            .setInitialDelay(getDelayUntilTime(workEndTime), TimeUnit.MILLISECONDS)
            .addTag("work_end")
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "work_start_reminder",
            ExistingPeriodicWorkPolicy.REPLACE,
            startRequest
        )
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "work_end_reminder",
            ExistingPeriodicWorkPolicy.REPLACE,
            endRequest
        )
    }

    private fun getDelayUntilTime(targetTime: String): Long {
        val parts = targetTime.trim().split(" ")
        if (parts.size != 2) throw IllegalArgumentException("Invalid time format: $targetTime")

        val timePart = parts[0]  // example "08:00"
        val amPmPart = parts[1].uppercase() // "AM" or "PM"

        val (hourString, minuteString) = timePart.split(":")
        var hour = hourString.toInt()
        val minute = minuteString.toInt()

        if (amPmPart == "PM" && hour != 12) {
            hour += 12
        } else if (amPmPart == "AM" && hour == 12) {
            hour = 0
        }

        val now = Calendar.getInstance()
        val then = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(now)) add(Calendar.DAY_OF_MONTH, 1)
        }
        return then.timeInMillis - now.timeInMillis
    }
}
