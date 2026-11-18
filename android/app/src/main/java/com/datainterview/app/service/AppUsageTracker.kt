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
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
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
    private var lastPollTime: Long = System.currentTimeMillis()

    /** Row id of the currently open "application" event, awaited when it closes. */
    private var openEventId: Deferred<Long>? = null

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
        // Complete the row for the app still in foreground. No usage event exists
        // here, so "now" is the correct close time: collection ends at this moment.
        if (lastForegroundApp != null) {
            logAppClose(System.currentTimeMillis())
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
                    // Close previous app: it left the foreground when this one entered
                    if (lastForegroundApp != null) {
                        logAppClose(event.timeStamp)
                    }
                    // Open new app
                    lastForegroundApp = packageName
                    logAppOpen(packageName, event.timeStamp)
                }
            } else if (isBackground) {
                val packageName = event.packageName
                if (packageName == lastForegroundApp) {
                    logAppClose(event.timeStamp)
                    lastForegroundApp = null
                }
            }
        }
    }

    private fun logAppOpen(packageName: String, timestamp: Long) {
        val appName = getAppLabel(packageName)
        val time = formatTime(timestamp)
        openEventId = scope.async {
            eventDao.insert(
                Event(
                    activationId = activationId,
                    interactionType = "application",
                    time = time,
                    appOrWidgetName = appName,
                    closeTime = null, // completed by logAppClose
                    widgetLocation = null
                )
            )
        }
    }

    /**
     * Completes the open row for the current foreground app. [timestamp] must be
     * the time the app actually left the foreground (the usage event's own
     * timestamp), not the poll time — polling runs every 5s, so using the poll
     * time would push every close up to 5s late and overlap the next row.
     *
     * Does nothing if no open row exists (e.g. the app was already in foreground
     * when collection started), so no orphan row is created.
     */
    private fun logAppClose(timestamp: Long) {
        val pending = openEventId ?: return
        openEventId = null
        val closeTime = formatTime(timestamp)
        scope.launch {
            eventDao.updateCloseTime(pending.await(), closeTime)
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
