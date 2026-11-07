package com.fauzi.tweaklauncher

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.rememberCoroutineScope
import kotlin.math.roundToInt

/** Ikon speaker kecil untuk tray. */
@Composable
fun TraySpeaker(modifier: Modifier = Modifier, tint: Color = Color.Black) {
    Canvas(modifier.size(16.dp)) {
        val w = size.width
        val p = Path().apply {
            moveTo(w * 0.1f, w * 0.38f)
            lineTo(w * 0.3f, w * 0.38f)
            lineTo(w * 0.55f, w * 0.15f)
            lineTo(w * 0.55f, w * 0.85f)
            lineTo(w * 0.3f, w * 0.62f)
            lineTo(w * 0.1f, w * 0.62f)
            close()
        }
        drawPath(p, tint)
        drawArc(tint, -50f, 100f, false, Offset(w * 0.5f, w * 0.3f), Size(w * 0.35f, w * 0.4f), style = Stroke(w * 0.08f))
    }
}

/** Ikon Wi-Fi kecil (tiga batang) untuk tray. */
@Composable
fun TrayWifi(modifier: Modifier = Modifier, tint: Color = Color.Black) {
    Canvas(modifier.size(16.dp)) {
        val w = size.width
        for (i in 0 until 3) {
            val h = w * (0.35f + 0.25f * i)
            drawRect(tint, Offset(w * (0.1f + 0.3f * i), w * 0.9f - h), Size(w * 0.2f, h))
        }
    }
}

/** Ikon baterai kecil, terisi sesuai [pct]. */
@Composable
fun TrayBattery(pct: Int, modifier: Modifier = Modifier, tint: Color = Color.Black) {
    Canvas(modifier.size(width = 18.dp, height = 12.dp)) {
        val w = size.width
        val h = size.height
        drawRect(tint, Offset(0f, 0f), Size(w * 0.9f, h), style = Stroke(1.5.dp.toPx()))
        drawRect(tint, Offset(w * 0.9f, h * 0.3f), Size(w * 0.1f, h * 0.4f))
        val f = (pct.coerceIn(0, 100) / 100f)
        drawRect(
            if (pct <= 20) Color(0xFFC62828) else Color(0xFF008000),
            Offset(2.dp.toPx(), 2.dp.toPx()),
            Size((w * 0.9f - 4.dp.toPx()) * f, h - 4.dp.toPx())
        )
    }
}

@Composable
private fun TrayButton(label: String, xp: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .then(
                if (xp) Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFF1EFE2))
                    .border(1.dp, Color(0xFF003C74), RoundedCornerShape(4.dp))
                else Modifier.background(Color(0xFFC0C0C0)).bevel(true)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) { Text(label, color = Color.Black, fontSize = 13.sp) }
}

private fun wifiOn(ctx: Context): Boolean = try {
    (ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager).isWifiEnabled
} catch (e: Exception) {
    false
}

private fun netText(ctx: Context): String = try {
    val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val caps = cm.getNetworkCapabilities(cm.activeNetwork)
    when {
        caps == null -> "Tidak terhubung"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Terhubung lewat Wi-Fi"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Terhubung lewat data seluler"
        else -> "Terhubung"
    }
} catch (e: Exception) {
    "Status tidak diketahui"
}

/** Popup volume atau Wi-Fi saat ikon tray diketuk (mode Windows 95). */
@Composable
fun W95TrayPopup(vm: MainVm, barHeight: Dp, xp: Boolean = false) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val which = vm.trayPop
    Box(
        Modifier
            .fillMaxSize()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { vm.trayPop = "" }
    ) {
        Column(
            Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 4.dp, bottom = barHeight + 2.dp)
                .width(220.dp)
                .then(
                    if (xp) Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFECE9D8))
                        .border(2.dp, Color(0xFF0A4FC8), RoundedCornerShape(8.dp))
                    else Modifier.background(Color(0xFFC0C0C0)).bevel(true)
                )
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (which == "vol") {
                val am = remember { ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
                val max = remember { am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }
                var vol by remember { mutableIntStateOf(am.getStreamVolume(AudioManager.STREAM_MUSIC)) }
                var muted by remember { mutableStateOf(am.isStreamMute(AudioManager.STREAM_MUSIC)) }
                Text("Volume", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Slider(
                    value = vol.toFloat(),
                    onValueChange = {
                        vol = it.roundToInt()
                        am.setStreamVolume(AudioManager.STREAM_MUSIC, vol, 0)
                        muted = am.isStreamMute(AudioManager.STREAM_MUSIC)
                    },
                    valueRange = 0f..max.toFloat(),
                    steps = (max - 1).coerceAtLeast(0),
                    colors = SliderDefaults.colors(
                        thumbColor = if (xp) Color(0xFF245EDC) else Color(0xFF000080),
                        activeTrackColor = if (xp) Color(0xFF245EDC) else Color(0xFF000080),
                        inactiveTrackColor = Color(0xFF808080),
                        activeTickColor = Color.Transparent,
                        inactiveTickColor = Color.Transparent
                    )
                )
                Text("$vol / $max", color = Color.Black, fontSize = 12.sp)
                TrayButton(if (muted) "Suarakan" else "Bisukan", xp) {
                    am.adjustStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        if (muted) AudioManager.ADJUST_UNMUTE else AudioManager.ADJUST_MUTE,
                        0
                    )
                    muted = am.isStreamMute(AudioManager.STREAM_MUSIC)
                }
            } else {
                var on by remember { mutableStateOf(wifiOn(ctx)) }
                var net by remember { mutableStateOf(netText(ctx)) }
                LaunchedEffect(Unit) {
                    while (true) {
                        on = wifiOn(ctx)
                        net = netText(ctx)
                        delay(1500)
                    }
                }
                Text("Wi-Fi", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(if (on) "Wi-Fi aktif" else "Wi-Fi mati", color = Color.Black, fontSize = 13.sp)
                Text(net, color = Color.Black, fontSize = 12.sp)
                TrayButton(if (on) "Matikan Wi-Fi" else "Nyalakan Wi-Fi", xp) {
                    scope.launch {
                        val ok = withContext(Dispatchers.IO) {
                            if (Shell.state() == ShizukuState.READY) {
                                Shell.run(if (on) "svc wifi disable" else "svc wifi enable").first == 0
                            } else false
                        }
                        if (!ok) openWifiPanel(ctx)
                    }
                }
                TrayButton("Setelan Wi-Fi...", xp) { openWifiPanel(ctx) }
            }
        }
    }
}

private fun openWifiPanel(ctx: Context) {
    try {
        ctx.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: Exception) {
    }
}
