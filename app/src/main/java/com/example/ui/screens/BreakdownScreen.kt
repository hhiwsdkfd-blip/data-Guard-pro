package com.example.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.TimeframeSelection
import com.example.ui.components.SectionHeader
import com.example.ui.theme.AmberAlert
import com.example.ui.theme.ColorDownload
import com.example.ui.theme.ColorMobile
import com.example.ui.theme.ColorUpload
import com.example.ui.theme.ColorWifi
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.MintSecondary
import com.example.ui.viewmodel.MainViewModel
import com.example.util.DataFormatUtils

@Composable
fun BreakdownScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val todayUsage by viewModel.todayUsage.collectAsState()
    val thisWeekUsage by viewModel.thisWeekUsage.collectAsState()
    val thisMonthUsage by viewModel.thisMonthUsage.collectAsState()
    val billingCycleUsage by viewModel.billingCycleUsage.collectAsState()
    val connectionState by viewModel.liveConnectionState.collectAsState()

    var selectedTf by remember { mutableStateOf(TimeframeSelection.TODAY) }

    val currentUsage = when (selectedTf) {
        TimeframeSelection.TODAY -> todayUsage
        TimeframeSelection.YESTERDAY -> todayUsage
        TimeframeSelection.THIS_WEEK -> thisWeekUsage
        TimeframeSelection.LAST_WEEK -> thisWeekUsage
        TimeframeSelection.THIS_MONTH -> thisMonthUsage
        TimeframeSelection.LAST_MONTH -> thisMonthUsage
        TimeframeSelection.BILLING_CYCLE -> billingCycleUsage
        TimeframeSelection.SPECIFIC_DAY, TimeframeSelection.CUSTOM_RANGE -> todayUsage
    }

    val grandTotal = currentUsage.grandTotalBytes
    val mobileRatio = if (grandTotal > 0L) (currentUsage.mobileTotalBytes.toFloat() / grandTotal.toFloat()) else 0.5f
    val wifiRatio = 1.0f - mobileRatio

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .testTag("breakdown_screen")
    ) {
        // App Bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("breakdown_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = stringResource(R.string.breakdown_title),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Timeframe selector row
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            FilterChip(
                selected = selectedTf == TimeframeSelection.TODAY,
                onClick = { selectedTf = TimeframeSelection.TODAY },
                label = { Text(stringResource(R.string.today)) }
            )
            FilterChip(
                selected = selectedTf == TimeframeSelection.THIS_WEEK,
                onClick = { selectedTf = TimeframeSelection.THIS_WEEK },
                label = { Text(stringResource(R.string.this_week)) }
            )
            FilterChip(
                selected = selectedTf == TimeframeSelection.THIS_MONTH,
                onClick = { selectedTf = TimeframeSelection.THIS_MONTH },
                label = { Text(stringResource(R.string.this_month)) }
            )
            FilterChip(
                selected = selectedTf == TimeframeSelection.BILLING_CYCLE,
                onClick = { selectedTf = TimeframeSelection.BILLING_CYCLE },
                label = { Text(stringResource(R.string.billing_cycle)) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Mobile vs Wi-Fi Split Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = stringResource(R.string.traffic_split),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Total Consumed: ${DataFormatUtils.formatBytes(grandTotal)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Split Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .clip(RoundedCornerShape(7.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .weight(mobileRatio.coerceAtLeast(0.01f))
                            .fillMaxSize()
                            .background(ColorMobile)
                    )
                    Box(
                        modifier = Modifier
                            .weight(wifiRatio.coerceAtLeast(0.01f))
                            .fillMaxSize()
                            .background(ColorWifi)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(ColorMobile)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${stringResource(R.string.mobile_data)} (${(mobileRatio * 100).toInt()}%)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(ColorWifi)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${stringResource(R.string.wifi_data)} (${(wifiRatio * 100).toInt()}%)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Mobile Details Card
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CellTower, contentDescription = null, tint = ColorMobile, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.mobile_data),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = DataFormatUtils.formatBytes(currentUsage.mobileTotalBytes),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = ColorMobile
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(stringResource(R.string.download), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(DataFormatUtils.formatBytes(currentUsage.mobileRxBytes), fontWeight = FontWeight.Bold, color = ColorDownload)
                    }
                    Column {
                        Text(stringResource(R.string.upload), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(DataFormatUtils.formatBytes(currentUsage.mobileTxBytes), fontWeight = FontWeight.Bold, color = ColorUpload)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Wi-Fi Details Card
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Wifi, contentDescription = null, tint = ColorWifi, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.wifi_data),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = DataFormatUtils.formatBytes(currentUsage.wifiTotalBytes),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = ColorWifi
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(stringResource(R.string.download), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(DataFormatUtils.formatBytes(currentUsage.wifiRxBytes), fontWeight = FontWeight.Bold, color = ColorDownload)
                    }
                    Column {
                        Text(stringResource(R.string.upload), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(DataFormatUtils.formatBytes(currentUsage.wifiTxBytes), fontWeight = FontWeight.Bold, color = ColorUpload)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Network Capabilities & Telemetry
        SectionHeader(title = stringResource(R.string.network_caps))

        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.carrier_info), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(connectionState.networkName, fontWeight = FontWeight.Bold)
                }
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.link_down_speed), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${connectionState.downlinkSpeedKbps / 1000} Mbps", fontWeight = FontWeight.Bold, color = CyanPrimary)
                }
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.link_up_speed), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${connectionState.uplinkSpeedKbps / 1000} Mbps", fontWeight = FontWeight.Bold, color = MintSecondary)
                }
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.metered_connection), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(if (connectionState.isMetered) "Yes (Metered)" else "No (Unmetered)", fontWeight = FontWeight.Bold, color = if (connectionState.isMetered) AmberAlert else MintSecondary)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
