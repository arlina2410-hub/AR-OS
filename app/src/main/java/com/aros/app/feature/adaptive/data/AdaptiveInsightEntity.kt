package com.aros.app.feature.adaptive.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "adaptive_insights")
data class AdaptiveInsightEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val headline: String,
    val body: String,
    val scoreImpact: Int,
    val isDismissed: Boolean
)

@Dao
interface AdaptiveInsightDao {
    @Query("SELECT * FROM adaptive_insights WHERE isDismissed = 0 ORDER BY timestamp DESC")
    fun getVisibleInsightsFlow(): Flow<List<AdaptiveInsightEntity>>

    @Query("SELECT * FROM adaptive_insights ORDER BY timestamp DESC")
    suspend fun getAllList(): List<AdaptiveInsightEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: AdaptiveInsightEntity): Long

    @Query("UPDATE adaptive_insights SET isDismissed = 1 WHERE id = :id")
    suspend fun dismissInsight(id: Long)

    @Query("DELETE FROM adaptive_insights")
    suspend fun clearAll()
}
