package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "usage_snapshots",
    indices = [Index(value = ["dateKey"], unique = true)]
)
data class UsageSnapshotEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dateKey: String, // e.g. "2026-10-27"
    val timestamp: Long,
    val mobileRxBytes: Long,
    val mobileTxBytes: Long,
    val wifiRxBytes: Long,
    val wifiTxBytes: Long,
    val totalBytes: Long
)

@Entity(
    tableName = "app_usage_snapshots",
    indices = [Index(value = ["dateKey", "packageName"], unique = true)]
)
data class AppUsageSnapshotEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dateKey: String,
    val packageName: String,
    val appName: String,
    val mobileBytes: Long,
    val wifiBytes: Long,
    val totalBytes: Long,
    val lastUpdated: Long
)

@Entity(tableName = "notification_logs")
data class NotificationLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val thresholdPercent: Int,
    val networkType: String, // "MOBILE" or "WIFI"
    val title: String,
    val message: String,
    val dateKey: String
)
