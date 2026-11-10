package com.aros.app.feature.ledger.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "activity_ledger")
data class ActivityLedgerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val eventCategory: String, // PHONE_UNLOCK, FOCUS_BLOCK, SOS_ALERT, DISCIPLINE_PASS, ROUTINE_DONE
    val durationSeconds: Long,
    val appLabel: String,
    val isIntentional: Boolean
)

@Dao
interface ActivityLedgerDao {
    @Query("SELECT * FROM activity_ledger ORDER BY timestamp DESC")
    fun getAllFlow(): Flow<List<ActivityLedgerEntity>>

    @Query("SELECT * FROM activity_ledger WHERE timestamp >= :sinceTime ORDER BY timestamp ASC")
    suspend fun getEventsSince(sinceTime: Long): List<ActivityLedgerEntity>

    @Query("SELECT * FROM activity_ledger WHERE timestamp >= :sinceTime ORDER BY timestamp ASC")
    fun getEventsSinceFlow(sinceTime: Long): Flow<List<ActivityLedgerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ActivityLedgerEntity): Long

    @Query("DELETE FROM activity_ledger")
    suspend fun clearAll()
}
