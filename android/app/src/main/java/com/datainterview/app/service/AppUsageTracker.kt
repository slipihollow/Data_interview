package com.datainterview.app.service

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.datainterview.app.data.dao.EventDao
import com.datainterview.app.data.entity.Event
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AppUsageTracker(
    private val context: Context,
    private val eventDao: EventDao,
    private val scope: CoroutineScope
) {
    private val handler = Handler(Looper.getMainLooper())
    private var activationId: Long = -1L
    private var running = false
    private var lastForegroundApp: String? = null
    private var lastForegroundTime: Long = 0
    private var lastPollTime: Long = System.currentTimeMillis()

    companion object {
        private const val POLL_INTERVAL_MS = 5000L // 5 seconds
    }

    private val pollRunnable = object : Runnable {
        override fun run() {
            if (!running) return
            pollUsageEvents()
            handler.postDelayed(this, POLL_INTERVAL_MS)
        }
    }

    fun start(activationId: Long) {
        this.activationId = activationId
        this.running = true
        this.lastPollTime = System.currentTimeMillis()
        handler.post(pollRunnable)
    }

    fun stop() {
        running = false
        handler.removeCallbacks(pollRunnable)
        // Log close event for currently foreground app
        lastForegroundApp?.let { app ->
            logAppClose(app, lastForegroundTime)
        }
        lastForegroundApp = null
    }

    private fun pollUsageEvents() {
        val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                ?: return

        val now = System.currentTimeMillis()
        val events = usageStatsManager.queryEvents(lastPollTime, now)
        lastPollTime = now

        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)

            // Use ACTIVITY_RESUMED/PAUSED on API 29+, MOVE_TO_FOREGROUND/BACKGROUND on older APIs
            val isForeground = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                event.eventType == UsageEvents.Event.ACTIVITY_RESUMED
            } else {
                @Suppress("DEPRECATION")
                event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND
            }

            val isBackground = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                event.eventType == UsageEvents.Event.ACTIVITY_PAUSED
            } else {
                @Suppress("DEPRECATION")
                event.eventType == UsageEvents.Event.MOVE_TO_BACKGROUND
            }

            if (isForeground) {
                val packageName = event.packageName
                if (packageName != lastForegroundApp) {
                    // Close previous app
                    lastForegroundApp?.let { prevApp ->
                        logAppClose(prevApp, lastForegroundTime)
                    }
                    // Open new app
                    lastForegroundApp = packageName
                    lastForegroundTime = event.timeStamp
                    logAppOpen(packageName, event.timeStamp)
                }
            } else if (isBackground) {
                val packageName = event.packageName
                if (packageName == lastForegroundApp) {
                    logAppClose(packageName, lastForegroundTime)
                    lastForegroundApp = null
                }
            }
        }
    }

    private fun logAppOpen(packageName: String, timestamp: Long) {
        val appName = getAppLabel(packageName)
        val time = formatTime(timestamp)
        scope.launch {
            eventDao.insert(
                Event(
                    activationId = activationId,
                    interactionType = "application",
                    time = time,
                    appOrWidgetName = appName,
                    closeTime = null, // will be updated on close
                    widgetLocation = null
                )
            )
        }
    }

    private fun logAppClose(packageName: String, openTimestamp: Long) {
        val appName = getAppLabel(packageName)
        val openTime = formatTime(openTimestamp)
        val closeTime = formatTime(System.currentTimeMillis())
        scope.launch {
            eventDao.insert(
                Event(
                    activationId = activationId,
                    interactionType = "application",
                    time = openTime,
                    appOrWidgetName = appName,
                    closeTime = closeTime,
                    widgetLocation = null
                )
            )
        }
    }

    private fun getAppLabel(packageName: String): String {
        return try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (_: Exception) {
            packageName
        }
    }

    private fun formatTime(millis: Long): String {
        return SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(millis))
    }
}
