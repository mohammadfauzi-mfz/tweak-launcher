package com.fauzi.tweaklauncher

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Mendeteksi geseran tanpa menghalangi anak (daftar yang bisa digulir tetap bekerja).
 * [onEnd] menerima total perpindahan dan apakah ada anak yang sudah memakai geseran itu
 * (misalnya daftar yang sedang digulir).
 */
fun Modifier.swipes(
    onStart: () -> Unit = {},
    onEnd: (delta: Offset, consumed: Boolean) -> Unit
): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        onStart()
        var consumed = false
        var last = down.position
        var multi = false
        while (true) {
            val ev = awaitPointerEvent(PointerEventPass.Final)
            if (ev.changes.size > 1) multi = true
            val ch = ev.changes.firstOrNull { it.id == down.id } ?: break
            last = ch.position
            val moved = ch.position != ch.previousPosition
            if (moved && ch.isConsumed) consumed = true
            if (!ch.pressed) break
        }
        if (!multi) onEnd(last - down.position, consumed)
    }
}
