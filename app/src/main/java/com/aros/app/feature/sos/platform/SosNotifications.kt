package com.aros.app.feature.sos.platform

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.aros.app.MainActivity
import com.aros.app.feature.sos.domain.OverallReadinessStatus
import com.aros.app.feature.sos.domain.SosContact
import com.aros.app.feature.sos.domain.SosMode
import com.aros.app.feature.sos.domain.SosNotifier
import com.aros.app.feature.sos.domain.SosState

class SosNotifications(private val context: Context) : SosNotifier {

    companion object {
        const val CHANNEL_STATUS = "sos_status"
        const val CHANNEL_ALERT = "sos_alert"
        const val CHANNEL_WARNING = "sos_warning"

        const val NOTIFICATION_ID_STATUS = 990
        const val NOTIFICATION_ID_COUNTDOWN = 991
        const val NOTIFICATION_ID_DIALING = 992
        const val NOTIFICATION_ID_WARNING = 993
        const val NOTIFICATION_ID_FALLBACK = 994
        const val NOTIFICATION_ID_BACKUPS = 995
    }

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createChannels()
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val alertChannel = NotificationChannel(
                CHANNEL_ALERT,
                "Emergency SOS Alert",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Urgent alerts during active AR SOS countdown and dialing"
                setSound(null, null)
                enableVibration(false)
            }

            val statusChannel = NotificationChannel(
                CHANNEL_STATUS,
                "AR SOS Service Status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows AR SOS operational readiness"
            }

            val warningChannel = NotificationChannel(
                CHANNEL_WARNING,
                "AR SOS Warnings",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Health and permission warnings for AR SOS"
            }

            notificationManager.createNotificationChannel(alertChannel)
            notificationManager.createNotificationChannel(statusChannel)
            notificationManager.createNotificationChannel(warningChannel)
        }
    }

    override fun showOngoingNotification(state: SosState.Countdown) {
        val cancelIntent = Intent(context, SosActionReceiver::class.java).apply {
            action = SosActionReceiver.ACTION_CANCEL
        }
        val cancelPendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val callNowIntent = Intent(context, SosActionReceiver::class.java).apply {
            action = SosActionReceiver.ACTION_CALL_NOW
        }
        val callNowPendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            callNowIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (state.mode == SosMode.LIVE) {
            "EMERGENCY SOS: Calling in ${state.remainingSeconds}s"
        } else {
            "TEST SOS: Simulation in ${state.remainingSeconds}s"
        }

        val cancelAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_close_clear_cancel,
            "CANCEL SOS",
            cancelPendingIntent
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                setAuthenticationRequired(false)
            }
        }.build()

        val callNowAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_call,
            "CALL NOW",
            callNowPendingIntent
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                setAuthenticationRequired(false)
            }
        }.build()

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(SosScreenLauncherImpl.EXTRA_SHOW_SOS, true)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            2,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ALERT)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText("Target: ${state.targetContact.name}")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setAutoCancel(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(contentPendingIntent)
            .addAction(cancelAction)
            .addAction(callNowAction)

        try {
            notificationManager.notify(NOTIFICATION_ID_COUNTDOWN, builder.build())
        } catch (_: SecurityException) {
            // Missing POST_NOTIFICATIONS runtime permission on Android 13+
        }
    }

    override fun showCallingNotification(state: SosState.Dialing) {
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(SosScreenLauncherImpl.EXTRA_SHOW_SOS, true)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            3,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ALERT)
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setContentTitle(if (state.mode == SosMode.LIVE) "Calling ${state.targetContact.name}..." else "Simulating Call...")
            .setContentText(state.method)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(contentPendingIntent)

        try {
            notificationManager.notify(NOTIFICATION_ID_DIALING, builder.build())
        } catch (_: SecurityException) {}
    }

    override fun showDialFailedFallback(targetContact: SosContact) {
        val retryIntent = Intent(context, SosActionReceiver::class.java).apply {
            action = SosActionReceiver.ACTION_RETRY_DIAL
        }
        val retryPendingIntent = PendingIntent.getBroadcast(
            context,
            4,
            retryIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(SosScreenLauncherImpl.EXTRA_SHOW_SOS, true)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            5,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ALERT)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("Call could not be placed automatically")
            .setContentText("Tap to open AR SOS and retry calling ${targetContact.name}.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(contentPendingIntent)
            .addAction(android.R.drawable.ic_menu_call, "RETRY DIAL", retryPendingIntent)

        try {
            notificationManager.notify(NOTIFICATION_ID_FALLBACK, builder.build())
        } catch (_: SecurityException) {}
    }

    override fun showBackupContactsNotification(backupContacts: List<SosContact>) {
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            6,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ALERT)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Call backup contact?")
            .setContentText("Primary dial initiated. Call a backup guardian if unanswered:")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(contentPendingIntent)

        backupContacts.take(3).forEachIndexed { index, contact ->
            val callIntent = Intent(context, SosActionReceiver::class.java).apply {
                action = SosActionReceiver.ACTION_CALL_CONTACT
                putExtra(SosActionReceiver.EXTRA_CONTACT_ID, contact.id)
            }
            val callPendingIntent = PendingIntent.getBroadcast(
                context,
                100 + index,
                callIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(
                android.R.drawable.ic_menu_call,
                "Call ${contact.name}",
                callPendingIntent
            )
        }

        try {
            notificationManager.notify(NOTIFICATION_ID_BACKUPS, builder.build())
        } catch (_: SecurityException) {}
    }

    override fun showStatusNotification(mode: SosMode, readiness: OverallReadinessStatus) {
        val startIntent = Intent(context, SosActionReceiver::class.java).apply {
            action = SosActionReceiver.ACTION_START_SOS
        }
        val startPendingIntent = PendingIntent.getBroadcast(
            context,
            7,
            startIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            8,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val modeLabel = if (mode == SosMode.LIVE) "LIVE MODE" else "TEST MODE"
        val statusText = "Mode: $modeLabel • Status: ${readiness.displayLabel}"

        val builder = NotificationCompat.Builder(context, CHANNEL_STATUS)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("AR SOS Active")
            .setContentText(statusText)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(contentPendingIntent)
            .addAction(android.R.drawable.ic_media_play, "Start SOS", startPendingIntent)

        try {
            notificationManager.notify(NOTIFICATION_ID_STATUS, builder.build())
        } catch (_: SecurityException) {}
    }

    override fun dismissStatusNotification() {
        notificationManager.cancel(NOTIFICATION_ID_STATUS)
    }

    override fun showReadinessWarning(issues: List<String>) {
        val builder = NotificationCompat.Builder(context, CHANNEL_WARNING)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("AR SOS Needs Attention")
            .setContentText(issues.firstOrNull() ?: "System readiness impaired")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        try {
            notificationManager.notify(NOTIFICATION_ID_WARNING, builder.build())
        } catch (_: SecurityException) {}
    }

    override fun dismissSosNotification() {
        notificationManager.cancel(NOTIFICATION_ID_COUNTDOWN)
        notificationManager.cancel(NOTIFICATION_ID_DIALING)
    }
}
