package com.fauzi.tweaklauncher

import android.content.ComponentName
import android.content.Context
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.SystemClock
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private val WA_BG = Color(0xFF232333)
private val WA_EDGE = Color(0xFF8A8FAA)
private val WA_DARK = Color(0xFF0C0C14)
private val WA_BTN = Color(0xFF4A4E66)
private val WA_GREEN = Color(0xFF00E000)
private const val YT_MUSIC = "com.google.android.apps.youtube.music"

private data class NowPlaying(
    val reason: String = "", // "perm" = tidak ada izin, "none" = tidak ada pemutar
    val title: String = "",
    val artist: String = "",
    val state: Int = PlaybackState.STATE_NONE,
    val pos: Long = 0L,
    val dur: Long = 0L,
    val pkg: String = ""
) {
    val playing get() = state == PlaybackState.STATE_PLAYING || state == PlaybackState.STATE_BUFFERING
}

private fun isPlay(c: MediaController): Boolean {
    val s = c.playbackState?.state
    return s == PlaybackState.STATE_PLAYING || s == PlaybackState.STATE_BUFFERING
}

/** Membaca sesi media yang aktif (YouTube Music diutamakan). */
private fun readSession(ctx: Context): Pair<NowPlaying, MediaController?> {
    try {
        val msm = ctx.getSystemService(MediaSessionManager::class.java)
        val list = msm.getActiveSessions(ComponentName(ctx, NotifListener::class.java))
        val c = list.firstOrNull { it.packageName == YT_MUSIC && isPlay(it) }
            ?: list.firstOrNull { it.packageName == YT_MUSIC }
            ?: list.firstOrNull { isPlay(it) }
            ?: list.firstOrNull()
            ?: return NowPlaying(reason = "none") to null
        val md = c.metadata
        val ps = c.playbackState
        val dur = md?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
        var pos = ps?.position ?: 0L
        if (ps != null && ps.state == PlaybackState.STATE_PLAYING) {
            pos += ((SystemClock.elapsedRealtime() - ps.lastPositionUpdateTime) * ps.playbackSpeed).toLong()
        }
        if (dur > 0) pos = pos.coerceIn(0L, dur) else pos = pos.coerceAtLeast(0L)
        val title = md?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: ""
        val artist = md?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: md?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST) ?: ""
        return NowPlaying("", title, artist, ps?.state ?: PlaybackState.STATE_NONE, pos, dur, c.packageName) to c
    } catch (e: SecurityException) {
        return NowPlaying(reason = "perm") to null
    } catch (e: Exception) {
        return NowPlaying(reason = "none") to null
    }
}

private fun fmt(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%02d:%02d".format(s / 60, s % 60)
}

@Composable
fun WinampIcon(dp: androidx.compose.ui.unit.Dp) {
    Canvas(Modifier.size(dp)) {
        val w = size.width
        drawRect(Color(0xFF2A2C40))
        drawRect(Color(0xFF8A8FAA), style = androidx.compose.ui.graphics.drawscope.Stroke(w * 0.06f))
        val bars = listOf(0.35f, 0.6f, 0.8f, 0.5f, 0.7f)
        bars.forEachIndexed { i, h ->
            val bw = w * 0.1f
            val x = w * 0.15f + i * w * 0.14f
            drawRect(Color(0xFF00E000), Offset(x, w * 0.85f - w * 0.7f * h), Size(bw, w * 0.7f * h))
        }
    }
}

@Composable
fun WinampWindow(vm: MainVm, w: GameWin, maxW: Float, maxH: Float) {
    val d = LocalDensity.current
    val minX = -maxW * 0.6f
    val maxX = maxW - with(d) { 90.dp.toPx() }
    val maxY = maxH - with(d) { 50.dp.toPx() }
    Column(
        Modifier
            .offset { IntOffset(w.x.roundToInt(), w.y.roundToInt()) }
            .zIndex(w.z.toFloat())
            .width(300.dp)
            .background(WA_BG)
            .border(1.dp, WA_EDGE)
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    vm.gameFront(w)
                }
            }
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color(0xFF6A6F96), Color(0xFF2A2C40))))
                .pointerInput(Unit) {
                    detectDragGestures { change, drag ->
                        change.consume()
                        w.x = (w.x + drag.x).coerceIn(minX, maxX)
                        w.y = (w.y + drag.y).coerceIn(0f, maxY)
                    }
                }
                .padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            WinampIcon(14.dp)
            Text(
                "WINAMP", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center, modifier = Modifier.weight(1f)
            )
            Box(
                Modifier.size(18.dp).background(WA_BTN).waBevel(true).clickable { vm.gameWins.remove(w) },
                contentAlignment = Alignment.Center
            ) { Text("x", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
        }
        WinampBody(vm)
    }
}

private fun Modifier.waBevel(raised: Boolean): Modifier = this.drawBehind {
    val t = 1.dp.toPx()
    val hi = Color(0xFF9A9FBA)
    val lo = Color(0xFF10101A)
    val tl = if (raised) hi else lo
    val br = if (raised) lo else hi
    drawRect(tl, Offset(0f, 0f), Size(size.width, t))
    drawRect(tl, Offset(0f, 0f), Size(t, size.height))
    drawRect(br, Offset(0f, size.height - t), Size(size.width, t))
    drawRect(br, Offset(size.width - t, 0f), Size(t, size.height))
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WinampBody(vm: MainVm) {
    val ctx = LocalContext.current
    var np by remember { mutableStateOf(NowPlaying(reason = "none")) }
    val ctrl = remember { arrayOfNulls<MediaController>(1) }
    var phase by remember { mutableFloatStateOf(0f) }
    var level by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            val (n, c) = readSession(ctx)
            np = n
            ctrl[0] = c
            delay(500)
        }
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(80)
            phase += 0.18f
            level = if (np.playing) minOf(1f, level + 0.2f) else maxOf(0f, level - 0.12f)
        }
    }

    val am = remember { ctx.getSystemService(AudioManager::class.java) }
    val maxVol = remember { am?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15 }
    var vol by remember { mutableFloatStateOf((am?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0).toFloat() / maxVol) }

    Column(Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            // layar waktu dan spektrum
            Column(
                Modifier.width(96.dp).background(WA_DARK).waBevel(false).padding(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    (if (np.state == PlaybackState.STATE_PLAYING) "▶ " else if (np.state == PlaybackState.STATE_PAUSED) "❙❙ " else "■ ") + fmt(np.pos),
                    color = WA_GREEN, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, maxLines = 1
                )
                Canvas(Modifier.fillMaxWidth().height(28.dp)) {
                    val n = 16
                    val gap = 2.dp.toPx()
                    val bw = (size.width - gap * (n - 1)) / n
                    val seg = 3.dp.toPx()
                    for (i in 0 until n) {
                        val a = abs(sin(phase * 1.7f + i * 0.9f) * cos(phase * 0.8f + i * 0.37f))
                        val h = 0.08f + 0.92f * a * level
                        val cnt = (h * (size.height / seg)).toInt().coerceAtLeast(1)
                        for (j in 0 until cnt) {
                            val col = if (j < 5) Color(0xFF00E000) else if (j < 7) Color(0xFFE0E000) else Color(0xFFE02000)
                            drawRect(col, Offset(i * (bw + gap), size.height - (j + 1) * seg), Size(bw, seg - 1f))
                        }
                    }
                }
            }
            // layar judul
            Column(
                Modifier.weight(1f).height(66.dp).background(WA_DARK).waBevel(false).padding(6.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                val line = when {
                    np.reason == "perm" -> "Izinkan akses notifikasi untuk membaca pemutar"
                    np.reason == "none" -> "Belum ada pemutar aktif. Putar lagu di YouTube Music."
                    else -> (np.title.ifEmpty { "(tanpa judul)" }) + (if (np.artist.isNotEmpty()) " - " + np.artist else "") +
                        (if (np.dur > 0) " (" + fmt(np.dur) + ")" else "")
                }
                Text(
                    line, color = WA_GREEN, fontSize = 13.sp, fontFamily = FontFamily.Monospace,
                    maxLines = 1, softWrap = false,
                    modifier = Modifier.fillMaxWidth().basicMarquee(iterations = Int.MAX_VALUE)
                )
                Text(
                    when (np.state) {
                        PlaybackState.STATE_PLAYING -> "PLAY"
                        PlaybackState.STATE_BUFFERING -> "MEMUAT"
                        PlaybackState.STATE_PAUSED -> "JEDA"
                        else -> "STOP"
                    } + if (np.pkg == YT_MUSIC) "  YT MUSIC" else if (np.pkg.isNotEmpty()) "  " + np.pkg.substringAfterLast('.').uppercase() else "",
                    color = Color(0xFF8AE08A), fontSize = 11.sp, fontFamily = FontFamily.Monospace, maxLines = 1
                )
                if (np.reason == "perm") {
                    Text(
                        "[ IZINKAN ]", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { vm.openNotifAccess() }
                    )
                } else if (np.reason == "none") {
                    Text(
                        "[ BUKA YT MUSIC ]", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { vm.launchApp(YT_MUSIC) }
                    )
                }
            }
        }
        // bar posisi
        val frac = if (np.dur > 0) (np.pos.toFloat() / np.dur).coerceIn(0f, 1f) else 0f
        WaSlider(frac, np.dur > 0, Modifier.fillMaxWidth(), thumbWide = true, onChange = {}, onDone = { f ->
            ctrl[0]?.transportControls?.seekTo((np.dur * f).toLong())
        })
        // volume
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("VOL", color = WA_GREEN, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            WaSlider(vol, true, Modifier.weight(1f), thumbWide = false, onChange = { f ->
                vol = f
                am?.setStreamVolume(AudioManager.STREAM_MUSIC, (f * maxVol).roundToInt(), 0)
            }, onDone = {})
            Text("${(vol * 100).roundToInt()}%", color = WA_GREEN, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        }
        // tombol transport
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            WaKey(0) { ctrl[0]?.transportControls?.skipToPrevious() }
            WaKey(1) { ctrl[0]?.transportControls?.play() }
            WaKey(2) { ctrl[0]?.transportControls?.pause() }
            WaKey(3) { ctrl[0]?.transportControls?.stop() }
            WaKey(4) { ctrl[0]?.transportControls?.skipToNext() }
            Spacer(Modifier.weight(1f))
            WaKey(5) { vm.launchApp(ctrl[0]?.packageName ?: YT_MUSIC) }
        }
    }
}

@Composable
private fun WaSlider(
    value: Float,
    enabled: Boolean,
    modifier: Modifier,
    thumbWide: Boolean,
    onChange: (Float) -> Unit,
    onDone: (Float) -> Unit
) {
    var w by remember { mutableIntStateOf(1) }
    var drag by remember { mutableStateOf<Float?>(null) }
    val v = drag ?: value
    Box(
        modifier
            .height(14.dp)
            .onSizeChanged { w = it.width.coerceAtLeast(1) }
            .pointerInput(enabled) {
                if (enabled) detectTapGestures { o ->
                    val f = (o.x / w).coerceIn(0f, 1f)
                    onChange(f)
                    onDone(f)
                }
            }
            .pointerInput(enabled) {
                if (enabled) detectHorizontalDragGestures(
                    onDragStart = { o -> drag = (o.x / w).coerceIn(0f, 1f) },
                    onDragEnd = {
                        drag?.let { onDone(it) }
                        drag = null
                    },
                    onDragCancel = { drag = null }
                ) { change, amount ->
                    change.consume()
                    val nv = ((drag ?: v) + amount / w).coerceIn(0f, 1f)
                    drag = nv
                    onChange(nv)
                }
            }
            .drawBehind {
                val gh = 6.dp.toPx()
                val gy = (size.height - gh) / 2f
                drawRect(WA_DARK, Offset(0f, gy), Size(size.width, gh))
                drawRect(Color(0xFF00A000), Offset(0f, gy), Size(size.width * v, gh))
                val tw = (if (thumbWide) 28.dp else 12.dp).toPx()
                val tx = (size.width - tw) * v
                drawRect(if (enabled) Color(0xFFB0B4CC) else Color(0xFF5A5E74), Offset(tx, 0f), Size(tw, size.height))
                drawRect(Color(0xFF10101A), Offset(tx, 0f), Size(tw, size.height), style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))
            }
    )
}

/** 0 sebelumnya, 1 putar, 2 jeda, 3 berhenti, 4 berikutnya, 5 buka aplikasi. */
@Composable
private fun WaKey(kind: Int, onClick: () -> Unit) {
    Box(
        Modifier.width(40.dp).height(24.dp).background(WA_BTN).waBevel(true).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(12.dp)) {
            val w = size.width
            val h = size.height
            val c = Color(0xFFE8EAF8)
            fun tri(x0: Float, x1: Float, right: Boolean): Path = Path().apply {
                if (right) {
                    moveTo(x0, 0f); lineTo(x1, h / 2f); lineTo(x0, h)
                } else {
                    moveTo(x1, 0f); lineTo(x0, h / 2f); lineTo(x1, h)
                }
                close()
            }
            when (kind) {
                0 -> {
                    drawRect(c, Offset(0f, 0f), Size(w * 0.15f, h))
                    drawPath(tri(w * 0.2f, w, false), c)
                }
                1 -> drawPath(tri(w * 0.1f, w * 0.95f, true), c)
                2 -> {
                    drawRect(c, Offset(w * 0.15f, 0f), Size(w * 0.25f, h))
                    drawRect(c, Offset(w * 0.6f, 0f), Size(w * 0.25f, h))
                }
                3 -> drawRect(c, Offset(w * 0.1f, h * 0.1f), Size(w * 0.8f, h * 0.8f))
                4 -> {
                    drawPath(tri(0f, w * 0.8f, true), c)
                    drawRect(c, Offset(w * 0.85f, 0f), Size(w * 0.15f, h))
                }
                else -> {
                    val p = Path().apply {
                        moveTo(w / 2f, 0f); lineTo(w, h * 0.6f); lineTo(0f, h * 0.6f); close()
                    }
                    drawPath(p, c)
                    drawRect(c, Offset(0f, h * 0.75f), Size(w, h * 0.25f))
                }
            }
        }
    }
}
