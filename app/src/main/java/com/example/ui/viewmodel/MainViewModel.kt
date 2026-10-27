package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.local.NotificationLogEntity
import com.example.data.local.UsageSnapshotEntity
import com.example.data.model.AppNetworkUsage
import com.example.data.model.AppSortOrder
import com.example.data.model.DateRangeSelection
import com.example.data.model.LiveConnectionState
import com.example.data.model.TimeframeSelection
import com.example.data.model.TimeframeUsage
import com.example.data.model.TrafficFilter
import com.example.data.network.NetworkStatsHelper
import com.example.data.preferences.UserPreferences
import com.example.data.repository.UsageRepository
import com.example.domain.analytics.AnalyticsEngine
import com.example.domain.analytics.AnalyticsReport
import com.example.domain.analytics.DailyUsagePoint
import com.example.domain.billing.BillingCycleCalculator
import com.example.domain.billing.DateInterval
import com.example.notification.NotificationHelper
import com.example.util.DataFormatUtils
import com.example.worker.DataMonitorWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppScreen {
    SPLASH,
    ONBOARDING,
    DASHBOARD,
    APP_USAGE,
    APP_DETAIL,
    CHARTS_REPORTS,
    SETTINGS,
    PERMISSIONS,
    ABOUT,
    LIMITS,
    NOTIFICATIONS,
    BREAKDOWN,
    HISTORY,
    ANALYTICS
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val repository = UsageRepository(application)

    private val _currentScreen = MutableStateFlow(AppScreen.SPLASH)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val screenBackStack = mutableListOf<AppScreen>()

    val liveConnectionState: StateFlow<LiveConnectionState> = repository.liveConnectionState
    val userPreferences: StateFlow<UserPreferences> = repository.userPreferences

    val todayUsage: StateFlow<TimeframeUsage> = repository.todayUsage
    val thisWeekUsage: StateFlow<TimeframeUsage> = repository.thisWeekUsage
    val thisMonthUsage: StateFlow<TimeframeUsage> = repository.thisMonthUsage
    val billingCycleUsage: StateFlow<TimeframeUsage> = repository.billingCycleUsage

    val historySnapshots: StateFlow<List<UsageSnapshotEntity>> = repository.allSnapshots
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notificationLogs: StateFlow<List<NotificationLogEntity>> = repository.notificationLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Date and Period Selection State
    private val initialInterval = BillingCycleCalculator.todayInterval()
    private val _selectedDateRange = MutableStateFlow(
        DateRangeSelection(
            preset = TimeframeSelection.TODAY,
            startMillis = initialInterval.startMillis,
            endMillis = initialInterval.endMillis,
            titleAr = initialInterval.titleAr,
            titleEn = initialInterval.titleEn
        )
    )
    val selectedDateRange: StateFlow<DateRangeSelection> = _selectedDateRange.asStateFlow()

    private val _selectedPeriodUsage = MutableStateFlow(TimeframeUsage())
    val selectedPeriodUsage: StateFlow<TimeframeUsage> = _selectedPeriodUsage.asStateFlow()

    private val _selectedPeriodDailyTrend = MutableStateFlow<List<DailyUsagePoint>>(emptyList())
    val selectedPeriodDailyTrend: StateFlow<List<DailyUsagePoint>> = _selectedPeriodDailyTrend.asStateFlow()

    // App Usage state
    private val _rawAppUsageList = MutableStateFlow<List<AppNetworkUsage>>(emptyList())
    private val _isAppUsageLoading = MutableStateFlow(false)
    val isAppUsageLoading: StateFlow<Boolean> = _isAppUsageLoading.asStateFlow()

    private val _trafficFilter = MutableStateFlow(TrafficFilter.ALL)
    val trafficFilter: StateFlow<TrafficFilter> = _trafficFilter.asStateFlow()

    private val _appSortOrder = MutableStateFlow(AppSortOrder.HIGHEST_USAGE)
    val appSortOrder: StateFlow<AppSortOrder> = _appSortOrder.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedAppForDetail = MutableStateFlow<AppNetworkUsage?>(null)
    val selectedAppForDetail: StateFlow<AppNetworkUsage?> = _selectedAppForDetail.asStateFlow()

    val filteredAppUsage: StateFlow<List<AppNetworkUsage>> = combine(
        _rawAppUsageList,
        _trafficFilter,
        _appSortOrder,
        _searchQuery
    ) { apps, filter, sort, query ->
        var list = apps

        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            list = list.filter {
                it.appName.lowercase().contains(q) || it.packageName.lowercase().contains(q)
            }
        }

        list = when (filter) {
            TrafficFilter.ALL -> list
            TrafficFilter.MOBILE_ONLY -> list.filter { it.mobileBytes > 0L }
            TrafficFilter.WIFI_ONLY -> list.filter { it.wifiBytes > 0L }
        }

        when (sort) {
            AppSortOrder.HIGHEST_USAGE -> list.sortedByDescending { it.totalBytes }
            AppSortOrder.LOWEST_USAGE -> list.sortedBy { it.totalBytes }
            AppSortOrder.MOBILE_USAGE -> list.sortedByDescending { it.mobileBytes }
            AppSortOrder.WIFI_USAGE -> list.sortedByDescending { it.wifiBytes }
            AppSortOrder.ALPHABETICAL -> list.sortedBy { it.appName.lowercase() }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Analytics state
    private val _analyticsReport = MutableStateFlow<AnalyticsReport?>(null)
    val analyticsReport: StateFlow<AnalyticsReport?> = _analyticsReport.asStateFlow()

    private val _hasUsagePermission = MutableStateFlow(false)
    val hasUsagePermission: StateFlow<Boolean> = _hasUsagePermission.asStateFlow()

    init {
        checkPermissions()
        refreshAll()
        scheduleBackgroundWorker()
    }

    fun navigateTo(screen: AppScreen) {
        if (_currentScreen.value != screen) {
            screenBackStack.add(_currentScreen.value)
            _currentScreen.value = screen
            if (screen == AppScreen.APP_USAGE || screen == AppScreen.CHARTS_REPORTS) {
                loadAppUsageForCurrentSelection()
            }
        }
    }

    fun selectAppForDetail(app: AppNetworkUsage) {
        _selectedAppForDetail.value = app
        navigateTo(AppScreen.APP_DETAIL)
    }

    fun navigateBack(): Boolean {
        if (screenBackStack.isNotEmpty()) {
            _currentScreen.value = screenBackStack.removeAt(screenBackStack.size - 1)
            return true
        }
        return false
    }

    fun checkPermissions() {
        val granted = NetworkStatsHelper.hasUsageStatsPermission(getApplication())
        _hasUsagePermission.value = granted
    }

    fun refreshAll() {
        checkPermissions()
        repository.refreshUsage()
        loadDataForSelectedRange()
        computeAnalytics()
    }

    fun selectDatePreset(preset: TimeframeSelection, specificTimestamp: Long? = null, customStart: Long? = null, customEnd: Long? = null) {
        val now = System.currentTimeMillis()
        val interval: DateInterval = when (preset) {
            TimeframeSelection.TODAY -> BillingCycleCalculator.todayInterval(now)
            TimeframeSelection.YESTERDAY -> BillingCycleCalculator.yesterdayInterval(now)
            TimeframeSelection.THIS_WEEK -> BillingCycleCalculator.thisWeekInterval(now)
            TimeframeSelection.LAST_WEEK -> BillingCycleCalculator.lastWeekInterval(now)
            TimeframeSelection.THIS_MONTH -> BillingCycleCalculator.thisMonthInterval(now)
            TimeframeSelection.LAST_MONTH -> BillingCycleCalculator.lastMonthInterval(now)
            TimeframeSelection.BILLING_CYCLE -> {
                val bounds = BillingCycleCalculator.calculate(userPreferences.value.cycleStartDay, now)
                DateInterval(
                    startMillis = bounds.startMillis,
                    endMillis = bounds.endMillis,
                    titleAr = "دورة الفاتورة",
                    titleEn = "Billing Cycle"
                )
            }
            TimeframeSelection.SPECIFIC_DAY -> {
                val ts = specificTimestamp ?: now
                BillingCycleCalculator.specificDayInterval(ts)
            }
            TimeframeSelection.CUSTOM_RANGE -> {
                val s = customStart ?: (now - 7 * 24 * 3600 * 1000L)
                val e = customEnd ?: now
                BillingCycleCalculator.customRangeInterval(s, e)
            }
        }

        _selectedDateRange.value = DateRangeSelection(
            preset = preset,
            startMillis = interval.startMillis,
            endMillis = interval.endMillis,
            titleAr = interval.titleAr,
            titleEn = interval.titleEn
        )

        loadDataForSelectedRange()
    }

    private fun loadDataForSelectedRange() {
        viewModelScope.launch {
            val range = _selectedDateRange.value
            val usage = repository.queryUsageForInterval(range.startMillis, range.endMillis)
            _selectedPeriodUsage.value = usage
            loadAppUsageForCurrentSelection()

            // Build daily trend for the selected range
            val snapshots = repository.getSnapshotsBetween(range.startMillis, range.endMillis)
            val points = snapshots.map {
                DailyUsagePoint(
                    dateKey = it.dateKey,
                    timestamp = it.timestamp,
                    mobileBytes = it.mobileRxBytes + it.mobileTxBytes,
                    wifiBytes = it.wifiRxBytes + it.wifiTxBytes
                )
            }
            _selectedPeriodDailyTrend.value = points
        }
    }

    fun setTrafficFilter(filter: TrafficFilter) {
        _trafficFilter.value = filter
    }

    fun setSortOrder(order: AppSortOrder) {
        _appSortOrder.value = order
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun loadAppUsageForCurrentSelection() {
        viewModelScope.launch {
            _isAppUsageLoading.value = true
            val range = _selectedDateRange.value
            val apps = repository.fetchAppUsageForInterval(range.startMillis, range.endMillis)
            _rawAppUsageList.value = apps
            _isAppUsageLoading.value = false
        }
    }

    fun computeAnalytics() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val prefs = userPreferences.value
            val bounds = BillingCycleCalculator.calculate(prefs.cycleStartDay, now)
            val cycleBytes = billingCycleUsage.value.mobileTotalBytes
            val quotaLimitBytes = (prefs.monthlyLimitGb * 1024L * 1024L * 1024L).toLong()

            val snapshots = repository.getSnapshotsSince(bounds.startMillis)
            val dailyPoints = snapshots.map {
                DailyUsagePoint(
                    dateKey = it.dateKey,
                    timestamp = it.timestamp,
                    mobileBytes = it.mobileRxBytes + it.mobileTxBytes,
                    wifiBytes = it.wifiRxBytes + it.wifiTxBytes
                )
            }

            val report = AnalyticsEngine.computeAnalytics(
                usedBytesInCycle = cycleBytes,
                quotaLimitBytes = quotaLimitBytes,
                daysElapsed = bounds.daysElapsed,
                daysRemaining = bounds.daysRemaining,
                todayTotalBytes = todayUsage.value.grandTotalBytes,
                recentDays = dailyPoints
            )
            _analyticsReport.value = report
        }
    }

    fun savePlanSettings(
        monthlyLimitGb: Float,
        cycleStartDay: Int,
        dailyLimitMb: Float,
        mobileLimitEnabled: Boolean,
        wifiQuotaEnabled: Boolean,
        dailyLimitEnabled: Boolean,
        threshold50: Boolean,
        threshold75: Boolean,
        threshold90: Boolean,
        threshold100: Boolean
    ) {
        repository.preferencesManager.updatePlan(
            monthlyLimitGb = monthlyLimitGb,
            cycleStartDay = cycleStartDay,
            dailyLimitMb = dailyLimitMb,
            mobileLimitEnabled = mobileLimitEnabled,
            wifiQuotaEnabled = wifiQuotaEnabled,
            dailyLimitEnabled = dailyLimitEnabled,
            threshold50 = threshold50,
            threshold75 = threshold75,
            threshold90 = threshold90,
            threshold100 = threshold100
        )
        refreshAll()
    }

    fun setLanguage(language: String) {
        repository.preferencesManager.setLanguage(language)
    }

    fun setTheme(theme: String) {
        repository.preferencesManager.setTheme(theme)
    }

    fun completeOnboarding() {
        repository.preferencesManager.setOnboardingCompleted(true)
        _currentScreen.value = AppScreen.DASHBOARD
    }

    fun setBackgroundInterval(minutes: Int) {
        repository.preferencesManager.setBackgroundInterval(minutes)
        DataMonitorWorker.schedule(getApplication(), minutes)
    }

    private fun scheduleBackgroundWorker() {
        val interval = repository.preferencesManager.getPreferences().backgroundIntervalMinutes
        DataMonitorWorker.schedule(getApplication(), interval)
    }

    fun triggerTestNotification() {
        val app = getApplication<Application>()
        val title = app.getString(R.string.notif_75_title)
        val body = app.getString(
            R.string.notif_body_format,
            DataFormatUtils.formatBytes(11L * 1024L * 1024L * 1024L),
            DataFormatUtils.formatBytes(15L * 1024L * 1024L * 1024L)
        )
        NotificationHelper.showThresholdNotification(app, 9999, title, body)
        viewModelScope.launch {
            repository.logNotification(
                thresholdPercent = 75,
                networkType = "MOBILE",
                title = title,
                message = body,
                dateKey = DataFormatUtils.formatDate(System.currentTimeMillis(), "yyyy-MM")
            )
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
            refreshAll()
        }
    }

    fun generateReportText(): String {
        val app = getApplication<Application>()
        val range = selectedDateRange.value
        val periodUsage = selectedPeriodUsage.value
        val prefs = userPreferences.value
        val topApps = _rawAppUsageList.value.take(5)

        val sb = StringBuilder()
        sb.append("========================================\n")
        sb.append(app.getString(R.string.report_header)).append("\n")
        sb.append("Period: ").append(range.titleAr).append(" / ").append(range.titleEn).append("\n")
        sb.append("Date Generated: ").append(DataFormatUtils.formatDate(System.currentTimeMillis())).append("\n")
        sb.append("========================================\n\n")

        sb.append("[1] PERIOD CONSUMPTION\n")
        sb.append(" • Mobile (Rx/Tx): ").append(DataFormatUtils.formatBytes(periodUsage.mobileTotalBytes)).append("\n")
        sb.append(" • Wi-Fi (Rx/Tx): ").append(DataFormatUtils.formatBytes(periodUsage.wifiTotalBytes)).append("\n")
        sb.append(" • Total Period Traffic: ").append(DataFormatUtils.formatBytes(periodUsage.grandTotalBytes)).append("\n")
        sb.append(" • Download (Rx): ").append(DataFormatUtils.formatBytes(periodUsage.mobileRxBytes + periodUsage.wifiRxBytes)).append("\n")
        sb.append(" • Upload (Tx): ").append(DataFormatUtils.formatBytes(periodUsage.mobileTxBytes + periodUsage.wifiTxBytes)).append("\n\n")

        sb.append("[2] PLAN CONFIGURATION\n")
        if (prefs.mobileLimitEnabled) {
            val limitBytes = (prefs.monthlyLimitGb * 1024L * 1024L * 1024L).toLong()
            sb.append(" • Monthly Limit: ").append(DataFormatUtils.formatBytes(limitBytes)).append("\n")
            sb.append(" • Cycle Start Day: ").append(prefs.cycleStartDay).append(" of month\n\n")
        }

        sb.append("[3] TOP APPS IN THIS PERIOD\n")
        if (topApps.isNotEmpty()) {
            topApps.forEachIndexed { i, a ->
                sb.append(" ${i + 1}. ").append(a.appName)
                    .append(" (").append(DataFormatUtils.formatBytes(a.totalBytes)).append(")\n")
            }
        } else {
            sb.append(" • No per-app stats found (Usage access permission required)\n")
        }

        sb.append("\n100% On-Device Telemetry via NetworkStatsManager.\n")
        return sb.toString()
    }

    fun generateReportCsv(): String {
        val sb = StringBuilder()
        sb.append("App Name,Package,Mobile Bytes,WiFi Bytes,Total Bytes,Percentage\n")
        for (app in _rawAppUsageList.value) {
            sb.append("\"").append(app.appName.replace("\"", "\"\"")).append("\",")
            sb.append("\"").append(app.packageName).append("\",")
            sb.append(app.mobileBytes).append(",")
            sb.append(app.wifiBytes).append(",")
            sb.append(app.totalBytes).append(",")
            sb.append(String.format(java.util.Locale.US, "%.2f%%", app.percentage * 100f)).append("\n")
        }
        return sb.toString()
    }
}
