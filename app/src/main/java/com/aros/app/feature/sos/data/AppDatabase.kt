package com.aros.app.feature.sos.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.aros.app.feature.adaptive.data.AdaptiveInsightDao
import com.aros.app.feature.adaptive.data.AdaptiveInsightEntity
import com.aros.app.feature.discipline.data.DisciplineDao
import com.aros.app.feature.discipline.data.DisciplineRuleEntity
import com.aros.app.feature.focus.data.FocusDao
import com.aros.app.feature.focus.data.FocusSessionEntity
import com.aros.app.feature.guardian.data.GuardianDao
import com.aros.app.feature.guardian.data.GuardianScanEntity
import com.aros.app.feature.ledger.data.ActivityLedgerDao
import com.aros.app.feature.ledger.data.ActivityLedgerEntity
import com.aros.app.feature.routine.data.RoutineDao
import com.aros.app.feature.routine.data.RoutineEntity

@Database(
    entities = [
        SosContactEntity::class,
        SosEventEntity::class,
        RoutineEntity::class,
        FocusSessionEntity::class,
        GuardianScanEntity::class,
        ActivityLedgerEntity::class,
        DisciplineRuleEntity::class,
        AdaptiveInsightEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun contactDao(): SosContactDao
    abstract fun eventDao(): SosEventDao
    abstract fun routineDao(): RoutineDao
    abstract fun focusDao(): FocusDao
    abstract fun guardianDao(): GuardianDao
    abstract fun ledgerDao(): ActivityLedgerDao
    abstract fun disciplineDao(): DisciplineDao
    abstract fun adaptiveDao(): AdaptiveInsightDao

    companion object {
        private const val DB_NAME = "ar_os.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DB_NAME
                ).fallbackToDestructiveMigration(true).build().also { INSTANCE = it }
            }
        }
    }
}
