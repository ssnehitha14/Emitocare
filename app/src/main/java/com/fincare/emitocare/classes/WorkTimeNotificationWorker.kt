package com.fincare.emitocare

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class WorkTimeNotificationWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val context = applicationContext
        val workType = inputData.getString("workType") ?: return Result.failure()

        // Get the array of messages based on workType
        val messages = when (workType) {
            "start" -> context.resources.getStringArray(R.array.work_start_messages)
            "end" -> context.resources.getStringArray(R.array.work_end_messages)
            else -> return Result.failure()
        }

        // Pick a random message from the array
        val randomMessage = messages.random()

        // Define the notification title based on the type
        val title = when (workType) {
            "start" -> context.getString(R.string.motivation_reminder_title)
            "end" -> context.getString(R.string.well_done_title)
            else -> context.getString(R.string.reminder_title)
        }

        // Send the notification
        NotificationHelper.createNotification(context, title, randomMessage)
        return Result.success()
    }
}
