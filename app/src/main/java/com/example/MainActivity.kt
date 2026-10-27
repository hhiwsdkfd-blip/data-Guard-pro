package com.example

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.example.ui.navigation.AppBottomNavigation
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.AppDetailScreen
import com.example.ui.screens.AppUsageScreen
import com.example.ui.screens.BreakdownScreen
import com.example.ui.screens.ChartsReportsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.LimitsScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.PermissionsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.DataMonitorTheme
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.MainViewModel
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val prefs by viewModel.userPreferences.collectAsState()
            val currentScreen by viewModel.currentScreen.collectAsState()

            // 1. Theme Selection
            val isDark = when (prefs.appTheme) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }

            // 2. Language & Layout Direction (Arabic RTL vs English LTR)
            val isArabic = prefs.selectedLanguage == "ar"
            val layoutDirection = if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr

            // Update configuration locale so string resources match selected language
            val context = LocalContext.current
            val locale = Locale.forLanguageTag(if (isArabic) "ar" else "en")
            val config = Configuration(LocalConfiguration.current).apply {
                setLocale(locale)
                setLayoutDirection(locale)
            }
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(config, context.resources.displayMetrics)

            CompositionLocalProvider(
                LocalLayoutDirection provides layoutDirection
            ) {
                DataMonitorTheme(darkTheme = isDark) {
                    when (currentScreen) {
                        AppScreen.SPLASH -> {
                            SplashScreen(
                                isOnboardingDone = prefs.onboardingCompleted,
                                onFinish = { done ->
                                    if (done) {
                                        viewModel.navigateTo(AppScreen.DASHBOARD)
                                    } else {
                                        viewModel.navigateTo(AppScreen.ONBOARDING)
                                    }
                                }
                            )
                        }

                        AppScreen.ONBOARDING -> {
                            OnboardingScreen(
                                onContinue = { viewModel.completeOnboarding() }
                            )
                        }

                        else -> {
                            val showBottomNav = currentScreen in listOf(
                                AppScreen.DASHBOARD,
                                AppScreen.APP_USAGE,
                                AppScreen.CHARTS_REPORTS,
                                AppScreen.PERMISSIONS,
                                AppScreen.SETTINGS
                            )

                            Scaffold(
                                contentWindowInsets = WindowInsets.safeDrawing,
                                bottomBar = {
                                    if (showBottomNav) {
                                        AppBottomNavigation(
                                            currentScreen = currentScreen,
                                            onNavigate = { viewModel.navigateTo(it) }
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            ) { innerPadding ->
                                val screenModifier = Modifier.padding(innerPadding)

                                when (currentScreen) {
                                    AppScreen.DASHBOARD -> DashboardScreen(
                                        viewModel = viewModel,
                                        onNavigate = { viewModel.navigateTo(it) },
                                        modifier = screenModifier
                                    )

                                    AppScreen.APP_USAGE -> AppUsageScreen(
                                        viewModel = viewModel,
                                        onBack = { viewModel.navigateBack() },
                                        modifier = screenModifier
                                    )

                                    AppScreen.APP_DETAIL -> AppDetailScreen(
                                        viewModel = viewModel,
                                        onBack = { viewModel.navigateBack() },
                                        modifier = screenModifier
                                    )

                                    AppScreen.CHARTS_REPORTS -> ChartsReportsScreen(
                                        viewModel = viewModel,
                                        onBack = { viewModel.navigateBack() },
                                        modifier = screenModifier
                                    )

                                    AppScreen.PERMISSIONS -> PermissionsScreen(
                                        viewModel = viewModel,
                                        onBack = { viewModel.navigateBack() },
                                        modifier = screenModifier
                                    )

                                    AppScreen.ABOUT -> AboutScreen(
                                        viewModel = viewModel,
                                        onBack = { viewModel.navigateBack() },
                                        modifier = screenModifier
                                    )

                                    AppScreen.BREAKDOWN -> BreakdownScreen(
                                        viewModel = viewModel,
                                        onBack = { viewModel.navigateBack() },
                                        modifier = screenModifier
                                    )

                                    AppScreen.HISTORY -> HistoryScreen(
                                        viewModel = viewModel,
                                        onBack = { viewModel.navigateBack() },
                                        modifier = screenModifier
                                    )

                                    AppScreen.ANALYTICS -> AnalyticsScreen(
                                        viewModel = viewModel,
                                        onBack = { viewModel.navigateBack() },
                                        modifier = screenModifier
                                    )

                                    AppScreen.LIMITS -> LimitsScreen(
                                        viewModel = viewModel,
                                        onBack = { viewModel.navigateBack() },
                                        modifier = screenModifier
                                    )

                                    AppScreen.NOTIFICATIONS -> NotificationsScreen(
                                        viewModel = viewModel,
                                        onBack = { viewModel.navigateBack() },
                                        modifier = screenModifier
                                    )

                                    AppScreen.SETTINGS -> SettingsScreen(
                                        viewModel = viewModel,
                                        onNavigate = { viewModel.navigateTo(it) },
                                        onBack = { viewModel.navigateBack() },
                                        modifier = screenModifier
                                    )

                                    else -> {}
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
