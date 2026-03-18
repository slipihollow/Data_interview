package com.datainterview.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

/**
 * Restarts the tracking foreground service after the device reboots,
 * provided there was an active activation when the device shut down.
 *
 * The active activation ID is persisted in SharedPreferences so it
 * survives the reboot.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_LOCKED_BOOT_COMPLETED
        ) return

        // Check SharedPreferences for an active activation
        val prefs = context.getSharedPreferences("data_interview", Context.MODE_PRIVATE)
        val activeActivationId = prefs.getLong("active_activation_id", -1L)

        if (activeActivationId != -1L) {
            // Restart the tracking service
            val serviceIntent = Intent(context, DataInterviewService::class.java).apply {
                putExtra(DataInterviewService.EXTRA_ACTIVATION_ID, activeActivationId)
            }
            // ContextCompat.startForegroundService handles the API level split:
            // API 26+ calls startForegroundService(), older APIs call startService().
            ContextCompat.startForegroundService(context, serviceIntent)
        }
    }
}
