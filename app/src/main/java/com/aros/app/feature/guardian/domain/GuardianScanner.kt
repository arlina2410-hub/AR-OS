package com.aros.app.feature.guardian.domain

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.aros.app.feature.guardian.data.GuardianDao
import com.aros.app.feature.guardian.data.GuardianScanEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

data class GuardianAuditReport(
    val sovereigntyScore: Int,
    val totalAppsAudited: Int,
    val cameraAppsCount: Int,
    val micAppsCount: Int,
    val locationAppsCount: Int,
    val highRiskAppsCount: Int,
    val sampleHighRiskApps: List<String>,
    val timestamp: Long = System.currentTimeMillis()
)

class GuardianScanner(
    private val context: Context,
    private val dao: GuardianDao,
    private val ioDispatcher: CoroutineDispatcher
) {
    val latestScanFlow: Flow<GuardianScanEntity?> = dao.getLatestScanFlow()

    suspend fun performAudit(): GuardianAuditReport = withContext(ioDispatcher) {
        val pm = context.packageManager
        val packages = try {
            pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)
        } catch (_: Exception) {
            emptyList()
        }

        var cameraCount = 0
        var micCount = 0
        var locationCount = 0
        val highRiskApps = mutableListOf<String>()

        for (pkg in packages) {
            // Focus primarily on user-installed third party apps
            val isSystem = (pkg.applicationInfo?.flags ?: 0) and ApplicationInfo.FLAG_SYSTEM != 0
            if (isSystem && pkg.packageName != "com.google.android.gms") continue

            val perms = pkg.requestedPermissions ?: continue
            val hasCamera = perms.contains(android.Manifest.permission.CAMERA)
            val hasMic = perms.contains(android.Manifest.permission.RECORD_AUDIO)
            val hasLocation = perms.contains(android.Manifest.permission.ACCESS_FINE_LOCATION) ||
                    perms.contains(android.Manifest.permission.ACCESS_COARSE_LOCATION)

            if (hasCamera) cameraCount++
            if (hasMic) micCount++
            if (hasLocation) locationCount++

            if (hasCamera && hasMic) {
                val label = pkg.applicationInfo?.loadLabel(pm)?.toString() ?: pkg.packageName
                highRiskApps.add(label)
            }
        }

        // Calculate Sovereignty Score (0 to 100)
        // Deduct points for apps holding both Camera & Mic, plus background location
        val penalty = (highRiskApps.size * 6) + (locationCount * 2)
        val score = (100 - penalty).coerceIn(40, 100)

        val entity = GuardianScanEntity(
            timestamp = System.currentTimeMillis(),
            sovereigntyScore = score,
            cameraAppsCount = cameraCount,
            micAppsCount = micCount,
            locationAppsCount = locationCount,
            highRiskAppsCount = highRiskApps.size
        )
        dao.insert(entity)

        GuardianAuditReport(
            sovereigntyScore = score,
            totalAppsAudited = packages.size,
            cameraAppsCount = cameraCount,
            micAppsCount = micCount,
            locationAppsCount = locationCount,
            highRiskAppsCount = highRiskApps.size,
            sampleHighRiskApps = highRiskApps.take(4)
        )
    }

    suspend fun runAudit(): GuardianAuditReport = performAudit()
}
