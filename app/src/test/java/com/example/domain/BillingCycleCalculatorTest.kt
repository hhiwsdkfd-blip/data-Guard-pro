package com.example.domain

import com.example.domain.billing.BillingCycleCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class BillingCycleCalculatorTest {

    @Test
    fun calculate_cycleStartDayOne() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 15, 12, 0, 0)
        }
        val bounds = BillingCycleCalculator.calculate(1, cal.timeInMillis)

        val startCal = Calendar.getInstance().apply { timeInMillis = bounds.startMillis }
        assertEquals(2026, startCal.get(Calendar.YEAR))
        assertEquals(Calendar.OCTOBER, startCal.get(Calendar.MONTH))
        assertEquals(1, startCal.get(Calendar.DAY_OF_MONTH))

        assertTrue(bounds.daysElapsed in 14..16)
        assertTrue(bounds.daysRemaining in 15..17)
        assertTrue(bounds.totalDays in 30..31)
        assertTrue(bounds.progressFraction in 0.4f..0.6f)
    }

    @Test
    fun calculate_cycleStartMidMonth_beforeStartDay() {
        // Today is Oct 10, but cycle starts on the 15th (so current cycle started Sep 15)
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 10, 10, 0, 0)
        }
        val bounds = BillingCycleCalculator.calculate(15, cal.timeInMillis)

        val startCal = Calendar.getInstance().apply { timeInMillis = bounds.startMillis }
        assertEquals(Calendar.SEPTEMBER, startCal.get(Calendar.MONTH))
        assertEquals(15, startCal.get(Calendar.DAY_OF_MONTH))
        assertTrue(bounds.daysRemaining >= 4)
    }

    @Test
    fun calculate_cycleStartClampedToMonthMaximum() {
        // February does not have 31 days
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.FEBRUARY, 10, 0, 0, 0)
        }
        val bounds = BillingCycleCalculator.calculate(31, cal.timeInMillis)
        val startCal = Calendar.getInstance().apply { timeInMillis = bounds.startMillis }
        assertTrue(startCal.get(Calendar.DAY_OF_MONTH) in 28..31)
    }

    @Test
    fun yesterdayInterval_exactBoundaries() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 20, 15, 30, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val interval = BillingCycleCalculator.yesterdayInterval(cal.timeInMillis)

        val startCal = Calendar.getInstance().apply { timeInMillis = interval.startMillis }
        val endCal = Calendar.getInstance().apply { timeInMillis = interval.endMillis }

        assertEquals(19, startCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(0, startCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, startCal.get(Calendar.MINUTE))
        assertEquals(0, startCal.get(Calendar.SECOND))

        assertEquals(19, endCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(23, endCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(59, endCal.get(Calendar.MINUTE))
        assertEquals(59, endCal.get(Calendar.SECOND))
        assertEquals(999, endCal.get(Calendar.MILLISECOND))

        assertEquals("أمس", interval.titleAr)
        assertEquals("Yesterday", interval.titleEn)
    }

    @Test
    fun specificDayInterval_boundsAndFormat() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 14, 8, 45, 12)
        }
        val interval = BillingCycleCalculator.specificDayInterval(cal.timeInMillis)

        val startCal = Calendar.getInstance().apply { timeInMillis = interval.startMillis }
        val endCal = Calendar.getInstance().apply { timeInMillis = interval.endMillis }

        assertEquals(14, startCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(Calendar.OCTOBER, startCal.get(Calendar.MONTH))
        assertEquals(0, startCal.get(Calendar.HOUR_OF_DAY))

        assertEquals(14, endCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(23, endCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(59, endCal.get(Calendar.MINUTE))
        assertEquals(999, endCal.get(Calendar.MILLISECOND))

        // Title matches "14/10/2026"
        assertEquals("14/10/2026", interval.titleAr)
        assertEquals("14/10/2026", interval.titleEn)
    }

    @Test
    fun lastMonthInterval_spansFullPreviousMonth() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 15, 12, 0, 0)
        }
        val interval = BillingCycleCalculator.lastMonthInterval(cal.timeInMillis)

        val startCal = Calendar.getInstance().apply { timeInMillis = interval.startMillis }
        val endCal = Calendar.getInstance().apply { timeInMillis = interval.endMillis }

        // Last month was September (has 30 days)
        assertEquals(Calendar.SEPTEMBER, startCal.get(Calendar.MONTH))
        assertEquals(1, startCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(0, startCal.get(Calendar.HOUR_OF_DAY))

        assertEquals(Calendar.SEPTEMBER, endCal.get(Calendar.MONTH))
        assertEquals(30, endCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(23, endCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(59, endCal.get(Calendar.MINUTE))
        assertEquals(999, endCal.get(Calendar.MILLISECOND))

        assertEquals("الشهر السابق", interval.titleAr)
        assertEquals("Last Month", interval.titleEn)
    }

    @Test
    fun customRangeInterval_coversFullStartToEndDays() {
        val startCal = Calendar.getInstance().apply { set(2026, Calendar.MAY, 5, 10, 0, 0) }
        val endCal = Calendar.getInstance().apply { set(2026, Calendar.MAY, 10, 16, 0, 0) }

        val interval = BillingCycleCalculator.customRangeInterval(startCal.timeInMillis, endCal.timeInMillis)

        val s = Calendar.getInstance().apply { timeInMillis = interval.startMillis }
        val e = Calendar.getInstance().apply { timeInMillis = interval.endMillis }

        assertEquals(5, s.get(Calendar.DAY_OF_MONTH))
        assertEquals(0, s.get(Calendar.HOUR_OF_DAY))

        assertEquals(10, e.get(Calendar.DAY_OF_MONTH))
        assertEquals(23, e.get(Calendar.HOUR_OF_DAY))
        assertEquals(59, e.get(Calendar.MINUTE))
        assertEquals(999, e.get(Calendar.MILLISECOND))
    }
}

