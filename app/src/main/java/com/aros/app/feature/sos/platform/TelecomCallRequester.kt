package com.aros.app.feature.sos.platform

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.telecom.TelecomManager
import androidx.core.content.ContextCompat
import com.aros.app.feature.sos.domain.CallRequester

class TelecomCallRequester(
    private val context: Context,
    private val simManager: SimManager
) : CallRequester {

    override suspend fun requestCall(
        phoneNumber: String,
        isTestMode: Boolean,
        startCallOnSpeaker: Boolean
    ): CallRequester.CallResult {
        if (isTestMode) {
            return CallRequester.CallResult.PlacedDirectly
        }

        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            return CallRequester.CallResult.Failed("CALL_PHONE permission is not granted.")
        }

        val uri = Uri.parse("tel:${Uri.encode(phoneNumber)}")
        val phoneAccount = simManager.getDefaultCallingSim()?.handle

        // Tier 1: TelecomManager.placeCall
        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
        if (telecomManager != null) {
            try {
                val extras = Bundle().apply {
                    if (startCallOnSpeaker) {
                        putBoolean(TelecomManager.EXTRA_START_CALL_WITH_SPEAKERPHONE, true)
                    }
                    if (phoneAccount != null) {
                        putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, phoneAccount)
                    }
                }
                telecomManager.placeCall(uri, extras)
                return CallRequester.CallResult.PlacedDirectly
            } catch (e: Exception) {
                // Fall through to Tier 2
            }
        }

        // Tier 2: Intent.ACTION_CALL
        return try {
            val callIntent = Intent(Intent.ACTION_CALL, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                if (phoneAccount != null) {
                    putExtra(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, phoneAccount)
                }
            }
            context.startActivity(callIntent)
            CallRequester.CallResult.DialFallback("Placed via Intent.ACTION_CALL fallback")
        } catch (e: Exception) {
            CallRequester.CallResult.Failed(e.message ?: "Unable to launch call intent")
        }
    }
}
