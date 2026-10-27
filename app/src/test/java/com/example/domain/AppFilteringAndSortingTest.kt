package com.example.domain

import com.example.data.model.AppNetworkUsage
import com.example.data.model.AppSortOrder
import com.example.data.model.TrafficFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppFilteringAndSortingTest {

    private val sampleApps = listOf(
        AppNetworkUsage(
            uid = 1001,
            packageName = "com.google.android.youtube",
            appName = "YouTube",
            mobileBytes = 500_000_000L,
            wifiBytes = 1_500_000_000L,
            rxBytes = 1_800_000_000L,
            txBytes = 200_000_000L,
            totalBytes = 2_000_000_000L
        ),
        AppNetworkUsage(
            uid = 1002,
            packageName = "org.telegram.messenger",
            appName = "Telegram",
            mobileBytes = 800_000_000L,
            wifiBytes = 200_000_000L,
            rxBytes = 600_000_000L,
            txBytes = 400_000_000L,
            totalBytes = 1_000_000_000L
        ),
        AppNetworkUsage(
            uid = 1003,
            packageName = "com.whatsapp",
            appName = "WhatsApp",
            mobileBytes = 0L,
            wifiBytes = 300_000_000L,
            rxBytes = 200_000_000L,
            txBytes = 100_000_000L,
            totalBytes = 300_000_000L
        ),
        AppNetworkUsage(
            uid = 1004,
            packageName = "com.spotify.music",
            appName = "Spotify",
            mobileBytes = 400_000_000L,
            wifiBytes = 0L,
            rxBytes = 380_000_000L,
            txBytes = 20_000_000L,
            totalBytes = 400_000_000L
        )
    )

    private fun filterAndSort(
        apps: List<AppNetworkUsage>,
        filter: TrafficFilter,
        sort: AppSortOrder,
        query: String
    ): List<AppNetworkUsage> {
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

        return when (sort) {
            AppSortOrder.HIGHEST_USAGE -> list.sortedByDescending { it.totalBytes }
            AppSortOrder.LOWEST_USAGE -> list.sortedBy { it.totalBytes }
            AppSortOrder.MOBILE_USAGE -> list.sortedByDescending { it.mobileBytes }
            AppSortOrder.WIFI_USAGE -> list.sortedByDescending { it.wifiBytes }
            AppSortOrder.ALPHABETICAL -> list.sortedBy { it.appName.lowercase() }
        }
    }

    @Test
    fun sort_highestUsage() {
        val result = filterAndSort(sampleApps, TrafficFilter.ALL, AppSortOrder.HIGHEST_USAGE, "")
        assertEquals("YouTube", result[0].appName)
        assertEquals("Telegram", result[1].appName)
        assertEquals("Spotify", result[2].appName)
        assertEquals("WhatsApp", result[3].appName)
    }

    @Test
    fun sort_lowestUsage() {
        val result = filterAndSort(sampleApps, TrafficFilter.ALL, AppSortOrder.LOWEST_USAGE, "")
        assertEquals("WhatsApp", result[0].appName)
        assertEquals("Spotify", result[1].appName)
        assertEquals("Telegram", result[2].appName)
        assertEquals("YouTube", result[3].appName)
    }

    @Test
    fun sort_mobileUsage() {
        val result = filterAndSort(sampleApps, TrafficFilter.ALL, AppSortOrder.MOBILE_USAGE, "")
        assertEquals("Telegram", result[0].appName) // 800 MB mobile
        assertEquals("YouTube", result[1].appName)  // 500 MB mobile
        assertEquals("Spotify", result[2].appName)  // 400 MB mobile
        assertEquals("WhatsApp", result[3].appName) // 0 MB mobile
    }

    @Test
    fun sort_wifiUsage() {
        val result = filterAndSort(sampleApps, TrafficFilter.ALL, AppSortOrder.WIFI_USAGE, "")
        assertEquals("YouTube", result[0].appName)  // 1500 MB wifi
        assertEquals("WhatsApp", result[1].appName) // 300 MB wifi
        assertEquals("Telegram", result[2].appName) // 200 MB wifi
        assertEquals("Spotify", result[3].appName)  // 0 MB wifi
    }

    @Test
    fun sort_alphabetical() {
        val result = filterAndSort(sampleApps, TrafficFilter.ALL, AppSortOrder.ALPHABETICAL, "")
        assertEquals("Spotify", result[0].appName)
        assertEquals("Telegram", result[1].appName)
        assertEquals("WhatsApp", result[2].appName)
        assertEquals("YouTube", result[3].appName)
    }

    @Test
    fun filter_mobileOnly() {
        val result = filterAndSort(sampleApps, TrafficFilter.MOBILE_ONLY, AppSortOrder.HIGHEST_USAGE, "")
        assertEquals(3, result.size)
        assertTrue(result.none { it.appName == "WhatsApp" }) // WhatsApp had 0 mobile bytes
    }

    @Test
    fun filter_wifiOnly() {
        val result = filterAndSort(sampleApps, TrafficFilter.WIFI_ONLY, AppSortOrder.HIGHEST_USAGE, "")
        assertEquals(3, result.size)
        assertTrue(result.none { it.appName == "Spotify" }) // Spotify had 0 wifi bytes
    }

    @Test
    fun search_byAppName() {
        val result = filterAndSort(sampleApps, TrafficFilter.ALL, AppSortOrder.HIGHEST_USAGE, "gram")
        assertEquals(1, result.size)
        assertEquals("Telegram", result[0].appName)
    }

    @Test
    fun search_byPackageName() {
        val result = filterAndSort(sampleApps, TrafficFilter.ALL, AppSortOrder.HIGHEST_USAGE, "google")
        assertEquals(1, result.size)
        assertEquals("YouTube", result[0].appName)
    }
}
