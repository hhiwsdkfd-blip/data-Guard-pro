package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TimeframeSelection
import com.example.ui.components.DateSelectionDialog
import com.example.ui.components.SectionHeader
import com.example.ui.theme.ColorMobile
import com.example.ui.theme.ColorWifi
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.MintSecondary
import com.example.ui.viewmodel.MainViewModel
import com.example.util.DataFormatUtils
import kotlin.math.max

@Composable
fun ChartsReportsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val prefs by viewModel.userPreferences.collectAsState()
    val isArabic = prefs.selectedLanguage == "ar"

    val dateRange by viewModel.selectedDateRange.collectAsState()
    val periodUsage by viewModel.selectedPeriodUsage.collectAsState()
    val topApps by viewModel.filteredAppUsage.collectAsState()
    val dailyTrendPoints by viewModel.selectedPeriodDailyTrend.collectAsState()
    val todayUsage by viewModel.todayUsage.collectAsState()
    val thisWeekUsage by viewModel.thisWeekUsage.collectAsState()

    var showDatePickerDialog by remember { mutableStateOf(false) }
    var selectedBarIndex by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .testTag("charts_reports_screen")
    ) {
        // App Bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("charts_reports_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = if (isArabic) "الرسوم البيانية والتقارير" else "Charts & Reports",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Date selection chip
        Surface(
            onClick = { showDatePickerDialog = true },
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .minimumInteractiveComponentSize()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isArabic) "فترة التقرير والرسوم البيانية" else "Report & Chart Period",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (isArabic) dateRange.titleAr else dateRange.titleEn,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
                Text(
                    text = if (isArabic) "تغيير" else "Change",
                    style = MaterialTheme.typography.labelLarge.copy(color = CyanPrimary, fontWeight = FontWeight.Bold)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 1. DAILY CONSUMPTION BAR CHART
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = if (isArabic) "مخطط استهلاك الإنترنت حسب اليوم" else "Daily Consumption Trend",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = if (isArabic) "اضغط على أي عمود لمعاينة استهلاك ذلك اليوم" else "Tap on any bar to inspect daily stats",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                val trendList = if (dailyTrendPoints.isNotEmpty()) dailyTrendPoints else {
                    // Fallback visual bar for today/current range
                    listOf(
                        com.example.domain.analytics.DailyUsagePoint(
                            dateKey = if (isArabic) dateRange.titleAr else dateRange.titleEn,
                            timestamp = dateRange.startMillis,
                            mobileBytes = periodUsage.mobileTotalBytes,
                            wifiBytes = periodUsage.wifiTotalBytes
                        )
                    )
                }

                val maxBytes = max(1L, trendList.maxOf { it.totalBytes })
                val trackBg = MaterialTheme.colorScheme.surfaceVariant
                val primaryColor = CyanPrimary
                val highlightColor = MintSecondary

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .pointerInput(trendList) {
                            detectTapGestures { offset ->
                                val count = trendList.size
                                if (count > 0) {
                                    val barSlotWidth = size.width / count
                                    val idx = (offset.x / barSlotWidth).toInt().coerceIn(0, count - 1)
                                    selectedBarIndex = idx
                                }
                            }
                        }
                ) {
                    val count = trendList.size
                    val barSpacing = 8.dp.toPx()
                    val totalSpacing = barSpacing * (count + 1)
                    val barWidth = ((size.width - totalSpacing) / count).coerceAtLeast(10.dp.toPx())

                    trendList.forEachIndexed { i, pt ->
                        val x = barSpacing + i * (barWidth + barSpacing)
                        val frac = (pt.totalBytes.toFloat() / maxBytes.toFloat()).coerceIn(0.06f, 1f)
                        val barHeight = size.height * frac
                        val y = size.height - barHeight
                        val isSelected = selectedBarIndex == i

                        drawRoundRect(
                            color = trackBg,
                            topLeft = Offset(x, 0f),
                            size = Size(barWidth, size.height),
                            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )

                        drawRoundRect(
                            color = if (isSelected) highlightColor else primaryColor,
                            topLeft = Offset(x, y),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )
                    }
                }

                selectedBarIndex?.let { idx ->
                    if (idx in trendList.indices) {
                        val pt = trendList[idx]
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.padding(10.dp)
                            ) {
                                Text(pt.dateKey, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                Text("Total: ${DataFormatUtils.formatBytes(pt.totalBytes)}", color = CyanPrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. MOBILE VS WI-FI COMPARISON
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = if (isArabic) "مقارنة بيانات الجوال مع Wi-Fi" else "Mobile vs Wi-Fi Comparison",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))

                val total = periodUsage.grandTotalBytes
                val mobRatio = if (total > 0L) (periodUsage.mobileTotalBytes.toFloat() / total.toFloat()) else 0.5f
                val wifiRatio = 1.0f - mobRatio

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .clip(RoundedCornerShape(7.dp))
                ) {
                    Box(modifier = Modifier.weight(mobRatio.coerceAtLeast(0.02f)).fillMaxSize().background(ColorMobile))
                    Box(modifier = Modifier.weight(wifiRatio.coerceAtLeast(0.02f)).fillMaxSize().background(ColorWifi))
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(ColorMobile))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${if (isArabic) "بيانات الجوال" else "Mobile"}: ${DataFormatUtils.formatBytes(periodUsage.mobileTotalBytes)} (${(mobRatio * 100).toInt()}%)",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(ColorWifi))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Wi-Fi: ${DataFormatUtils.formatBytes(periodUsage.wifiTotalBytes)} (${(wifiRatio * 100).toInt()}%)",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. TOP CONSUMING APPS CHART
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = if (isArabic) "أكثر التطبيقات استهلاكاً" else "Top Consuming Applications",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (topApps.isEmpty()) {
                    Text(
                        text = if (isArabic) "لا تتوفر إحصائيات تطبيقات لهذه الفترة." else "No app statistics available for this timeframe.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    topApps.take(5).forEach { app ->
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text(app.appName, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                                Text(DataFormatUtils.formatBytes(app.totalBytes), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            LinearProgressIndicator(
                                progress = { app.percentage },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = CyanPrimary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 4. PERIOD COMPARISON: Today vs Yesterday
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.CompareArrows, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isArabic) "مقارنة الاستهلاك بين الفترات" else "Period Comparison",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(if (isArabic) "اليوم" else "Today", style = MaterialTheme.typography.labelSmall)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(DataFormatUtils.formatBytes(todayUsage.grandTotalBytes), fontWeight = FontWeight.Bold)
                        }
                    }

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(if (isArabic) "هذا الأسبوع" else "This Week", style = MaterialTheme.typography.labelSmall)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(DataFormatUtils.formatBytes(thisWeekUsage.grandTotalBytes), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 5. EXPORT ACTIONS: CSV & TEXT REPORT
        SectionHeader(title = if (isArabic) "تصدير التقارير" else "Report Export")

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = {
                    val reportText = viewModel.generateReportText()
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "Data Monitor Report")
                        putExtra(Intent.EXTRA_TEXT, reportText)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share Report via"))
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyanPrimary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .minimumInteractiveComponentSize()
                    .testTag("share_report_button")
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isArabic) "مشاركة التقرير" else "Share Report")
            }

            OutlinedButton(
                onClick = {
                    val csvData = viewModel.generateReportCsv()
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/csv"
                        putExtra(Intent.EXTRA_SUBJECT, "data_monitor_export.csv")
                        putExtra(Intent.EXTRA_TEXT, csvData)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Export CSV via"))
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .minimumInteractiveComponentSize()
                    .testTag("export_csv_button")
            ) {
                Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isArabic) "تصدير CSV" else "Export CSV")
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
    }

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
