package com.fauzi.tweaklauncher

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.os.Build
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

/**
 * Ikon monokrom putih seperti ubin Windows Phone asli. Memakai lapisan monokrom ikon adaptif
 * (Android 13 ke atas) bila aplikasi menyediakannya; jika tidak, mengembalikan null.
 */
object WpIcons {
    private val cache = HashMap<String, ImageBitmap?>()

    fun mono(ctx: Context, pkg: String, px: Int): ImageBitmap? {
        val k = "$pkg@$px"
        if (cache.containsKey(k)) return cache[k]
        val bmp: ImageBitmap? = try {
            if (Build.VERSION.SDK_INT < 33) {
                null
            } else {
                val d = ctx.packageManager.getApplicationIcon(pkg)
                val m = (d as? AdaptiveIconDrawable)?.monochrome
                if (m == null) {
                    null
                } else {
                    val s = (px * 1.5f).toInt().coerceAtLeast(12)
                    val full = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
                    val c = Canvas(full)
                    val md = m.mutate()
                    md.setTint(android.graphics.Color.WHITE)
                    md.setBounds(0, 0, s, s)
                    md.draw(c)
                    // lapisan adaptif 108dp, bagian terlihat 72dp: potong bagian tengah
                    val inset = s / 6
                    Bitmap.createBitmap(full, inset, inset, s - 2 * inset, s - 2 * inset).asImageBitmap()
                }
            }
        } catch (e: Exception) {
            null
        }
        cache[k] = bmp
        return bmp
    }
}
