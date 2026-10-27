package com.example.util

import org.junit.Assert.assertEquals
import org.junit.Test

class DataFormatUtilsTest {

    @Test
    fun formatBytes_zeroAndNegative_returnsZeroBytes() {
        assertEquals("0 B", DataFormatUtils.formatBytes(0L))
        assertEquals("0 B", DataFormatUtils.formatBytes(-500L))
    }

    @Test
    fun formatBytes_bytesRange() {
        assertEquals("512 B", DataFormatUtils.formatBytes(512L))
        assertEquals("1023 B", DataFormatUtils.formatBytes(1023L))
    }

    @Test
    fun formatBytes_kilobytesRange() {
        assertEquals("1.0 KB", DataFormatUtils.formatBytes(1024L))
        assertEquals("1.5 KB", DataFormatUtils.formatBytes(1536L))
    }

    @Test
    fun formatBytes_megabytesRange() {
        val oneMb = 1024L * 1024L
        assertEquals("1.00 MB", DataFormatUtils.formatBytes(oneMb))
        assertEquals("500.00 MB", DataFormatUtils.formatBytes(500L * oneMb))
    }

    @Test
    fun formatBytes_gigabytesRange() {
        val oneGb = 1024L * 1024L * 1024L
        assertEquals("1.00 GB", DataFormatUtils.formatBytes(oneGb))
        assertEquals("15.50 GB", DataFormatUtils.formatBytes((15.5 * oneGb).toLong()))
    }

    @Test
    fun formatSpeed_convertsCorrectly() {
        assertEquals("0 B/s", DataFormatUtils.formatSpeed(0L))
        assertEquals("100 B/s", DataFormatUtils.formatSpeed(100L))
        assertEquals("1.5 KB/s", DataFormatUtils.formatSpeed(1536L))
        assertEquals("10.0 MB/s", DataFormatUtils.formatSpeed(10L * 1024L * 1024L))
    }

    @Test
    fun formatPercentage_handlesEdgeCases() {
        assertEquals("0%", DataFormatUtils.formatPercentage(0.0))
        assertEquals("0%", DataFormatUtils.formatPercentage(-0.5))
        assertEquals("50.0%", DataFormatUtils.formatPercentage(0.50))
        assertEquals("100%", DataFormatUtils.formatPercentage(1.0))
        assertEquals("125%", DataFormatUtils.formatPercentage(1.25))
    }
}
