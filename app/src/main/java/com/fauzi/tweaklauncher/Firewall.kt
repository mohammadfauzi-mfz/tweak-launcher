package com.fauzi.tweaklauncher

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.VpnService
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.FileInputStream

/**
 * Firewall tanpa root: VPN lokal yang hanya menampung aplikasi yang diblokir, lalu membuang paketnya.
 * Aplikasi lain tidak lewat VPN ini. Mode per aplikasi: "all" (semua jaringan), "wifi", atau "mobile".
 */
class FirewallService : VpnService() {
    private var tun: ParcelFileDescriptor? = null
    private var lastKey = ""
    private var wifiUp = false
    private var cellUp = false
    private val main = Handler(Looper.getMainLooper())
    private lateinit var cm: ConnectivityManager

    private val wifiCb = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            wifiUp = true
            main.post { rebuild() }
        }

        override fun onLost(network: Network) {
            wifiUp = false
            main.post { rebuild() }
        }
    }
    private val cellCb = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            cellUp = true
            main.post { rebuild() }
        }

        override fun onLost(network: Network) {
            cellUp = false
            main.post { rebuild() }
        }
    }

    override fun onCreate() {
        super.onCreate()
        cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        try {
            cm.registerNetworkCallback(
                NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
                    .build(),
                wifiCb
            )
            cm.registerNetworkCallback(
                NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
                    .build(),
                cellCb
            )
        } catch (e: Exception) {
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val c0 = Cfg(this)
        if (!c0.fwOn || c0.fwEngine != "vpn") {
            closeTun()
            stopSelf()
            return START_NOT_STICKY
        }
        rebuild()
        return START_STICKY
    }

    private fun rebuild() {
        val cfg = Cfg(this)
        if (!cfg.fwOn || cfg.fwEngine != "vpn") {
            closeTun()
            return
        }
        val net = if (wifiUp) "wifi" else if (cellUp) "mobile" else "none"
        val pkgs = Store.map(cfg.fwJson)
            .filter { (p, v) -> p != packageName && (v == "all" || v == net) }
            .keys.sorted()
        if (pkgs.isEmpty()) {
            closeTun()
            return
        }
        val key = pkgs.joinToString(",")
        if (key == lastKey && tun != null) return
        val b = Builder()
            .setSession("Firewall")
            .addAddress("10.255.0.1", 32)
            .addRoute("0.0.0.0", 0)
            .setBlocking(true)
        try {
            b.addAddress("fd00:255::1", 128)
            b.addRoute("::", 0)
        } catch (e: Exception) {
        }
        var added = 0
        for (p in pkgs) {
            try {
                b.addAllowedApplication(p)
                added++
            } catch (e: PackageManager.NameNotFoundException) {
            }
        }
        // Tanpa aplikasi yang valid, VPN akan menampung SEMUA aplikasi. Jangan dibuat.
        if (added == 0) {
            closeTun()
            return
        }
        val fresh = try {
            b.establish()
        } catch (e: Exception) {
            null
        }
        if (fresh == null) return
        val old = tun
        tun = fresh
        lastKey = key
        try {
            old?.close()
        } catch (e: Exception) {
        }
        // Buang semua paket yang masuk ke VPN: aplikasi yang diblokir tidak mendapat internet.
        Thread {
            try {
                val ins = FileInputStream(fresh.fileDescriptor)
                val buf = ByteArray(32767)
                while (ins.read(buf) >= 0) {
                }
            } catch (e: Exception) {
            }
        }.start()
    }

    private fun closeTun() {
        try {
            tun?.close()
        } catch (e: Exception) {
        }
        tun = null
        lastKey = ""
    }

    override fun onRevoke() {
        // Pengguna menyalakan VPN lain atau mematikan dari sistem.
        try {
            Cfg(this).fwOn = false
        } catch (e: Exception) {
        }
        closeTun()
        stopSelf()
        super.onRevoke()
    }

    override fun onDestroy() {
        try {
            cm.unregisterNetworkCallback(wifiCb)
            cm.unregisterNetworkCallback(cellCb)
        } catch (e: Exception) {
        }
        closeTun()
        super.onDestroy()
    }
}

object Firewall {
    fun start(ctx: Context) {
        try {
            ctx.startService(Intent(ctx, FirewallService::class.java))
        } catch (e: Exception) {
        }
    }

    fun stop(ctx: Context) {
        try {
            ctx.stopService(Intent(ctx, FirewallService::class.java))
        } catch (e: Exception) {
        }
    }

    /** Dipanggil saat launcher dibuka: nyalakan ulang VPN jika sebelumnya aktif dan izinnya masih ada. */
    fun sync(ctx: Context, cfg: Cfg) {
        if (cfg.fwEngine != "vpn") return
        if (cfg.fwOn && VpnService.prepare(ctx) == null) start(ctx) else if (cfg.fwOn) cfg.fwOn = false
    }

    /**
     * Mode Shizuku: aktifkan atau matikan jaringan per paket lewat perintah sistem (tanpa VPN).
     * Mengembalikan pesan hasil untuk ditampilkan.
     */
    fun applyShizuku(cfg: Cfg): String {
        if (Shell.state() != ShizukuState.READY) return "Shizuku belum aktif, aturan belum diterapkan."
        val desired = if (cfg.fwOn) Store.map(cfg.fwJson).keys.toSet() else emptySet()
        val applied = Sys.list(cfg.fwAppliedCsv).toSet()
        var firstErr = ""
        var okCount = 0
        for (p in applied - desired) {
            Shell.run("cmd connectivity set-package-networking-enabled true $p")
        }
        for (p in desired) {
            val r = Shell.run("cmd connectivity set-package-networking-enabled false $p")
            if (r.first == 0 && !r.second.contains("Exception", true) && !r.second.contains("Unknown", true)) okCount++
            else if (firstErr.isEmpty()) firstErr = r.second.trim().take(140)
        }
        cfg.fwAppliedCsv = Sys.join(desired)
        return if (firstErr.isNotEmpty()) "Gagal untuk sebagian aplikasi. Perintah ini mungkin tidak didukung di versi Android ini: $firstErr"
        else if (desired.isEmpty()) "Semua aturan dibersihkan."
        else "$okCount aplikasi diblokir lewat Shizuku."
    }

    fun modeLabel(m: String?): String = when (m) {
        "all" -> "Diblokir"
        "wifi" -> "Blokir Wi-Fi"
        "mobile" -> "Blokir data seluler"
        else -> "Diizinkan"
    }

    fun nextMode(m: String?, shizuku: Boolean = false): String? = when (m) {
        null, "" -> "all"
        "all" -> if (shizuku) null else "wifi"
        "wifi" -> "mobile"
        else -> null
    }
}

fun MainVm.setFwMode(ctx: Context, pkg: String, mode: String?) {
    val m = Store.map(cfg.fwJson)
    if (mode == null) m.remove(pkg) else m[pkg] = mode
    cfg.fwJson = Store.mapJson(m)
    fwRefresh(ctx)
}

/** Terapkan ulang aturan sesuai cara kerja yang dipilih (VPN lokal atau Shizuku). */
fun MainVm.fwRefresh(ctx: Context) {
    if (cfg.fwEngine == "shizuku") {
        Firewall.stop(ctx)
        viewModelScope.launch { fwMsg = withContext(Dispatchers.IO) { Firewall.applyShizuku(cfg) } }
    } else {
        if (cfg.fwOn) Firewall.start(ctx) else Firewall.stop(ctx)
    }
}

@Composable
fun FirewallCard(vm: MainVm) {
    val cfg = vm.cfg
    val ctx = LocalContext.current
    val count = Store.map(cfg.fwJson).size
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        if (r.resultCode == Activity.RESULT_OK) {
            cfg.fwOn = true
            Firewall.start(ctx)
        }
    }
    SCard {
        Text("Firewall (blok internet)", color = Pal.text, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        Text(
            "Pilih aplikasi yang tidak boleh memakai internet, semuanya, hanya Wi-Fi, atau hanya data seluler. Memakai VPN lokal di HP, datamu tidak dikirim ke mana pun.",
            color = Pal.soft, fontSize = 13.sp
        )
        ChoiceRow(
            "Cara kerja",
            listOf("vpn" to "VPN lokal", "shizuku" to "Shizuku (tanpa VPN)"),
            cfg.fwEngine
        ) { e ->
            if (e != cfg.fwEngine) {
                val wasShizuku = cfg.fwEngine == "shizuku"
                cfg.fwEngine = e
                if (wasShizuku) {
                    // bersihkan aturan Shizuku lama sebelum pindah ke VPN
                    val on = cfg.fwOn
                    cfg.fwOn = false
                    vm.fwRefresh(ctx)
                    cfg.fwOn = on
                    if (on) {
                        if (VpnService.prepare(ctx) == null) Firewall.start(ctx) else cfg.fwOn = false
                    }
                } else {
                    Firewall.stop(ctx)
                    vm.fwRefresh(ctx)
                }
            }
        }
        SwitchRow(
            "Firewall aktif",
            if (count == 0) "Belum ada aplikasi diblokir" else "$count aplikasi diatur",
            cfg.fwOn
        ) { on ->
            if (cfg.fwEngine == "shizuku") {
                cfg.fwOn = on
                vm.fwRefresh(ctx)
            } else if (on) {
                val i = VpnService.prepare(ctx)
                if (i != null) launcher.launch(i) else {
                    cfg.fwOn = true
                    Firewall.start(ctx)
                }
            } else {
                cfg.fwOn = false
                Firewall.stop(ctx)
            }
        }
        ActionButton("Atur aplikasi...", Modifier.fillMaxWidth(), primary = true) { vm.fwOpen = true }
        if (cfg.fwEngine == "shizuku") {
            Text(
                "Mode Shizuku tidak memakai VPN, tapi butuh Shizuku aktif dan hanya bisa blokir total (bukan per Wi-Fi atau data). Aturan perlu diterapkan ulang setelah HP restart.",
                color = Pal.sub, fontSize = 12.sp
            )
            if (vm.fwMsg.isNotEmpty()) Text(vm.fwMsg, color = Pal.soft, fontSize = 12.sp)
        } else {
            Text(
                "Android hanya mengizinkan satu VPN aktif. Jika kamu memakai VPN lain (mis. 1.1.1.1), salah satunya akan mati.",
                color = Pal.sub, fontSize = 12.sp
            )
        }
    }
}

@Composable
fun FirewallDialog(vm: MainVm) {
    if (!vm.fwOpen) return
    val ctx = LocalContext.current
    val cfg = vm.cfg
    var q by remember { mutableStateOf("") }
    var showSys by remember { mutableStateOf(false) }
    val modes = Store.map(cfg.fwJson)
    val list = vm.apps
        .filter { a -> (showSys || !a.system || a.pkg in modes) && (q.isBlank() || a.label.contains(q, ignoreCase = true)) }
        .sortedBy { it.label.lowercase() }
    TweakDialog(
        onDismissRequest = { vm.fwOpen = false },
        containerColor = Pal.card,
        title = { Text("Firewall per aplikasi", color = Pal.text, fontSize = 17.sp) },
        text = {
            Column {
                InputBox(q, { q = it }, "Cari aplikasi")
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (showSys) "Sembunyikan aplikasi sistem" else "Tampilkan aplikasi sistem",
                        color = Pal.accent, fontSize = 12.sp,
                        modifier = Modifier.clickable { showSys = !showSys }
                    )
                }
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Blokir semua non-sistem", color = Pal.accent, fontSize = 12.sp, modifier = Modifier.clickable {
                        val m = Store.map(cfg.fwJson)
                        vm.apps.filter { !it.system && it.pkg != ctx.packageName }.forEach { m[it.pkg] = "all" }
                        cfg.fwJson = Store.mapJson(m)
                        vm.fwRefresh(ctx)
                    })
                    Text("Bebaskan semua", color = Pal.accent, fontSize = 12.sp, modifier = Modifier.clickable {
                        cfg.fwJson = "{}"
                        vm.fwRefresh(ctx)
                    })
                }
                Spacer(Modifier.height(6.dp))
                Box(Modifier.height(380.dp)) {
                    LazyColumn {
                        items(list, key = { it.pkg }) { a ->
                            val mode = modes[a.pkg]
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { vm.setFwMode(ctx, a.pkg, Firewall.nextMode(mode, cfg.fwEngine == "shizuku")) }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                AppIcon(a, 32.dp, badge = false)
                                Text(
                                    a.label, color = Pal.text, fontSize = 14.sp, maxLines = 1,
                                    overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
                                )
                                Text(
                                    Firewall.modeLabel(mode),
                                    color = if (mode == null) Pal.sub else Color(0xFFD32F2F),
                                    fontSize = 12.sp,
                                    fontWeight = if (mode == null) FontWeight.Normal else FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                Text(
                    if (cfg.fwEngine == "shizuku") "Ketuk aplikasi untuk mengganti: Diizinkan atau Diblokir."
                    else "Ketuk aplikasi untuk mengganti: Diizinkan, Diblokir, Blokir Wi-Fi, Blokir data seluler.",
                    color = Pal.sub, fontSize = 11.sp
                )
            }
        },
        confirmButton = { DlgButton(onClick = { vm.fwOpen = false }) { Text("Tutup", color = Pal.accent) } }
    )
}
