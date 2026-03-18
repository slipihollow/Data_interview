package com.datainterview.app.activation

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import com.datainterview.app.BuildConfig
import com.datainterview.app.data.AppDatabase
import com.datainterview.app.data.csv.CsvGenerator
import com.datainterview.app.data.entity.Activation
import com.datainterview.app.service.DataInterviewService
import com.datainterview.app.upload.TelegramUploader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ActivationManager(private val context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val prefs = context.getSharedPreferences("data_interview", Context.MODE_PRIVATE)

    suspend fun startActivation(): Long {
        // Create activation record
        val activation = Activation(startTime = System.currentTimeMillis())
        val id = db.activationDao().insert(activation)

        // Save to SharedPreferences for boot receiver
        prefs.edit().putLong("active_activation_id", id).apply()

        // Start foreground service
        val serviceIntent = Intent(context, DataInterviewService::class.java).apply {
            putExtra(DataInterviewService.EXTRA_ACTIVATION_ID, id)
        }
        ContextCompat.startForegroundService(context, serviceIntent)

        return id
    }

    suspend fun stopActivation() {
        val activation = db.activationDao().getActive() ?: return

        // Stop service
        context.stopService(Intent(context, DataInterviewService::class.java))

        // Clear SharedPreferences
        prefs.edit().remove("active_activation_id").apply()

        // Generate CSV
        val events = db.eventDao().getByActivation(activation.id)
        val csvFile = CsvGenerator(context).generate(events, activation.id)

        // Update activation record
        val eventCount = db.eventDao().countByActivation(activation.id)
        db.activationDao().update(
            activation.copy(
                endTime = System.currentTimeMillis(),
                status = Activation.STATUS_COMPLETED,
                csvFilePath = csvFile.absolutePath,
                eventCount = eventCount,
                uploadStatus = "pending"
            )
        )

        // Auto-upload via Telegram (credentials baked in at build time)
        val token = BuildConfig.TELEGRAM_BOT_TOKEN
        val chatId = BuildConfig.TELEGRAM_CHAT_ID
        if (token.isNotEmpty() && chatId.isNotEmpty()) {
            val success = withContext(Dispatchers.IO) {
                TelegramUploader(token, chatId).upload(csvFile)
            }
            db.activationDao().update(
                db.activationDao().getById(activation.id)!!.copy(
                    uploadStatus = if (success) "success" else "failed"
                )
            )
        }
    }

    suspend fun scheduleActivation(startTime: Long, endTime: Long): Long {
        val activation = Activation(
            startTime = startTime,
            scheduledStart = startTime,
            scheduledEnd = endTime,
            status = Activation.STATUS_SCHEDULED
        )
        val id = db.activationDao().insert(activation)

        // Schedule alarms
        scheduleAlarm(id, startTime, ACTION_START)
        scheduleAlarm(id, endTime, ACTION_STOP)

        return id
    }

    private fun scheduleAlarm(activationId: Long, triggerTime: Long, action: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            this.action = action
            putExtra(EXTRA_ACTIVATION_ID, activationId)
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val requestCode = (activationId * 10 + if (action == ACTION_START) 1 else 2).toInt()
        val pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, flags)

        // Use exact alarm with backward compat
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }
    }

    suspend fun getActiveActivation(): Activation? = db.activationDao().getActive()

    suspend fun getAllActivations(): List<Activation> = db.activationDao().getAll()

    suspend fun getActivation(id: Long): Activation? = db.activationDao().getById(id)

    companion object {
        const val ACTION_START = "com.datainterview.app.ACTION_START_ACTIVATION"
        const val ACTION_STOP = "com.datainterview.app.ACTION_STOP_ACTIVATION"
        const val EXTRA_ACTIVATION_ID = "activation_id"
    }
}
