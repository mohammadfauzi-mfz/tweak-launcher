package com.fauzi.tweaklauncher

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Satu baris keluaran terminal. Jika [pkg] terisi, baris bisa diketuk untuk menjalankan aplikasi. */
data class DosLine(val text: String, val pkg: String? = null)

// ---------------------------------------------------------------- Tampilan

@Composable
fun DosFrame(vm: MainVm) {
    LaunchedEffect(Unit) {
        if (vm.dosLines.isEmpty()) {
            vm.dosPrint("TWEAK-DOS [Versi ${BuildConfig.VERSION_NAME}]")
            vm.dosPrint("(C) Tweak Launcher. Ketik HELP untuk bantuan.")
            vm.dosPrint("")
        }
    }
    Box(Modifier.fillMaxSize().statusBarsPadding().imePadding().navigationBarsPadding()) {
        if (vm.screen == Screen.Home) DosTerminal(vm) else DosPanel(vm)
    }
}

@Composable
private fun DosPanel(vm: MainVm) {
    val title = when (vm.screen) {
        Screen.Care -> "CARE"
        Screen.Drawer -> "DIR"
        Screen.Settings -> "SETUP"
        Screen.Home -> ""
    }
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().border(1.dp, Pal.line).padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("C:\\>$title", color = Pal.text, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text(
                "[ESC]",
                color = Pal.text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { vm.screen = Screen.Home }.padding(6.dp)
            )
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (vm.screen) {
                Screen.Care -> CareScreen(vm)
                Screen.Drawer -> PdaPrograms(vm)
                Screen.Settings -> SettingsScreen(vm)
                Screen.Home -> {}
            }
        }
    }
}

@Composable
private fun DosTerminal(vm: MainVm) {
    val cfg = vm.cfg
    val listState = rememberLazyListState()
    var input by remember { mutableStateOf("") }
    val lines = vm.dosLines
    val canUp = remember { booleanArrayOf(false) }
    val canDown = remember { booleanArrayOf(false) }

    LaunchedEffect(lines.size) {
        if (lines.isNotEmpty()) listState.scrollToItem(lines.size - 1)
    }

    fun run(cmd: String) {
        vm.dosPrint("C:\\>$cmd")
        vm.dosExec(cmd)
    }

    Column(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { vm.runGesture(cfg.gDouble) },
                    onLongPress = { vm.runGesture(cfg.gLong) }
                )
            }
            .swipes(
                onStart = { canUp[0] = listState.canScrollForward; canDown[0] = listState.canScrollBackward },
                onEnd = { d, _ ->
                    if (kotlin.math.abs(d.y) > kotlin.math.abs(d.x) * 1.5f) {
                        if (d.y < -140f && !canUp[0]) vm.runGesture(cfg.gSwipeUp)
                        else if (d.y > 140f && !canDown[0]) vm.runGesture(cfg.gSwipeDown)
                    }
                }
            )
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            items(count = lines.size) { i ->
                val l = lines.getOrNull(i) ?: DosLine("")
                Text(
                    l.text.ifEmpty { " " },
                    color = Pal.text,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = if (l.pkg != null) Modifier.fillMaxWidth().clickable { vm.launchApp(l.pkg) } else Modifier.fillMaxWidth()
                )
            }
        }

        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            listOf("DIR", "RECENT", "MEM", "MODE", "CARE", "SET", "NOTIF", "LOCK", "HELP", "CLS").forEach { c ->
                Text(
                    "[$c]",
                    color = Pal.text,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.clickable { run(c.lowercase(Locale.ROOT)) }.padding(horizontal = 6.dp, vertical = 8.dp)
                )
            }
        }

        Row(
            Modifier.fillMaxWidth().border(1.dp, Pal.line).padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("C:\\>", color = Pal.text, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Spacer(Modifier.width(6.dp))
            BasicTextField(
                value = input,
                onValueChange = { input = it },
                singleLine = true,
                textStyle = TextStyle(color = Pal.text, fontSize = 15.sp, fontFamily = FontFamily.Monospace),
                cursorBrush = SolidColor(Pal.text),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    val c = input
                    input = ""
                    run(c)
                }),
                modifier = Modifier.weight(1f)
            )
            Text(
                "[ENTER]",
                color = Pal.text,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.clickable {
                    val c = input
                    input = ""
                    run(c)
                }.padding(start = 6.dp)
            )
        }
    }
}

// ---------------------------------------------------------------- Perintah

private fun MainVm.dosList(list: List<AppItem>) {
    dosListed = list
    list.forEachIndexed { i, a ->
        val n = Badges.counts[a.pkg] ?: 0
        val badge = if (cfg.badgeOn && n > 0) "  [$n]" else ""
        val sleep = if (a.pkg in sleeping) "  (tidur)" else ""
        dosPrint(String.format(Locale.US, "%3d  %s%s%s", i + 1, a.label, badge, sleep), a.pkg)
    }
}

private fun MainVm.dosOpen(q: String) {
    val key = q.trim().lowercase(Locale.ROOT)
    val all = visibleApps
    val exact = all.filter { it.label.lowercase(Locale.ROOT) == key }
    val hits = if (exact.size == 1) exact else all.filter { it.label.lowercase(Locale.ROOT).contains(key) }
    when {
        hits.size == 1 -> {
            dosPrint("Menjalankan ${hits[0].label}...")
            launchApp(hits[0].pkg)
        }
        hits.size > 1 -> {
            dosPrint("Beberapa program cocok. Ketuk baris atau ketik nomor:")
            dosList(hits)
        }
        else -> dosPrint("Perintah atau nama berkas salah: $q")
    }
}

private fun bar(p: Float): String {
    val n = 20
    val on = (p.coerceIn(0f, 1f) * n).toInt()
    return "[" + "#".repeat(on) + ".".repeat(n - on) + "]"
}

fun MainVm.dosExec(raw: String) {
    val t = raw.trim()
    if (t.isEmpty()) return
    val parts = t.split(Regex("\\s+"), limit = 2)
    val cmd = parts[0].lowercase(Locale.ROOT)
    val arg = if (parts.size > 1) parts[1].trim() else ""
    when (cmd) {
        "help", "?" -> listOf(
            "Perintah:",
            " DIR [kata]      daftar program (ketuk baris atau ketik nomor)",
            " RECENT          program yang terakhir dipakai",
            " <nama>          jalankan program, mis. whatsapp",
            " MEM             RAM, baterai, suhu, penyimpanan",
            " MODE [id]       lihat atau ganti profil performa",
            " CARE            halaman Perawatan",
            " SET             Setelan",
            " NOTIF           buka panel notifikasi",
            " LOCK            kunci layar",
            " SH <perintah>   perintah shell (butuh Shizuku)",
            " COLOR g|a|w     warna layar: green, amber, gray",
            " VER, DATE, TIME",
            " CLS             bersihkan layar",
            " MODERN          kembali ke mode Modern"
        ).forEach { dosPrint(it) }
        "cls", "clear" -> dosLines.clear()
        "ver" -> dosPrint("TWEAK-DOS [Versi ${BuildConfig.VERSION_NAME}]")
        "date" -> dosPrint(SimpleDateFormat("EEEE, d MMMM yyyy", Locale("id", "ID")).format(Date()))
        "time" -> dosPrint(SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()))
        "dir", "ls" -> {
            val key = arg.lowercase(Locale.ROOT)
            val list = if (key.isEmpty()) visibleApps else visibleApps.filter { it.label.lowercase(Locale.ROOT).contains(key) }
            dosPrint(" Direktori C:\\PROGRAM")
            dosPrint("")
            dosList(list)
            dosPrint("")
            dosPrint("       ${list.size} berkas")
        }
        "recent" -> {
            loadUsage()
            val r = recentApps
            if (r.isEmpty()) {
                dosPrint("Belum ada data. Beri Akses penggunaan di halaman Perawatan, lalu ulangi.")
            } else {
                dosList(r)
            }
        }
        "mem" -> {
            refreshStats()
            val st = stats
            dosPrint("RAM       ${bar(st.ramPct)} ${st.ramText}")
            dosPrint("BATERAI   ${bar(st.battery / 100f)} ${st.battery}%")
            dosPrint("SUHU      ${bar(st.tempPct)} ${st.tempText}")
            dosPrint("SIMPANAN  ${bar(st.storagePct)} ${(st.storagePct * 100).toInt()}%")
            if (st.hz > 0) dosPrint("LAYAR     ${st.hz} Hz")
        }
        "mode", "profil" -> {
            if (arg.isEmpty()) {
                profiles.forEach { dosPrint("${if (it.id == profile.id) "*" else " "} ${it.id.padEnd(8)} ${it.label}") }
                dosPrint("Ketik MODE <id> untuk mengganti.")
            } else {
                val k = arg.lowercase(Locale.ROOT)
                val p = profiles.firstOrNull { it.id.lowercase(Locale.ROOT) == k || it.label.lowercase(Locale.ROOT) == k }
                if (p == null) {
                    dosPrint("Profil tidak ditemukan: $arg")
                } else {
                    dosPrint("Menerapkan profil ${p.label}...")
                    setProfile(p.id)
                }
            }
        }
        "care" -> screen = Screen.Care
        "set", "setup", "settings" -> {
            settingsPage = ""
            screen = Screen.Settings
        }
        "notif" -> runGesture("notif")
        "lock" -> runGesture("lock")
        "color", "colour" -> {
            val c = when (arg.lowercase(Locale.ROOT)) {
                "g", "green", "hijau" -> "green"
                "a", "amber" -> "amber"
                "w", "gray", "grey", "abu" -> "gray"
                else -> ""
            }
            if (c.isEmpty()) dosPrint("Pakai: COLOR green | amber | gray") else cfg.dosColor = c
        }
        "sh" -> if (arg.isEmpty()) dosPrint("Pakai: SH <perintah>, mis. SH getprop ro.product.model") else dosShell(arg)
        "run", "start", "open" -> if (arg.isEmpty()) dosPrint("Pakai: RUN <nama program>") else dosOpen(arg)
        "modern", "exit" -> cfg.uiMode = "modern"
        else -> {
            val n = t.toIntOrNull()
            if (n != null) {
                val src = if (dosListed.isNotEmpty()) dosListed else visibleApps
                val a = src.getOrNull(n - 1)
                if (a == null) {
                    dosPrint("Nomor tidak ada: $n")
                } else {
                    dosPrint("Menjalankan ${a.label}...")
                    launchApp(a.pkg)
                }
            } else {
                dosOpen(t)
            }
        }
    }
}
