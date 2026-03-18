package com.datainterview.app.service

import android.content.ComponentName
import android.content.Context
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import com.datainterview.app.data.dao.EventDao
import com.datainterview.app.data.entity.Event
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MediaTracker private constructor(
    private val context: Context,
    private val eventDao: EventDao,
    private val scope: CoroutineScope
) {
    private var activationId: Long = -1L
    private val sessionManager: MediaSessionManager =
        context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
    private val controllers = mutableMapOf<MediaSession.Token, Pair<MediaController, MediaController.Callback>>()
    private val listenerComponent =
        ComponentName(context, MediaNotificationListenerService::class.java)

    companion object {
        /**
         * Creates a MediaTracker if MediaSessionManager is available (API 21+).
         * Returns null if initialization fails.
         */
        fun createIfAvailable(
            context: Context,
            eventDao: EventDao,
            scope: CoroutineScope
        ): MediaTracker? {
            return try {
                MediaTracker(context, eventDao, scope)
            } catch (_: Exception) {
                null
            }
        }
    }

    private val sessionsChangedListener =
        MediaSessionManager.OnActiveSessionsChangedListener { sessionControllers ->
            updateSessionCallbacks(sessionControllers ?: emptyList())
        }

    fun start(activationId: Long) {
        this.activationId = activationId
        try {
            val activeControllers = sessionManager.getActiveSessions(listenerComponent)
            updateSessionCallbacks(activeControllers)
            sessionManager.addOnActiveSessionsChangedListener(
                sessionsChangedListener,
                listenerComponent
            )
        } catch (_: SecurityException) {
            // NotificationListenerService permission not granted
        }
    }

    fun stop() {
        try {
            sessionManager.removeOnActiveSessionsChangedListener(sessionsChangedListener)
        } catch (_: Exception) {
        }
        // Unregister all callbacks
        controllers.forEach { (_, pair) ->
            val (controller, callback) = pair
            controller.unregisterCallback(callback)
        }
        controllers.clear()
    }

    private fun updateSessionCallbacks(activeControllers: List<MediaController>) {
        // Determine which tokens are currently active
        val activeTokens = activeControllers.map { it.sessionToken }.toSet()

        // Remove callbacks for sessions that are no longer active
        val tokensToRemove = controllers.keys.filter { it !in activeTokens }
        for (token in tokensToRemove) {
            controllers[token]?.let { (controller, callback) ->
                controller.unregisterCallback(callback)
            }
            controllers.remove(token)
        }

        // Register callbacks for new sessions
        for (controller in activeControllers) {
            if (controller.sessionToken !in controllers) {
                val callback = createCallback(controller)
                controllers[controller.sessionToken] = Pair(controller, callback)
                controller.registerCallback(callback)
            }
        }
    }

    private fun createCallback(controller: MediaController): MediaController.Callback {
        return object : MediaController.Callback() {
            override fun onPlaybackStateChanged(state: PlaybackState?) {
                state ?: return
                val action = when (state.state) {
                    PlaybackState.STATE_PLAYING -> "lecture"
                    PlaybackState.STATE_PAUSED -> "pause"
                    PlaybackState.STATE_SKIPPING_TO_NEXT -> "suivant"
                    PlaybackState.STATE_SKIPPING_TO_PREVIOUS -> "precedent"
                    else -> return
                }
                val appName =
                    controller.packageName?.let { getAppLabel(it) } ?: "inconnu"
                val time = SimpleDateFormat("HH:mm", Locale.US).format(Date())

                // Media controls on the lock screen
                val location = "ecran_verrouillage"

                scope.launch {
                    eventDao.insert(
                        Event(
                            activationId = activationId,
                            interactionType = "widget",
                            time = time,
                            appOrWidgetName = "$appName ($action)",
                            closeTime = null,
                            widgetLocation = location
                        )
                    )
                }
            }
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
}
