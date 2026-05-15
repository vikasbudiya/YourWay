package com.example.yourway.data

import com.example.yourway.BuildConfig
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object SmsSyncClient {
    fun sync(sender: String, message: String): Int {
        val url = URL("${BuildConfig.API_BASE_URL.trimEnd('/')}/sms")
        val body = JSONObject()
            .put("sender", sender)
            .put("message", message)
            .put("receivedAt", System.currentTimeMillis())
            .put("category", YourWayRepository.categorizeSms(message).name)
            .toString()

        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 5000
            readTimeout = 5000
            setRequestProperty("Content-Type", "application/json")
        }

        return connection.use { conn ->
            conn.outputStream.use { stream -> stream.write(body.toByteArray()) }
            conn.responseCode
        }
    }

    private inline fun <T : HttpURLConnection, R> T.use(block: (T) -> R): R {
        return try {
            block(this)
        } finally {
            disconnect()
        }
    }
}
