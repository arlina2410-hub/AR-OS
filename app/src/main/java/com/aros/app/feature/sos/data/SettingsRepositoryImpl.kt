package com.aros.app.feature.sos.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aros.app.feature.sos.domain.SensitivityPreset
import com.aros.app.feature.sos.domain.SettingsRepository
import com.aros.app.feature.sos.domain.SosConfig
import com.aros.app.feature.sos.domain.SosMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ar_os_settings")

class SettingsRepositoryImpl(
    private val context: Context,
    scope: CoroutineScope
) : SettingsRepository {

    private object Keys {
        val SOS_MODE = stringPreferencesKey("sos_mode")
        val COUNTDOWN_SECONDS = intPreferencesKey("sos_countdown_seconds")
        val SENSITIVITY_PRESET = stringPreferencesKey("sos_sensitivity_preset")
        val RESTORE_VOLUME_AFTER_SOS = booleanPreferencesKey("sos_restore_volume")
        val START_CALL_ON_SPEAKER = booleanPreferencesKey("sos_speakerphone")
        val SHOW_STATUS_NOTIFICATION = booleanPreferencesKey("sos_show_status_notification")
        val SELECTED_SIM_ID = stringPreferencesKey("sos_selected_sim_id")
        val SETUP_COMPLETED = booleanPreferencesKey("sos_setup_completed")
        val USE_24_HOUR_TIME = booleanPreferencesKey("sos_use_24_hour_time")
    }

    override val configFlow: Flow<SosConfig> = context.dataStore.data.map { prefs ->
        val modeStr = prefs[Keys.SOS_MODE] ?: SosMode.TEST.name
        val mode = try { SosMode.valueOf(modeStr) } catch (_: Exception) { SosMode.TEST }
        val countdown = (prefs[Keys.COUNTDOWN_SECONDS] ?: 5).coerceIn(0, 10)
        val rawSensitivity = prefs[Keys.SENSITIVITY_PRESET] ?: "Normal"
        val sensitivity = SensitivityPreset.fromString(rawSensitivity).label
        val restoreVolume = prefs[Keys.RESTORE_VOLUME_AFTER_SOS] ?: true
        val speaker = prefs[Keys.START_CALL_ON_SPEAKER] ?: false
        val statusNotif = prefs[Keys.SHOW_STATUS_NOTIFICATION] ?: true
        val simId = prefs[Keys.SELECTED_SIM_ID]
        val setupDone = prefs[Keys.SETUP_COMPLETED] ?: false
        val use24H = prefs[Keys.USE_24_HOUR_TIME] ?: false

        SosConfig(
            mode = mode,
            countdownSeconds = countdown,
            sensitivityPreset = sensitivity,
            restoreVolumeAfterSos = restoreVolume,
            startCallOnSpeaker = speaker,
            showStatusNotification = statusNotif,
            selectedSimId = simId,
            setupCompleted = setupDone,
            use24HourTime = use24H
        )
    }

    override val configSnapshot: StateFlow<SosConfig> = configFlow.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = SosConfig()
    )

    override suspend fun setMode(mode: SosMode) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SOS_MODE] = mode.name
        }
    }

    override suspend fun setCountdownSeconds(seconds: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.COUNTDOWN_SECONDS] = seconds.coerceIn(0, 10)
        }
    }

    override suspend fun setSensitivityPreset(preset: String) {
        val normalized = SensitivityPreset.fromString(preset).label
        context.dataStore.edit { prefs ->
            prefs[Keys.SENSITIVITY_PRESET] = normalized
        }
    }

    override suspend fun setRestoreVolumeAfterSos(restore: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.RESTORE_VOLUME_AFTER_SOS] = restore
        }
    }

    override suspend fun setStartCallOnSpeaker(speaker: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.START_CALL_ON_SPEAKER] = speaker
        }
    }

    override suspend fun setShowStatusNotification(show: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SHOW_STATUS_NOTIFICATION] = show
        }
    }

    override suspend fun setSelectedSimId(simId: String?) {
        context.dataStore.edit { prefs ->
            if (simId == null) {
                prefs.remove(Keys.SELECTED_SIM_ID)
            } else {
                prefs[Keys.SELECTED_SIM_ID] = simId
            }
        }
    }

    override suspend fun setSetupCompleted(completed: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SETUP_COMPLETED] = completed
        }
    }

    override suspend fun setUse24HourTime(use24Hour: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.USE_24_HOUR_TIME] = use24Hour
        }
    }
}
