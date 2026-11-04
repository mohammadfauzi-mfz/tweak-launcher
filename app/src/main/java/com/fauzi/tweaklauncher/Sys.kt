package com.fauzi.tweaklauncher

import android.app.ActivityManager
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.hardware.display.DisplayManager
import android.os.BatteryManager
import android.os.Environment
import android.os.Process
import android.os.StatFs
import android.view.Display
import java.util.Locale

data class Stats(
    val ramText: String = "-",
    val ramPct: Float = 0f,
    val battery: Int = 0,
    val tempText: String = "-",
    val tempPct: Float = 0f,
    val storagePct: Float = 0f,
    val hz: Int = 0
)

object Sys {

    fun list(s: String): List<String> = s.split("\n").filter { it.isNotBlank() }

    fun join(c: Collection<String>): String = c.joinToString("\n")

    fun stats(ctx: Context): Stats {
        val loc = Locale("id", "ID")
        val am = ctx.getSystemService(ActivityManager::class.java)
        val mi = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)
        val gb = 1024.0 * 1024.0 * 1024.0
        val used = mi.totalMem - mi.availMem
        val ramPct = if (mi.totalMem > 0) used.toFloat() / mi.totalMem.toFloat() else 0f

        val b = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = b?.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) ?: 0
        val scale = b?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val battery = if (scale > 0) level * 100 / scale else 0
        val temp = (b?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10f

        val st = StatFs(Environment.getDataDirectory().path)
        val total = st.totalBytes.toFloat()
        val storagePct = if (total > 0) 1f - st.availableBytes.toFloat() / total else 0f

        return Stats(
            ramText = String.format(loc, "%.1f GB", used / gb),
            ramPct = ramPct,
            battery = battery,
            tempText = String.format(loc, "%.0f°C", temp),
            tempPct = temp / 50f,
            storagePct = storagePct,
            hz = refreshRate(ctx)
        )
    }

    fun refreshRate(ctx: Context): Int {
        return try {
            val d = ctx.getSystemService(DisplayManager::class.java).getDisplay(Display.DEFAULT_DISPLAY)
            if (d == null) 0 else Math.round(d.refreshRate)
        } catch (e: Exception) {
            0
        }
    }

    fun hasUsageAccess(ctx: Context): Boolean {
        return try {
            val ao = ctx.getSystemService(AppOpsManager::class.java)
            ao.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                ctx.packageName
            ) == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Menggambar ikon tanpa topeng bentuk bawaan sistem supaya bentuknya bisa diganti di Setelan.
     * Ikon adaptif berukuran 108dp dengan area tampak 72dp di tengah, jadi dipotong ke tengah.
     */
    fun renderIcon(d: Drawable, px: Int = 160): Bitmap {
        val bmp = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        if (d is AdaptiveIconDrawable) {
            val pad = px / 4
            d.background?.let {
                it.setBounds(-pad, -pad, px + pad, px + pad)
                it.draw(c)
            }
            d.foreground?.let {
                it.setBounds(-pad, -pad, px + pad, px + pad)
                it.draw(c)
            }
        } else {
            d.setBounds(0, 0, px, px)
            d.draw(c)
        }
        return bmp
    }
}
