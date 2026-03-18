package com.datainterview.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.datainterview.app.data.dao.EventDao
import com.datainterview.app.data.entity.Event
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UnlockReceiver(
    private val eventDao: EventDao,
    private val scope: CoroutineScope
) : BroadcastReceiver() {

    var activationId: Long = -1L
    private var screenOn = true

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_USER_PRESENT -> {
                // Phone unlocked
                if (activationId != -1L) {
                    val time = SimpleDateFormat("HH:mm", Locale.US).format(Date())
                    scope.launch {
                        eventDao.insert(
                            Event(
                                activationId = activationId,
                                interactionType = "deverrouillage",
                                time = time,
                                appOrWidgetName = null,
                                closeTime = null,
                                widgetLocation = null
                            )
                        )
                    }
                }
            }
            Intent.ACTION_SCREEN_OFF -> screenOn = false
            Intent.ACTION_SCREEN_ON -> screenOn = true
        }
    }
}
