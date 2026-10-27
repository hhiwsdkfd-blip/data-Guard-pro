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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.components.MetricCard
import com.example.ui.components.SectionHeader
import com.example.ui.theme.CoralWarning
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.MintSecondary
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.MainViewModel

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onNavigate: (AppScreen) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val prefs by viewModel.userPreferences.collectAsState()
    val isArabic = prefs.selectedLanguage == "ar"
    var showClearDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .testTag("settings_screen")
    ) {
        // App Bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("settings_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = if (isArabic) "الإعدادات العامة" else "Settings",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Tools & Sections Navigation
        SectionHeader(title = if (isArabic) "الأدوات والميزات" else "Tools & Features")

        MetricCard(
            title = if (isArabic) "الرسوم البيانية والتقارير" else "Charts & Reports",
            value = if (isArabic) "مخططات الاستهلاك والتصدير" else "Visual Trends & Export",
            subtitle = if (isArabic) "مقارنة الفترات وتصدير ملف CSV" else "Period comparisons & CSV export",
            icon = Icons.Default.PieChart,
            iconTint = CyanPrimary,
            onClick = { onNavigate(AppScreen.CHARTS_REPORTS) },
            modifier = Modifier.testTag("nav_charts_reports_card")
        )

        Spacer(modifier = Modifier.height(8.dp))

        MetricCard(
            title = if (isArabic) "الصلاحيات والخصوصية" else "Permissions & Privacy",
            value = if (isArabic) "إذن الاستخدام والإشعارات" else "Usage Access & Notifications",
            subtitle = if (isArabic) "100٪ خصوصية محلية بدون سحابة" else "100% on-device telemetry",
            icon = Icons.Default.Security,
            iconTint = MintSecondary,
            onClick = { onNavigate(AppScreen.PERMISSIONS) },
            modifier = Modifier.testTag("nav_permissions_card")
        )

        Spacer(modifier = Modifier.height(8.dp))

        MetricCard(
            title = if (isArabic) "باقة البيانات والحدود" else "Data Limits & Plan",
            value = if (isArabic) "الحد الشهري وتنبيهات الباقة" else "Monthly Limits & Alerts",
            subtitle = if (isArabic) "تحديد السعة ونسب التنبيه 50٪، 75٪، 90٪، 100٪" else "Set quotas & warning thresholds",
            icon = Icons.Default.Tune,
            iconTint = MaterialTheme.colorScheme.tertiary,
            onClick = { onNavigate(AppScreen.LIMITS) },
            modifier = Modifier.testTag("nav_limits_card")
        )

        Spacer(modifier = Modifier.height(8.dp))

        MetricCard(
            title = if (isArabic) "معلومات التطبيق" else "About Application",
            value = if (isArabic) "Data Guard Pro v1.0" else "Data Guard Pro v1.0",
            subtitle = if (isArabic) "التقنيات المستخدمة والقيود التقنية لنظام Android" else "Tech stack & Android OS limitations",
            icon = Icons.Default.Info,
            iconTint = CyanPrimary,
            onClick = { onNavigate(AppScreen.ABOUT) },
            modifier = Modifier.testTag("nav_about_card")
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Language Section
        SectionHeader(title = if (isArabic) "لغة التطبيق" else "Language")

        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Language, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (isArabic) "اللغة الأساسية والاتجاه (RTL / LTR)" else "Display Language",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterChip(
                        selected = prefs.selectedLanguage == "ar",
                        onClick = { viewModel.setLanguage("ar") },
                        label = { Text("العربية (Arabic)") },
                        modifier = Modifier.testTag("lang_ar_chip")
                    )
                    FilterChip(
                        selected = prefs.selectedLanguage == "en",
                        onClick = { viewModel.setLanguage("en") },
                        label = { Text("English") },
                        modifier = Modifier.testTag("lang_en_chip")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Theme Section
        SectionHeader(title = if (isArabic) "سمة الواجهة" else "Theme")

        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Palette, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (isArabic) "المظهر (فاتح / داكن)" else "Color Scheme",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterChip(
                        selected = prefs.appTheme == "system",
                        onClick = { viewModel.setTheme("system") },
                        label = { Text(if (isArabic) "افتراضي النظام" else "System") }
                    )
                    FilterChip(
                        selected = prefs.appTheme == "dark",
                        onClick = { viewModel.setTheme("dark") },
                        label = { Text(if (isArabic) "داكن" else "Dark") }
                    )
                    FilterChip(
                        selected = prefs.appTheme == "light",
                        onClick = { viewModel.setTheme("light") },
                        label = { Text(if (isArabic) "فاتح" else "Light") }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Reset Data
        SectionHeader(title = if (isArabic) "إدارة البيانات المحلية" else "Data Management")

        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (isArabic) "حذف السجلات المحفوظة في قاعدة بيانات الهاتف" else "Clear Local Database Records",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { showClearDialog = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CoralWarning),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("reset_data_button")
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isArabic) "إعادة ضبط وسجل الاستهلاك" else "Reset Stored History")
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(if (isArabic) "إعادة ضبط السجلات المحلية" else "Reset Local History") },
            text = { Text(if (isArabic) "هل أنت متأكد من رغبتك في حذف جميع السجلات واللقطات المحفوظة محلياً؟" else "Are you sure you want to delete all stored snapshots?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllData()
                        showClearDialog = false
                        Toast.makeText(context, if (isArabic) "تم مسح السجلات بنجاح" else "Data reset complete", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text(if (isArabic) "تأكيد الحذف" else "Confirm", color = CoralWarning)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(if (isArabic) "إلغاء" else "Cancel")
                }
            }
        )
    }
}
