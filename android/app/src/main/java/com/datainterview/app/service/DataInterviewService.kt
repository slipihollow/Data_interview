package com.datainterview.app.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.datainterview.app.DataInterviewApp
import com.datainterview.app.R
import com.datainterview.app.data.AppDatabase
import com.datainterview.app.ui.main.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class DataInterviewService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var unlockReceiver: UnlockReceiver
    private lateinit var appUsageTracker: AppUsageTracker
    private var mediaTracker: MediaTracker? = null

    companion object {
        const val NOTIFICATION_ID = 1
        const val EXTRA_ACTIVATION_ID = "activation_id"
    }

    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.getInstance(this)

        // Initialize trackers
        unlockReceiver = UnlockReceiver(db.eventDao(), serviceScope)
        appUsageTracker = AppUsageTracker(this, db.eventDao(), serviceScope)
        mediaTracker = MediaTracker.createIfAvailable(this, db.eventDao(), serviceScope)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val activationId = intent?.getLongExtra(EXTRA_ACTIVATION_ID, -1L) ?: -1L
        if (activationId == -1L) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, createNotification())

        // Register unlock receiver
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        registerReceiver(unlockReceiver, filter)

        // Start trackers
        unlockReceiver.activationId = activationId
        appUsageTracker.start(activationId)
        mediaTracker?.start(activationId)

        return START_STICKY
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(unlockReceiver)
        } catch (_: Exception) {
        }
        appUsageTracker.stop()
        mediaTracker?.stop()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            pendingIntentFlags()
        )

        return NotificationCompat.Builder(this, DataInterviewApp.CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.notification_tracking))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    /**
     * Returns the appropriate PendingIntent flags for the current API level.
     * FLAG_IMMUTABLE is only available from API 23 (M) onwards.
     */
    private fun pendingIntentFlags(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
    }
}
