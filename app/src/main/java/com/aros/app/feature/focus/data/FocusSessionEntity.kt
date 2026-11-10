package com.aros.app.feature.focus.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "focus_sessions")
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startTime: Long,
    val endTime: Long,
    val durationMinutes: Int,
    val sessionType: String, // DEEP_WORK, POMODORO, BREATH
    val completed: Boolean,
    val interruptions: Int
)

@Dao
interface FocusDao {
    @Query("SELECT * FROM focus_sessions ORDER BY startTime DESC")
    fun getAllFlow(): Flow<List<FocusSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: FocusSessionEntity): Long

    @Query("SELECT SUM(durationMinutes) FROM focus_sessions WHERE startTime >= :startOfDay AND completed = 1")
    fun getTodayFocusMinutesFlow(startOfDay: Long): Flow<Int?>

    @Query("SELECT COUNT(*) FROM focus_sessions WHERE startTime >= :startOfDay AND completed = 1")
    fun getTodayCompletedSessionsCountFlow(startOfDay: Long): Flow<Int?>
}
