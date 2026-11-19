package com.fauzi.tweaklauncher

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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

    val box = Modifier
        .fillMaxWidth()
        .clip(Pal.shape)
        .background(Pal.card)
        .border(1.dp, Pal.line, Pal.shape)
        .padding(horizontal = 12.dp, vertical = 8.dp)
    if (perm) {
        Row(box, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Beri akses notifikasi untuk menampilkan pemutar musik.", color = Pal.sub, fontSize = 12.sp, modifier = Modifier.weight(1f))
            ActionButton("Beri akses", Modifier.height(38.dp)) { vm.openNotifAccess() }
        }
        return
    }
    val frac = if (np.dur > 0) (np.pos.toFloat() / np.dur).coerceIn(0f, 1f) else 0f
    val bg = Pal.bg
    Row(box, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(Modifier.weight(1f)) {
            // Spektrum: batang yang sudah lewat berwarna penuh, sisanya redup. Animasi, mengikuti warna aksen tema.
            Canvas(Modifier.fillMaxWidth().height(26.dp)) {
                val n = 36
                val gap = size.width * 0.008f
                val bw = (size.width - gap * (n - 1)) / n
                for (i in 0 until n) {
                    val t = i / (n - 1f)
                    val wave = 0.5f + 0.5f * sin(phase * 1.3f + i * 0.55f)
                    val wave2 = 0.5f + 0.5f * sin(phase * 0.8f + i * 1.7f + 1.2f)
                    val env = 0.4f + 0.6f * (1f - 0.5f * t)
                    val h = (0.1f + level * env * (0.25f + 0.75f * (wave * 0.6f + wave2 * 0.4f))).coerceIn(0.08f, 1f)
                    val bh = size.height * h
                    drawRoundRect(
                        color = if (t <= frac) acc else acc.copy(alpha = 0.3f),
                        topLeft = Offset(i * (bw + gap), (size.height - bh) / 2),
                        size = Size(bw, bh),
                        cornerRadius = CornerRadius(bw / 2, bw / 2)
                    )
                }
            }
            Row(Modifier.fillMaxWidth().clickable { if (np.pkg.isNotEmpty()) vm.launchApp(np.pkg) }, verticalAlignment = Alignment.CenterVertically) {
                val label = np.title.ifEmpty { "(tanpa judul)" } + if (np.artist.isNotEmpty()) " - ${np.artist}" else ""
                Text(label, color = Pal.text, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                if (np.dur > 0) Text(" ${fmt(np.pos)} / ${fmt(np.dur)}", color = Pal.sub, fontSize = 10.sp, maxLines = 1)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            MusicBtn(0, Pal.text) { WaHub.ctrl?.transportControls?.skipToPrevious() }
            Box(
                Modifier.size(38.dp).clip(CircleShape).background(acc).clickable {
                    if (np.playing) WaHub.ctrl?.transportControls?.pause() else WaHub.ctrl?.transportControls?.play()
                },
                contentAlignment = Alignment.Center
            ) {
                Canvas(Modifier.size(14.dp)) {
                    val w = size.width
                    val h = size.height
                    if (np.playing) {
                        drawRect(bg, Offset(w * 0.1f, 0f), Size(w * 0.3f, h))
                        drawRect(bg, Offset(w * 0.6f, 0f), Size(w * 0.3f, h))
                    } else {
                        drawPath(Path().apply { moveTo(w * 0.2f, 0f); lineTo(w, h / 2); lineTo(w * 0.2f, h); close() }, bg)
                    }
                }
            }
            MusicBtn(3, Pal.text) { WaHub.ctrl?.transportControls?.skipToNext() }
        }
    }
}

private fun fmt(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "${s / 60}:${(s % 60).toString().padStart(2, '0')}"
}

/** kind: 0 sebelumnya, 1 putar, 2 jeda, 3 berikutnya. */
@Composable
private fun MusicBtn(kind: Int, color: Color, big: Boolean = false, onClick: () -> Unit) {
    Box(Modifier.size(if (big) 52.dp else 34.dp).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(if (big) 26.dp else 12.dp)) {
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
