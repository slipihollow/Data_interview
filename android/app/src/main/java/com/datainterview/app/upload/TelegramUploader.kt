package com.datainterview.app.upload

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.util.concurrent.TimeUnit

class TelegramUploader(
    private val botToken: String,
    private val chatId: String
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Upload CSV file to Telegram chat via Bot API.
     * Returns true on success, false on failure.
     * Must be called from a background thread.
     */
    fun upload(file: File, caption: String? = null): Boolean {
        val url = "https://api.telegram.org/bot$botToken/sendDocument"

        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("chat_id", chatId)
            .addFormDataPart(
                "document",
                file.name,
                val contentType = if (file.extension == "enc") "application/octet-stream" else "text/csv"
                file.asRequestBody(contentType.toMediaType())
            )
            .apply {
                if (caption != null) {
                    addFormDataPart("caption", caption)
                }
            }
            .build()

        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        return try {
            val response = client.newCall(request).execute()
            response.use { it.isSuccessful }
        } catch (_: Exception) {
            false
        }
    }
}
