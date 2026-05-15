package com.example.yourway.data

import android.util.Log
import com.example.yourway.BuildConfig
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object SmsSyncClient {
    private const val TAG = "SmsSyncClient"
    private const val MAX_RETRIES = 3

    fun sync(sender: String, message: String): Int {
        val urlStr = "${BuildConfig.API_BASE_URL.trimEnd('/')}/sms"
        Log.d(TAG, "Syncing SMS from $sender to $urlStr")

        val body = JSONObject()
            .put("sender", sender)
            .put("message", message)
            .put("receivedAt", System.currentTimeMillis())
            .put("category", YourWayRepository.categorizeSms(message).name)
            .toString()

        var lastException: Exception? = null

        repeat(MAX_RETRIES) { attempt ->
            try {
                val url = URL(urlStr)
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    doOutput = true
                    connectTimeout = 60000
                    readTimeout = 60000
                    setRequestProperty("Content-Type", "application/json")
                }

                val code = connection.use { conn ->
                    conn.outputStream.use { stream -> stream.write(body.toByteArray()) }
                    conn.responseCode
                }
                Log.d(TAG, "SMS sync success on attempt ${attempt + 1}, response code: $code")
                return code
            } catch (e: Exception) {
                lastException = e
                Log.w(TAG, "SMS sync attempt ${attempt + 1} failed: ${e.message}")
                if (attempt < MAX_RETRIES - 1) {
                    Thread.sleep(2000L * (attempt + 1))
                }
            }
        }

        throw lastException ?: RuntimeException("SMS sync failed after $MAX_RETRIES retries")
    }

    private inline fun <T : HttpURLConnection, R> T.use(block: (T) -> R): R {
        return try {
            block(this)
        } finally {
            disconnect()
        }
    }
}
