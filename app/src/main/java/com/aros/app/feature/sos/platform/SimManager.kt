package com.aros.app.feature.sos.platform

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import androidx.core.content.ContextCompat

data class SimAccountInfo(
    val id: String, // String representation of PhoneAccountHandle for storage
    val label: String,
    val slotIndex: Int,
    val isDefault: Boolean,
    val handle: PhoneAccountHandle?
)

interface SimManager {
    fun getAvailableSims(): List<SimAccountInfo>
    fun getDefaultCallingSim(): SimAccountInfo?
    fun getStoredOrResolvedAccount(storedSimId: String?): PhoneAccountHandle?
}

class SimManagerImpl(private val context: Context) : SimManager {

    private val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager

    override fun getAvailableSims(): List<SimAccountInfo> {
        val hasPhoneState = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPhoneState || telecomManager == null) {
            return emptyList()
        }

        return try {
            val accounts: List<PhoneAccountHandle> = telecomManager.callCapablePhoneAccounts ?: emptyList()
            val defaultAccount: PhoneAccountHandle? = try {
                telecomManager.getDefaultOutgoingPhoneAccount("tel")
            } catch (_: Exception) {
                null
            }

            accounts.mapIndexed { index, handle ->
                val phoneAccount = telecomManager.getPhoneAccount(handle)
                val label = phoneAccount?.label?.toString() ?: "SIM ${index + 1}"
                val isDefault = defaultAccount != null && handle == defaultAccount
                SimAccountInfo(
                    id = handle.id ?: "${handle.componentName.flattenToString()}#$index",
                    label = label,
                    slotIndex = index,
                    isDefault = isDefault,
                    handle = handle
                )
            }
        } catch (e: SecurityException) {
            emptyList()
        }
    }

    override fun getDefaultCallingSim(): SimAccountInfo? {
        val sims = getAvailableSims()
        return sims.find { it.isDefault } ?: sims.firstOrNull()
    }

    override fun getStoredOrResolvedAccount(storedSimId: String?): PhoneAccountHandle? {
        val sims = getAvailableSims()
        if (sims.isEmpty()) return null

        if (!storedSimId.isNullOrBlank()) {
            val matched = sims.find { it.id == storedSimId }
            if (matched?.handle != null) {
                return matched.handle
            }
        }

        // Fall back to default calling SIM
        return getDefaultCallingSim()?.handle
    }
}
