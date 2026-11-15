package com.fauzi.tweaklauncher

import android.content.Context
import android.net.wifi.WifiManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

data class RunApp(val app: AppItem, val mb: Int)

/** Daftar aplikasi yang prosesnya sedang hidup (lewat Shizuku). */
object Running {
    private val KEEP = listOf("moe.shizuku", "inputmethod", "keyboard", "com.fauzi.tweaklauncher")

    fun load(vm: MainVm): Pair<List<RunApp>, String?> {
        if (Shell.state() != ShizukuState.READY) return Pair(emptyList(), "Shizuku belum siap. Jalankan Shizuku dan beri izin dulu.")
        val (code, out) = Shell.run("ps -A -o RSS,NAME")
        if (code != 0 && out.isBlank()) return Pair(emptyList(), "Gagal membaca daftar proses.")
        val rss = HashMap<String, Long>()
        out.lines().forEach { ln ->
            val t = ln.trim().split(Regex("\\s+"))
            if (t.size < 2) return@forEach
            val kb = t[0].toLongOrNull() ?: return@forEach
            val pkg = t[1].substringBefore(':')
            rss[pkg] = (rss[pkg] ?: 0L) + kb
        }
        val byPkg = vm.apps.associateBy { it.pkg }
        val list = rss.mapNotNull { (p, kb) ->
            val a = byPkg[p] ?: return@mapNotNull null
            if (KEEP.any { p.contains(it) }) return@mapNotNull null
            RunApp(a, (kb / 1024).toInt())
        }.sortedByDescending { it.mb }
        return Pair(list, null)
    }
}

@Composable
fun RunningAppsCard(vm: MainVm) {
    val scope = rememberCoroutineScope()
    var list by remember { mutableStateOf<List<RunApp>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf("") }
    var loaded by remember { mutableStateOf(false) }

    fun refresh() {
        if (busy) return
        busy = true
        scope.launch {
            val (l, err) = withContext(Dispatchers.IO) { Running.load(vm) }
            list = l
            msg = err ?: ""
            loaded = true
            busy = false
        }
    }

    FoldCard("running", "Aplikasi berjalan", if (loaded) "${list.size} aplikasi" else "Ketuk Muat ulang", defaultOpen = false) {
        Hint("Daftar aplikasi yang prosesnya masih hidup di latar, urut dari RAM terbesar. Hentikan untuk menutupnya, Tidurkan untuk melarangnya jalan di latar. Butuh Shizuku.")
        ActionButton(if (busy) "Memuat..." else "Muat ulang", Modifier.fillMaxWidth(), primary = true, enabled = !busy) { refresh() }
        if (msg.isNotEmpty()) Text(msg, color = Pal.sub, fontSize = 12.sp)
        if (loaded && list.isEmpty() && msg.isEmpty()) Text("Tidak ada aplikasi yang berjalan di latar.", color = Pal.sub, fontSize = 13.sp)
        if (list.isNotEmpty()) {
            Text("Total sekitar ${list.sumOf { it.mb }} MB", color = Pal.soft, fontSize = 12.sp)
        }
        list.forEach { r ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppIcon(r.app, 34.dp, badge = false)
                Column(Modifier.weight(1f)) {
                    Text(r.app.label, color = Pal.text, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${r.mb} MB", color = Pal.sub, fontSize = 11.sp)
                }
                Text(
                    "Hentikan", color = Pal.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable {
                        vm.forceStop(r.app.pkg)
                        list = list - r
                    }.padding(horizontal = 6.dp, vertical = 8.dp)
                )
                Text(
                    "Tidurkan", color = Pal.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable {
                        vm.sleepApp(r.app.pkg)
                        list = list - r
                    }.padding(horizontal = 6.dp, vertical = 8.dp)
                )
            }
        }
        if (list.any { !it.app.system }) {
            val victims = list.filter { !it.app.system }
            ActionButton("Hentikan semua aplikasi pengguna (${victims.size})", Modifier.fillMaxWidth()) {
                victims.forEach { vm.forceStop(it.app.pkg) }
                list = list - victims.toSet()
            }
        }
    }
}

// ---------------------------------------------------------------- Jaringan

private fun tcpPing(host: String, port: Int = 443, tries: Int = 4): Pair<Int, Int> {
    var ok = 0
    var sum = 0L
    repeat(tries) {
        val t0 = System.nanoTime()
        try {
            Socket().use { it.connect(InetSocketAddress(host, port), 3000) }
            sum += (System.nanoTime() - t0) / 1_000_000
            ok++
        } catch (e: Exception) {
        }
    }
    return Pair(if (ok == 0) -1 else (sum / ok).toInt(), tries - ok)
}

private fun signalText(ctx: Context): String {
    val parts = ArrayList<String>()
    try {
        @Suppress("DEPRECATION")
        val wm = ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        @Suppress("DEPRECATION")
        val info = wm.connectionInfo
        if (wm.isWifiEnabled && info != null && info.rssi > -127 && info.networkId != -1) {
            val q = when {
                info.rssi >= -60 -> "kuat"
                info.rssi >= -70 -> "baik"
                info.rssi >= -80 -> "lemah"
                else -> "sangat lemah"
            }
            parts.add("Wi-Fi ${info.rssi} dBm ($q)")
        }
    } catch (e: Exception) {
    }
    if (Shell.state() == ShizukuState.READY) {
        val out = Shell.run("dumpsys telephony.registry").second
        val rsrp = Regex("rsrp=(-?\\d+)").find(out)?.groupValues?.get(1)?.toIntOrNull()
        val rssi = Regex("rssi=(-\\d+)").find(out)?.groupValues?.get(1)?.toIntOrNull()
        val v = rsrp?.takeIf { it in -140..-40 }
        if (v != null) {
            val q = when {
                v >= -90 -> "kuat"
                v >= -105 -> "cukup"
                v >= -115 -> "lemah"
                else -> "sangat lemah"
            }
            parts.add("Seluler 4G/5G $v dBm ($q)")
        } else if (rssi != null && rssi in -120..-40) {
            parts.add("Seluler $rssi dBm")
        }
    }
    return if (parts.isEmpty()) "Kekuatan sinyal tidak terbaca." else parts.joinToString(" · ")
}

@Composable
fun NetworkCard(vm: MainVm) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var lines by remember { mutableStateOf<List<String>>(emptyList()) }
    var tip by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf("") }

    FoldCard("network", "Jaringan", "Cek koneksi, hemat data, sambung ulang", defaultOpen = false) {
        Hint("Aplikasi tidak bisa menambah sinyal dari menara. Yang bisa dibantu: mengukur kondisi koneksi, menghemat data supaya sinyal lemah terpakai untuk aplikasi penting, dan menyambung ulang jaringan yang macet.")
        ActionButton(if (busy) "Mengukur..." else "Cek kualitas koneksi", Modifier.fillMaxWidth(), primary = true, enabled = !busy) {
            busy = true
            lines = emptyList()
            tip = ""
            scope.launch {
                val res = withContext(Dispatchers.IO) {
                    val targets = listOf("Cloudflare" to "1.1.1.1", "Google" to "8.8.8.8", "AdGuard" to "94.140.14.14")
                    val r = targets.map { (n, h) -> Triple(n, h, tcpPing(h)) }
                    Pair(r, signalText(ctx))
                }
                val out = ArrayList<String>()
                out.add(res.second)
                res.first.forEach { (n, _, p) ->
                    out.add(if (p.first < 0) "$n: tidak terjangkau" else "$n: ${p.first} ms, hilang ${p.second} dari 4")
                }
                lines = out
                val good = res.first.map { it.third.first }.filter { it >= 0 }
                val avg = if (good.isEmpty()) -1 else good.average().toInt()
                val loss = res.first.sumOf { it.third.second }
                tip = when {
                    avg < 0 -> "Tidak ada internet. Coba Sambung ulang jaringan, atau pindah ke tempat yang sinyalnya lebih baik."
                    loss >= 4 || avg > 400 -> "Koneksi buruk. Coba Sambung ulang jaringan, nyalakan Hemat data, atau pindah posisi."
                    avg > 150 -> "Koneksi lambat tapi masih bisa dipakai. Hemat data bisa membantu."
                    else -> "Koneksi baik."
                }
                busy = false
            }
        }
        lines.forEach { Text(it, color = Pal.text, fontSize = 13.sp) }
        if (tip.isNotEmpty()) Text(tip, color = Pal.accent, fontSize = 13.sp)

        ActionButton("Sambung ulang jaringan", Modifier.fillMaxWidth()) {
            msg = "Menyambung ulang..."
            scope.launch {
                msg = withContext(Dispatchers.IO) {
                    if (Shell.state() != ShizukuState.READY) return@withContext "Shizuku belum siap. Jalankan Shizuku dan beri izin dulu."
                    val wifiOn = try {
                        (ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager).isWifiEnabled
                    } catch (e: Exception) {
                        false
                    }
                    val dataOn = Shell.run("settings get global mobile_data").second.trim() == "1"
                    val sb = StringBuilder()
                    if (wifiOn) sb.append("svc wifi disable; ")
                    if (dataOn) sb.append("svc data disable; ")
                    sb.append("sleep 2; ")
                    if (wifiOn) sb.append("svc wifi enable; ")
                    if (dataOn) sb.append("svc data enable; ")
                    if (!wifiOn && !dataOn) "Wi-Fi dan data seluler sedang mati." else {
                        Shell.run(sb.toString())
                        "Jaringan disambung ulang. Tunggu beberapa detik."
                    }
                }
            }
        }
        ActionButton("Nyalakan hemat data", Modifier.fillMaxWidth()) {
            vm.runTool(null) {
                if (Shell.state() != ShizukuState.READY) TweakResult(false, "Shizuku belum siap.")
                else {
                    val (c, o) = Shell.run("cmd netpolicy set restrict-background true")
                    if (c == 0) TweakResult(true, "Hemat data aktif. Aplikasi latar dibatasi dari data seluler.") else TweakResult(false, "Gagal: $o")
                }
            }
        }
        ActionButton("Matikan hemat data", Modifier.fillMaxWidth()) {
            vm.runTool(null) {
                if (Shell.state() != ShizukuState.READY) TweakResult(false, "Shizuku belum siap.")
                else {
                    val (c, o) = Shell.run("cmd netpolicy set restrict-background false")
                    if (c == 0) TweakResult(true, "Hemat data dimatikan.") else TweakResult(false, "Gagal: $o")
                }
            }
        }
        if (msg.isNotEmpty()) Text(msg, color = Pal.sub, fontSize = 12.sp)
    }
}
