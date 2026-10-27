package com.example.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.AppNetworkUsage
import com.example.data.model.AppSortOrder
import com.example.data.model.TimeframeSelection
import com.example.data.model.TrafficFilter
import com.example.ui.components.DateSelectionDialog
import com.example.ui.components.EmptyStateView
import com.example.ui.theme.ColorMobile
import com.example.ui.theme.ColorWifi
import com.example.ui.theme.CyanPrimary
import com.example.ui.viewmodel.MainViewModel
import com.example.util.DataFormatUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppUsageScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val prefs by viewModel.userPreferences.collectAsState()
    val isArabic = prefs.selectedLanguage == "ar"

    val appList by viewModel.filteredAppUsage.collectAsState()
    val isLoading by viewModel.isAppUsageLoading.collectAsState()
    val dateRange by viewModel.selectedDateRange.collectAsState()
    val trafficFilter by viewModel.trafficFilter.collectAsState()
    val sortOrder by viewModel.appSortOrder.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val hasPermission by viewModel.hasUsagePermission.collectAsState()

    var showDatePickerDialog by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("app_usage_screen")
    ) {
        // App Bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("app_usage_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = if (isArabic) "إحصائيات استهلاك التطبيقات" else "App Data Usage",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )

            // Date picker quick launcher
            IconButton(
                onClick = { showDatePickerDialog = true },
                modifier = Modifier.testTag("app_usage_calendar_button")
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = "Date",
                    tint = CyanPrimary
                )
            }

            // Sort Toggle Button with Dropdown
            Box {
                IconButton(
                    onClick = { showSortMenu = true },
                    modifier = Modifier.testTag("app_usage_sort_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Sort,
                        contentDescription = "Sort",
                        tint = CyanPrimary
                    )
                }

                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(if (isArabic) "الأعلى استهلاكاً" else "Highest Usage") },
                        onClick = {
                            viewModel.setSortOrder(AppSortOrder.HIGHEST_USAGE)
                            showSortMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(if (isArabic) "الأقل استهلاكاً" else "Lowest Usage") },
                        onClick = {
                            viewModel.setSortOrder(AppSortOrder.LOWEST_USAGE)
                            showSortMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(if (isArabic) "استهلاك بيانات الجوال" else "Mobile Data Usage") },
                        onClick = {
                            viewModel.setSortOrder(AppSortOrder.MOBILE_USAGE)
                            showSortMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(if (isArabic) "استهلاك Wi-Fi" else "Wi-Fi Data Usage") },
                        onClick = {
                            viewModel.setSortOrder(AppSortOrder.WIFI_USAGE)
                            showSortMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(if (isArabic) "الترتيب الأبجدي" else "Alphabetical") },
                        onClick = {
                            viewModel.setSortOrder(AppSortOrder.ALPHABETICAL)
                            showSortMenu = false
                        }
                    )
                }
            }
        }

        // Active Date Indicator Chip
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp)
        ) {
            Surface(
                onClick = { showDatePickerDialog = true },
                shape = RoundedCornerShape(8.dp),
                color = CyanPrimary.copy(alpha = 0.15f),
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${if (isArabic) "الفترة المعروضة:" else "Active Period:"} ${if (isArabic) dateRange.titleAr else dateRange.titleEn}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = CyanPrimary
                    )
                }
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            placeholder = { Text(if (isArabic) "بحث عن تطبيق بالاسم أو الحزمة..." else "Search apps by name...", fontSize = 14.sp) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .testTag("search_apps_input")
        )

        // Traffic Filter Chips (All, Mobile, WiFi)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            FilterChip(
                selected = trafficFilter == TrafficFilter.ALL,
                onClick = { viewModel.setTrafficFilter(TrafficFilter.ALL) },
                label = { Text(if (isArabic) "الكل" else "All") }
            )
            FilterChip(
                selected = trafficFilter == TrafficFilter.MOBILE_ONLY,
                onClick = { viewModel.setTrafficFilter(TrafficFilter.MOBILE_ONLY) },
                label = { Text(if (isArabic) "بيانات الجوال" else "Mobile") }
            )
            FilterChip(
                selected = trafficFilter == TrafficFilter.WIFI_ONLY,
                onClick = { viewModel.setTrafficFilter(TrafficFilter.WIFI_ONLY) },
                label = { Text(if (isArabic) "واي فاي" else "Wi-Fi") }
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Content
        if (!hasPermission) {
            EmptyStateView(
                icon = Icons.Default.Info,
                title = if (isArabic) "إذن الاستخدام مطلوب" else "Usage Access Required",
                message = if (isArabic) "يتطلب أندرويد إذن الوصول لبيانات الاستخدام لحساب استهلاك كل تطبيق." else "Android requires Usage Access to query per-application telemetry.",
                actionLabel = if (isArabic) "منح الإذن من الإعدادات" else "Grant in Settings",
                onAction = {
                    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                }
            )
        } else if (isLoading) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp)
            ) {
                CircularProgressIndicator(color = CyanPrimary)
            }
        } else if (appList.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.Info,
                title = if (isArabic) "لا توجد تطبيقات مستهلكة" else "No applications recorded",
                message = if (isArabic) "لم يتم رصد استهلاك لأي تطبيق في هذا التاريخ المحدد." else "No network traffic recorded for this specific date range."
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                items(appList, key = { "${it.packageName}_${it.uid}" }) { app ->
                    Card(
                        onClick = { viewModel.selectAppForDetail(app) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = CardDefaults.outlinedCardBorder(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .testTag("app_item_${app.packageName}")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(12.dp))
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
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = app.appName,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        if (app.isSystemApp) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant
                                            ) {
                                                Text(
                                                    text = "SYS",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    Text(
                                        text = app.packageName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
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

                            Spacer(modifier = Modifier.height(10.dp))

                            LinearProgressIndicator(
                                progress = { app.percentage },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = CyanPrimary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "${if (isArabic) "جوال" else "Mobile"}: ${DataFormatUtils.formatBytes(app.mobileBytes)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ColorMobile
                                )
                                Text(
                                    text = "Wi-Fi: ${DataFormatUtils.formatBytes(app.wifiBytes)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ColorWifi
                                )
                            }
                        }
                    }
                }
            }
        }
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
