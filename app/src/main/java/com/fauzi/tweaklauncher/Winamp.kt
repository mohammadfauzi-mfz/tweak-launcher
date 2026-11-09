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
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
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

data class NowPlaying(
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

class WaItem(val id: Long, val text: String, val dur: Long)

/** Data bersama untuk jendela Winamp (utama, equalizer, playlist). */
object WaHub {
    var np by mutableStateOf(NowPlaying(reason = "none"))
    var ctrl: MediaController? = null
    var queue by mutableStateOf<List<WaItem>>(emptyList())
    var activeId by mutableLongStateOf(-1L)
    val history = mutableStateListOf<WaItem>()
    val eq = mutableStateListOf<Float>().apply { repeat(11) { add(0f) } }
    var eqOn by mutableStateOf(true)
    var eqAuto by mutableStateOf(false)
    var shuffle by mutableStateOf(false)
    var repeat by mutableStateOf(false)
    var preset by mutableIntStateOf(0)

    fun refresh(ctx: Context) {
        val (n, c) = readSession(ctx)
        np = n
        ctrl = c
        val q = try {
            c?.queue?.map {
                WaItem(
                    it.queueId,
                    (it.description.title?.toString() ?: "") +
                        (it.description.subtitle?.toString()?.let { s -> if (s.isNotEmpty()) " - $s" else "" } ?: ""),
                    0L
                )
            } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
        queue = q
        activeId = c?.playbackState?.activeQueueItemId ?: -1L
        val label = n.label()
        if (n.title.isNotEmpty() && history.lastOrNull()?.text != label) {
            history.add(WaItem(-1L, label, n.dur))
            while (history.size > 200) history.removeAt(0)
        }
    }
}

fun NowPlaying.label(): String =
    title.ifEmpty { "(tanpa judul)" } + (if (artist.isNotEmpty()) " - $artist" else "")

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

/** Pembaca sesi media: dijalankan sekali selama ada jendela Winamp terbuka. */
@Composable
fun WinampPoller() {
    val ctx = LocalContext.current
    LaunchedEffect(Unit) {
        while (true) {
            WaHub.refresh(ctx)
            delay(500)
        }
    }
}

@Composable
fun WinampWindow(vm: MainVm, w: GameWin, maxW: Float, maxH: Float) {
    val d = LocalDensity.current
    val minX = -maxW * 0.6f
    val maxX = maxW - with(d) { 90.dp.toPx() }
    val maxY = maxH - with(d) { 50.dp.toPx() }
    val title = when (w.kind) {
        "waeq" -> "WINAMP EQUALIZER"
        "wapl" -> "WINAMP PLAYLIST"
        else -> "WINAMP"
    }
    Column(
        Modifier
            .offset { IntOffset(w.x.roundToInt(), w.y.roundToInt()) }
            .zIndex(w.z.toFloat())
            .width(300.dp)
            .background(WA_BG)
            .waBevel(true)
            .padding(2.dp)
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
                .background(Brush.verticalGradient(listOf(Color(0xFF5C617F), Color(0xFF30334A))))
                .pointerInput(Unit) {
                    detectDragGestures { change, drag ->
                        change.consume()
                        w.x = (w.x + drag.x).coerceIn(minX, maxX)
                        w.y = (w.y + drag.y).coerceIn(0f, maxY)
                    }
                }
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            WaStripes(Modifier.weight(1f))
            Text(title, color = Color(0xFFE8EAF8), fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            WaStripes(Modifier.weight(1f))
            Box(
                Modifier.size(14.dp).background(WA_BTN).waBevel(true).clickable { vm.gameWins.remove(w) },
                contentAlignment = Alignment.Center
            ) { Text("x", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
        }
        when (w.kind) {
            "waeq" -> WaEq()
            "wapl" -> WaPlaylist(vm)
            else -> WaMain(vm)
        }
    }
}

@Composable
private fun WaStripes(modifier: Modifier) {
    Canvas(modifier.height(10.dp)) {
        val t = 1.dp.toPx()
        for (k in 0 until 4) {
            val y = k * 3 * t
            drawRect(Color(0xFFB0B5D2), Offset(0f, y), Size(size.width, t))
            drawRect(Color(0xFF1A1C2A), Offset(0f, y + t), Size(size.width, t))
        }
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

@Composable
private fun WaToggle(text: String, on: Boolean, width: androidx.compose.ui.unit.Dp, onClick: () -> Unit) {
    Box(
        Modifier.width(width).height(16.dp).background(WA_BTN).waBevel(!on).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1,
            color = if (on) Color(0xFF9CFF9C) else Color(0xFFC8CBE0)
        )
    }
}

@Composable
private fun Lcd(text: String, on: Boolean = true, size: Int = 10) {
    Text(text, color = if (on) WA_GREEN else Color(0xFF1E5A1E), fontSize = size.sp, fontFamily = FontFamily.Monospace, maxLines = 1)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WaMain(vm: MainVm) {
    val ctx = LocalContext.current
    val np = WaHub.np
    var phase by remember { mutableFloatStateOf(0f) }
    var level by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(80)
            phase += 0.18f
            level = if (WaHub.np.playing) minOf(1f, level + 0.2f) else maxOf(0f, level - 0.12f)
        }
    }
    val am = remember { ctx.getSystemService(AudioManager::class.java) }
    val maxVol = remember { am?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15 }
    var vol by remember { mutableFloatStateOf((am?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0).toFloat() / maxVol) }
    val eqOpen = vm.gameWins.any { it.kind == "waeq" }
    val plOpen = vm.gameWins.any { it.kind == "wapl" }

    Column(Modifier.padding(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            // layar kiri: waktu dan spektrum
            Row(
                Modifier.width(112.dp).height(54.dp).background(WA_DARK).waBevel(false).padding(horizontal = 4.dp, vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Column(Modifier.fillMaxSize().weight(0.12f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Lcd("\u25B6", np.state == PlaybackState.STATE_PLAYING, 8)
                    Lcd("\u2759\u2759", np.state == PlaybackState.STATE_PAUSED, 7)
                    Lcd("\u25A0", np.state != PlaybackState.STATE_PLAYING && np.state != PlaybackState.STATE_PAUSED, 8)
                }
                Column(Modifier.weight(0.88f)) {
                    Text(fmt(np.pos), color = WA_GREEN, fontSize = 25.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, maxLines = 1)
                    Canvas(Modifier.fillMaxWidth().height(16.dp)) {
                        val n = 19
                        val gap = 1.dp.toPx()
                        val bw = (size.width - gap * (n - 1)) / n
                        val seg = 2.dp.toPx()
                        for (i in 0 until n) {
                            val a = abs(sin(phase * 1.7f + i * 0.9f) * cos(phase * 0.8f + i * 0.37f))
                            val h = 0.1f + 0.9f * a * level
                            val cnt = (h * (size.height / seg)).toInt().coerceAtLeast(1)
                            for (j in 0 until cnt) {
                                val col = if (j < 4) Color(0xFF20D020) else if (j < 6) Color(0xFFD0D020) else Color(0xFFE08020)
                                drawRect(col, Offset(i * (bw + gap), size.height - (j + 1) * seg), Size(bw, seg - 0.5f))
                            }
                        }
                    }
                }
            }
            // layar kanan: judul dan info
            Column(
                Modifier.weight(1f).height(54.dp).background(WA_DARK).waBevel(false).padding(horizontal = 5.dp, vertical = 3.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                val line = when (np.reason) {
                    "perm" -> "Izinkan akses notifikasi untuk membaca pemutar"
                    "none" -> "Belum ada pemutar aktif. Putar lagu di YouTube Music."
                    else -> np.label() + (if (np.dur > 0) " (" + fmt(np.dur) + ")" else "")
                }
                Text(
                    line, color = WA_GREEN, fontSize = 12.sp, fontFamily = FontFamily.Monospace,
                    maxLines = 1, softWrap = false,
                    modifier = Modifier.fillMaxWidth().basicMarquee(iterations = Int.MAX_VALUE)
                )
                when (np.reason) {
                    "perm" -> Text("[ IZINKAN ]", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { vm.openNotifAccess() })
                    "none" -> Text("[ BUKA YT MUSIC ]", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { vm.launchApp(YT_MUSIC) })
                    else -> Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Lcd("--", true, 9); Lcd("kbps", false, 8)
                        Lcd("--", true, 9); Lcd("kHz", false, 8)
                        Spacer(Modifier.weight(1f))
                        Lcd("mono", false, 8)
                        Lcd("stereo", np.playing, 8)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Lcd(if (np.pkg == YT_MUSIC) "YT MUSIC" else np.pkg.substringAfterLast('.').uppercase(), np.pkg.isNotEmpty(), 8)
                    Lcd(when (np.state) {
                        PlaybackState.STATE_PLAYING -> "PLAY"
                        PlaybackState.STATE_BUFFERING -> "MEMUAT"
                        PlaybackState.STATE_PAUSED -> "JEDA"
                        else -> "STOP"
                    }, true, 8)
                }
            }
        }
        // bar posisi
        val frac = if (np.dur > 0) (np.pos.toFloat() / np.dur).coerceIn(0f, 1f) else 0f
        WaSlider(frac, np.dur > 0, Modifier.fillMaxWidth(), thumbWide = true, gradient = false, onChange = {}, onDone = { f ->
            WaHub.ctrl?.transportControls?.seekTo((np.dur * f).toLong())
        })
        // volume, EQ, PL
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            WaSlider(vol, true, Modifier.width(150.dp), thumbWide = false, gradient = true, onChange = { f ->
                vol = f
                am?.setStreamVolume(AudioManager.STREAM_MUSIC, (f * maxVol).roundToInt(), 0)
            }, onDone = {})
            Lcd("${(vol * 100).roundToInt()}", true, 9)
            Spacer(Modifier.weight(1f))
            WaToggle("EQ", eqOpen, 30.dp) { vm.toggleGame("waeq") }
            WaToggle("PL", plOpen, 30.dp) { vm.toggleGame("wapl") }
        }
        // tombol transport
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
            WaKey(0) { WaHub.ctrl?.transportControls?.skipToPrevious() }
            WaKey(1) { WaHub.ctrl?.transportControls?.play() }
            WaKey(2) { WaHub.ctrl?.transportControls?.pause() }
            WaKey(3) { WaHub.ctrl?.transportControls?.stop() }
            WaKey(4) { WaHub.ctrl?.transportControls?.skipToNext() }
            WaKey(5) { vm.launchApp(WaHub.ctrl?.packageName ?: YT_MUSIC) }
            Spacer(Modifier.weight(1f))
            WaToggle("SHUFFLE", WaHub.shuffle, 52.dp) { WaHub.shuffle = !WaHub.shuffle }
            WaToggle("\u21BB", WaHub.repeat, 26.dp) { WaHub.repeat = !WaHub.repeat }
        }
    }
}

// ---------------------------------------------------------------- Equalizer

private val EQ_LABELS = listOf("70", "180", "320", "600", "1K", "3K", "6K", "12K", "14K", "16K")
private val EQ_PRESETS = listOf(
    "Flat" to List(11) { 0f },
    "Rock" to listOf(0f, 0.45f, 0.3f, 0.1f, -0.2f, -0.3f, 0.1f, 0.3f, 0.5f, 0.5f, 0.5f),
    "Pop" to listOf(0f, -0.2f, 0.1f, 0.3f, 0.45f, 0.3f, 0f, -0.1f, -0.2f, -0.2f, -0.2f),
    "Bass" to listOf(0f, 0.7f, 0.6f, 0.45f, 0.2f, 0f, 0f, 0f, 0f, 0f, 0f),
    "Treble" to listOf(0f, 0f, 0f, 0f, 0f, 0f, 0.2f, 0.4f, 0.6f, 0.7f, 0.7f),
    "Vokal" to listOf(0f, -0.3f, -0.2f, 0f, 0.4f, 0.5f, 0.4f, 0.2f, 0f, -0.1f, -0.2f)
)

@Composable
private fun WaEq() {
    Column(Modifier.padding(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            WaToggle("ON", WaHub.eqOn, 32.dp) { WaHub.eqOn = !WaHub.eqOn }
            WaToggle("AUTO", WaHub.eqAuto, 38.dp) { WaHub.eqAuto = !WaHub.eqAuto }
            Canvas(Modifier.weight(1f).height(26.dp).background(WA_DARK).waBevel(false)) {
                val n = 10
                val p = Path()
                for (i in 0 until n) {
                    val x = size.width * (i + 0.5f) / n
                    val y = size.height * (0.5f - 0.45f * WaHub.eq[i + 1])
                    if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
                }
                drawPath(p, if (WaHub.eqOn) Color(0xFF20D020) else Color(0xFF1E5A1E), style = androidx.compose.ui.graphics.drawscope.Stroke(1.5.dp.toPx()))
            }
            WaToggle("PRESETS", false, 56.dp) {
                WaHub.preset = (WaHub.preset + 1) % EQ_PRESETS.size
                EQ_PRESETS[WaHub.preset].second.forEachIndexed { i, v -> WaHub.eq[i] = v }
            }
        }
        Text(EQ_PRESETS[WaHub.preset].first, color = Color(0xFF8AE08A), fontSize = 9.sp, fontFamily = FontFamily.Monospace)
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            Column(Modifier.width(28.dp).height(62.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Lcd("+12db", true, 8)
                Lcd("0 db", true, 8)
                Lcd("-12db", true, 8)
            }
            Column(Modifier.width(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                WaVSlider(WaHub.eq[0]) { WaHub.eq[0] = it }
                Lcd("PRE", true, 8)
            }
            Spacer(Modifier.width(6.dp))
            for (i in 0 until 10) {
                Column(Modifier.width(21.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    WaVSlider(WaHub.eq[i + 1]) { WaHub.eq[i + 1] = it }
                    Lcd(EQ_LABELS[i], true, 7)
                }
            }
        }
    }
}

@Composable
private fun WaVSlider(value: Float, onChange: (Float) -> Unit) {
    var h by remember { mutableIntStateOf(1) }
    Box(
        Modifier
            .width(16.dp)
            .height(62.dp)
            .onSizeChanged { h = it.height.coerceAtLeast(1) }
            .pointerInput(Unit) {
                detectTapGestures { o -> onChange((1f - 2f * (o.y / h)).coerceIn(-1f, 1f)) }
            }
            .pointerInput(Unit) {
                detectVerticalDragGestures { change, _ ->
                    change.consume()
                    onChange((1f - 2f * (change.position.y / h)).coerceIn(-1f, 1f))
                }
            }
            .drawBehind {
                val gw = 6.dp.toPx()
                val gx = (size.width - gw) / 2f
                drawRect(
                    Brush.verticalGradient(listOf(Color(0xFFE02020), Color(0xFFE0E020), Color(0xFF20C020))),
                    Offset(gx, 0f), Size(gw, size.height)
                )
                drawRect(Color(0x99000000), Offset(gx, 0f), Size(gw, size.height))
                val th = 8.dp.toPx()
                val y = (size.height - th) * (1f - value) / 2f
                drawRect(Color(0xFFB0B4CC), Offset(0f, y), Size(size.width, th))
                drawRect(Color(0xFF10101A), Offset(0f, y), Size(size.width, th), style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))
                drawRect(Color(0xFF10101A), Offset(0f, y + th / 2f), Size(size.width, 1.dp.toPx()))
            }
    )
}

// ---------------------------------------------------------------- Playlist

@Composable
private fun WaPlaylist(vm: MainVm) {
    val np = WaHub.np
    val useQueue = WaHub.queue.isNotEmpty()
    val items = if (useQueue) WaHub.queue else WaHub.history.toList()
    val cur = np.label()
    Column(Modifier.padding(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Column(
            Modifier.fillMaxWidth().height(150.dp).background(WA_DARK).waBevel(false)
                .verticalScroll(rememberScrollState()).padding(horizontal = 5.dp, vertical = 3.dp)
        ) {
            if (items.isEmpty()) {
                Text(
                    if (np.reason == "perm") "Izinkan akses notifikasi dulu." else "Playlist kosong. Lagu yang diputar akan tercatat di sini.",
                    color = Color(0xFF8AE08A), fontSize = 11.sp, fontFamily = FontFamily.Monospace
                )
            }
            items.forEachIndexed { i, it ->
                val now = if (useQueue) it.id == WaHub.activeId else (np.title.isNotEmpty() && it.text == cur && i == items.lastIndex)
                Row(
                    Modifier.fillMaxWidth().clickable {
                        if (useQueue) WaHub.ctrl?.transportControls?.skipToQueueItem(it.id)
                    }.padding(vertical = 1.dp)
                ) {
                    Text(
                        "${i + 1}. ${it.text}", color = if (now) Color.White else WA_GREEN, fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace, maxLines = 1, modifier = Modifier.weight(1f)
                    )
                    val dur = if (now && np.dur > 0) np.dur else it.dur
                    if (dur > 0) Text(" " + fmt(dur).trimStart('0').ifEmpty { "0" }.let { s -> if (s.startsWith(":")) "0$s" else s }, color = if (now) Color.White else WA_GREEN, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            WaToggle("ADD", false, 32.dp) { vm.launchApp(WaHub.ctrl?.packageName ?: YT_MUSIC) }
            WaToggle("REM", false, 32.dp) { WaHub.history.clear() }
            WaToggle("SEL", false, 32.dp) {}
            WaToggle("MISC", false, 36.dp) {}
            Spacer(Modifier.weight(1f))
            Lcd(fmt(np.pos).trimStart('0').ifEmpty { "0" } + "/" + (if (np.dur > 0) fmt(np.dur) else "--:--"), true, 9)
            Spacer(Modifier.weight(1f))
            WaToggle("LIST OPTS", false, 56.dp) {}
        }
    }
}

@Composable
private fun WaSlider(
    value: Float,
    enabled: Boolean,
    modifier: Modifier,
    thumbWide: Boolean,
    gradient: Boolean,
    onChange: (Float) -> Unit,
    onDone: (Float) -> Unit
) {
    var w by remember { mutableIntStateOf(1) }
    var drag by remember { mutableStateOf<Float?>(null) }
    val v = drag ?: value
    Box(
        modifier
            .height(12.dp)
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
                if (gradient) {
                    drawRect(
                        Brush.horizontalGradient(listOf(Color(0xFF20C020), Color(0xFFE0E020), Color(0xFFE02020))),
                        Offset(0f, gy), Size(size.width, gh)
                    )
                    drawRect(Color(0xCC000000), Offset(size.width * v, gy), Size(size.width * (1f - v), gh))
                } else {
                    drawRect(Color(0xFF00A000), Offset(0f, gy), Size(size.width * v, gh))
                }
                val tw = (if (thumbWide) 26.dp else 12.dp).toPx()
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
        Modifier.width(30.dp).height(20.dp).background(WA_BTN).waBevel(true).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(10.dp)) {
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
