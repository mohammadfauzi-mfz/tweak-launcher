package com.fauzi.tweaklauncher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.clickable
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.Random

class DnsResult(val name: String, val host: String, val ms: Int, val blocks: Boolean?)

/** Uji kecepatan dan kemampuan blok iklan beberapa penyedia DNS lewat kueri UDP langsung. */
object DnsTest {
    class Srv(val name: String, val ip: String, val host: String)

    val SERVERS = listOf(
        Srv("AdGuard", "94.140.14.14", "dns.adguard-dns.com"),
        Srv("AdGuard Keluarga", "94.140.14.15", "family.adguard-dns.com"),
        Srv("Mullvad (blok iklan)", "194.242.2.3", "adblock.dns.mullvad.net"),
        Srv("Cloudflare", "1.1.1.1", "one.one.one.one"),
        Srv("Google", "8.8.8.8", "dns.google"),
        Srv("Quad9", "9.9.9.9", "dns.quad9.net")
    )

    private fun build(domain: String): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        val id = Random().nextInt(65536)
        out.write(id shr 8); out.write(id and 255)
        out.write(0x01); out.write(0x00) // recursion desired
        out.write(0); out.write(1)       // QDCOUNT
        out.write(0); out.write(0); out.write(0); out.write(0); out.write(0); out.write(0)
        for (label in domain.split('.')) {
            out.write(label.length)
            out.write(label.toByteArray())
        }
        out.write(0)
        out.write(0); out.write(1) // A
        out.write(0); out.write(1) // IN
        return out.toByteArray()
    }

    /** Kembalikan (waktu ms, respons) atau (-1, null) jika waktu habis. */
    private fun query(ip: String, domain: String): Pair<Int, ByteArray?> {
        return try {
            DatagramSocket().use { s ->
                s.soTimeout = 2000
                val q = build(domain)
                val addr = InetAddress.getByName(ip)
                val t0 = System.nanoTime()
                s.send(DatagramPacket(q, q.size, addr, 53))
                val buf = ByteArray(1500)
                val p = DatagramPacket(buf, buf.size)
                s.receive(p)
                val ms = ((System.nanoTime() - t0) / 1_000_000).toInt()
                ms to buf.copyOf(p.length)
            }
        } catch (e: Exception) {
            -1 to null
        }
    }

    /** Apakah jawaban menandakan domain diblokir: NXDOMAIN, kosong, atau alamat 0.0.0.0. */
    private fun blocked(r: ByteArray): Boolean {
        if (r.size < 12) return false
        val rcode = r[3].toInt() and 0x0F
        val an = ((r[6].toInt() and 255) shl 8) or (r[7].toInt() and 255)
        if (rcode == 3) return true
        if (an == 0) return true
        var i = 12
        while (i < r.size && r[i].toInt() != 0) i += (r[i].toInt() and 255) + 1
        i += 5 // nol penutup + QTYPE + QCLASS
        var anyReal = false
        repeat(an) {
            if (i + 12 > r.size) return@repeat
            i += if ((r[i].toInt() and 0xC0) == 0xC0) 2 else {
                var j = i
                while (j < r.size && r[j].toInt() != 0) j += (r[j].toInt() and 255) + 1
                j + 1 - i
            }
            val type = ((r[i].toInt() and 255) shl 8) or (r[i + 1].toInt() and 255)
            val len = ((r[i + 8].toInt() and 255) shl 8) or (r[i + 9].toInt() and 255)
            val d = i + 10
            if (type == 1 && len == 4 && d + 4 <= r.size) {
                val z = (0 until 4).all { r[d + it].toInt() == 0 }
                if (!z) anyReal = true
            } else anyReal = true
            i = d + len
        }
        return !anyReal
    }

    fun run(): List<DnsResult> = SERVERS.map { s ->
        val times = ArrayList<Int>()
        repeat(3) {
            val sub = "t" + Random().nextInt(1_000_000_000) + ".example.com"
            val (ms, _) = query(s.ip, sub)
            if (ms >= 0) times.add(ms)
        }
        val ms = if (times.isEmpty()) -1 else times.sorted()[times.size / 2]
        val bl = if (ms < 0) null else query(s.ip, "doubleclick.net").second?.let { blocked(it) }
        DnsResult(s.name, s.host, ms, bl)
    }
}

fun MainVm.runDnsTest() {
    if (dnsBusy) return
    dnsBusy = true
    dnsRes = emptyList()
    viewModelScope.launch {
        dnsRes = withContext(Dispatchers.IO) { DnsTest.run() }.sortedBy { if (it.ms < 0) Int.MAX_VALUE else it.ms }
        dnsBusy = false
    }
}

@Composable
fun DnsTestBlock(vm: MainVm) {
    ActionButton(
        if (vm.dnsBusy) "Menguji..." else "Uji kecepatan dan blok iklan DNS",
        Modifier.fillMaxWidth(),
        enabled = !vm.dnsBusy
    ) { vm.runDnsTest() }
    vm.dnsRes.forEach { r ->
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f)) {
                Text(r.name, color = Pal.text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text(
                    (if (r.ms < 0) "tidak merespons" else "${r.ms} ms") + " · " +
                        when (r.blocks) {
                            true -> "memblokir iklan"
                            false -> "tidak memblokir iklan"
                            null -> "-"
                        },
                    color = if (r.blocks == true) Color(0xFF2E7D32) else Pal.sub,
                    fontSize = 12.sp
                )
            }
            if (r.ms >= 0) {
                Text("Pakai", color = Pal.accent, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { vm.useDns(r.host) }.padding(horizontal = 8.dp, vertical = 8.dp))
            }
        }
    }
    if (vm.dnsRes.isNotEmpty()) {
        Text("Ukuran waktu dari HP ini lewat jaringan saat ini. Hasil bisa berubah di Wi-Fi atau data seluler lain.", color = Pal.sub, fontSize = 11.sp)
    }
}

fun MainVm.useDns(host: String) {
    cfg.adsProvider = "custom"
    cfg.adsCustom = host
    adsApply(true)
}
