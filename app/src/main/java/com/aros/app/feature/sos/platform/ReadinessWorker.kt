package com.aros.app.feature.sos.platform

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.aros.app.ArOsApplication
import com.aros.app.feature.sos.domain.OverallReadinessStatus
import com.aros.app.feature.sos.domain.SosEventType
import java.util.concurrent.TimeUnit

class ReadinessWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? ArOsApplication ?: return Result.success()
        val container = app.container

        val report = container.readinessChecker.checkReadiness()
        if (report.overallStatus == OverallReadinessStatus.NEEDS_ATTENTION) {
            val failing = report.checks.filter { !it.isPassing }.map { it.title }
            container.notifier.showReadinessWarning(failing)

            container.eventLog.logEvent(
                type = SosEventType.READINESS_WARNING,
                mode = report.mode,
                contactName = null,
                deviceLocked = container.deviceState.isDeviceLocked(),
                screenInteractive = container.deviceState.isScreenInteractive(),
                detail = "Readiness issues: ${failing.joinToString(", ")}"
            )
        }

        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "ar_sos_readiness_audit"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<ReadinessWorker>(15, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
