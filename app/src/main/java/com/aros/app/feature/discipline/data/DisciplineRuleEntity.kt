package com.aros.app.feature.discipline.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "discipline_rules")
data class DisciplineRuleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val challengeType: String, // BREATH_GATE, PLEDGE_TYPING, ARITHMETIC
    val frictionSeconds: Int,
    val targetAppOrHabit: String,
    val isActive: Boolean,
    val passCount: Int
)

@Dao
interface DisciplineDao {
    @Query("SELECT * FROM discipline_rules ORDER BY id ASC")
    fun getAllFlow(): Flow<List<DisciplineRuleEntity>>

    @Query("SELECT * FROM discipline_rules ORDER BY id ASC")
    suspend fun getAllList(): List<DisciplineRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: DisciplineRuleEntity): Long

    @Update
    suspend fun update(entity: DisciplineRuleEntity)

    @Query("DELETE FROM discipline_rules WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE discipline_rules SET passCount = passCount + 1 WHERE id = :id")
    suspend fun incrementPassCount(id: Long)
}
