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
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
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
        if (job == null) {
            job = scope.launch {
                while (isActive) {
                    cfg.reload()
                    if (!cfg.overlayOn && !cfg.autoApp && !cfg.autoSched) {
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
        val text = "${st.hz} Hz  •  RAM ${(st.ramPct * 100).toInt()}%  •  ${st.tempText}  •  ${st.battery}%"
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
            if (cfg.overlayOn || cfg.autoApp || cfg.autoSched) {
                ContextCompat.startForegroundService(context, Intent(context, TweakService::class.java))
            }
        } catch (e: Exception) {
        }
    }
}
