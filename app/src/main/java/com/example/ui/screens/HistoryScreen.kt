package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.UsageSnapshotEntity
import com.example.ui.components.EmptyStateView
import com.example.ui.components.SectionHeader
import com.example.ui.theme.ColorMobile
import com.example.ui.theme.ColorWifi
import com.example.ui.theme.CyanPrimary
import com.example.ui.viewmodel.MainViewModel
import com.example.util.DataFormatUtils
import kotlin.math.max

@Composable
fun HistoryScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val snapshots by viewModel.historySnapshots.collectAsState()
    var selectedDaysCount by remember { mutableStateOf(7) }
    var selectedBarIndex by remember { mutableStateOf<Int?>(null) }

    val displaySnapshots = snapshots.take(selectedDaysCount)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .testTag("history_screen")
    ) {
        // App Bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("history_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = stringResource(R.string.history_title),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Timeframe filter chips
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            FilterChip(
                selected = selectedDaysCount == 7,
                onClick = {
                    selectedDaysCount = 7
                    selectedBarIndex = null
                },
                label = { Text(stringResource(R.string.timeframe_7d)) }
            )
            FilterChip(
                selected = selectedDaysCount == 14,
                onClick = {
                    selectedDaysCount = 14
                    selectedBarIndex = null
                },
                label = { Text(stringResource(R.string.timeframe_14d)) }
            )
            FilterChip(
                selected = selectedDaysCount == 30,
                onClick = {
                    selectedDaysCount = 30
                    selectedBarIndex = null
                },
                label = { Text(stringResource(R.string.timeframe_30d)) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (displaySnapshots.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.History,
                title = "No Snapshots Recorded Yet",
                message = "Usage snapshots are recorded automatically as your device consumes network data."
            )
        } else {
            // Interactive Bar Chart Card
            val maxBytes = max(1L, displaySnapshots.maxOf { it.totalBytes })
            val reversedForChart = displaySnapshots.reversed()

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Daily Consumption Trend",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Tap on any bar to inspect daily statistics",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    val trackBg = MaterialTheme.colorScheme.surfaceVariant
                    val primaryColor = CyanPrimary
                    val selectedHighlight = MaterialTheme.colorScheme.secondary

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .pointerInput(reversedForChart) {
                                detectTapGestures { offset ->
                                    val count = reversedForChart.size
                                    if (count > 0) {
                                        val barSlotWidth = size.width / count
                                        val idx = (offset.x / barSlotWidth).toInt().coerceIn(0, count - 1)
                                        selectedBarIndex = idx
                                    }
                                }
                            }
                    ) {
                        val count = reversedForChart.size
                        val barSpacing = 8.dp.toPx()
                        val totalSpacing = barSpacing * (count + 1)
                        val barWidth = ((size.width - totalSpacing) / count).coerceAtLeast(6.dp.toPx())

                        reversedForChart.forEachIndexed { i, snap ->
                            val x = barSpacing + i * (barWidth + barSpacing)
                            val barHeightFraction = (snap.totalBytes.toFloat() / maxBytes.toFloat()).coerceIn(0.04f, 1f)
                            val barHeight = size.height * barHeightFraction
                            val y = size.height - barHeight

                            val isSelected = selectedBarIndex == i

                            // Draw Track
                            drawRoundRect(
                                color = trackBg,
                                topLeft = Offset(x, 0f),
                                size = Size(barWidth, size.height),
                                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                            )

                            // Draw Value Bar
                            drawRoundRect(
                                color = if (isSelected) selectedHighlight else primaryColor,
                                topLeft = Offset(x, y),
                                size = Size(barWidth, barHeight),
                                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Selected Bar Info
                    selectedBarIndex?.let { idx ->
                        if (idx in reversedForChart.indices) {
                            val selected = reversedForChart[idx]
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = selected.dateKey,
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Total: ${DataFormatUtils.formatBytes(selected.totalBytes)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = CyanPrimary
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "Mob: ${DataFormatUtils.formatBytes(selected.mobileRxBytes + selected.mobileTxBytes)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ColorMobile
                                        )
                                        Text(
                                            text = "WiFi: ${DataFormatUtils.formatBytes(selected.wifiRxBytes + selected.wifiTxBytes)}",
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

            Spacer(modifier = Modifier.height(16.dp))

            SectionHeader(title = "Recorded Logs")

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(displaySnapshots, key = { it.id }) { snap ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = CardDefaults.outlinedCardBorder(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = snap.dateKey,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "Mobile: ${DataFormatUtils.formatBytes(snap.mobileRxBytes + snap.mobileTxBytes)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ColorMobile
                                    )
                                    Text(
                                        text = "•",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                    Text(
                                        text = "Wi-Fi: ${DataFormatUtils.formatBytes(snap.wifiRxBytes + snap.wifiTxBytes)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ColorWifi
                                    )
                                }
                            }

                            Text(
                                text = DataFormatUtils.formatBytes(snap.totalBytes),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}
