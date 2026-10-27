package com.example.data.repository

import android.content.Context
import com.example.data.local.AppUsageSnapshotEntity
import com.example.data.local.DataMonitorDatabase
import com.example.data.local.NotificationLogEntity
import com.example.data.local.UsageDao
import com.example.data.local.UsageSnapshotEntity
import com.example.data.model.AppNetworkUsage
import com.example.data.model.LiveConnectionState
import com.example.data.model.TimeframeSelection
import com.example.data.model.TimeframeUsage
import com.example.data.network.LiveConnectionMonitor
import com.example.data.network.NetworkStatsHelper
import com.example.data.preferences.PreferencesManager
import com.example.data.preferences.UserPreferences
import com.example.domain.billing.BillingCycleCalculator
import com.example.util.DataFormatUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UsageRepository(
    private val context: Context,
    private val usageDao: UsageDao = DataMonitorDatabase.getInstance(context).usageDao(),
    val preferencesManager: PreferencesManager = PreferencesManager(context),
    private val liveConnectionMonitor: LiveConnectionMonitor = LiveConnectionMonitor(context)
) {
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    val liveConnectionState: StateFlow<LiveConnectionState> = liveConnectionMonitor.connectionState
    val userPreferences: StateFlow<UserPreferences> = preferencesManager.userPreferencesFlow

    private val _todayUsage = MutableStateFlow(TimeframeUsage())
    val todayUsage: StateFlow<TimeframeUsage> = _todayUsage.asStateFlow()

    private val _thisWeekUsage = MutableStateFlow(TimeframeUsage())
    val thisWeekUsage: StateFlow<TimeframeUsage> = _thisWeekUsage.asStateFlow()

    private val _thisMonthUsage = MutableStateFlow(TimeframeUsage())
    val thisMonthUsage: StateFlow<TimeframeUsage> = _thisMonthUsage.asStateFlow()

    private val _billingCycleUsage = MutableStateFlow(TimeframeUsage())
    val billingCycleUsage: StateFlow<TimeframeUsage> = _billingCycleUsage.asStateFlow()

    val allSnapshots: Flow<List<UsageSnapshotEntity>> = usageDao.getAllSnapshots()
    val notificationLogs: Flow<List<NotificationLogEntity>> = usageDao.getAllNotificationLogs()

    init {
        refreshUsage()
    }

    fun refreshUsage() {
        repositoryScope.launch {
            val now = System.currentTimeMillis()

            // 1. Today
            val todayStart = BillingCycleCalculator.startOfToday(now)
            val todayStats = NetworkStatsHelper.queryDeviceUsage(context, todayStart, now)
            _todayUsage.value = todayStats

            // 2. This Week
            val weekStart = BillingCycleCalculator.startOfThisWeek(now)
            val weekStats = NetworkStatsHelper.queryDeviceUsage(context, weekStart, now)
            _thisWeekUsage.value = weekStats

            // 3. This Month
            val monthStart = BillingCycleCalculator.startOfThisMonth(now)
            val monthStats = NetworkStatsHelper.queryDeviceUsage(context, monthStart, now)
            _thisMonthUsage.value = monthStats

            // 4. Billing Cycle
            val cycleDay = preferencesManager.getPreferences().cycleStartDay
            val cycleBounds = BillingCycleCalculator.calculate(cycleDay, now)
            val cycleStats = NetworkStatsHelper.queryDeviceUsage(context, cycleBounds.startMillis, now)
            _billingCycleUsage.value = cycleStats

            // Save snapshot for today in Room
            val todayDateKey = DataFormatUtils.formatDate(now, "yyyy-MM-dd")
            val snapshot = UsageSnapshotEntity(
                dateKey = todayDateKey,
                timestamp = todayStart,
                mobileRxBytes = todayStats.mobileRxBytes,
                mobileTxBytes = todayStats.mobileTxBytes,
                wifiRxBytes = todayStats.wifiRxBytes,
                wifiTxBytes = todayStats.wifiTxBytes,
                totalBytes = todayStats.grandTotalBytes
            )
            usageDao.insertOrUpdateSnapshot(snapshot)

            // Cache today's apps if usage access is granted
            if (NetworkStatsHelper.hasUsageStatsPermission(context)) {
                val appList = NetworkStatsHelper.queryAppUsage(context, todayStart, now)
                if (appList.isNotEmpty()) {
                    val entities = appList.map { app ->
                        AppUsageSnapshotEntity(
                            dateKey = todayDateKey,
                            packageName = app.packageName,
                            appName = app.appName,
                            mobileBytes = app.mobileBytes,
                            wifiBytes = app.wifiBytes,
                            totalBytes = app.totalBytes,
                            lastUpdated = now
                        )
                    }
                    usageDao.insertOrUpdateAppSnapshots(entities)
                }
            }
        }
    }

    suspend fun queryUsageForInterval(startTime: Long, endTime: Long): TimeframeUsage = withContext(Dispatchers.IO) {
        val stats = NetworkStatsHelper.queryDeviceUsage(context, startTime, endTime)
        if (stats.grandTotalBytes > 0L) {
            return@withContext stats.copy(isDataAvailable = true)
        }

        // Check local database for historical snapshots in this range
        val snapshots = usageDao.getSnapshotsBetween(startTime, endTime)
        if (snapshots.isNotEmpty()) {
            val mobRx = snapshots.sumOf { it.mobileRxBytes }
            val mobTx = snapshots.sumOf { it.mobileTxBytes }
            val wifiRx = snapshots.sumOf { it.wifiRxBytes }
            val wifiTx = snapshots.sumOf { it.wifiTxBytes }
            return@withContext TimeframeUsage(
                mobileRxBytes = mobRx,
                mobileTxBytes = mobTx,
                wifiRxBytes = wifiRx,
                wifiTxBytes = wifiTx,
                isDataAvailable = true
            )
        }

        // If queried for a past date and neither system nor Room has records, mark isDataAvailable = false
        val now = System.currentTimeMillis()
        val isPastDate = endTime < (now - (12 * 60 * 60 * 1000L))
        if (isPastDate) {
            TimeframeUsage(isDataAvailable = false)
        } else {
            stats.copy(isDataAvailable = true)
        }
    }

    suspend fun fetchAppUsageForInterval(startTime: Long, endTime: Long): List<AppNetworkUsage> = withContext(Dispatchers.IO) {
        NetworkStatsHelper.queryAppUsage(context, startTime, endTime)
    }

    suspend fun fetchAppUsage(timeframe: TimeframeSelection): List<AppNetworkUsage> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val interval = when (timeframe) {
            TimeframeSelection.TODAY -> BillingCycleCalculator.todayInterval(now)
            TimeframeSelection.YESTERDAY -> BillingCycleCalculator.yesterdayInterval(now)
            TimeframeSelection.THIS_WEEK -> BillingCycleCalculator.thisWeekInterval(now)
            TimeframeSelection.LAST_WEEK -> BillingCycleCalculator.lastWeekInterval(now)
            TimeframeSelection.THIS_MONTH -> BillingCycleCalculator.thisMonthInterval(now)
            TimeframeSelection.LAST_MONTH -> BillingCycleCalculator.lastMonthInterval(now)
            else -> BillingCycleCalculator.todayInterval(now)
        }
        NetworkStatsHelper.queryAppUsage(context, interval.startMillis, interval.endMillis)
    }

    suspend fun getSnapshotsSince(fromMillis: Long): List<UsageSnapshotEntity> = withContext(Dispatchers.IO) {
        usageDao.getSnapshotsSince(fromMillis)
    }

    suspend fun getSnapshotsBetween(fromMillis: Long, toMillis: Long): List<UsageSnapshotEntity> = withContext(Dispatchers.IO) {
        usageDao.getSnapshotsBetween(fromMillis, toMillis)
    }

    suspend fun logNotification(
        thresholdPercent: Int,
        networkType: String,
        title: String,
        message: String,
        dateKey: String
    ) = withContext(Dispatchers.IO) {
        usageDao.insertNotificationLog(
            NotificationLogEntity(
                timestamp = System.currentTimeMillis(),
                thresholdPercent = thresholdPercent,
                networkType = networkType,
                title = title,
                message = message,
                dateKey = dateKey
            )
        )
    }

    suspend fun getNotifiedThresholds(dateKey: String, networkType: String): List<Int> = withContext(Dispatchers.IO) {
        usageDao.getNotifiedThresholds(dateKey, networkType)
    }

    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        usageDao.clearSnapshots()
        usageDao.clearAppSnapshots()
        usageDao.clearNotificationLogs()
        refreshUsage()
    }
}
