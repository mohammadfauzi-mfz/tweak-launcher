package com.fauzi.tweaklauncher

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Blok iklan lewat DNS privat sistem (Android 9 ke atas). Berlaku untuk seluruh HP, bukan per aplikasi.
 * Pengaturan ditulis lewat Shizuku, tanpa VPN.
 */
object AdsBlock {
    val PROVIDERS = listOf(
        "adguard" to "AdGuard",
        "family" to "AdGuard Keluarga",
        "custom" to "Kustom"
    )

    private val HOST = Regex("[A-Za-z0-9]([A-Za-z0-9.-]*[A-Za-z0-9])?")

    fun host(cfg: Cfg): String = when (cfg.adsProvider) {
        "family" -> "family.adguard-dns.com"
        "custom" -> cfg.adsCustom.trim()
        else -> "dns.adguard-dns.com"
    }

    fun validHost(h: String) = h.length in 3..253 && h.contains('.') && HOST.matches(h)

    private fun get(key: String): String = Shell.run("settings get global $key").second.trim()

    /** Nyalakan DNS privat dengan host pilihan. Mengembalikan pesan hasil. */
    fun enable(cfg: Cfg): String {
        if (Shell.state() != ShizukuState.READY) return "Shizuku belum aktif."
        val h = host(cfg)
        if (!validHost(h)) return "Nama host tidak valid."
        val curMode = get("private_dns_mode")
        val curHost = get("private_dns_specifier")
        if (!cfg.adsDnsOn) {
            cfg.adsPrevMode = curMode
            cfg.adsPrevHost = curHost
        }
        val a = Shell.run("settings put global private_dns_specifier $h")
        val b = Shell.run("settings put global private_dns_mode hostname")
        if (a.first != 0 || b.first != 0) return "Gagal menulis pengaturan: " + (a.second + b.second).trim().take(140)
        val ok = get("private_dns_mode") == "hostname" && get("private_dns_specifier") == h
        if (!ok) return "Pengaturan tidak tersimpan. Sistem mungkin menolak perubahan."
        cfg.adsDnsOn = true
        return "Aktif: DNS privat $h"
    }

    /** Kembalikan DNS privat ke pengaturan sebelum dinyalakan. */
    fun disable(cfg: Cfg): String {
        if (Shell.state() != ShizukuState.READY) return "Shizuku belum aktif."
        val pm = cfg.adsPrevMode.takeIf { it.isNotBlank() && it != "null" } ?: "opportunistic"
        val ph = cfg.adsPrevHost
        val a = Shell.run("settings put global private_dns_mode $pm")
        if (ph.isBlank() || ph == "null") Shell.run("settings delete global private_dns_specifier")
        else if (validHost(ph)) Shell.run("settings put global private_dns_specifier $ph")
        if (a.first != 0) return "Gagal mengembalikan: " + a.second.trim().take(140)
        cfg.adsDnsOn = false
        return "Dimatikan. DNS dikembalikan ke pengaturan semula."
    }
}

fun MainVm.adsApply(on: Boolean) {
    adsMsg = "Memproses..."
    viewModelScope.launch {
        adsMsg = withContext(Dispatchers.IO) { if (on) AdsBlock.enable(cfg) else AdsBlock.disable(cfg) }
        adsRev++
    }
}

@Composable
fun AdsBlockCard(vm: MainVm) {
    val cfg = vm.cfg
    var cur by remember { mutableStateOf("") }
    LaunchedEffect(vm.adsRev) {
        cur = withContext(Dispatchers.IO) {
            if (Shell.state() != ShizukuState.READY) ""
            else Shell.run("settings get global private_dns_mode").second.trim() + " / " +
                Shell.run("settings get global private_dns_specifier").second.trim()
        }
    }
    FoldCard(
        "ads", "Blok iklan (DNS privat)",
        if (cfg.adsDnsOn) "Aktif · ${AdsBlock.host(cfg)}" else "Mati",
        defaultOpen = false
    ) {
        Hint("Memakai DNS pemfilter iklan dan pelacak untuk seluruh HP, lewat Shizuku, tanpa VPN. Tidak bisa dipilih per aplikasi. Iklan yang menyatu dengan konten (mis. video YouTube) tidak terblokir.")
        ChoiceRow("Penyedia DNS", AdsBlock.PROVIDERS, cfg.adsProvider) { p ->
            cfg.adsProvider = p
            if (cfg.adsDnsOn && (p != "custom" || AdsBlock.validHost(cfg.adsCustom.trim()))) vm.adsApply(true)
        }
        if (cfg.adsProvider == "custom") {
            InputBox(cfg.adsCustom, { cfg.adsCustom = it }, "Nama host, mis. dns.adguard-dns.com", Modifier.fillMaxWidth())
        }
        SwitchRow(
            "Blok iklan aktif",
            if (cfg.adsDnsOn) "Host: ${AdsBlock.host(cfg)}" else "Mati",
            cfg.adsDnsOn
        ) { on ->
            vm.adsApply(on)
        }
        if (vm.adsMsg.isNotEmpty()) Text(vm.adsMsg, color = Pal.soft, fontSize = 12.sp)
        if (cur.isNotBlank()) Text("Status sistem: $cur", color = Pal.sub, fontSize = 11.sp)
        DnsTestBlock(vm)
        Hint("Jika kamu memakai VPN lain (mis. 1.1.1.1), DNS dari VPN itu yang dipakai dan blok iklan ini tidak berlaku selama VPN menyala.")
    }
}
