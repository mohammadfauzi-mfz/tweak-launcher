package com.fauzi.tweaklauncher

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.net.TrafficStats
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

/**
 * Layanan latar: overlay monitor, profil otomatis per aplikasi, dan jadwal profil.
 * Berhenti sendiri jika semua fitur itu dimatikan.
 */
class TweakService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null
    private lateinit var cfg: Cfg
    private var overlay: TextView? = null
    private var overlayKey = ""
    private var lastRx = -1L
    private var lastTx = -1L
    private var lastT = 0L
    private var chgRegistered = false
    private var lastPlug = -1
    private var lastPct = -1
    private var announced = -1

    private val chgReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            val lv = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val sc = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
            if (lv < 0 || sc <= 0) return
            val pct = lv * 100 / sc
            val plug = i.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
            val first = lastPlug < 0
            val before = lastPlug
            val beforePct = lastPct
            lastPlug = plug
            lastPct = pct
            if (!cfg.chgOn) return
            if (plug == 0) {
                announced = -1
                return
            }
            fun say(t: String) = ChargeVoice.speak(this@TweakService, t.replace("{n}", pct.toString()), cfg.chgLang)
            if (first) {
                announced = pct
            } else if (before == 0) {
                announced = pct
                if (cfg.chgStart && cfg.chgMsgStart.isNotBlank()) say(cfg.chgMsgStart)
            } else if (pct != announced && pct >= beforePct) {
                val levels = cfg.chgLevels.split(",").mapNotNull { it.trim().toIntOrNull() }
                if (pct in levels) {
                    announced = pct
                    say(if (pct >= 100) cfg.chgMsgFull else cfg.chgMsgLevel)
                }
            }
        }
    }

    private fun speed(bytesPerSec: Double): String = when {
        bytesPerSec >= 1_048_576 -> String.format(java.util.Locale.US, "%.1f MB/s", bytesPerSec / 1_048_576)
        bytesPerSec >= 1024 -> String.format(java.util.Locale.US, "%.0f KB/s", bytesPerSec / 1024)
        else -> String.format(java.util.Locale.US, "%.0f B/s", bytesPerSec)
    }

    /** Kecepatan unduh dan unggah sejak pembacaan sebelumnya. */
    private fun netText(): String {
        val rx = TrafficStats.getTotalRxBytes()
        val tx = TrafficStats.getTotalTxBytes()
        val now = System.currentTimeMillis()
        if (rx < 0 || tx < 0) return "↓ - ↑ -"
        val out = if (lastRx < 0 || now <= lastT) "↓ 0 B/s  ↑ 0 B/s" else {
            val dt = (now - lastT) / 1000.0
            "↓ " + speed(((rx - lastRx).coerceAtLeast(0)) / dt) + "  ↑ " + speed(((tx - lastTx).coerceAtLeast(0)) / dt)
        }
        lastRx = rx
        lastTx = tx
        lastT = now
        return out
    }
    private var fgPkg: String? = null
    private var lastEvent = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        cfg = Cfg(this)
        lastEvent = System.currentTimeMillis() - 600_000L
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL, "Layanan Tweak Launcher", NotificationManager.IMPORTANCE_LOW))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val pi = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE
        )
        val n = android.app.Notification.Builder(this, CHANNEL)
            .setContentTitle("Tweak Launcher aktif")
            .setContentText("Overlay dan profil otomatis berjalan")
            .setSmallIcon(R.drawable.ic_stat)
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NID, n)
        }
        if (!chgRegistered) {
            try {
                ContextCompat.registerReceiver(this, chgReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED)
                chgRegistered = true
            } catch (e: Exception) {
            }
        }
        if (job == null) {
            job = scope.launch {
                while (isActive) {
                    cfg.reload()
                    if (!cfg.bgNeeded()) {
                        withContext(Dispatchers.Main) { removeOverlay() }
                        stopSelf()
                        return@launch
                    }
                    try {
                        autoProfile()
                    } catch (e: Exception) {
                    }
                    withContext(Dispatchers.Main) { syncOverlay() }
                    delay(2000)
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        if (chgRegistered) {
            try {
                unregisterReceiver(chgReceiver)
            } catch (e: Exception) {
            }
            chgRegistered = false
        }
        job?.cancel()
        scope.cancel()
        removeOverlay()
        super.onDestroy()
    }

    // ---------------- Profil otomatis ----------------

    private fun autoProfile() {
        if (!cfg.autoApp && !cfg.autoSched) return
        if (Shell.state() != ShizukuState.READY) return
        val profiles = ProfileStore.parse(cfg.profilesJson)

        var target: String? = null
        if (cfg.autoApp) {
            pollForeground()
            fgPkg?.let { target = cfg.ruleFor(it) }
        }
        if (target == null && cfg.autoSched && inSchedule()) target = cfg.schedProfile
        if (target == null) target = cfg.baseProfile

        val wanted = target
        if (wanted != null && wanted != cfg.applied) {
            profiles.firstOrNull { it.id == wanted }?.let { Tweaks.apply(cfg, it) }
        }
    }

    private fun pollForeground() {
        val usm = getSystemService(UsageStatsManager::class.java)
        val now = System.currentTimeMillis()
        val ev = usm.queryEvents(lastEvent, now)
        val e = UsageEvents.Event()
        while (ev.hasNextEvent()) {
            ev.getNextEvent(e)
            when (e.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> fgPkg = e.packageName
                UsageEvents.Event.ACTIVITY_PAUSED -> if (fgPkg == e.packageName) fgPkg = null
            }
        }
        lastEvent = now - 1000L
    }

    private fun inSchedule(): Boolean {
        val c = Calendar.getInstance()
        val m = c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE)
        val s = cfg.schedStart
        val e = cfg.schedEnd
        return if (s <= e) m in s until e else (m >= s || m < e)
    }

    // ---------------- Overlay ----------------

    private fun syncOverlay() {
        if (!cfg.overlayOn || !Settings.canDrawOverlays(this)) {
            removeOverlay()
            return
        }
        val st = Sys.stats(this)
        val items = cfg.overlayItems.split(",").filter { it.isNotBlank() }.toSet()
        val parts = ArrayList<String>()
        if ("clock" in items) parts.add(java.text.SimpleDateFormat("HH:mm", java.util.Locale.US).format(java.util.Date()))
        if ("hz" in items) parts.add("${st.hz} Hz")
        if ("ram" in items) parts.add("RAM ${(st.ramPct * 100).toInt()}%")
        if ("temp" in items) parts.add(st.tempText)
        if ("bat" in items) parts.add("${st.battery}%")
        if ("storage" in items) parts.add("Disk ${(st.storagePct * 100).toInt()}%")
        var text = parts.joinToString("  •  ")
        if ("net" in items) {
            val n = netText()
            text = if (text.isEmpty()) n else text + "\n" + n
        }
        if (text.isEmpty()) {
            removeOverlay()
            return
        }
        val key = cfg.overlayPos
        if (overlay == null || overlayKey != key) {
            removeOverlay()
            addOverlay(key)
        }
        overlay?.text = text
        (overlay?.background as? GradientDrawable)?.alpha = cfg.overlayAlpha * 255 / 100
    }

    private fun addOverlay(pos: String) {
        val wm = getSystemService(WindowManager::class.java)
        val tv = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 11f
            typeface = Typeface.MONOSPACE
            setPadding(24, 10, 24, 10)
            background = GradientDrawable().apply {
                setColor(Color.BLACK)
                cornerRadius = 40f
            }
        }
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = when (pos) {
                "topleft" -> Gravity.TOP or Gravity.START
                "botleft" -> Gravity.BOTTOM or Gravity.START
                "botright" -> Gravity.BOTTOM or Gravity.END
                else -> Gravity.TOP or Gravity.END
            }
            x = 24
            y = 120
        }
        try {
            wm.addView(tv, lp)
            overlay = tv
            overlayKey = pos
        } catch (e: Exception) {
            overlay = null
        }
    }

    private fun removeOverlay() {
        val v = overlay ?: return
        try {
            getSystemService(WindowManager::class.java).removeView(v)
        } catch (e: Exception) {
        }
        overlay = null
    }

    companion object {
        private const val CHANNEL = "tweak_service"
        private const val NID = 42
    }
}

/** Menyalakan kembali layanan setelah HP restart, jika ada fitur latar yang aktif. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        try {
            val cfg = Cfg(context)
            if (cfg.bgNeeded()) {
                ContextCompat.startForegroundService(context, Intent(context, TweakService::class.java))
            }
            if (cfg.lockOn) {
                ContextCompat.startForegroundService(context, Intent(context, LockService::class.java))
            }
        } catch (e: Exception) {
        }
    }
}
