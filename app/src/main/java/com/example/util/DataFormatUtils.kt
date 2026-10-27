package com.example.util

import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DataFormatUtils {

    private val dfOne = DecimalFormat("#,##0.0")
    private val dfTwo = DecimalFormat("#,##0.00")
    private val dfSpeed = DecimalFormat("#,##0.0")

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0L) return "0 B"
        val kb = 1024.0
        val mb = kb * 1024.0
        val gb = mb * 1024.0
        val tb = gb * 1024.0

        val b = bytes.toDouble()
        return when {
            b >= tb -> "${dfTwo.format(b / tb)} TB"
            b >= gb -> "${dfTwo.format(b / gb)} GB"
            b >= mb -> "${dfTwo.format(b / mb)} MB"
            b >= kb -> "${dfOne.format(b / kb)} KB"
            else -> "$bytes B"
        }
    }

    fun formatSpeed(bytesPerSec: Long): String {
        if (bytesPerSec <= 0L) return "0 B/s"
        val kb = 1024.0
        val mb = kb * 1024.0
        val gb = mb * 1024.0

        val b = bytesPerSec.toDouble()
        return when {
            b >= gb -> "${dfSpeed.format(b / gb)} GB/s"
            b >= mb -> "${dfSpeed.format(b / mb)} MB/s"
            b >= kb -> "${dfSpeed.format(b / kb)} KB/s"
            else -> "$bytesPerSec B/s"
        }
    }

    fun formatPercentage(ratio: Double): String {
        if (ratio.isNaN() || ratio <= 0.0) return "0%"
        val pct = ratio * 100.0
        return if (pct >= 100.0) {
            "${pct.toInt()}%"
        } else {
            "${dfOne.format(pct)}%"
        }
    }

    fun formatDate(timestampMs: Long, pattern: String = "MMM dd, yyyy"): String {
        return try {
            val sdf = SimpleDateFormat(pattern, Locale.getDefault())
            sdf.format(Date(timestampMs))
        } catch (_: Exception) {
            ""
        }
    }

    fun formatShortDay(timestampMs: Long): String {
        return try {
            val sdf = SimpleDateFormat("EEE d", Locale.getDefault())
            sdf.format(Date(timestampMs))
        } catch (_: Exception) {
            ""
        }
    }

    fun formatTime(timestampMs: Long): String {
        return try {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            sdf.format(Date(timestampMs))
        } catch (_: Exception) {
            ""
        }
    }

    fun formatDuration(ms: Long): String {
        if (ms <= 0L) return "0m"
        val minutes = (ms / (1000 * 60)) % 60
        val hours = (ms / (1000 * 60 * 60))
        return if (hours > 0) {
            "${hours}h ${minutes}m"
        } else {
            "${minutes}m"
        }
    }
}
