package com.example.data.model

enum class NetworkMedium {
    CELLULAR,
    WIFI,
    ETHERNET,
    VPN,
    NONE
}

data class LiveConnectionState(
    val medium: NetworkMedium = NetworkMedium.NONE,
    val isConnected: Boolean = false,
    val isMetered: Boolean = false,
    val networkName: String = "Disconnected",
    val downlinkSpeedKbps: Int = 0,
    val uplinkSpeedKbps: Int = 0,
    val rxBytesPerSec: Long = 0L,
    val txBytesPerSec: Long = 0L
)

data class TimeframeUsage(
    val mobileRxBytes: Long = 0L,
    val mobileTxBytes: Long = 0L,
    val wifiRxBytes: Long = 0L,
    val wifiTxBytes: Long = 0L,
    val isDataAvailable: Boolean = true
) {
    val mobileTotalBytes: Long get() = mobileRxBytes + mobileTxBytes
    val wifiTotalBytes: Long get() = wifiRxBytes + wifiTxBytes
    val grandTotalBytes: Long get() = mobileTotalBytes + wifiTotalBytes
}

data class AppNetworkUsage(
    val packageName: String,
    val appName: String,
    val uid: Int,
    val mobileBytes: Long,
    val wifiBytes: Long,
    val rxBytes: Long = 0L,
    val txBytes: Long = 0L,
    val totalBytes: Long,
    val percentage: Float = 0f,
    val isSystemApp: Boolean = false
)

enum class TimeframeSelection {
    TODAY,
    YESTERDAY,
    THIS_WEEK,
    LAST_WEEK,
    THIS_MONTH,
    LAST_MONTH,
    BILLING_CYCLE,
    SPECIFIC_DAY,
    CUSTOM_RANGE
}

enum class TrafficFilter {
    ALL,
    MOBILE_ONLY,
    WIFI_ONLY
}

enum class AppSortOrder {
    HIGHEST_USAGE,
    LOWEST_USAGE,
    MOBILE_USAGE,
    WIFI_USAGE,
    ALPHABETICAL
}

data class DateRangeSelection(
    val preset: TimeframeSelection = TimeframeSelection.TODAY,
    val startMillis: Long,
    val endMillis: Long,
    val titleAr: String = "اليوم",
    val titleEn: String = "Today"
)
