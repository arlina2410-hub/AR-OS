package com.aros.app.feature.guardian.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "guardian_scans")
data class GuardianScanEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val sovereigntyScore: Int,
    val cameraAppsCount: Int,
    val micAppsCount: Int,
    val locationAppsCount: Int,
    val highRiskAppsCount: Int
)

@Dao
interface GuardianDao {
    @Query("SELECT * FROM guardian_scans ORDER BY timestamp DESC LIMIT 1")
    fun getLatestScanFlow(): Flow<GuardianScanEntity?>

    @Query("SELECT * FROM guardian_scans ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestScan(): GuardianScanEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: GuardianScanEntity): Long
}
