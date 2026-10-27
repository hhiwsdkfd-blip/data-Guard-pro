package com.example.domain.limits

data class ThresholdTrigger(
    val thresholdPercent: Int,
    val consumedBytes: Long,
    val limitBytes: Long
)

object QuotaEvaluator {

    val STANDARD_THRESHOLDS = listOf(50, 75, 90, 100)

    /**
     * Evaluates whether any standard or custom threshold has been newly crossed.
     * Prevents duplicate alerts by filtering against alreadyNotifiedThresholds.
     */
    fun evaluate(
        consumedBytes: Long,
        limitBytes: Long,
        alreadyNotifiedThresholds: Set<Int>,
        enabledThresholds: Set<Int> = STANDARD_THRESHOLDS.toSet()
    ): List<ThresholdTrigger> {
        if (limitBytes <= 0L || consumedBytes <= 0L) return emptyList()

        val ratio = consumedBytes.toDouble() / limitBytes.toDouble()
        val currentPercent = (ratio * 100.0).toInt()

        val triggers = mutableListOf<ThresholdTrigger>()
        for (threshold in STANDARD_THRESHOLDS) {
            if (threshold in enabledThresholds && currentPercent >= threshold) {
                if (threshold !in alreadyNotifiedThresholds) {
                    triggers.add(
                        ThresholdTrigger(
                            thresholdPercent = threshold,
                            consumedBytes = consumedBytes,
                            limitBytes = limitBytes
                        )
                    )
                }
            }
        }
        return triggers
    }
}
