package com.example.data.network

import android.app.AppOpsManager
import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.TrafficStats
import android.os.Build
import android.os.Process
import com.example.data.model.AppNetworkUsage
import com.example.data.model.TimeframeUsage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object NetworkStatsHelper {

    fun hasUsageStatsPermission(context: Context): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            }
            mode == AppOpsManager.MODE_ALLOWED
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Queries total mobile and Wi-Fi device-level usage for the given time interval.
     */
    suspend fun queryDeviceUsage(
        context: Context,
        startTimeMillis: Long,
        endTimeMillis: Long
    ): TimeframeUsage = withContext(Dispatchers.IO) {
        val nsm = context.getSystemService(Context.NETWORK_STATS_SERVICE) as? NetworkStatsManager
            ?: return@withContext TimeframeUsage()

        var mobileRx = 0L
        var mobileTx = 0L
        var wifiRx = 0L
        var wifiTx = 0L

        // Query Mobile
        try {
            val mobileBucket = nsm.querySummaryForDevice(
                ConnectivityManager.TYPE_MOBILE,
                null,
                startTimeMillis,
                endTimeMillis
            )
            mobileRx = mobileBucket.rxBytes
            mobileTx = mobileBucket.txBytes
        } catch (_: Exception) {
            // Mobile stats might be restricted without permission or on non-cellular devices
        }

        // Query Wi-Fi
        try {
            val wifiBucket = nsm.querySummaryForDevice(
                ConnectivityManager.TYPE_WIFI,
                null,
                startTimeMillis,
                endTimeMillis
            )
            wifiRx = wifiBucket.rxBytes
            wifiTx = wifiBucket.txBytes
        } catch (_: Exception) {
            // Wi-Fi stats fallback
        }

        // If both returned 0 (e.g. on emulator without active cellular simulation or permission pending),
        // fallback to TrafficStats cumulative if applicable
        if (mobileRx == 0L && mobileTx == 0L && wifiRx == 0L && wifiTx == 0L) {
            val totalRx = TrafficStats.getTotalRxBytes()
            val totalTx = TrafficStats.getTotalTxBytes()
            val mobRx = TrafficStats.getMobileRxBytes()
            val mobTx = TrafficStats.getMobileTxBytes()

            if (totalRx > 0L) {
                val safeMobRx = if (mobRx > 0L) mobRx else 0L
                val safeMobTx = if (mobTx > 0L) mobTx else 0L
                val safeWifiRx = (totalRx - safeMobRx).coerceAtLeast(0L)
                val safeWifiTx = (totalTx - safeMobTx).coerceAtLeast(0L)
                return@withContext TimeframeUsage(
                    mobileRxBytes = safeMobRx,
                    mobileTxBytes = safeMobTx,
                    wifiRxBytes = safeWifiRx,
                    wifiTxBytes = safeWifiTx
                )
            }
        }

        TimeframeUsage(
            mobileRxBytes = mobileRx,
            mobileTxBytes = mobileTx,
            wifiRxBytes = wifiRx,
            wifiTxBytes = wifiTx
        )
    }

    /**
     * Queries per-application network usage. Requires PACKAGE_USAGE_STATS permission.
     */
    suspend fun queryAppUsage(
        context: Context,
        startTimeMillis: Long,
        endTimeMillis: Long
    ): List<AppNetworkUsage> = withContext(Dispatchers.IO) {
        if (!hasUsageStatsPermission(context)) {
            return@withContext emptyList()
        }

        val nsm = context.getSystemService(Context.NETWORK_STATS_SERVICE) as? NetworkStatsManager
            ?: return@withContext emptyList()

        val pm = context.packageManager

        class UidStat(
            var mobileRx: Long = 0L,
            var mobileTx: Long = 0L,
            var wifiRx: Long = 0L,
            var wifiTx: Long = 0L
        )

        val uidMap = mutableMapOf<Int, UidStat>()

        // 1. Mobile query
        try {
            val mobileStats = nsm.querySummary(
                ConnectivityManager.TYPE_MOBILE,
                null,
                startTimeMillis,
                endTimeMillis
            )
            val bucket = NetworkStats.Bucket()
            while (mobileStats.hasNextBucket()) {
                mobileStats.getNextBucket(bucket)
                val stat = uidMap.getOrPut(bucket.uid) { UidStat() }
                stat.mobileRx += bucket.rxBytes
                stat.mobileTx += bucket.txBytes
            }
            mobileStats.close()
        } catch (_: Exception) {
        }

        // 2. Wi-Fi query
        try {
            val wifiStats = nsm.querySummary(
                ConnectivityManager.TYPE_WIFI,
                null,
                startTimeMillis,
                endTimeMillis
            )
            val bucket = NetworkStats.Bucket()
            while (wifiStats.hasNextBucket()) {
                wifiStats.getNextBucket(bucket)
                val stat = uidMap.getOrPut(bucket.uid) { UidStat() }
                stat.wifiRx += bucket.rxBytes
                stat.wifiTx += bucket.txBytes
            }
            wifiStats.close()
        } catch (_: Exception) {
        }

        val resultList = mutableListOf<AppNetworkUsage>()
        var grandTotal = 0L

        for ((uid, stat) in uidMap) {
            val totalMobile = stat.mobileRx + stat.mobileTx
            val totalWifi = stat.wifiRx + stat.wifiTx
            val totalBytes = totalMobile + totalWifi
            if (totalBytes <= 0L) continue

            grandTotal += totalBytes

            var resolvedPkg = "uid.$uid"
            var resolvedName = "System Service ($uid)"
            var isSystem = uid < 10000

            val packages = pm.getPackagesForUid(uid)
            if (!packages.isNullOrEmpty()) {
                resolvedPkg = packages[0]
                try {
                    val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        pm.getApplicationInfo(resolvedPkg, PackageManager.ApplicationInfoFlags.of(0))
                    } else {
                        @Suppress("DEPRECATION")
                        pm.getApplicationInfo(resolvedPkg, 0)
                    }
                    resolvedName = pm.getApplicationLabel(appInfo).toString()
                    isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                } catch (_: Exception) {
                    resolvedName = resolvedPkg
                }
            }

            resultList.add(
                AppNetworkUsage(
                    packageName = resolvedPkg,
                    appName = resolvedName,
                    uid = uid,
                    mobileBytes = totalMobile,
                    wifiBytes = totalWifi,
                    rxBytes = stat.mobileRx + stat.wifiRx,
                    txBytes = stat.mobileTx + stat.wifiTx,
                    totalBytes = totalBytes,
                    isSystemApp = isSystem
                )
            )
        }

        // Calculate percentage of grand total
        val enrichedList = if (grandTotal > 0L) {
            resultList.map { item ->
                item.copy(percentage = (item.totalBytes.toFloat() / grandTotal.toFloat()).coerceIn(0f, 1f))
            }
        } else {
            resultList
        }

        enrichedList.sortedByDescending { it.totalBytes }
    }
}
