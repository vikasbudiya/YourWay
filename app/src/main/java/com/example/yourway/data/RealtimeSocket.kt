package com.example.yourway.data

import android.util.Log
import com.example.yourway.BuildConfig
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject

class RealtimeSocket {
    private var socket: Socket? = null

    fun connect(
        onRefreshRequested: () -> Unit,
        onNotification: (String, String) -> Unit,
        onSupportMessage: (SupportMessageDto) -> Unit
    ) {
        runCatching {
            socket = IO.socket(BuildConfig.API_BASE_URL).apply {
                on(Socket.EVENT_CONNECT) {
                    Log.d(TAG, "Socket connected")
                }
                on("wallet_updated") { onRefreshRequested() }
                on("support_message_created") { args -> parseSupportMessage(args)?.let(onSupportMessage) }
                on("support_message_replied") { args -> parseSupportMessage(args)?.let(onSupportMessage) }
                on("notification") { args ->
                    val title = args.getOrNull(0)?.toString() ?: "YourWay"
                    val body = args.getOrNull(1)?.toString() ?: "Live update received."
                    if (!isSmsNotification(title, body)) onNotification(title, body)
                }
                connect()
            }
        }.onFailure {
            Log.w(TAG, "Socket unavailable: ${it.message}")
        }
    }

    fun disconnect() {
        socket?.disconnect()
        socket = null
    }

    private fun parseSupportMessage(args: Array<Any>): SupportMessageDto? {
        val payload = args.firstOrNull() as? JSONObject ?: return null
        return SupportMessageDto(
            id = payload.optString("id").takeIf { it.isNotBlank() },
            userId = payload.optString("userId").takeIf { it.isNotBlank() },
            userEmail = payload.optString("userEmail").takeIf { it.isNotBlank() },
            body = payload.optString("body"),
            fromSupport = payload.optBoolean("fromSupport"),
            createdAt = payload.optLong("createdAt", System.currentTimeMillis())
        )
    }

    private fun isSmsNotification(title: String, body: String): Boolean {
        return title.contains("sms", ignoreCase = true) || body.contains("sms", ignoreCase = true)
    }

    private companion object {
        const val TAG = "RealtimeSocket"
    }
}
