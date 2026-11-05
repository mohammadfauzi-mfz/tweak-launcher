package com.fauzi.tweaklauncher

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.compose.runtime.mutableStateMapOf

/** Jumlah notifikasi aktif per aplikasi. Diisi oleh [NotifListener], dibaca oleh ikon. */
object Badges {
    val counts = mutableStateMapOf<String, Int>()
}

class NotifListener : NotificationListenerService() {

    override fun onListenerConnected() {
        recount()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        recount()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        recount()
    }

    override fun onListenerDisconnected() {
        Badges.counts.clear()
    }

    private fun recount() {
        val map = HashMap<String, Int>()
        try {
            val list = activeNotifications ?: emptyArray()
            for (n in list) {
                val f = n.notification.flags
                if (f and Notification.FLAG_ONGOING_EVENT != 0) continue
                if (f and Notification.FLAG_FOREGROUND_SERVICE != 0) continue
                if (f and Notification.FLAG_GROUP_SUMMARY != 0) continue
                if (n.packageName == packageName) continue
                map[n.packageName] = (map[n.packageName] ?: 0) + 1
            }
        } catch (e: Exception) {
        }
        Badges.counts.keys.toList().forEach { if (it !in map) Badges.counts.remove(it) }
        map.forEach { (k, v) -> if (Badges.counts[k] != v) Badges.counts[k] = v }
    }
}
