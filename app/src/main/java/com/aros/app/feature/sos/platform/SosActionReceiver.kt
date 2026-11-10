package com.aros.app.feature.sos.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.aros.app.ArOsApplication

class SosActionReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_CANCEL = "com.aros.app.ACTION_CANCEL_SOS"
        const val ACTION_CALL_NOW = "com.aros.app.ACTION_CALL_NOW"
        const val ACTION_START_SOS = "com.aros.app.ACTION_START_SOS"
        const val ACTION_CALL_CONTACT = "com.aros.app.ACTION_CALL_CONTACT"
        const val ACTION_RETRY_DIAL = "com.aros.app.ACTION_RETRY_DIAL"

        const val EXTRA_CONTACT_ID = "extra_contact_id"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        val app = context?.applicationContext as? ArOsApplication ?: return
        val controller = app.container.sosController

        when (intent?.action) {
            ACTION_CANCEL -> {
                controller.cancelSos()
            }
            ACTION_CALL_NOW -> {
                controller.callNow()
            }
            ACTION_START_SOS -> {
                controller.onTrigger()
            }
            ACTION_CALL_CONTACT -> {
                val contactId = intent.getLongExtra(EXTRA_CONTACT_ID, -1L)
                if (contactId != -1L) {
                    controller.dialContactById(contactId)
                }
            }
            ACTION_RETRY_DIAL -> {
                controller.callNow()
            }
        }
    }
}
