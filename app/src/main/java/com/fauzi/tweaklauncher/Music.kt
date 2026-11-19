package com.fauzi.tweaklauncher

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.sin

/** Pemutar musik minimalis untuk Beranda mode Modern: judul, spektrum (animasi), dan tombol kontrol. */
@Composable
fun MusicWidget(vm: MainVm) {
    val ctx = LocalContext.current
    LaunchedEffect(Unit) {
        while (true) {
            WaHub.refresh(ctx)
            delay(1000)
        }
    }
    val np = WaHub.np
    val perm = np.reason == "perm"
    if (np.title.isEmpty() && !perm && !np.playing) return

    var phase by remember { mutableFloatStateOf(0f) }
    var level by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60)
            phase += 0.22f
            level = if (WaHub.np.playing) minOf(1f, level + 0.15f) else maxOf(0f, level - 0.08f)
        }
    }
    val acc = Pal.accent

    SCard(Modifier.fillMaxWidth()) {
        if (perm) {
            Text("Pemutar musik", color = Pal.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text("Beri akses notifikasi supaya musik yang sedang diputar bisa tampil di sini.", color = Pal.sub, fontSize = 12.sp)
            ActionButton("Beri akses", Modifier.fillMaxWidth()) { vm.openNotifAccess() }
            return@SCard
        }
        Column(
            Modifier.fillMaxWidth().clickable { if (np.pkg.isNotEmpty()) vm.launchApp(np.pkg) },
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(np.title.ifEmpty { "(tanpa judul)" }, color = Pal.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (np.artist.isNotEmpty()) Text(np.artist, color = Pal.sub, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        // Spektrum: animasi, mengikuti status putar dan warna aksen tema.
        Canvas(Modifier.fillMaxWidth().height(44.dp)) {
            val n = 28
            val gap = size.width * 0.012f
            val bw = (size.width - gap * (n - 1)) / n
            for (i in 0 until n) {
                val t = i / (n - 1f)
                val wave = 0.5f + 0.5f * sin(phase * 1.3f + i * 0.55f)
                val wave2 = 0.5f + 0.5f * sin(phase * 0.8f + i * 1.7f + 1.2f)
                val env = 0.35f + 0.65f * (1f - 0.6f * t)
                val h = (0.08f + level * env * (0.25f + 0.75f * (wave * 0.6f + wave2 * 0.4f))).coerceIn(0.06f, 1f)
                val bh = size.height * h
                drawRoundRect(
                    color = acc.copy(alpha = 0.45f + 0.55f * h),
                    topLeft = Offset(i * (bw + gap), size.height - bh),
                    size = Size(bw, bh),
                    cornerRadius = CornerRadius(bw / 2, bw / 2)
                )
            }
        }
        // Garis kemajuan.
        val frac = if (np.dur > 0) (np.pos.toFloat() / np.dur).coerceIn(0f, 1f) else 0f
        Canvas(Modifier.fillMaxWidth().height(3.dp)) {
            drawRoundRect(Pal.line, size = size, cornerRadius = CornerRadius(size.height))
            drawRoundRect(acc, size = Size(size.width * frac, size.height), cornerRadius = CornerRadius(size.height))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            MusicBtn(0, acc) { WaHub.ctrl?.transportControls?.skipToPrevious() }
            MusicBtn(if (np.playing) 2 else 1, acc, big = true) {
                if (np.playing) WaHub.ctrl?.transportControls?.pause() else WaHub.ctrl?.transportControls?.play()
            }
            MusicBtn(3, acc) { WaHub.ctrl?.transportControls?.skipToNext() }
        }
    }
}

/** kind: 0 sebelumnya, 1 putar, 2 jeda, 3 berikutnya. */
@Composable
private fun MusicBtn(kind: Int, color: Color, big: Boolean = false, onClick: () -> Unit) {
    Box(Modifier.size(if (big) 52.dp else 44.dp).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(if (big) 26.dp else 20.dp)) {
            val w = size.width
            val h = size.height
            when (kind) {
                1 -> drawPath(Path().apply { moveTo(w * 0.2f, 0f); lineTo(w, h / 2); lineTo(w * 0.2f, h); close() }, color)
                2 -> {
                    drawRect(color, Offset(w * 0.15f, 0f), Size(w * 0.25f, h))
                    drawRect(color, Offset(w * 0.6f, 0f), Size(w * 0.25f, h))
                }
                0 -> {
                    drawRect(color, Offset(0f, 0f), Size(w * 0.14f, h))
                    drawPath(Path().apply { moveTo(w, 0f); lineTo(w * 0.2f, h / 2); lineTo(w, h); close() }, color)
                }
                else -> {
                    drawRect(color, Offset(w * 0.86f, 0f), Size(w * 0.14f, h))
                    drawPath(Path().apply { moveTo(0f, 0f); lineTo(w * 0.8f, h / 2); lineTo(0f, h); close() }, color)
                }
            }
        }
    }
}
