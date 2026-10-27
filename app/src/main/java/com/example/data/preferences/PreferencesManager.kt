package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UserPreferences(
    val monthlyLimitGb: Float = 15.0f,
    val cycleStartDay: Int = 1,
    val dailyLimitMb: Float = 1024.0f,
    val mobileLimitEnabled: Boolean = true,
    val wifiQuotaEnabled: Boolean = false,
    val dailyLimitEnabled: Boolean = false,
    val threshold50: Boolean = true,
    val threshold75: Boolean = true,
    val threshold90: Boolean = true,
    val threshold100: Boolean = true,
    val selectedLanguage: String = "en", // "en" or "ar"
    val appTheme: String = "system", // "system", "dark", "light"
    val onboardingCompleted: Boolean = false,
    val backgroundIntervalMinutes: Int = 15
)

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _userPreferencesFlow = MutableStateFlow(loadPreferences())
    val userPreferencesFlow: StateFlow<UserPreferences> = _userPreferencesFlow.asStateFlow()

    private fun loadPreferences(): UserPreferences {
        return UserPreferences(
            monthlyLimitGb = prefs.getFloat(KEY_MONTHLY_LIMIT, 15.0f),
            cycleStartDay = prefs.getInt(KEY_CYCLE_START_DAY, 1),
            dailyLimitMb = prefs.getFloat(KEY_DAILY_LIMIT, 1024.0f),
            mobileLimitEnabled = prefs.getBoolean(KEY_MOBILE_LIMIT_ENABLED, true),
            wifiQuotaEnabled = prefs.getBoolean(KEY_WIFI_QUOTA_ENABLED, false),
            dailyLimitEnabled = prefs.getBoolean(KEY_DAILY_LIMIT_ENABLED, false),
            threshold50 = prefs.getBoolean(KEY_THRESH_50, true),
            threshold75 = prefs.getBoolean(KEY_THRESH_75, true),
            threshold90 = prefs.getBoolean(KEY_THRESH_90, true),
            threshold100 = prefs.getBoolean(KEY_THRESH_100, true),
            selectedLanguage = prefs.getString(KEY_LANGUAGE, "ar") ?: "ar",
            appTheme = prefs.getString(KEY_THEME, "system") ?: "system",
            onboardingCompleted = prefs.getBoolean(KEY_ONBOARDING_DONE, false),
            backgroundIntervalMinutes = prefs.getInt(KEY_BG_INTERVAL, 15)
        )
    }

    fun updatePlan(
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
        prefs.edit()
            .putFloat(KEY_MONTHLY_LIMIT, monthlyLimitGb)
            .putInt(KEY_CYCLE_START_DAY, cycleStartDay)
            .putFloat(KEY_DAILY_LIMIT, dailyLimitMb)
            .putBoolean(KEY_MOBILE_LIMIT_ENABLED, mobileLimitEnabled)
            .putBoolean(KEY_WIFI_QUOTA_ENABLED, wifiQuotaEnabled)
            .putBoolean(KEY_DAILY_LIMIT_ENABLED, dailyLimitEnabled)
            .putBoolean(KEY_THRESH_50, threshold50)
            .putBoolean(KEY_THRESH_75, threshold75)
            .putBoolean(KEY_THRESH_90, threshold90)
            .putBoolean(KEY_THRESH_100, threshold100)
            .apply()
        _userPreferencesFlow.value = loadPreferences()
    }

    fun setLanguage(language: String) {
        prefs.edit().putString(KEY_LANGUAGE, language).apply()
        _userPreferencesFlow.value = loadPreferences()
    }

    fun setTheme(theme: String) {
        prefs.edit().putString(KEY_THEME, theme).apply()
        _userPreferencesFlow.value = loadPreferences()
    }

    fun setOnboardingCompleted(completed: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDING_DONE, completed).apply()
        _userPreferencesFlow.value = loadPreferences()
    }

    fun setBackgroundInterval(minutes: Int) {
        prefs.edit().putInt(KEY_BG_INTERVAL, minutes).apply()
        _userPreferencesFlow.value = loadPreferences()
    }

    fun getPreferences(): UserPreferences = _userPreferencesFlow.value

    companion object {
        private const val PREFS_NAME = "data_monitor_prefs"
        private const val KEY_MONTHLY_LIMIT = "monthly_limit_gb"
        private const val KEY_CYCLE_START_DAY = "cycle_start_day"
        private const val KEY_DAILY_LIMIT = "daily_limit_mb"
        private const val KEY_MOBILE_LIMIT_ENABLED = "mobile_limit_enabled"
        private const val KEY_WIFI_QUOTA_ENABLED = "wifi_quota_enabled"
        private const val KEY_DAILY_LIMIT_ENABLED = "daily_limit_enabled"
        private const val KEY_THRESH_50 = "thresh_50"
        private const val KEY_THRESH_75 = "thresh_75"
        private const val KEY_THRESH_90 = "thresh_90"
        private const val KEY_THRESH_100 = "thresh_100"
        private const val KEY_LANGUAGE = "selected_language"
        private const val KEY_THEME = "app_theme"
        private const val KEY_ONBOARDING_DONE = "onboarding_done"
        private const val KEY_BG_INTERVAL = "bg_interval"
    }
}
