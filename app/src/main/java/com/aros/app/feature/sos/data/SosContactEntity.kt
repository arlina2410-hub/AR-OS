package com.aros.app.feature.sos.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "sos_contacts")
data class SosContactEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val number: String,
    val role: String, // "PRIMARY" or "BACKUP"
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface SosContactDao {
    @Query("SELECT * FROM sos_contacts ORDER BY sortOrder ASC, id ASC")
    fun getAllFlow(): Flow<List<SosContactEntity>>

    @Query("SELECT * FROM sos_contacts ORDER BY sortOrder ASC, id ASC")
    suspend fun getAllList(): List<SosContactEntity>

    @Query("SELECT * FROM sos_contacts WHERE role = 'PRIMARY' LIMIT 1")
    fun getPrimaryFlow(): Flow<SosContactEntity?>

    @Query("SELECT * FROM sos_contacts WHERE role = 'PRIMARY' LIMIT 1")
    suspend fun getPrimary(): SosContactEntity?

    @Query("SELECT COUNT(*) FROM sos_contacts")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(contact: SosContactEntity): Long

    @Update
    suspend fun update(contact: SosContactEntity)

    @Query("DELETE FROM sos_contacts WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE sos_contacts SET role = 'BACKUP' WHERE role = 'PRIMARY'")
    suspend fun clearPrimaryRole()

    @Query("UPDATE sos_contacts SET role = 'PRIMARY' WHERE id = :id")
    suspend fun setPrimaryRole(id: Long)
}
