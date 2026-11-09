package com.fauzi.tweaklauncher

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Palet gelap-cyan untuk tampilan HUD (mode Modern). */
fun hudPalette() = Palette(
    bg = Color(0xFF050B14),
    card = Color(0xB30A1826),
    inset = Color(0xFF08121E),
    line = Color(0xFF1E5A73),
    text = Color(0xFFD8F6FF),
    soft = Color(0xFF9ED8EA),
    sub = Color(0xFF6FA9BC),
    accent = Color(0xFF29D8FF),
    onAccent = Color(0xFF021018),
    radius = 6.dp,
    dark = true,
    wallpaper = false
)

val MainVm.hudOn: Boolean get() = cfg.hud && cfg.uiMode == "modern"

/** Kalimat status singkat berdasarkan kondisi HP. */
fun hudStatus(st: Stats): String = when {
    st.battery in 1..19 -> "Baterai rendah, colokkan charger."
    st.ramPct > 0.88f -> "Memori hampir penuh."
    st.tempPct > 0.85f -> "Suhu perangkat tinggi."
    else -> "Semua sistem normal."
}

private fun greeting(name: String): String {
    val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val g = when {
        h < 11 -> "Selamat pagi"
        h < 15 -> "Selamat siang"
        h < 18 -> "Selamat sore"
        else -> "Selamat malam"
    }
    return if (name.isBlank()) "$g." else "$g, ${name.trim()}."
}

@Composable
fun HudHeader(vm: MainVm, now: Long) {
    val cfg = vm.cfg
    val st = vm.stats
    val loc = Locale("id", "ID")
    val time = SimpleDateFormat(if (cfg.clock24) "HH:mm" else "h:mm a", loc).format(Date(now))
    val date = SimpleDateFormat("EEEE, d MMMM", loc).format(Date(now)).uppercase()
    val tr = rememberInfiniteTransition(label = "hud")
    val a1 by tr.animateFloat(0f, 360f, infiniteRepeatable(tween(16000, easing = LinearEasing)), label = "a1")
    val a2 by tr.animateFloat(360f, 0f, infiniteRepeatable(tween(9000, easing = LinearEasing)), label = "a2")
    val pulse by tr.animateFloat(0.5f, 1f, infiniteRepeatable(tween(1600), RepeatMode.Reverse), label = "pulse")
    val acc = Pal.accent
    val line = Pal.line
    val ctx = LocalContext.current
    val perm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) Jarvis.listen(ctx, vm) else Jarvis.say(ctx, vm, "Izin mikrofon ditolak.")
    }
    val tap: () -> Unit = {
        if (ctx.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) Jarvis.listen(ctx, vm)
        else perm.launch(Manifest.permission.RECORD_AUDIO)
    }
    val listening = Jarvis.state != "idle"
    LaunchedEffect(now) { JarvisWatch.check(ctx, vm) }

    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            Modifier.size(250.dp).clip(CircleShape).then(if (cfg.jvOn) Modifier.clickable { tap() } else Modifier),
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val c = center
                val r = size.minDimension / 2f
                drawCircle(
                    Brush.radialGradient(listOf(acc.copy(alpha = 0.22f * pulse), Color.Transparent), center = c, radius = r * 0.66f),
                    r * 0.66f
                )
                drawCircle(line, r * 0.98f, style = Stroke(1.dp.toPx()))
                rotate(a1, c) {
                    for (i in 0 until 60) {
                        val major = i % 5 == 0
                        rotate(i * 6f, c) {
                            drawLine(
                                acc.copy(alpha = if (major) 0.9f else 0.35f),
                                Offset(c.x, c.y - r * 0.98f),
                                Offset(c.x, c.y - r * (if (major) 0.89f else 0.94f)),
                                1.5.dp.toPx()
                            )
                        }
                    }
                }
                rotate(a2, c) {
                    for (k in 0 until 3) {
                        drawArc(
                            acc, k * 120f + 10f, 80f, false,
                            Offset(c.x - r * 0.78f, c.y - r * 0.78f), Size(r * 1.56f, r * 1.56f),
                            style = Stroke(4.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }
                drawCircle(acc.copy(alpha = if (listening) pulse else 0.5f), r * 0.64f, style = Stroke(if (listening) 5.dp.toPx() else 2.dp.toPx()))
                drawCircle(line, r * 0.58f, style = Stroke(1.dp.toPx()))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(time, color = Pal.text, fontSize = 40.sp, fontWeight = FontWeight.Light)
                Text(date, color = Pal.sub, fontSize = 10.sp, letterSpacing = 1.5.sp)
            }
        }
        Text(greeting(cfg.hudName), color = Pal.text, fontSize = 17.sp, fontWeight = FontWeight.Medium)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Canvas(Modifier.size(8.dp)) { drawCircle(acc.copy(alpha = pulse)) }
            Text("STATUS  " + hudStatus(st), color = Pal.soft, fontSize = 12.sp, letterSpacing = 0.5.sp)
        }
        if (Jarvis.alert.isNotBlank()) {
            Text(
                "PERINGATAN  " + Jarvis.alert,
                color = Color(0xFFFFB74D),
                fontSize = 12.sp,
                modifier = Modifier.clip(CircleShape).clickable { Jarvis.alert = "" }.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
        if (cfg.jvOn) {
            val line2 = when {
                Jarvis.state == "listen" -> if (Jarvis.heard.isBlank()) "Mendengarkan..." else "\"${Jarvis.heard}\""
                Jarvis.state == "think" -> "Memproses..."
                Jarvis.reply.isNotBlank() -> Jarvis.reply
                else -> "Ketuk cincin lalu bicara"
            }
            Text(line2, color = if (Jarvis.reply.isNotBlank() && !listening) Pal.accent else Pal.sub, fontSize = 13.sp)
        }
    }
}

@Composable
private fun HudGauge(label: String, value: String, pct: Float, color: Color, modifier: Modifier = Modifier) {
    val track = Pal.line
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val s = 5.dp.toPx()
                val tl = Offset(s, s)
                val sz = Size(size.width - 2 * s, size.height - 2 * s)
                drawArc(track, 135f, 270f, false, tl, sz, style = Stroke(s, cap = StrokeCap.Round))
                drawArc(color, 135f, 270f * pct.coerceIn(0.02f, 1f), false, tl, sz, style = Stroke(s, cap = StrokeCap.Round))
            }
            Text(value, color = Pal.text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
        Text(label, color = Pal.sub, fontSize = 11.sp, letterSpacing = 0.5.sp)
    }
}

@Composable
fun HudStats(vm: MainVm) {
    val st = vm.stats
    FoldCard("home_stats", "Status sistem", "RAM ${st.ramText} · Baterai ${st.battery}%") {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            HudGauge("RAM", st.ramText, st.ramPct, Pal.accent)
            HudGauge("BATERAI", "${st.battery}%", st.battery / 100f, Green)
            HudGauge("SUHU", st.tempText, st.tempPct, Orange)
            HudGauge("DATA", "${(st.storagePct * 100).toInt()}%", st.storagePct, Blue)
        }
    }
}
