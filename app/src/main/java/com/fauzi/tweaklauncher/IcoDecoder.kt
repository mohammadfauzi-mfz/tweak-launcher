package com.fauzi.tweaklauncher

import android.graphics.Bitmap
import android.graphics.BitmapFactory

/** Pembaca berkas .ico: memilih gambar terbesar, mendukung PNG di dalam ICO dan BMP 1/4/8/24/32 bit dengan mask. */
object IcoDecoder {
    private fun u16(b: ByteArray, o: Int) = (b[o].toInt() and 0xFF) or ((b[o + 1].toInt() and 0xFF) shl 8)
    private fun i32(b: ByteArray, o: Int) = u16(b, o) or (u16(b, o + 2) shl 16)

    fun decode(b: ByteArray): Bitmap? = try {
        decodeInner(b)
    } catch (e: Exception) {
        null
    }

    private fun decodeInner(b: ByteArray): Bitmap? {
        if (b.size < 22 || u16(b, 0) != 0 || (u16(b, 2) != 1 && u16(b, 2) != 2)) return null
        val n = u16(b, 4)
        var best = -1
        var bestScore = -1L
        for (i in 0 until n) {
            val o = 6 + i * 16
            if (o + 16 > b.size) break
            val w = (b[o].toInt() and 0xFF).let { if (it == 0) 256 else it }
            val h = (b[o + 1].toInt() and 0xFF).let { if (it == 0) 256 else it }
            val bpp = u16(b, o + 6)
            val score = w.toLong() * h * 100 + bpp
            if (score > bestScore) {
                bestScore = score
                best = i
            }
        }
        if (best < 0) return null
        val o = 6 + best * 16
        val size = i32(b, o + 8)
        val off = i32(b, o + 12)
        if (off < 0 || size <= 0 || off + size > b.size) return null
        val d = b.copyOfRange(off, off + size)
        // PNG di dalam ICO
        if (d.size > 8 && (d[0].toInt() and 0xFF) == 0x89 && d[1] == 'P'.code.toByte()) {
            return BitmapFactory.decodeByteArray(d, 0, d.size)
        }
        return dib(d)
    }

    private fun dib(d: ByteArray): Bitmap? {
        val hs = i32(d, 0)
        if (hs < 40) return null
        val w = i32(d, 4)
        val h = i32(d, 8) / 2
        val bpp = u16(d, 14)
        val comp = i32(d, 16)
        if (w <= 0 || h <= 0 || w > 1024 || h > 1024 || comp != 0) return null
        var clr = i32(d, 32)
        if (bpp <= 8 && clr == 0) clr = 1 shl bpp
        val palOff = hs
        val pixOff = hs + (if (bpp <= 8) clr * 4 else 0)
        val rowBytes = ((w * bpp + 31) / 32) * 4
        val maskOff = pixOff + rowBytes * h
        val maskRow = ((w + 31) / 32) * 4
        val px = IntArray(w * h)
        var anyAlpha = false
        for (y in 0 until h) {
            val row = pixOff + (h - 1 - y) * rowBytes
            for (x in 0 until w) {
                var a = 255
                val c: Int
                when (bpp) {
                    32 -> {
                        val p = row + x * 4
                        a = d[p + 3].toInt() and 0xFF
                        if (a != 0) anyAlpha = true
                        c = ((d[p + 2].toInt() and 0xFF) shl 16) or ((d[p + 1].toInt() and 0xFF) shl 8) or (d[p].toInt() and 0xFF)
                    }
                    24 -> {
                        val p = row + x * 3
                        c = ((d[p + 2].toInt() and 0xFF) shl 16) or ((d[p + 1].toInt() and 0xFF) shl 8) or (d[p].toInt() and 0xFF)
                    }
                    8, 4, 1 -> {
                        val bit = x * bpp
                        val byte = d[row + bit / 8].toInt() and 0xFF
                        val shift = 8 - bpp - (bit % 8)
                        val idx = (byte shr shift) and ((1 shl bpp) - 1)
                        val p = palOff + idx * 4
                        c = ((d[p + 2].toInt() and 0xFF) shl 16) or ((d[p + 1].toInt() and 0xFF) shl 8) or (d[p].toInt() and 0xFF)
                    }
                    else -> return null
                }
                px[y * w + x] = (a shl 24) or c
            }
        }
        // Mask AND: dipakai jika tidak ada alpha asli (atau bukan 32 bit).
        if (!(bpp == 32 && anyAlpha) && maskOff + maskRow * h <= d.size) {
            for (y in 0 until h) {
                val row = maskOff + (h - 1 - y) * maskRow
                for (x in 0 until w) {
                    val bit = (d[row + x / 8].toInt() shr (7 - x % 8)) and 1
                    if (bit == 1) px[y * w + x] = 0
                    else px[y * w + x] = px[y * w + x] or (0xFF shl 24)
                }
            }
        }
        return Bitmap.createBitmap(px, w, h, Bitmap.Config.ARGB_8888)
    }
}
