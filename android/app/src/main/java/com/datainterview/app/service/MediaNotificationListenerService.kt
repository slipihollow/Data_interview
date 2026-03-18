package com.datainterview.app.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * NotificationListenerService required by MediaSessionManager.getActiveSessions().
 *
 * This service must be declared in the manifest with the
 * BIND_NOTIFICATION_LISTENER_SERVICE permission. The user must also grant
 * notification access in Settings for getActiveSessions() to work.
 *
 * We do not process notifications ourselves; this class exists solely to
 * satisfy the system requirement.
 */
class MediaNotificationListenerService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        // No-op
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // No-op
    }
}
