package com.aros.app.feature.sos.platform

import android.content.Context
import android.media.AudioManager
import com.aros.app.feature.sos.domain.SettingsRepository

/**
 * Snapshots and restores STREAM_MUSIC and STREAM_RING per AR SOS Spec §6.
 * Discards snapshot on timeout/clear.
 * Restores only if "Restore volume after SOS" setting is enabled.
 */
class VolumeRestorer(
    private val context: Context,
    private val settingsRepository: SettingsRepository? = null
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private var musicSnapshot: Int? = null
    private var ringSnapshot: Int? = null

    @Synchronized
    fun snapshotVolume() {
        if (musicSnapshot != null || ringSnapshot != null) return
        try {
            audioManager?.let { am ->
                musicSnapshot = am.getStreamVolume(AudioManager.STREAM_MUSIC)
                ringSnapshot = am.getStreamVolume(AudioManager.STREAM_RING)
            }
        } catch (_: Exception) {
            // Do Not Disturb or audio policy restriction
        }
    }

    @Synchronized
    fun restoreVolume() {
        val shouldRestore = settingsRepository?.configSnapshot?.value?.restoreVolumeAfterSos ?: true
        val savedMusic = musicSnapshot
        val savedRing = ringSnapshot
        clear()

        if (!shouldRestore || audioManager == null) return

        try {
            if (savedMusic != null) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, savedMusic, 0)
            }
            if (savedRing != null) {
                audioManager.setStreamVolume(AudioManager.STREAM_RING, savedRing, 0)
            }
        } catch (_: Exception) {
            // Do Not Disturb or audio policy restriction
        }
    }

    @Synchronized
    fun clear() {
        musicSnapshot = null
        ringSnapshot = null
    }
}
