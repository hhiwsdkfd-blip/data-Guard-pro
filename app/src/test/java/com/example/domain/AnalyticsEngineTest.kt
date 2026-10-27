package com.example.domain

import com.example.domain.analytics.AnalyticsEngine
import com.example.domain.analytics.DailyUsagePoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalyticsEngineTest {

    @Test
    fun computeAnalytics_calculatesPaceAndProjection() {
        val oneGb = 1024L * 1024L * 1024L
        val history = listOf(
            DailyUsagePoint("2026-10-21", 1000L, 1L * oneGb, 1L * oneGb),
            DailyUsagePoint("2026-10-22", 2000L, 1L * oneGb, 1L * oneGb),
            DailyUsagePoint("2026-10-23", 3000L, 1L * oneGb, 1L * oneGb)
        )

        val report = AnalyticsEngine.computeAnalytics(
            usedBytesInCycle = 10L * oneGb,
            quotaLimitBytes = 20L * oneGb,
            daysElapsed = 10,
            daysRemaining = 20,
            todayTotalBytes = 2L * oneGb,
            recentDays = history
        )

        // daily average = 10GB / 10 days = 1GB/day
        assertEquals(oneGb, report.dailyAverageBytes)

        // 7-day average from history = (2GB + 2GB + 2GB) / 3 = 2GB/day
        assertEquals(2L * oneGb, report.sevenDayAverageBytes)

        // projected = 10GB + (2GB/day * 20 days left) = 50GB
        assertEquals(50L * oneGb, report.projectedCycleBytes)

        // days until quota exhaustion: (20GB - 10GB) / 2GB/day = 5 days
        assertEquals(5, report.estimatedDaysUntilExhaustion)

        // Burn rate warning should trigger because 5 days < 20 days remaining!
        assertTrue(report.isBurnRateWarning)
    }

    @Test
    fun computeAnalytics_surgeDetection() {
        val oneMb = 1024L * 1024L
        val normalHistory = listOf(
            DailyUsagePoint("2026-10-21", 1000L, 50L * oneMb, 50L * oneMb), // 100MB
            DailyUsagePoint("2026-10-22", 2000L, 50L * oneMb, 50L * oneMb)  // 100MB
        )

        // Today usage is 300MB, which is 200% higher than 100MB 7-day average (> 1.5x)
        val report = AnalyticsEngine.computeAnalytics(
            usedBytesInCycle = 500L * oneMb,
            quotaLimitBytes = 10L * 1024L * oneMb,
            daysElapsed = 5,
            daysRemaining = 25,
            todayTotalBytes = 300L * oneMb,
            recentDays = normalHistory
        )

        assertTrue(report.isSurgeDetected)
        assertTrue(report.surgePercentage >= 150)
    }

    @Test
    fun computeAnalytics_noSurgeOnNormalTraffic() {
        val oneMb = 1024L * 1024L
        val history = listOf(
            DailyUsagePoint("2026-10-21", 1000L, 100L * oneMb, 100L * oneMb)
        )

        val report = AnalyticsEngine.computeAnalytics(
            usedBytesInCycle = 200L * oneMb,
            quotaLimitBytes = 10L * 1024L * oneMb,
            daysElapsed = 1,
            daysRemaining = 29,
            todayTotalBytes = 210L * oneMb,
            recentDays = history
        )

        assertFalse(report.isSurgeDetected)
    }
}
