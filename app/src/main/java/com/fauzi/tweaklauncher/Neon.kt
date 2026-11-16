package com.fauzi.tweaklauncher

import android.graphics.Bitmap
import android.graphics.Color as AColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/** Mengubah ikon aplikasi menjadi garis neon: deteksi tepi, pewarnaan, lalu efek cahaya. */
object Neon {
    private const val N = 128
    private val cache = ConcurrentHashMap<String, ImageBitmap>()

    @Composable
    fun rememberNeon(app: AppItem, cfg: Cfg): ImageBitmap? {
        val accent = if (cfg.neonColor == "accent") cfg.accent else ""
        val key = "${app.pkg}|${cfg.neonThick}|${cfg.neonGlow}|${cfg.neonColor}|$accent"
        val st = produceState<ImageBitmap?>(cache[key], key) {
            if (value == null) {
                value = withContext(Dispatchers.Default) {
                    try {
                        val src = app.icon.asAndroidBitmap()
                        val fixed = if (src.config == Bitmap.Config.ARGB_8888 && !src.isMutable) src else src.copy(Bitmap.Config.ARGB_8888, false)
                        val acc = if (cfg.neonColor == "accent") runCatching { AColor.parseColor(cfg.accent) }.getOrNull() else null
                        make(fixed, cfg.neonThick, cfg.neonGlow, acc).asImageBitmap().also { cache[key] = it }
                    } catch (e: Throwable) {
                        null
                    }
                }
            }
        }
        return st.value
    }

    private fun lum(c: Int) = 0.299f * AColor.red(c) + 0.587f * AColor.green(c) + 0.114f * AColor.blue(c)

    fun make(src: Bitmap, thick: Int, glow: Int, fixedColor: Int?): Bitmap {
        val b = Bitmap.createScaledBitmap(src, N, N, true)
        val px = IntArray(N * N)
        b.getPixels(px, 0, N, 0, 0, N, N)

        // Perkirakan warna latar seragam dari tepi gambar.
        var br = 0L
        var bg = 0L
        var bb = 0L
        var cnt = 0
        var opaqueEdge = 0
        fun edgePx(x: Int, y: Int) {
            val c = px[y * N + x]
            if (AColor.alpha(c) > 200) {
                opaqueEdge++
                br += AColor.red(c)
                bg += AColor.green(c)
                bb += AColor.blue(c)
            }
            cnt++
        }
        for (i in 0 until N) {
            edgePx(i, 2); edgePx(i, N - 3); edgePx(2, i); edgePx(N - 3, i)
        }
        var uniform = false
        var bgc = 0
        if (opaqueEdge > cnt * 0.9) {
            val mr = (br / opaqueEdge).toInt()
            val mg = (bg / opaqueEdge).toInt()
            val mb = (bb / opaqueEdge).toInt()
            var dev = 0
            for (i in 0 until N) {
                for (c in intArrayOf(px[2 * N + i], px[(N - 3) * N + i], px[i * N + 2], px[i * N + N - 3])) {
                    dev += abs(AColor.red(c) - mr) + abs(AColor.green(c) - mg) + abs(AColor.blue(c) - mb)
                }
            }
            if (dev / cnt < 30) {
                uniform = true
                bgc = AColor.rgb(mr, mg, mb)
            }
        }

        // Gambar pembanding untuk deteksi tepi: latar seragam ditiadakan, transparan dianggap hitam.
        val r = FloatArray(N * N)
        val g = FloatArray(N * N)
        val bl = FloatArray(N * N)
        val fg = BooleanArray(N * N)
        for (i in px.indices) {
            val c = px[i]
            val a = AColor.alpha(c) / 255f
            val isBg = if (uniform) {
                abs(AColor.red(c) - AColor.red(bgc)) + abs(AColor.green(c) - AColor.green(bgc)) + abs(AColor.blue(c) - AColor.blue(bgc)) < 40
            } else a < 0.1f
            fg[i] = !isBg
            r[i] = if (isBg) 0f else AColor.red(c) * a
            g[i] = if (isBg) 0f else AColor.green(c) * a
            bl[i] = if (isBg) 0f else AColor.blue(c) * a
        }

        // Sobel per kanal, ambil yang terbesar.
        val e = FloatArray(N * N)
        fun mag(ch: FloatArray, x: Int, y: Int): Float {
            fun v(xx: Int, yy: Int) = ch[yy * N + xx]
            val gx = -v(x - 1, y - 1) - 2 * v(x - 1, y) - v(x - 1, y + 1) + v(x + 1, y - 1) + 2 * v(x + 1, y) + v(x + 1, y + 1)
            val gy = -v(x - 1, y - 1) - 2 * v(x, y - 1) - v(x + 1, y - 1) + v(x - 1, y + 1) + 2 * v(x, y + 1) + v(x + 1, y + 1)
            return sqrt(gx * gx + gy * gy)
        }
        for (y in 1 until N - 1) for (x in 1 until N - 1) {
            val m = max(mag(r, x, y), max(mag(g, x, y), mag(bl, x, y)))
            e[y * N + x] = ((m - 90f) / 160f).coerceIn(0f, 1f)
        }

        // Warna dasar ikon: rata-rata warna berkejenuhan tinggi.
        val hsv = FloatArray(3)
        var sx = 0f
        var sy = 0f
        var sw = 0f
        for (i in px.indices) if (fg[i]) {
            AColor.colorToHSV(px[i] or (0xFF shl 24), hsv)
            val w = hsv[1] * hsv[2]
            if (w > 0.15f) {
                val rad = Math.toRadians(hsv[0].toDouble())
                sx += (Math.cos(rad) * w).toFloat()
                sy += (Math.sin(rad) * w).toFloat()
                sw += w
            }
        }
        val domHue = if (sw > 0.5f) ((Math.toDegrees(Math.atan2(sy.toDouble(), sx.toDouble())) + 360) % 360).toFloat() else -1f

        // Pelebaran garis sesuai ketebalan.
        var line = e
        repeat((thick - 1).coerceIn(0, 2)) {
            val d = FloatArray(N * N)
            for (y in 1 until N - 1) for (x in 1 until N - 1) {
                var m = 0f
                for (dy in -1..1) for (dx in -1..1) m = max(m, line[(y + dy) * N + x + dx])
                d[y * N + x] = m
            }
            line = d
        }

        // Lapisan garis berwarna (premultiplied).
        val lr = FloatArray(N * N)
        val lg = FloatArray(N * N)
        val lb = FloatArray(N * N)
        val la = FloatArray(N * N)
        for (i in px.indices) {
            val a = line[i]
            if (a <= 0.01f) continue
            val c: Int = if (fixedColor != null) fixedColor else {
                AColor.colorToHSV(px[i] or (0xFF shl 24), hsv)
                val h = if (hsv[1] < 0.25f && domHue >= 0f) domHue else hsv[0]
                val s = if (hsv[1] < 0.25f && domHue < 0f) 0.15f else max(hsv[1], 0.75f)
                AColor.HSVToColor(floatArrayOf(h, s, 1f))
            }
            la[i] = a
            lr[i] = AColor.red(c) / 255f * a
            lg[i] = AColor.green(c) / 255f * a
            lb[i] = AColor.blue(c) / 255f * a
        }

        // Cahaya: kaburkan lapisan garis lalu gabungkan.
        val gs = glow / 100f
        val outA = FloatArray(N * N)
        val outR = FloatArray(N * N)
        val outG = FloatArray(N * N)
        val outB = FloatArray(N * N)
        if (gs > 0f) {
            val ba = blur(la)
            val br2 = blur(lr)
            val bg2 = blur(lg)
            val bb2 = blur(lb)
            for (i in px.indices) {
                val k = gs * 2.2f * (1f - la[i])
                outA[i] = min(1f, la[i] + ba[i] * k)
                outR[i] = lr[i] + br2[i] * k
                outG[i] = lg[i] + bg2[i] * k
                outB[i] = lb[i] + bb2[i] * k
            }
        } else {
            for (i in px.indices) {
                outA[i] = la[i]; outR[i] = lr[i]; outG[i] = lg[i]; outB[i] = lb[i]
            }
        }
        val res = IntArray(N * N)
        for (i in px.indices) {
            val a = outA[i]
            if (a <= 0.004f) continue
            val rr = (outR[i] / a).coerceIn(0f, 1f)
            val gg = (outG[i] / a).coerceIn(0f, 1f)
            val bb = (outB[i] / a).coerceIn(0f, 1f)
            res[i] = AColor.argb((a * 255).toInt(), (rr * 255).toInt(), (gg * 255).toInt(), (bb * 255).toInt())
        }
        return Bitmap.createBitmap(res, N, N, Bitmap.Config.ARGB_8888)
    }

    private fun blur(src: FloatArray): FloatArray {
        var cur = src
        repeat(3) {
            val tmp = FloatArray(N * N)
            val out = FloatArray(N * N)
            val rad = 3
            for (y in 0 until N) for (x in 0 until N) {
                var s = 0f
                for (k in -rad..rad) s += cur[y * N + (x + k).coerceIn(0, N - 1)]
                tmp[y * N + x] = s / (2 * rad + 1)
            }
            for (y in 0 until N) for (x in 0 until N) {
                var s = 0f
                for (k in -rad..rad) s += tmp[(y + k).coerceIn(0, N - 1) * N + x]
                out[y * N + x] = s / (2 * rad + 1)
            }
            cur = out
        }
        return cur
    }
}
