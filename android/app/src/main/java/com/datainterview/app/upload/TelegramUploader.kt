package com.datainterview.app.upload

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import android.util.Log
import java.io.File
import java.util.concurrent.TimeUnit

class TelegramUploader(
    private val botToken: String,
    private val chatId: String
) {
    companion object {
        private const val TAG = "TelegramUploader"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Upload file to Telegram chat via Bot API.
     * Returns null on success, or an error message on failure.
     * Must be called from a background thread.
     */
    fun upload(file: File, caption: String? = null): String? {
        val url = "https://api.telegram.org/bot$botToken/sendDocument"

        val contentType = if (file.extension == "enc") "application/octet-stream" else "text/csv"

        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("chat_id", chatId)
            .addFormDataPart(
                "document",
                file.name,
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
            Log.d(TAG, "Uploading ${file.name} (${file.length()} bytes) to chat $chatId")
            val response = client.newCall(request).execute()
            response.use {
                if (it.isSuccessful) {
                    Log.d(TAG, "Upload successful: ${it.code}")
                    null
                } else {
                    val errorBody = it.body?.string() ?: "no body"
                    Log.e(TAG, "Upload failed: HTTP ${it.code} — $errorBody")
                    "HTTP ${it.code}: $errorBody"
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Upload exception", e)
            e.message ?: e.javaClass.simpleName
        }
    }
}
