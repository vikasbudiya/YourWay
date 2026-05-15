package com.example.yourway

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.telephony.SmsMessage
import android.util.Log
import com.example.yourway.data.SmsSyncClient
import com.example.yourway.data.YourWayRepository

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "android.provider.Telephony.SMS_RECEIVED") return

        val bundle: Bundle = intent.extras ?: return
        val pdus = bundle["pdus"] as? Array<*> ?: return
        val format = bundle.getString("format")

        pdus.forEach { pdu ->
            val bytes = pdu as? ByteArray ?: return@forEach
            val sms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                SmsMessage.createFromPdu(bytes, format)
            } else {
                @Suppress("DEPRECATION")
                SmsMessage.createFromPdu(bytes)
            }

            val sender = sms.originatingAddress ?: "Unknown"
            val message = sms.messageBody ?: ""
            val category = YourWayRepository.categorizeSms(message)
            Log.d(TAG, "Incoming SMS queued for backend sync with category $category")

            Thread {
                runCatching {
                    SmsSyncClient.sync(sender, message)
                }.onSuccess { code ->
                    Log.d(TAG, "Backend SMS sync response: $code")
                }.onFailure { error ->
                    Log.e(TAG, "Backend SMS sync failed: ${error.message}")
                }
            }.start()
        }
    }

    private companion object {
        const val TAG = "SmsReceiver"
    }
}
