package com.example.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.R
import com.example.data.network.NetworkStatsHelper
import com.example.data.repository.UsageRepository
import com.example.domain.billing.BillingCycleCalculator
import com.example.domain.limits.QuotaEvaluator
import com.example.notification.NotificationHelper
import com.example.util.DataFormatUtils
import java.util.concurrent.TimeUnit

class DataMonitorWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val repository = UsageRepository(appContext)
        val prefs = repository.preferencesManager.getPreferences()
        val now = System.currentTimeMillis()

        // 1. Refresh & snapshot usage
        repository.refreshUsage()

        // 2. Evaluate limits if mobile limit is enabled
        if (prefs.mobileLimitEnabled && prefs.monthlyLimitGb > 0f) {
            val limitBytes = (prefs.monthlyLimitGb * 1024L * 1024L * 1024L).toLong()
            val bounds = BillingCycleCalculator.calculate(prefs.cycleStartDay, now)
            val cycleUsage = NetworkStatsHelper.queryDeviceUsage(appContext, bounds.startMillis, now)
            val consumedBytes = cycleUsage.mobileTotalBytes

            val dateKey = DataFormatUtils.formatDate(bounds.startMillis, "yyyy-MM")
            val alreadyNotified = repository.getNotifiedThresholds(dateKey, "MOBILE").toSet()

            val enabledThresholds = mutableSetOf<Int>()
            if (prefs.threshold50) enabledThresholds.add(50)
            if (prefs.threshold75) enabledThresholds.add(75)
            if (prefs.threshold90) enabledThresholds.add(90)
            if (prefs.threshold100) enabledThresholds.add(100)

            val triggers = QuotaEvaluator.evaluate(
                consumedBytes = consumedBytes,
                limitBytes = limitBytes,
                alreadyNotifiedThresholds = alreadyNotified,
                enabledThresholds = enabledThresholds
            )

            for (trigger in triggers) {
                val title = when (trigger.thresholdPercent) {
                    50 -> appContext.getString(R.string.notif_50_title)
                    75 -> appContext.getString(R.string.notif_75_title)
                    90 -> appContext.getString(R.string.notif_90_title)
                    else -> appContext.getString(R.string.notif_100_title)
                }
                val body = appContext.getString(
                    R.string.notif_body_format,
                    DataFormatUtils.formatBytes(trigger.consumedBytes),
                    DataFormatUtils.formatBytes(trigger.limitBytes)
                )

                NotificationHelper.showThresholdNotification(
                    context = appContext,
                    notificationId = 1000 + trigger.thresholdPercent,
                    title = title,
                    message = body
                )

                repository.logNotification(
                    thresholdPercent = trigger.thresholdPercent,
                    networkType = "MOBILE",
                    title = title,
                    message = body,
                    dateKey = dateKey
                )
            }
        }

        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "periodic_data_monitor_work"

        fun schedule(context: Context, intervalMinutes: Int = 15) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val safeInterval = intervalMinutes.coerceAtLeast(15).toLong()

            val workRequest = PeriodicWorkRequestBuilder<DataMonitorWorker>(
                safeInterval,
                TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
        }
    }
}
