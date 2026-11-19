package com.fauzi.tweaklauncher

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent

/**
 * Mode mandiri: menulis pengaturan sistem langsung lewat izin WRITE_SECURE_SETTINGS
 * (diberikan sekali lewat Shizuku atau adb) sehingga Shizuku tidak perlu menyala terus.
 */
object Sec {
    var ctx: Context? = null
        private set

    fun init(c: Context) {
        ctx = c.applicationContext
    }

    fun canWriteSecure(): Boolean =
        ctx?.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED

    fun canWriteSystem(): Boolean = ctx?.let { Settings.System.canWrite(it) } == true

    private val RE = Regex("^settings (put|delete|get) (global|system|secure) (\\S+)(?: (.+))?$")

    /** Menjalankan perintah "settings put/delete/get" lewat API. Null jika bukan perintah settings. */
    fun settings(cmd: String): Pair<Int, String>? {
        val m = RE.find(cmd.trim()) ?: return null
        val c = ctx ?: return null
        val op = m.groupValues[1]
        val tbl = m.groupValues[2]
        val key = m.groupValues[3]
        val v = m.groupValues[4]
        val cr = c.contentResolver
        return try {
            if (op == "get") {
                val s = when (tbl) {
                    "global" -> Settings.Global.getString(cr, key)
                    "system" -> Settings.System.getString(cr, key)
                    else -> Settings.Secure.getString(cr, key)
                }
                0 to (s ?: "null")
            } else {
                val value: String? = if (op == "delete") null else v
                val ok = when (tbl) {
                    "global" -> Settings.Global.putString(cr, key, value)
                    "system" -> Settings.System.putString(cr, key, value)
                    else -> Settings.Secure.putString(cr, key, value)
                }
                if (ok) 0 to "" else 1 to "ditolak sistem"
            }
        } catch (e: Throwable) {
            1 to (e.message ?: "ditolak")
        }
    }

    /** Memberi izin mandiri lewat Shizuku. Mengembalikan pesan hasil. */
    fun grant(): String {
        val c = ctx ?: return "Aplikasi belum siap."
        if (Shell.state() != ShizukuState.READY) return "Shizuku belum siap. Jalankan Shizuku dan beri izin, atau pakai perintah adb di bawah."
        val pkg = c.packageName
        Shell.run("pm grant $pkg android.permission.WRITE_SECURE_SETTINGS")
        Shell.run("appops set $pkg WRITE_SETTINGS allow")
        return if (canWriteSecure()) "Mode mandiri aktif. Shizuku tidak perlu menyala untuk fitur pengaturan." else "Izin belum tercatat. Coba lagi atau pakai adb."
    }
}

/** Layanan aksesibilitas ringan: hanya untuk kunci layar dan panel notifikasi tanpa Shizuku. */
class TweakAccess : AccessibilityService() {
    companion object {
        @Volatile
        var inst: TweakAccess? = null

        fun lock(): Boolean = inst?.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN) ?: false
        fun notifications(): Boolean = inst?.performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS) ?: false
    }

    override fun onServiceConnected() {
        inst = this
    }

    override fun onUnbind(intent: Intent?): Boolean {
        inst = null
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        inst = null
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}
}
