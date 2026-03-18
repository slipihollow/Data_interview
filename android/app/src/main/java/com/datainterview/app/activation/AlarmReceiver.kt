package com.datainterview.app.activation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val activationId = intent.getLongExtra(ActivationManager.EXTRA_ACTIVATION_ID, -1L)
        if (activationId == -1L) return

        val manager = ActivationManager(context)

        when (intent.action) {
            ActivationManager.ACTION_START -> {
                CoroutineScope(Dispatchers.IO).launch {
                    manager.startActivation()
                }
            }
            ActivationManager.ACTION_STOP -> {
                CoroutineScope(Dispatchers.IO).launch {
                    manager.stopActivation()
                }
            }
        }
    }
}
