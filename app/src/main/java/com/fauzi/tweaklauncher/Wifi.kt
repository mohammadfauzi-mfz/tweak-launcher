package com.fauzi.tweaklauncher

import android.content.Context
import android.net.wifi.WifiManager
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

data class WifiNow(
    val connected: Boolean = false,
    val band: String = "",
    val channel: Int = 0,
    val rssi: Int = 0,
    val link: Int = 0,
    val tx: Int = 0,
    val rx: Int = 0,
    val std: String = ""
)

object WifiTools {
    fun info(ctx: Context): WifiNow = try {
        val wm = ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        @Suppress("DEPRECATION")
        val i = wm.connectionInfo
        if (!wm.isWifiEnabled || i == null || i.networkId == -1 || i.frequency <= 0) WifiNow()
        else {
            val f = i.frequency
            val band = when {
                f < 3000 -> "2,4 GHz"
                f < 5925 -> "5 GHz"
                else -> "6 GHz"
            }
            val ch = when {
                f == 2484 -> 14
                f < 3000 -> (f - 2407) / 5
                f < 5925 -> (f - 5000) / 5
                else -> (f - 5950) / 5
            }
            val std = when (i.wifiStandard) {
                1 -> "Wi-Fi lama (802.11a/b/g)"
                4 -> "Wi-Fi 4 (802.11n)"
                5 -> "Wi-Fi 5 (802.11ac)"
                6 -> "Wi-Fi 6 (802.11ax)"
                8 -> "Wi-Fi 7 (802.11be)"
                else -> "tidak diketahui"
            }
            WifiNow(true, band, ch, i.rssi, i.linkSpeed, i.txLinkSpeedMbps, i.rxLinkSpeedMbps, std)
        }
    } catch (e: Throwable) {
        WifiNow()
    }

    fun advice(w: WifiNow): List<String> {
        if (!w.connected) return listOf("Wi-Fi tidak tersambung.")
        val l = ArrayList<String>()
        if (w.band == "2,4 GHz") l.add("Anda di 2,4 GHz. Jika router dual band, pindah ke jaringan 5 GHz (lebih cepat, tapi jangkauan lebih pendek).")
        if (w.rssi < -80) l.add("Sinyal sangat lemah (${w.rssi} dBm). Dekati router atau hilangkan penghalang.")
        else if (w.rssi < -70) l.add("Sinyal agak lemah (${w.rssi} dBm). Mendekat ke router akan menaikkan kecepatan.")
        if (w.link in 1..49) l.add("Kecepatan tautan rendah (${w.link} Mbps). Itu batas atas yang mungkin, di bawah ini hasil unduhan tidak bisa naik.")
        if (w.band == "2,4 GHz" && w.channel !in listOf(1, 6, 11)) l.add("Kanal ${w.channel} di 2,4 GHz kurang ideal. Kanal 1, 6, atau 11 umumnya paling bersih (ubah di router).")
        if (l.isEmpty()) l.add("Kondisi Wi-Fi baik. Jika masih lambat, penyebabnya kemungkinan di provider atau jumlah perangkat yang memakai jaringan.")
        return l
    }

    /** Unduh dari server uji Cloudflare maksimal 6 detik. Hasil dalam Mbps, atau null jika gagal. */
    fun download(): Double? = try {
        val c = URL("https://speed.cloudflare.com/__down?bytes=25000000").openConnection() as HttpURLConnection
        c.connectTimeout = 6000
        c.readTimeout = 8000
        var total = 0L
        var t0 = 0L
        try {
            c.inputStream.use { ins ->
                val buf = ByteArray(65536)
                if (ins.read(buf) > 0) {
                    t0 = System.nanoTime()
                    while (true) {
                        val n = ins.read(buf)
                        if (n < 0) break
                        total += n
                        if (System.nanoTime() - t0 > 6_000_000_000L) break
                    }
                }
            }
        } finally {
            c.disconnect()
        }
        val sec = (System.nanoTime() - t0) / 1e9
        if (total <= 0 || sec <= 0.0) null else total * 8 / 1e6 / sec
    } catch (e: Throwable) {
        null
    }

    /** Unggah ke server uji Cloudflare maksimal 5 detik atau 4 MB. Hasil dalam Mbps. */
    fun upload(): Double? = try {
        val c = URL("https://speed.cloudflare.com/__up").openConnection() as HttpURLConnection
        c.requestMethod = "POST"
        c.doOutput = true
        c.connectTimeout = 6000
        c.readTimeout = 8000
        c.setChunkedStreamingMode(16384)
        val chunk = ByteArray(16384) { (it * 31).toByte() }
        var sent = 0L
        val t0 = System.nanoTime()
        try {
            c.outputStream.use { os ->
                while (sent < 4_000_000L && System.nanoTime() - t0 < 5_000_000_000L) {
                    os.write(chunk)
                    sent += chunk.size
                }
                os.flush()
            }
        } finally {
            val sec0 = (System.nanoTime() - t0) / 1e9
            try {
                c.responseCode
            } catch (e: Exception) {
            }
            c.disconnect()
            lastUpSec = sec0
        }
        if (sent <= 0 || lastUpSec <= 0.0) null else sent * 8 / 1e6 / lastUpSec
    } catch (e: Throwable) {
        null
    }

    private var lastUpSec = 0.0

    // ---- Kunci Wi-Fi latensi rendah (dipegang oleh layanan latar) ----
    private var lock: WifiManager.WifiLock? = null

    fun syncLock(ctx: Context, on: Boolean) {
        try {
            if (on && lock == null) {
                val wm = ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                lock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL_LOW_LATENCY, "tweaklauncher:wifi").also {
                    it.setReferenceCounted(false)
                    it.acquire()
                }
            } else if (!on && lock != null) {
                release()
            }
        } catch (e: Throwable) {
        }
    }

    fun release() {
        try {
            lock?.let { if (it.isHeld) it.release() }
        } catch (e: Throwable) {
        }
        lock = null
    }
}

@Composable
fun WifiCard(vm: MainVm) {
    val ctx = LocalContext.current
    val cfg = vm.cfg
    val scope = rememberCoroutineScope()
    var now by remember { mutableStateOf(WifiNow()) }
    var busy by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf("") }
    FoldCard("wifi", "Wi-Fi", if (cfg.wifiLow) "Latensi rendah aktif" else "Info, uji kecepatan", defaultOpen = false) {
        LaunchedEffect(Unit) {
            while (true) {
                now = withContext(Dispatchers.IO) { WifiTools.info(ctx) }
                delay(3000)
            }
        }
        if (now.connected) {
            Text("${now.band} · kanal ${now.channel} · ${now.std}", color = Pal.text, fontSize = 13.sp)
            Text("Sinyal ${now.rssi} dBm · tautan ${now.link} Mbps (kirim ${now.tx}, terima ${now.rx})", color = Pal.text, fontSize = 13.sp)
        } else {
            Text("Wi-Fi tidak tersambung.", color = Pal.sub, fontSize = 13.sp)
        }
        WifiTools.advice(now).forEach { Text("• $it", color = Pal.sub, fontSize = 12.sp) }

        ActionButton(if (busy) "Menguji..." else "Uji kecepatan", Modifier.fillMaxWidth(), primary = true, enabled = !busy) {
            busy = true
            result = "Mengukur ping, unduh, lalu unggah..."
            scope.launch {
                val r = withContext(Dispatchers.IO) {
                    val ping = tcpPing("1.1.1.1").first
                    val dl = WifiTools.download()
                    val ul = WifiTools.upload()
                    Triple(ping, dl, ul)
                }
                fun f(v: Double?) = if (v == null) "gagal" else String.format(Locale.US, "%.1f Mbps", v)
                result = "Ping " + (if (r.first < 0) "gagal" else "${r.first} ms") + " · Unduh " + f(r.second) + " · Unggah " + f(r.third)
                busy = false
            }
        }
        if (result.isNotEmpty()) Text(result, color = Pal.accent, fontSize = 13.sp)
        Hint("Uji kecepatan memakai server publik Cloudflare dan mengunduh sampai sekitar 25 MB serta mengunggah sekitar 4 MB. Jika sedang memakai data seluler, tes ini memakai kuota. Hasil naik turun tergantung beban jaringan saat itu.")

        SwitchRow(
            "Mode latensi rendah",
            "Menahan Wi-Fi supaya tidak masuk mode hemat daya. Membantu panggilan video dan game yang tersendat. Baterai lebih boros. Android hanya mengaktifkannya saat aplikasi di depan dan layar menyala.",
            cfg.wifiLow
        ) {
            cfg.wifiLow = it
            vm.syncService()
        }
    }
}
