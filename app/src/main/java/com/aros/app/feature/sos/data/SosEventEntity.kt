package com.aros.app.feature.sos.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "sos_events")
data class SosEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val type: String, // TRIGGERED, COUNTDOWN_TICK, CANCELLED, DIALING, COMPLETED, TEST_RUN, REBOOT
    val mode: String, // LIVE, TEST
    val contactName: String? = null,
    val deviceLocked: Boolean = false,
    val screenInteractive: Boolean = true,
    val detail: String = ""
)

@Dao
interface SosEventDao {
    @Query("SELECT * FROM sos_events ORDER BY timestamp DESC")
    fun getAllFlow(): Flow<List<SosEventEntity>>

    @Query("SELECT * FROM sos_events ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 100): List<SosEventEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: SosEventEntity): Long

    @Query("DELETE FROM sos_events")
    suspend fun clearAll()

    @Query("DELETE FROM sos_events WHERE id NOT IN (SELECT id FROM sos_events ORDER BY timestamp DESC LIMIT 500)")
    suspend fun pruneOldEntries()
}
