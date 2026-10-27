package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UsageDao {

    @Query("SELECT * FROM usage_snapshots ORDER BY timestamp DESC")
    fun getAllSnapshots(): Flow<List<UsageSnapshotEntity>>

    @Query("SELECT * FROM usage_snapshots ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentSnapshots(limit: Int): Flow<List<UsageSnapshotEntity>>

    @Query("SELECT * FROM usage_snapshots WHERE timestamp >= :fromMillis ORDER BY timestamp ASC")
    suspend fun getSnapshotsSince(fromMillis: Long): List<UsageSnapshotEntity>

    @Query("SELECT * FROM usage_snapshots WHERE timestamp BETWEEN :fromMillis AND :toMillis ORDER BY timestamp ASC")
    suspend fun getSnapshotsBetween(fromMillis: Long, toMillis: Long): List<UsageSnapshotEntity>

    @Query("SELECT * FROM usage_snapshots WHERE dateKey = :dateKey LIMIT 1")
    suspend fun getSnapshotByDate(dateKey: String): UsageSnapshotEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSnapshot(snapshot: UsageSnapshotEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAppSnapshots(snapshots: List<AppUsageSnapshotEntity>)

    @Query("SELECT * FROM app_usage_snapshots WHERE dateKey = :dateKey ORDER BY totalBytes DESC")
    fun getAppUsageForDate(dateKey: String): Flow<List<AppUsageSnapshotEntity>>

    @Query("SELECT * FROM notification_logs ORDER BY timestamp DESC")
    fun getAllNotificationLogs(): Flow<List<NotificationLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotificationLog(log: NotificationLogEntity): Long

    @Query("SELECT thresholdPercent FROM notification_logs WHERE dateKey = :dateKey AND networkType = :networkType")
    suspend fun getNotifiedThresholds(dateKey: String, networkType: String): List<Int>

    @Query("DELETE FROM usage_snapshots")
    suspend fun clearSnapshots()

    @Query("DELETE FROM app_usage_snapshots")
    suspend fun clearAppSnapshots()

    @Query("DELETE FROM notification_logs")
    suspend fun clearNotificationLogs()
}
