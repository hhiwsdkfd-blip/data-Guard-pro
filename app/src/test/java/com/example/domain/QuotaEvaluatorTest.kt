package com.example.domain

import com.example.domain.limits.QuotaEvaluator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuotaEvaluatorTest {

    @Test
    fun evaluate_crosses50Percent() {
        val limit = 1000L
        val consumed = 550L
        val alreadyNotified = emptySet<Int>()

        val triggers = QuotaEvaluator.evaluate(consumed, limit, alreadyNotified)
        assertEquals(1, triggers.size)
        assertEquals(50, triggers[0].thresholdPercent)
    }

    @Test
    fun evaluate_crossesMultipleThresholdsInFastUsage() {
        val limit = 1000L
        val consumed = 950L // 95% -> crosses 50, 75, 90
        val alreadyNotified = emptySet<Int>()

        val triggers = QuotaEvaluator.evaluate(consumed, limit, alreadyNotified)
        assertEquals(3, triggers.size)
        assertEquals(50, triggers[0].thresholdPercent)
        assertEquals(75, triggers[1].thresholdPercent)
        assertEquals(90, triggers[2].thresholdPercent)
    }

    @Test
    fun evaluate_deduplicatesAlreadyNotifiedThresholds() {
        val limit = 1000L
        val consumed = 950L
        val alreadyNotified = setOf(50, 75) // 50 and 75 were already notified

        val triggers = QuotaEvaluator.evaluate(consumed, limit, alreadyNotified)
        assertEquals(1, triggers.size)
        assertEquals(90, triggers[0].thresholdPercent)
    }

    @Test
    fun evaluate_disabledOrZeroLimitReturnsEmpty() {
        assertTrue(QuotaEvaluator.evaluate(500L, 0L, emptySet()).isEmpty())
        assertTrue(QuotaEvaluator.evaluate(0L, 1000L, emptySet()).isEmpty())
    }
}
