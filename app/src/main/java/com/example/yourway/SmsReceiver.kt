package com.example.yourway

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.telephony.SmsMessage
import android.util.Log
import java.net.HttpURLConnection
import java.net.URL

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        if (intent.action == "android.provider.Telephony.SMS_RECEIVED") {

            val bundle: Bundle? = intent.extras

            if (bundle != null) {
                val pdus = bundle["pdus"] as Array<*>
                val format = bundle.getString("format")

                for (pdu in pdus) {

                    val sms: SmsMessage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        SmsMessage.createFromPdu(pdu as ByteArray, format)
                    } else {
                        SmsMessage.createFromPdu(pdu as ByteArray)
                    }

                    val sender = sms.originatingAddress ?: "Unknown"
                    val message = sms.messageBody ?: ""

                    // 🔍 Debug Log
                    Log.d("SMS_DEBUG", "From: $sender | Message: $message")

                    // 🌐 Send to server
                    sendToServer(sender, message)
                }
            }
        }
    }

    private fun sendToServer(sender: String, message: String) {

        Thread {
            try {
                // ⚠️ IMPORTANT: Replace with your laptop IP + port
                val url = URL("https://sms-server-gk6u.onrender.com/sms")

                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.connectTimeout = 5000
                conn.readTimeout = 5000

                conn.setRequestProperty("Content-Type", "application/json")

                val json = """
                    {
                        "sender": "$sender",
                        "message": "$message"
                    }
                """.trimIndent()

                conn.outputStream.use {
                    it.write(json.toByteArray())
                }

                val responseCode = conn.responseCode

                Log.d("SERVER_RESPONSE", "Response Code: $responseCode")

                conn.disconnect()

            } catch (e: Exception) {
                Log.e("SERVER_ERROR", "Failed to send SMS: ${e.message}")
            }
        }.start()
    }
}