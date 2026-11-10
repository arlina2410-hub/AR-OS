package com.aros.app.feature.routine.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "routines")
data class RoutineEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String,
    val timeMinutesOfDay: Int, // Minutes from midnight (e.g. 7:00 AM = 420)
    val daysMask: Int,         // Bitmask for Mon-Sun (127 = everyday)
    val durationMinutes: Int,
    val completedToday: Boolean,
    val streakCount: Int,
    val category: String,      // MORNING, FOCUS, EVENING, HEALTH
    val lastCompletedDate: String? = null
)

@Dao
interface RoutineDao {
    @Query("SELECT * FROM routines ORDER BY timeMinutesOfDay ASC")
    fun getAllFlow(): Flow<List<RoutineEntity>>

    @Query("SELECT * FROM routines ORDER BY timeMinutesOfDay ASC")
    suspend fun getAllList(): List<RoutineEntity>

    @Query("SELECT * FROM routines WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): RoutineEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: RoutineEntity): Long

    @Update
    suspend fun update(entity: RoutineEntity)

    @Query("DELETE FROM routines WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE routines SET completedToday = :completed, streakCount = :streak, lastCompletedDate = :date WHERE id = :id")
    suspend fun updateCompletion(id: Long, completed: Boolean, streak: Int, date: String?)

    @Query("UPDATE routines SET completedToday = 0")
    suspend fun resetDailyCompletion()
}
