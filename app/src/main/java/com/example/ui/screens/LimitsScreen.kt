package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.SectionHeader
import com.example.ui.theme.CyanPrimary
import com.example.ui.viewmodel.MainViewModel

@Composable
fun LimitsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val currentPrefs by viewModel.userPreferences.collectAsState()

    var monthlyLimitGb by remember(currentPrefs) { mutableFloatStateOf(currentPrefs.monthlyLimitGb) }
    var cycleStartDay by remember(currentPrefs) { mutableIntStateOf(currentPrefs.cycleStartDay) }
    var dailyLimitMb by remember(currentPrefs) { mutableFloatStateOf(currentPrefs.dailyLimitMb) }
    var mobileLimitEnabled by remember(currentPrefs) { mutableStateOf(currentPrefs.mobileLimitEnabled) }
    var dailyLimitEnabled by remember(currentPrefs) { mutableStateOf(currentPrefs.dailyLimitEnabled) }

    var threshold50 by remember(currentPrefs) { mutableStateOf(currentPrefs.threshold50) }
    var threshold75 by remember(currentPrefs) { mutableStateOf(currentPrefs.threshold75) }
    var threshold90 by remember(currentPrefs) { mutableStateOf(currentPrefs.threshold90) }
    var threshold100 by remember(currentPrefs) { mutableStateOf(currentPrefs.threshold100) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .testTag("limits_screen")
    ) {
        // App Bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("limits_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = stringResource(R.string.data_plan_settings),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Mobile Plan Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.enable_mobile_limit),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Track usage against your provider quota",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = mobileLimitEnabled,
                        onCheckedChange = { mobileLimitEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = CyanPrimary),
                        modifier = Modifier.testTag("mobile_limit_switch")
                    )
                }

                if (mobileLimitEnabled) {
                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.custom_limit_gb),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${monthlyLimitGb.toInt()} GB",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = CyanPrimary
                            )
                        )
                    }

                    Slider(
                        value = monthlyLimitGb,
                        onValueChange = { monthlyLimitGb = it },
                        valueRange = 1f..100f,
                        steps = 98,
                        colors = SliderDefaults.colors(
                            thumbColor = CyanPrimary,
                            activeTrackColor = CyanPrimary
                        ),
                        modifier = Modifier.testTag("monthly_limit_slider")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Billing Cycle Day Selector
                    Text(
                        text = stringResource(R.string.cycle_start_day),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = cycleStartDay.toString(),
                        onValueChange = {
                            val parsed = it.filter { ch -> ch.isDigit() }.toIntOrNull()
                            if (parsed != null && parsed in 1..31) {
                                cycleStartDay = parsed
                            } else if (it.isEmpty()) {
                                cycleStartDay = 1
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("cycle_start_day_input")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Daily Limit Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.enable_daily_limit),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Daily warning threshold (e.g. 1024 MB)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = dailyLimitEnabled,
                        onCheckedChange = { dailyLimitEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = CyanPrimary),
                        modifier = Modifier.testTag("daily_limit_switch")
                    )
                }

                if (dailyLimitEnabled) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = dailyLimitMb.toInt().toString(),
                        onValueChange = {
                            val parsed = it.filter { ch -> ch.isDigit() }.toFloatOrNull()
                            if (parsed != null) {
                                dailyLimitMb = parsed
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        label = { Text(stringResource(R.string.custom_daily_mb)) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Warning Thresholds Checkboxes
        SectionHeader(title = stringResource(R.string.thresholds))

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = threshold50, onCheckedChange = { threshold50 = it })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("50% of plan allowance", style = MaterialTheme.typography.bodyMedium)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = threshold75, onCheckedChange = { threshold75 = it })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("75% of plan allowance", style = MaterialTheme.typography.bodyMedium)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = threshold90, onCheckedChange = { threshold90 = it })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("90% warning threshold", style = MaterialTheme.typography.bodyMedium)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = threshold100, onCheckedChange = { threshold100 = it })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("100% quota exhausted", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Save Button
        Button(
            onClick = {
                viewModel.savePlanSettings(
                    monthlyLimitGb = monthlyLimitGb,
                    cycleStartDay = cycleStartDay,
                    dailyLimitMb = dailyLimitMb,
                    mobileLimitEnabled = mobileLimitEnabled,
                    wifiQuotaEnabled = false,
                    dailyLimitEnabled = dailyLimitEnabled,
                    threshold50 = threshold50,
                    threshold75 = threshold75,
                    threshold90 = threshold90,
                    threshold100 = threshold100
                )
                Toast.makeText(context, context.getString(R.string.plan_saved), Toast.LENGTH_SHORT).show()
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = CyanPrimary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .minimumInteractiveComponentSize()
                .testTag("save_plan_button")
        ) {
            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.save_plan),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
