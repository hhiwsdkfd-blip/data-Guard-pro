package com.example.domain.billing

import java.util.Calendar
import kotlin.math.max
import kotlin.math.min

data class BillingCycleBounds(
    val startMillis: Long,
    val endMillis: Long,
    val daysElapsed: Int,
    val daysRemaining: Int,
    val totalDays: Int,
    val progressFraction: Float
)

data class DateInterval(
    val startMillis: Long,
    val endMillis: Long,
    val titleAr: String,
    val titleEn: String
)

object BillingCycleCalculator {

    fun calculate(cycleStartDay: Int, referenceMillis: Long = System.currentTimeMillis()): BillingCycleBounds {
        val clampedDay = cycleStartDay.coerceIn(1, 31)

        val cal = Calendar.getInstance().apply {
            timeInMillis = referenceMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val currentDayOfMonth = cal.get(Calendar.DAY_OF_MONTH)

        val startCal = (cal.clone() as Calendar).apply {
            val maxDayInMonth = getActualMaximum(Calendar.DAY_OF_MONTH)
            val effectiveStartDay = min(clampedDay, maxDayInMonth)

            if (currentDayOfMonth >= effectiveStartDay) {
                set(Calendar.DAY_OF_MONTH, effectiveStartDay)
            } else {
                add(Calendar.MONTH, -1)
                val prevMax = getActualMaximum(Calendar.DAY_OF_MONTH)
                set(Calendar.DAY_OF_MONTH, min(clampedDay, prevMax))
            }
        }

        val endCal = (startCal.clone() as Calendar).apply {
            add(Calendar.MONTH, 1)
            val nextMax = getActualMaximum(Calendar.DAY_OF_MONTH)
            set(Calendar.DAY_OF_MONTH, min(clampedDay, nextMax))
            add(Calendar.MILLISECOND, -1)
        }

        val startMillis = startCal.timeInMillis
        val endMillis = endCal.timeInMillis

        val elapsedMs = max(0L, referenceMillis - startMillis)
        val totalMs = max(1L, endMillis - startMillis)

        val oneDayMs = 24 * 60 * 60 * 1000L
        val daysElapsed = max(1, (elapsedMs / oneDayMs).toInt())
        val totalDays = max(1, (totalMs / oneDayMs).toInt())
        val daysRemaining = max(0, totalDays - daysElapsed)

        val progressFraction = (elapsedMs.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)

        return BillingCycleBounds(
            startMillis = startMillis,
            endMillis = endMillis,
            daysElapsed = daysElapsed,
            daysRemaining = daysRemaining,
            totalDays = totalDays,
            progressFraction = progressFraction
        )
    }

    fun startOfDay(referenceMillis: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = referenceMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun endOfDay(referenceMillis: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = referenceMillis
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return cal.timeInMillis
    }

    fun startOfToday(referenceMillis: Long = System.currentTimeMillis()): Long = startOfDay(referenceMillis)

    fun todayInterval(now: Long = System.currentTimeMillis()): DateInterval {
        return DateInterval(
            startMillis = startOfToday(now),
            endMillis = now,
            titleAr = "اليوم",
            titleEn = "Today"
        )
    }

    fun yesterdayInterval(now: Long = System.currentTimeMillis()): DateInterval {
        val startOfToday = startOfToday(now)
        val yesterdayStart = startOfToday - (24 * 60 * 60 * 1000L)
        val yesterdayEnd = startOfToday - 1L
        return DateInterval(
            startMillis = yesterdayStart,
            endMillis = yesterdayEnd,
            titleAr = "أمس",
            titleEn = "Yesterday"
        )
    }

    fun specificDayInterval(dayMillis: Long): DateInterval {
        val start = startOfDay(dayMillis)
        val end = endOfDay(dayMillis)
        val cal = Calendar.getInstance().apply { timeInMillis = dayMillis }
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val month = cal.get(Calendar.MONTH) + 1
        val year = cal.get(Calendar.YEAR)
        val formatted = "$day/$month/$year"

        return DateInterval(
            startMillis = start,
            endMillis = end,
            titleAr = formatted,
            titleEn = formatted
        )
    }

    fun startOfThisWeek(referenceMillis: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = referenceMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
        }
        return cal.timeInMillis
    }

    fun thisWeekInterval(now: Long = System.currentTimeMillis()): DateInterval {
        return DateInterval(
            startMillis = startOfThisWeek(now),
            endMillis = now,
            titleAr = "الأسبوع الحالي",
            titleEn = "This Week"
        )
    }

    fun lastWeekInterval(now: Long = System.currentTimeMillis()): DateInterval {
        val thisWeekStart = startOfThisWeek(now)
        val lastWeekStart = thisWeekStart - (7 * 24 * 60 * 60 * 1000L)
        val lastWeekEnd = thisWeekStart - 1L
        return DateInterval(
            startMillis = lastWeekStart,
            endMillis = lastWeekEnd,
            titleAr = "الأسبوع السابق",
            titleEn = "Last Week"
        )
    }

    fun startOfThisMonth(referenceMillis: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = referenceMillis
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun thisMonthInterval(now: Long = System.currentTimeMillis()): DateInterval {
        return DateInterval(
            startMillis = startOfThisMonth(now),
            endMillis = now,
            titleAr = "الشهر الحالي",
            titleEn = "This Month"
        )
    }

    fun lastMonthInterval(now: Long = System.currentTimeMillis()): DateInterval {
        val cal = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.MONTH, -1)
        }
        val startMillis = cal.timeInMillis
        val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        cal.set(Calendar.DAY_OF_MONTH, maxDays)
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val endMillis = cal.timeInMillis

        return DateInterval(
            startMillis = startMillis,
            endMillis = endMillis,
            titleAr = "الشهر السابق",
            titleEn = "Last Month"
        )
    }

    fun customRangeInterval(startMillis: Long, endMillis: Long): DateInterval {
        val start = startOfDay(startMillis)
        val end = endOfDay(endMillis)
        val calS = Calendar.getInstance().apply { timeInMillis = start }
        val calE = Calendar.getInstance().apply { timeInMillis = end }
        val label = "${calS.get(Calendar.DAY_OF_MONTH)}/${calS.get(Calendar.MONTH) + 1} - ${calE.get(Calendar.DAY_OF_MONTH)}/${calE.get(Calendar.MONTH) + 1}"
        return DateInterval(
            startMillis = start,
            endMillis = end,
            titleAr = label,
            titleEn = label
        )
    }
}
