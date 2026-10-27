package com.example.domain.analytics

import kotlin.math.max
import kotlin.math.roundToInt

data class DailyUsagePoint(
    val dateKey: String,
    val timestamp: Long,
    val mobileBytes: Long,
    val wifiBytes: Long,
    val totalBytes: Long = mobileBytes + wifiBytes
)

data class AnalyticsReport(
    val dailyAverageBytes: Long,
    val sevenDayAverageBytes: Long,
    val projectedCycleBytes: Long,
    val daysRemainingInCycle: Int,
    val estimatedDaysUntilExhaustion: Int,
    val isBurnRateWarning: Boolean,
    val isSurgeDetected: Boolean,
    val surgePercentage: Int,
    val peakDayName: String,
    val peakDayBytes: Long,
    val mobileRatio: Float,
    val wifiRatio: Float
)

object AnalyticsEngine {

    /**
     * Computes statistics and forecasts from current cycle telemetry and history.
     */
    fun computeAnalytics(
        usedBytesInCycle: Long,
        quotaLimitBytes: Long,
        daysElapsed: Int,
        daysRemaining: Int,
        todayTotalBytes: Long,
        recentDays: List<DailyUsagePoint>
    ): AnalyticsReport {
        val safeElapsed = max(1, daysElapsed)
        val dailyAverageBytes = usedBytesInCycle / safeElapsed

        // 7-day average from historical snapshots
        val last7Days = recentDays.takeLast(7)
        val sevenDayAverageBytes = if (last7Days.isNotEmpty()) {
            val sum = last7Days.sumOf { it.totalBytes }
            sum / last7Days.size
        } else {
            dailyAverageBytes
        }

        // Projected consumption at end of cycle:
        // current used + (pace * days left)
        val paceToUse = if (sevenDayAverageBytes > 0L) sevenDayAverageBytes else dailyAverageBytes
        val projectedCycleBytes = usedBytesInCycle + (paceToUse * daysRemaining)

        // Burn rate & exhaustion
        val remainingBytes = max(0L, quotaLimitBytes - usedBytesInCycle)
        val estimatedDaysUntilExhaustion = if (quotaLimitBytes > 0L && paceToUse > 0L) {
            (remainingBytes.toDouble() / paceToUse.toDouble()).roundToInt()
        } else {
            -1
        }

        val isBurnRateWarning = quotaLimitBytes > 0L &&
                estimatedDaysUntilExhaustion in 0 until daysRemaining &&
                remainingBytes > 0L

        // Surge detection: if today is > 50% higher than 7-day average (and at least 50MB used)
        val minSurgeThreshold = 50L * 1024L * 1024L
        val isSurgeDetected = sevenDayAverageBytes > minSurgeThreshold &&
                todayTotalBytes > (sevenDayAverageBytes * 1.5).toLong()

        val surgePercentage = if (sevenDayAverageBytes > 0L && todayTotalBytes > sevenDayAverageBytes) {
            (((todayTotalBytes - sevenDayAverageBytes).toDouble() / sevenDayAverageBytes.toDouble()) * 100.0).toInt()
        } else {
            0
        }

        // Peak day in history
        val peak = recentDays.maxByOrNull { it.totalBytes }
        val peakDayName = peak?.dateKey ?: "Today"
        val peakDayBytes = peak?.totalBytes ?: todayTotalBytes

        // Mobile vs WiFi ratio in history
        val totalHistoryMobile = recentDays.sumOf { it.mobileBytes }
        val totalHistoryWifi = recentDays.sumOf { it.wifiBytes }
        val totalHistory = totalHistoryMobile + totalHistoryWifi

        val mobileRatio = if (totalHistory > 0L) {
            (totalHistoryMobile.toFloat() / totalHistory.toFloat()).coerceIn(0f, 1f)
        } else {
            0.5f
        }
        val wifiRatio = 1.0f - mobileRatio

        return AnalyticsReport(
            dailyAverageBytes = dailyAverageBytes,
            sevenDayAverageBytes = sevenDayAverageBytes,
            projectedCycleBytes = projectedCycleBytes,
            daysRemainingInCycle = daysRemaining,
            estimatedDaysUntilExhaustion = estimatedDaysUntilExhaustion,
            isBurnRateWarning = isBurnRateWarning,
            isSurgeDetected = isSurgeDetected,
            surgePercentage = surgePercentage,
            peakDayName = peakDayName,
            peakDayBytes = peakDayBytes,
            mobileRatio = mobileRatio,
            wifiRatio = wifiRatio
        )
    }
}
