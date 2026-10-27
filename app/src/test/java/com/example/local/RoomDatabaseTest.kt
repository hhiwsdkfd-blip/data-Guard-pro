package com.example.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.DataMonitorDatabase
import com.example.data.local.NotificationLogEntity
import com.example.data.local.UsageDao
import com.example.data.local.UsageSnapshotEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RoomDatabaseTest {

    private lateinit var db: DataMonitorDatabase
    private lateinit var dao: UsageDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, DataMonitorDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.usageDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertAndQuerySnapshot() = runBlocking {
        val snapshot = UsageSnapshotEntity(
            dateKey = "2026-10-27",
            timestamp = 1793116800000L,
            mobileRxBytes = 5000L,
            mobileTxBytes = 1000L,
            wifiRxBytes = 8000L,
            wifiTxBytes = 2000L,
            totalBytes = 16000L
        )
        dao.insertOrUpdateSnapshot(snapshot)

        val retrieved = dao.getSnapshotByDate("2026-10-27")
        assertNotNull(retrieved)
        assertEquals(16000L, retrieved?.totalBytes)
        assertEquals(5000L, retrieved?.mobileRxBytes)
    }

    @Test
    fun insertAndQueryNotificationLogs() = runBlocking {
        val log = NotificationLogEntity(
            timestamp = System.currentTimeMillis(),
            thresholdPercent = 75,
            networkType = "MOBILE",
            title = "75% Alert",
            message = "Plan is 75% consumed",
            dateKey = "2026-10"
        )
        dao.insertNotificationLog(log)

        val notified = dao.getNotifiedThresholds("2026-10", "MOBILE")
        assertEquals(1, notified.size)
        assertEquals(75, notified[0])
    }

    @Test
    fun clearSnapshots_removesAllRecords() = runBlocking {
        val snapshot = UsageSnapshotEntity(
            dateKey = "2026-10-27",
            timestamp = 1000L,
            mobileRxBytes = 100L,
            mobileTxBytes = 100L,
            wifiRxBytes = 100L,
            wifiTxBytes = 100L,
            totalBytes = 400L
        )
        dao.insertOrUpdateSnapshot(snapshot)
        dao.clearSnapshots()

        val list = dao.getSnapshotsSince(0L)
        assertTrue(list.isEmpty())
    }
}
