package com.example.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.NetworkMedium
import com.example.data.model.TimeframeSelection
import com.example.domain.billing.BillingCycleCalculator
import com.example.ui.components.DateSelectionDialog
import com.example.ui.components.EmptyStateView
import com.example.ui.components.LiveSpeedBadge
import com.example.ui.components.QuotaProgressGauge
import com.example.ui.components.SectionHeader
import com.example.ui.theme.AmberAlert
import com.example.ui.theme.ColorDownload
import com.example.ui.theme.ColorMobile
import com.example.ui.theme.ColorUpload
import com.example.ui.theme.ColorWifi
import com.example.ui.theme.CoralWarning
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.MintSecondary
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.MainViewModel
import com.example.util.DataFormatUtils
import kotlin.math.max

@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onNavigate: (AppScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val connectionState by viewModel.liveConnectionState.collectAsState()
    val preferences by viewModel.userPreferences.collectAsState()
    val isArabic = preferences.selectedLanguage == "ar"

    val dateRange by viewModel.selectedDateRange.collectAsState()
    val periodUsage by viewModel.selectedPeriodUsage.collectAsState()
    val todayUsage by viewModel.todayUsage.collectAsState()
    val thisWeekUsage by viewModel.thisWeekUsage.collectAsState()
    val thisMonthUsage by viewModel.thisMonthUsage.collectAsState()
    val billingCycleUsage by viewModel.billingCycleUsage.collectAsState()
    val topApps by viewModel.filteredAppUsage.collectAsState()
    val hasUsagePermission by viewModel.hasUsagePermission.collectAsState()
    val dailyTrendPoints by viewModel.selectedPeriodDailyTrend.collectAsState()

    var showDatePickerDialog by remember { mutableStateOf(false) }

    val cycleBounds = BillingCycleCalculator.calculate(
        preferences.cycleStartDay,
        System.currentTimeMillis()
    )

    val mobileLimitBytes = if (preferences.mobileLimitEnabled) {
        (preferences.monthlyLimitGb * 1024L * 1024L * 1024L).toLong()
    } else {
        0L
    }

    val usedCycleMobile = billingCycleUsage.mobileTotalBytes
    val ratio = if (mobileLimitBytes > 0L) usedCycleMobile.toDouble() / mobileLimitBytes.toDouble() else 0.0

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp)
            .testTag("dashboard_screen")
    ) {
        // Top App Bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text(
                    text = if (isArabic) "Data Guard Pro — حارس البيانات" else "Data Guard Pro",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 24.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = if (isArabic) "إحصائيات دقيقة لبيانات الجوال وشبكات Wi-Fi" else "Real-time Mobile & Wi-Fi Telemetry",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { viewModel.refreshAll() },
                    modifier = Modifier.testTag("dashboard_refresh_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = CyanPrimary
                    )
                }
                IconButton(
                    onClick = { onNavigate(AppScreen.ABOUT) },
                    modifier = Modifier.testTag("dashboard_about_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "About",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = { onNavigate(AppScreen.SETTINGS) },
                    modifier = Modifier.testTag("dashboard_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // DATE SELECTION HERO BANNER
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            ),
            border = CardDefaults.outlinedCardBorder(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CyanPrimary.copy(alpha = 0.15f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = CyanPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = if (isArabic) "الفترة المعروضة" else "Selected Timeframe",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (isArabic) dateRange.titleAr else dateRange.titleEn,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${DataFormatUtils.formatDate(dateRange.startMillis)} - ${DataFormatUtils.formatDate(dateRange.endMillis)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                Button(
                    onClick = { showDatePickerDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanPrimary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("pick_date_button")
                ) {
                    Text(
                        text = if (isArabic) "تغيير التاريخ" else "Pick Date",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Live Connection & Speed Meter Banner
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    when (connectionState.medium) {
                                        NetworkMedium.WIFI -> ColorWifi.copy(alpha = 0.2f)
                                        NetworkMedium.CELLULAR -> ColorMobile.copy(alpha = 0.2f)
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    }
                                )
                        ) {
                            Icon(
                                imageVector = when (connectionState.medium) {
                                    NetworkMedium.WIFI -> Icons.Default.Wifi
                                    NetworkMedium.CELLULAR -> Icons.Default.CellTower
                                    else -> Icons.Default.Speed
                                },
                                contentDescription = null,
                                tint = when (connectionState.medium) {
                                    NetworkMedium.WIFI -> ColorWifi
                                    NetworkMedium.CELLULAR -> ColorMobile
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = connectionState.networkName,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (connectionState.isMetered) {
                                    if (isArabic) "اتصال مقيد الاستهلاك" else "Metered Connection"
                                } else {
                                    if (isArabic) "اتصال غير مقيد" else "Unmetered"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (connectionState.isMetered) AmberAlert else MintSecondary
                            )
                        }
                    }

                    LiveSpeedBadge(
                        rxSpeed = connectionState.rxBytesPerSec,
                        txSpeed = connectionState.txBytesPerSec
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SELECTED PERIOD CONSUMPTION HERO CARD
        if (!periodUsage.isDataAvailable) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = AmberAlert,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isArabic) "لا توجد سجلات مسجلة لهذا التاريخ" else "No Data Recorded for this Date",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isArabic)
                            "نظام أندرويد لا يحتفظ ببيانات هذا اليوم السابق، أو لم يكن التطبيق يعمل حينها."
                        else
                            "The operating system did not retain telemetry for this day, or the app was not yet recording.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = if (isArabic) "إجمالي استهلاك الفترة (${dateRange.titleAr})" else "Total Usage for Period (${dateRange.titleEn})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = DataFormatUtils.formatBytes(periodUsage.grandTotalBytes),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 34.sp,
                            color = CyanPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Mobile Data Card
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CellTower, contentDescription = null, tint = ColorMobile, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isArabic) "بيانات الجوال" else "Mobile Data",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = DataFormatUtils.formatBytes(periodUsage.mobileTotalBytes),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = ColorMobile
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "↓ ${DataFormatUtils.formatBytes(periodUsage.mobileRxBytes)} • ↑ ${DataFormatUtils.formatBytes(periodUsage.mobileTxBytes)}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }

                        // Wi-Fi Data Card
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Wifi, contentDescription = null, tint = ColorWifi, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isArabic) "شبكة Wi-Fi" else "Wi-Fi Data",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = DataFormatUtils.formatBytes(periodUsage.wifiTotalBytes),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = ColorWifi
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "↓ ${DataFormatUtils.formatBytes(periodUsage.wifiRxBytes)} • ↑ ${DataFormatUtils.formatBytes(periodUsage.wifiTxBytes)}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // QUICK STATS OVERVIEW: Today, 7 Days, Month
        SectionHeader(title = if (isArabic) "نظرة سريعة على الفترات" else "Quick Overview")

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(if (isArabic) "اليوم" else "Today", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(DataFormatUtils.formatBytes(todayUsage.grandTotalBytes), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(if (isArabic) "آخر 7 أيام" else "Last 7 Days", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(DataFormatUtils.formatBytes(thisWeekUsage.grandTotalBytes), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(if (isArabic) "الشهر الحالي" else "This Month", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(DataFormatUtils.formatBytes(thisMonthUsage.grandTotalBytes), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Plan Quota progress if enabled
        if (preferences.mobileLimitEnabled) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(18.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isArabic) "سعة الباقة الشهرية" else "Monthly Data Quota",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = when {
                                ratio >= 1.0 -> CoralWarning.copy(alpha = 0.2f)
                                ratio >= 0.85 -> AmberAlert.copy(alpha = 0.2f)
                                else -> MintSecondary.copy(alpha = 0.2f)
                            }
                        ) {
                            Text(
                                text = "${cycleBounds.daysRemaining} ${if (isArabic) "أيام متبقية" else "days left"}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = when {
                                    ratio >= 1.0 -> CoralWarning
                                    ratio >= 0.85 -> AmberAlert
                                    else -> MintSecondary
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    QuotaProgressGauge(
                        usedBytes = usedCycleMobile,
                        limitBytes = mobileLimitBytes,
                        size = 170.dp
                    )
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
        }

        // TOP APPS IN SELECTED PERIOD
        SectionHeader(
            title = if (isArabic) "أعلى التطبيقات استهلاكاً في هذه الفترة" else "Top Consuming Apps in Period",
            actionLabel = if (isArabic) "عرض الكل" else "View All",
            onActionClick = { onNavigate(AppScreen.APP_USAGE) }
        )

        if (!hasUsagePermission) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = AmberAlert.copy(alpha = 0.12f)),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = AmberAlert, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isArabic) "إذن الاستخدام مطلوب لعرض التطبيقات" else "Usage Access Required",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = if (isArabic) "اضغط لمنح الإذن من إعدادات النظام" else "Tap to grant permission in Settings",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        }
                    ) {
                        Text(if (isArabic) "منح" else "Grant", fontSize = 12.sp)
                    }
                }
            }
        } else if (topApps.isEmpty()) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(20.dp).fillMaxWidth()) {
                    Text(
                        text = if (isArabic) "لم يتم رصد استهلاك للتطبيقات في هذه الفترة" else "No app consumption detected in this period",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            topApps.take(4).forEach { app ->
                Card(
                    onClick = { viewModel.selectAppForDetail(app) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(CyanPrimary.copy(alpha = 0.15f))
                        ) {
                            Text(
                                text = app.appName.take(1).uppercase(),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = CyanPrimary
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = app.appName,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${if (isArabic) "جوال" else "Mobile"}: ${DataFormatUtils.formatBytes(app.mobileBytes)} • Wi-Fi: ${DataFormatUtils.formatBytes(app.wifiBytes)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = DataFormatUtils.formatBytes(app.totalBytes),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = String.format(java.util.Locale.US, "%.1f%%", app.percentage * 100f),
                                style = MaterialTheme.typography.labelSmall,
                                color = CyanPrimary
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
    }

    // Date & Period Selection Dialog
    if (showDatePickerDialog) {
        DateSelectionDialog(
            currentSelection = dateRange,
            isArabic = isArabic,
            onDismiss = { showDatePickerDialog = false },
            onSelectPreset = { preset ->
                viewModel.selectDatePreset(preset)
            },
            onSelectSpecificDay = { dayMillis ->
                viewModel.selectDatePreset(TimeframeSelection.SPECIFIC_DAY, specificTimestamp = dayMillis)
            }
        )
    }
}
