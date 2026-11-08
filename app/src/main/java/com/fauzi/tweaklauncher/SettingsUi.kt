package com.fauzi.tweaklauncher

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

private val PAGES = listOf(
    Triple("tampilan", "Tampilan", "Tema, warna, font, latar, sudut kartu"),
    Triple("beranda", "Beranda", "Susunan bagian, jam, kolom, dock, widget"),
    Triple("ikon", "Ikon dan daftar aplikasi", "Bentuk ikon, ukuran, label, gaya daftar"),
    Triple("gestur", "Gestur", "Geser atas/bawah, ketuk dua kali, tahan lama"),
    Triple("profil", "Profil performa", "Ubah, tambah, dan hapus profil"),
    Triple("otomasi", "Otomasi dan overlay", "Profil per aplikasi, jadwal, overlay monitor"),
    Triple("layar", "Layar (DPI dan resolusi)", "Atur DPI dan resolusi dengan konfirmasi"),
    Triple("alat", "Alat", "Bersihkan cache, optimasi, tidurkan aplikasi"),
    Triple("cadangan", "Cadangan", "Ekspor dan impor pengaturan"),
    Triple("game", "Folder Game", "Isi folder Game di menu Start Win95/XP"),
    Triple("kunci", "Layar kunci", "Lockscreen sesuai mode di atas kunci HP"),
    Triple("update", "Pembaruan", "Periksa dan pasang versi baru"),
    Triple("tentang", "Tentang", "Versi dan status")
)

@Composable
fun SettingsScreen(vm: MainVm) {
    val page = vm.settingsPage
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (page.isEmpty()) {
            if (LocalCfg.current.uiMode != "wp") Text("Setelan", color = Pal.text, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            PAGES.forEach { (key, title, sub) ->
                SCard(Modifier.clickable { vm.settingsPage = key }) {
                    Text(title, color = Pal.text, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Text(sub, color = Pal.sub, fontSize = 12.sp)
                }
            }
        } else {
            Text(
                "‹ Setelan",
                color = Pal.accent,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { vm.settingsPage = "" }.padding(vertical = 4.dp)
            )
            Text(
                PAGES.firstOrNull { it.first == page }?.second ?: "",
                color = Pal.text, fontSize = 30.sp, fontWeight = FontWeight.Bold
            )
            when (page) {
                "tampilan" -> AppearancePage(vm)
                "beranda" -> HomePage(vm)
                "ikon" -> IconsPage(vm)
                "gestur" -> GesturePage(vm)
                "profil" -> ProfilesPage(vm)
                "otomasi" -> AutomationPage(vm)
                "layar" -> DisplayPage(vm)
                "alat" -> ToolsPage(vm)
                "cadangan" -> BackupPage(vm)
                "game" -> GamePage(vm)
                "kunci" -> LockPage(vm)
                "update" -> UpdatePage(vm)
                "tentang" -> AboutPage(vm)
            }
        }
    }
}

@Composable
private fun IntSlider(title: String, unit: String, value: Int, min: Int, max: Int, onChange: (Int) -> Unit) {
    SliderRow(
        title, "$value$unit", value.toFloat(), min.toFloat()..max.toFloat(),
        (max - min - 1).coerceAtLeast(0)
    ) { onChange(it.roundToInt()) }
}

private fun fmtTime(m: Int): String = String.format("%02d:%02d", m / 60, m % 60)

// ---------------------------------------------------------------- Tampilan

private fun applyPreset(cfg: Cfg, key: String) {
    when (key) {
        "lime" -> {
            cfg.theme = "amoled"; cfg.accent = "#C6F432"; cfg.radius = 28; cfg.cardAlpha = 100; cfg.background = "solid"
        }
        "cyan" -> {
            cfg.theme = "dark"; cfg.accent = "#38E1FF"; cfg.radius = 24; cfg.cardAlpha = 90; cfg.background = "solid"
        }
        "sunset" -> {
            cfg.theme = "amoled"; cfg.accent = "#FF7A45"; cfg.radius = 32; cfg.cardAlpha = 100; cfg.background = "solid"
        }
        "violet" -> {
            cfg.theme = "amoled"; cfg.accent = "#B58CFF"; cfg.radius = 28; cfg.cardAlpha = 60; cfg.background = "wallpaper"
        }
        "light" -> {
            cfg.theme = "light"; cfg.accent = "#5B8CFF"; cfg.radius = 20; cfg.cardAlpha = 100; cfg.background = "solid"
        }
    }
}

private val SWATCHES = listOf("#C6F432", "#38E1FF", "#FF7A45", "#B58CFF", "#FF5DA2", "#4ADE80", "#FFD60A", "#5B8CFF")

@Composable
private fun AppearancePage(vm: MainVm) {
    val cfg = vm.cfg
    FoldCard("set_mode", "Mode tampilan", cfg.uiMode) {
        ChoiceRow(
            "Mode tampilan",
            listOf("modern" to "Modern", "pda" to "PDA (Palm + Pocket PC)", "win95" to "Windows 95", "xp" to "Windows XP", "dos" to "DOS (terminal)", "mac" to "Mac OS klasik", "wp" to "Windows Phone", "sym" to "Symbian (Nokia S60)"),
            cfg.uiMode
        ) {
            cfg.uiMode = it
            vm.pdaStart = false
        }
        if (cfg.uiMode == "pda") {
            SwitchRow("Ikon monokrom LCD", "Ikon aplikasi jadi hijau-abu seperti layar PDA", cfg.pdaMono) { cfg.pdaMono = it }
        }
        if (cfg.uiMode == "dos") {
            ChoiceRow("Warna layar DOS", listOf("green" to "Hijau", "gray" to "Abu-abu", "amber" to "Amber"), cfg.dosColor) { cfg.dosColor = it }
        }
        if (cfg.uiMode != "modern") {
            Hint("Di mode ini, tema warna, aksen, font, dan latar diabaikan. Kembali ke Modern untuk memakainya lagi.")
        }
    }
    WallpaperCard(vm)
    WpAccentCard(vm)
    FoldCard("set_preset", "Tema siap pakai", defaultOpen = false) {
        ChoiceRow(
            "Tema siap pakai",
            listOf("lime" to "Lime Noir", "cyan" to "Cyan Night", "sunset" to "Sunset", "violet" to "Violet Kaca", "light" to "Terang Bersih"),
            ""
        ) { applyPreset(cfg, it) }
    }
    FoldCard("set_color", "Warna dan font", "Aksen ${cfg.accent}", defaultOpen = false) {
        ChoiceRow("Mode tema", listOf("amoled" to "AMOLED", "dark" to "Gelap", "light" to "Terang"), cfg.theme) { cfg.theme = it }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Warna aksen", color = Pal.text, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SWATCHES.forEach { c ->
                    val on = cfg.accent.equals(c, ignoreCase = true)
                    Box(
                        Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(parseColor(c))
                            .border(if (on) 3.dp else 1.dp, if (on) Pal.text else Pal.line, CircleShape)
                            .clickable { cfg.accent = c }
                    )
                }
            }
            var hex by remember { mutableStateOf(cfg.accent) }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InputBox(hex, { hex = it }, "#RRGGBB", Modifier.weight(1f))
                ActionButton("Terapkan") {
                    if (Regex("#?[0-9A-Fa-f]{6}").matches(hex)) cfg.accent = if (hex.startsWith("#")) hex else "#$hex"
                }
            }
        }
        ChoiceRow(
            "Font",
            listOf("sans" to "Standar", "serif" to "Serif", "mono" to "Mono", "cursive" to "Tulisan"),
            cfg.font
        ) { cfg.font = it }
    }
    FoldCard("set_bg", "Latar dan kartu", defaultOpen = false) {
        ChoiceRow("Latar", listOf("solid" to "Warna polos", "wallpaper" to "Wallpaper sistem"), cfg.background) { cfg.background = it }
        if (cfg.background == "wallpaper") {
            IntSlider("Redupkan latar", "%", cfg.dim, 0, 90) { cfg.dim = it }
        }
        IntSlider("Sudut kartu", " dp", cfg.radius, 8, 40) { cfg.radius = it }
        IntSlider("Kepekatan kartu", "%", cfg.cardAlpha, 30, 100) { cfg.cardAlpha = it }
    }
}

// ---------------------------------------------------------------- Beranda

@Composable
private fun HomePage(vm: MainVm) {
    val cfg = vm.cfg
    val secs = Layout.parse(cfg.homeSections)
    FoldCard("set_secs", "Bagian Beranda (urutan dan tampil)", defaultOpen = false) {
        secs.forEachIndexed { i, (k, on) ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(Layout.NAMES[k] ?: k, color = Pal.text, fontSize = 15.sp, modifier = Modifier.weight(1f))
                ActionButton("↑", Modifier.size(width = 44.dp, height = 38.dp), enabled = i > 0) {
                    val l = secs.toMutableList()
                    val t = l[i]; l[i] = l[i - 1]; l[i - 1] = t
                    cfg.homeSections = Layout.join(l)
                }
                Spacer(Modifier.size(6.dp))
                ActionButton("↓", Modifier.size(width = 44.dp, height = 38.dp), enabled = i < secs.size - 1) {
                    val l = secs.toMutableList()
                    val t = l[i]; l[i] = l[i + 1]; l[i + 1] = t
                    cfg.homeSections = Layout.join(l)
                }
                Spacer(Modifier.size(8.dp))
                Switch(
                    checked = on,
                    onCheckedChange = { v ->
                        cfg.homeSections = Layout.join(secs.map { if (it.first == k) Pair(k, v) else it })
                    },
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = Pal.accent, checkedThumbColor = Pal.onAccent,
                        uncheckedTrackColor = Pal.inset, uncheckedThumbColor = Pal.sub, uncheckedBorderColor = Pal.line
                    )
                )
            }
        }
    }
    FoldCard("set_clock", "Jam dan susunan", defaultOpen = false) {
        ChoiceRow(
            "Gaya jam",
            listOf("large" to "Besar", "small" to "Kecil", "hidden" to "Sembunyi"),
            cfg.clockStyle
        ) { cfg.clockStyle = it }
        SwitchRow("Format 24 jam", null, cfg.clock24) { cfg.clock24 = it }
        SwitchRow("Tampilkan tanggal", null, cfg.showDate) { cfg.showDate = it }
        IntSlider("Kolom aplikasi di Beranda", "", cfg.homeCols, 3, 6) { cfg.homeCols = it }
        SwitchRow("Dock di bawah", "Tahan lama aplikasi lalu pilih Taruh di dock (maks 5)", cfg.dockOn) { cfg.dockOn = it }
    }
    FoldCard("set_widget", "Widget", "${vm.widgetIds.size} widget", defaultOpen = false) {
        val ctx = LocalContext.current
        val ids = vm.widgetIds
        if (ids.isEmpty()) Text("Belum ada widget.", color = Pal.sub, fontSize = 13.sp)
        ids.forEach { id ->
            val label = try {
                WidgetHost.mgr.getAppWidgetInfo(id)?.loadLabel(ctx.packageManager) ?: "Widget $id"
            } catch (e: Exception) {
                "Widget $id"
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(label, color = Pal.text, fontSize = 15.sp, modifier = Modifier.weight(1f))
                ActionButton("Hapus") { vm.removeWidget(id) }
            }
        }
        ActionButton("Tambah widget", Modifier.fillMaxWidth(), primary = true) { vm.showWidgetPicker = true }
    }
}

// ---------------------------------------------------------------- Ikon

@Composable
private fun IconsPage(vm: MainVm) {
    val cfg = vm.cfg
    FoldCard("set_icon", "Bentuk dan ukuran ikon", cfg.iconShape) {
        ChoiceRow(
            "Bentuk ikon",
            listOf("squircle" to "Squircle", "circle" to "Bulat", "rounded" to "Kotak membulat", "square" to "Kotak", "teardrop" to "Tetesan"),
            cfg.iconShape
        ) { cfg.iconShape = it }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            vm.apps.take(6).forEach { AppIcon(it, 44.dp, badge = false) }
        }
        IntSlider("Ukuran ikon", " dp", cfg.iconSize, 40, 72) { cfg.iconSize = it }
        SwitchRow("Tampilkan nama aplikasi", null, cfg.showLabels) { cfg.showLabels = it }
        if (cfg.showLabels) IntSlider("Ukuran teks nama", " sp", cfg.labelSize, 10, 15) { cfg.labelSize = it }
    }
    FoldCard("set_badge", "Badge notifikasi", if (cfg.badgeOn) "Aktif" else "Mati", defaultOpen = false) {
        SwitchRow("Badge notifikasi", "Titik atau angka di ikon aplikasi yang punya notifikasi", cfg.badgeOn) {
            cfg.badgeOn = it
            if (it && !vm.notifAccess) vm.openNotifAccess()
        }
        if (cfg.badgeOn) {
            if (!vm.notifAccess) {
                Text(
                    "Akses notifikasi belum diberikan. Pilih Tweak Launcher di daftar, lalu nyalakan. Jika Android menolak, buka Info aplikasi > menu titik tiga > Izinkan setelan terbatas.",
                    color = Orange, fontSize = 12.sp
                )
                ActionButton("Beri akses notifikasi", Modifier.fillMaxWidth(), primary = true) { vm.openNotifAccess() }
            }
            ChoiceRow("Gaya badge", listOf("count" to "Angka", "dot" to "Titik"), cfg.badgeStyle) { cfg.badgeStyle = it }
            ChoiceRow(
                "Warna badge",
                listOf("FF5252" to "Merah", "accent" to "Aksen", "FFB300" to "Kuning", "4ADE80" to "Hijau", "7DB8FF" to "Biru"),
                cfg.badgeColor
            ) { cfg.badgeColor = it }
            Text("Untuk mematikan badge satu aplikasi, tahan lama aplikasinya lalu pilih Badge.", color = Pal.sub, fontSize = 12.sp)
        }
    }
    FoldCard("set_drawer", "Daftar aplikasi", defaultOpen = false) {
        ChoiceRow("Gaya daftar aplikasi", listOf("list" to "Daftar", "grid" to "Kotak"), cfg.drawerStyle) { cfg.drawerStyle = it }
        if (cfg.drawerStyle == "grid") IntSlider("Kolom daftar", "", cfg.drawerCols, 3, 6) { cfg.drawerCols = it }
        SwitchRow("Tampilkan aplikasi tersembunyi", "Di tab Semua", cfg.showHidden) { cfg.showHidden = it }
    }
    IconPackCard(vm)
    ImportPackCard(vm)
}

// ---------------------------------------------------------------- Gestur

@Composable
private fun GesturePage(vm: MainVm) {
    val cfg = vm.cfg
    SCard {
        Text(
            "Gestur bekerja di area kosong Beranda. Kunci layar dan panel notifikasi butuh Shizuku aktif.",
            color = Pal.sub, fontSize = 12.sp
        )
        ChoiceRow("Geser ke atas", GESTURE_ACTIONS, cfg.gSwipeUp) { cfg.gSwipeUp = it }
        ChoiceRow("Geser ke bawah", GESTURE_ACTIONS, cfg.gSwipeDown) { cfg.gSwipeDown = it }
        ChoiceRow("Ketuk dua kali", GESTURE_ACTIONS, cfg.gDouble) { cfg.gDouble = it }
        ChoiceRow("Tahan lama", GESTURE_ACTIONS, cfg.gLong) { cfg.gLong = it }
    }
}

// ---------------------------------------------------------------- Profil

@Composable
private fun ProfilesPage(vm: MainVm) {
    val cfg = vm.cfg
    val profiles = vm.profiles
    profiles.forEachIndexed { i, p ->
        FoldCard("set_prof_${p.id}", p.label.ifBlank { "Profil ${i + 1}" }, "Animasi ${p.anim}x · refresh ${p.refresh}", defaultOpen = false) {
            InputBox(p.label, { v ->
                vm.saveProfiles(profiles.mapIndexed { j, x -> if (j == i) p.copy(label = v) else x })
            }, "Nama profil")
            ChoiceRow(
                "Skala animasi",
                listOf("0" to "Mati", "0.5" to "0.5x", "1" to "1x", "1.5" to "1.5x"),
                p.anim
            ) { k -> vm.saveProfiles(profiles.mapIndexed { j, x -> if (j == i) p.copy(anim = k) else x }) }
            ChoiceRow(
                "Refresh rate",
                listOf("0" to "Bawaan", "60" to "60 Hz", "90" to "90 Hz", "120" to "120 Hz"),
                p.refresh.toString()
            ) { k -> vm.saveProfiles(profiles.mapIndexed { j, x -> if (j == i) p.copy(refresh = k.toInt()) else x }) }
            SwitchRow("Mode hemat daya", null, p.saver) { v ->
                vm.saveProfiles(profiles.mapIndexed { j, x -> if (j == i) p.copy(saver = v) else x })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionButton("Terapkan sekarang", Modifier.weight(1f), primary = true) { vm.setProfile(p.id) }
                if (profiles.size > 1) {
                    ActionButton("Hapus") {
                        val rest = profiles.filterIndexed { j, _ -> j != i }
                        vm.saveProfiles(rest)
                        if (cfg.baseProfile == p.id) cfg.baseProfile = rest.first().id
                    }
                }
            }
        }
    }
    if (profiles.size < 6) {
        ActionButton("Tambah profil", Modifier.fillMaxWidth()) {
            vm.saveProfiles(
                profiles + Profile("p" + System.currentTimeMillis(), "Profil ${profiles.size + 1}", "1", 0, false)
            )
        }
    }
    ActionButton("Kembalikan profil bawaan", Modifier.fillMaxWidth()) {
        vm.saveProfiles(ProfileStore.DEFAULT)
        cfg.baseProfile = "normal"
    }
}

// ---------------------------------------------------------------- Otomasi

@Composable
private fun AutomationPage(vm: MainVm) {
    val cfg = vm.cfg
    val profiles = vm.profiles
    FoldCard("set_autoapp", "Profil otomatis", if (cfg.autoApp) "Aktif" else "Mati", defaultOpen = false) {
        SwitchRow(
            "Profil otomatis per aplikasi",
            "Memantau aplikasi di layar lewat layanan latar. Butuh izin Akses penggunaan dan Shizuku.",
            cfg.autoApp
        ) { vm.setAutoApp(it) }
        Hint("Atur profil tiap aplikasi lewat tahan lama aplikasi, lalu Profil khusus. Aplikasi Boost aktif otomatis memakai profil Boost.")
    }
    FoldCard("set_sched", "Jadwal profil", if (cfg.autoSched) "Aktif" else "Mati", defaultOpen = false) {
        SwitchRow("Jadwal profil", "Contoh: mode Hemat tiap malam. Jadwal menimpa profil manual.", cfg.autoSched) { vm.setAutoSched(it) }
        StepRow(
            "Mulai", fmtTime(cfg.schedStart),
            { cfg.schedStart = (cfg.schedStart - 15).mod(1440) },
            { cfg.schedStart = (cfg.schedStart + 15).mod(1440) }
        )
        StepRow(
            "Selesai", fmtTime(cfg.schedEnd),
            { cfg.schedEnd = (cfg.schedEnd - 15).mod(1440) },
            { cfg.schedEnd = (cfg.schedEnd + 15).mod(1440) }
        )
        ChoiceRow("Profil saat jadwal", profiles.map { it.id to it.label }, cfg.schedProfile) { cfg.schedProfile = it }
    }
    FoldCard("set_overlay", "Overlay monitor", if (cfg.overlayOn) "Aktif" else "Mati", defaultOpen = false) {
        SwitchRow(
            "Overlay monitor",
            "Layar (Hz), RAM, suhu, baterai di atas aplikasi lain. Butuh izin Tampil di atas aplikasi lain.",
            cfg.overlayOn
        ) { vm.setOverlay(it) }
        ChoiceRow(
            "Posisi overlay",
            listOf("topleft" to "Kiri atas", "topright" to "Kanan atas", "botleft" to "Kiri bawah", "botright" to "Kanan bawah"),
            cfg.overlayPos
        ) { cfg.overlayPos = it }
        IntSlider("Kepekatan overlay", "%", cfg.overlayAlpha, 20, 100) { cfg.overlayAlpha = it }
    }
    val rules = Store.map(cfg.appProfilesJson)
    FoldCard("set_rules", "Aplikasi dengan profil khusus", "${rules.size} aplikasi", defaultOpen = false) {
        if (rules.isEmpty()) Text("Belum ada.", color = Pal.sub, fontSize = 13.sp)
        rules.forEach { (pkg, pid) ->
            val name = vm.apps.firstOrNull { it.pkg == pkg }?.label ?: pkg
            val pn = profiles.firstOrNull { it.id == pid }?.label ?: pid
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(name, color = Pal.text, fontSize = 15.sp)
                    Text(pn, color = Pal.sub, fontSize = 12.sp)
                }
                ActionButton("Hapus") { vm.setAppProfile(pkg, null) }
            }
        }
    }
}

// ---------------------------------------------------------------- Layar

@Composable
private fun DisplayPage(vm: MainVm) {
    LaunchedEffect(Unit) { vm.refreshDisplay() }
    val d = vm.display
    val ok = vm.shizuku == ShizukuState.READY && d.curDpi > 0
    FoldCard("set_dstat", "Status layar", "${d.curDpi} dpi") {
        if (vm.shizuku != ShizukuState.READY) {
            Text("Butuh Shizuku aktif.", color = Orange, fontSize = 13.sp)
        }
        Text(
            "Saat ini: ${d.curSize.ifEmpty { "-" }}  •  ${d.curDpi} dpi",
            color = Pal.text, fontSize = 15.sp, fontWeight = FontWeight.Medium
        )
        Hint("Setelah diubah, akan muncul konfirmasi 15 detik. Jika tidak dikonfirmasi, pengaturan kembali otomatis, jadi aman jika layar jadi sulit dipakai.")
    }
    FoldCard("set_dpi", "DPI", "${d.curDpi}", defaultOpen = false) {
        StepRow(
            "DPI", "${d.curDpi}",
            { if (ok) vm.setDensity(d.curDpi - 20) },
            { if (ok) vm.setDensity(d.curDpi + 20) }
        )
        ActionButton("Reset DPI", Modifier.fillMaxWidth(), enabled = ok) { vm.resetDensity() }
    }
    FoldCard("set_res", "Resolusi", defaultOpen = false) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(100, 85, 75, 60).forEach { pct ->
                ActionButton("$pct%", Modifier.weight(1f), enabled = ok && d.physSize.isNotEmpty()) { vm.setScale(pct) }
            }
        }
        Text("Resolusi lebih rendah membuat GPU lebih ringan.", color = Pal.sub, fontSize = 12.sp)
        ActionButton("Reset resolusi", Modifier.fillMaxWidth(), enabled = ok) { vm.resetSize() }
    }
}

// ---------------------------------------------------------------- Alat

@Composable
private fun ToolsPage(vm: MainVm) {
    val sleeping = vm.sleeping
    FoldCard("set_maint", "Pemeliharaan (butuh Shizuku)") {
        ActionButton("Bersihkan cache semua aplikasi", Modifier.fillMaxWidth()) {
            vm.runTool("Membersihkan cache") { Tweaks.trimCaches() }
        }
        ActionButton("Jalankan optimasi aplikasi (dexopt)", Modifier.fillMaxWidth()) {
            vm.runTool("Menjalankan optimasi") { Tweaks.dexoptAll() }
        }
    }
    FoldCard("set_sleep", "Tidurkan aplikasi jarang dipakai", "${sleeping.size} tertidur", defaultOpen = false) {
        Hint("Aplikasi yang ditidurkan dihentikan dan dilarang jalan di latar. Dibangunkan otomatis saat dibuka dari launcher. Aplikasi sistem tidak ikut.")
        ActionButton("Cari yang tidak dipakai 14 hari", Modifier.fillMaxWidth()) { vm.loadUnused(14) }
        if (vm.unused.isNotEmpty()) {
            Text(
                vm.unused.take(8).joinToString(", ") { it.label } + if (vm.unused.size > 8) ", ..." else "",
                color = Pal.soft, fontSize = 13.sp
            )
            ActionButton("Tidurkan semua (${vm.unused.size})", Modifier.fillMaxWidth(), primary = true) {
                vm.sleepAll(vm.unused)
            }
        }
        Text("Sedang tertidur: ${sleeping.size} aplikasi", color = Pal.soft, fontSize = 13.sp)
        ActionButton("Bangunkan semua", Modifier.fillMaxWidth(), enabled = sleeping.isNotEmpty()) { vm.wakeAll() }
    }
}

// ---------------------------------------------------------------- Cadangan

@Composable
private fun BackupPage(vm: MainVm) {
    val exportL = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) vm.exportTo(uri)
    }
    val importL = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.importFrom(uri)
    }
    SCard {
        Hint("Cadangan berisi semua tampilan, profil, folder, game, dan aturan. Widget tidak ikut karena terikat pada HP ini.")
        ActionButton("Ekspor pengaturan", Modifier.fillMaxWidth(), primary = true) {
            exportL.launch("tweak-launcher-backup.json")
        }
        ActionButton("Impor pengaturan", Modifier.fillMaxWidth()) {
            importL.launch(arrayOf("application/json", "text/plain", "*/*"))
        }
    }
}

// ---------------------------------------------------------------- Folder Game

@Composable
private fun GamePage(vm: MainVm) {
    val cfg = vm.cfg
    val chosen = Sys.list(cfg.gameApps).toSet()
    val apps = remember(vm.apps) { vm.apps.sortedWith(compareByDescending<AppItem> { it.gameCat }.thenBy { it.label.lowercase() }) }
    FoldCard("set_gamebuiltin", "Game bawaan", "Minesweeper dan Solitaire") {
        Text("Minesweeper dan Solitaire selalu ada di folder Game (Start > Game) untuk mode Windows 95 dan XP. Jendelanya mengambang dan bisa digeser dengan menahan bar judul.", color = Pal.sub, fontSize = 12.sp)
    }
    FoldCard("set_gameapps", "Game Anda di folder Game", "${chosen.size} dipilih") {
        Hint("Centang aplikasi yang mau muncul di folder Game, di bawah Minesweeper dan Solitaire. Aplikasi yang terdeteksi sebagai game ada di urutan atas.")
        apps.forEach { a ->
            Row(
                Modifier.fillMaxWidth().clickable {
                    val n = if (a.pkg in chosen) chosen - a.pkg else chosen + a.pkg
                    cfg.gameApps = Sys.join(n)
                },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AppIcon(a, 28.dp, badge = false)
                Text(a.label, color = Pal.text, fontSize = 14.sp, maxLines = 1, modifier = Modifier.weight(1f))
                androidx.compose.material3.Checkbox(
                    checked = a.pkg in chosen,
                    onCheckedChange = null
                )
            }
        }
    }
}

// ---------------------------------------------------------------- Layar kunci

@Composable
private fun LockPage(vm: MainVm) {
    val cfg = vm.cfg
    val ctx = LocalContext.current
    val overlayOk = android.provider.Settings.canDrawOverlays(ctx)
    FoldCard("set_lock", "Layar kunci launcher", if (cfg.lockOn) "Aktif" else "Mati") {
        SwitchRow("Layar kunci launcher", "Gaya mengikuti mode: Windows 95, XP, PDA, Mac (mode lain memakai Windows 95)", cfg.lockOn) { vm.setLock(it) }
        Hint("Ini lapisan tampilan, bukan pengganti kunci HP. Setelah menekan OK, lockscreen asli (PIN, pola, sidik jari) tetap muncul, jadi keamanan tidak berkurang. Butuh izin tampil di atas aplikasi lain, atau Shizuku aktif. Matikan saklar ini kapan saja untuk kembali ke lockscreen biasa.")
        Text(
            if (overlayOk) "Izin tampil di atas aplikasi lain: aktif" else "Izin tampil di atas aplikasi lain: belum (Shizuku bisa menggantikan)",
            color = if (overlayOk) Pal.sub else Orange, fontSize = 12.sp
        )
        if (!overlayOk) {
            ActionButton("Beri izin", Modifier.fillMaxWidth()) {
                vm.openOverlayPermission()
            }
        }
        ActionButton("Coba sekarang", Modifier.fillMaxWidth(), primary = true) { LockLauncher.preview(ctx) }
    }
}

// ---------------------------------------------------------------- Pembaruan

@Composable
private fun UpdatePage(vm: MainVm) {
    val cfg = vm.cfg
    val u = vm.update
    SCard {
        Text("Versi terpasang: ${BuildConfig.VERSION_NAME}", color = Pal.text, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        SwitchRow("Periksa otomatis", "Sekali tiap 12 jam saat launcher dibuka", cfg.autoUpdate) { cfg.autoUpdate = it }
        ActionButton("Periksa pembaruan", Modifier.fillMaxWidth(), primary = true, enabled = !vm.checking) {
            vm.checkUpdate(true)
        }
        if (vm.updateStatus.isNotEmpty()) Text(vm.updateStatus, color = Pal.soft, fontSize = 13.sp)
        if (u != null) {
            if (u.notes.isNotBlank()) Text(u.notes, color = Pal.sub, fontSize = 12.sp)
            if (vm.downloading) LinearProgressIndicator(progress = { vm.progress }, color = Pal.accent, trackColor = Pal.line)
            ActionButton(
                if (vm.downloaded != null) "Pasang sekarang" else "Unduh dan pasang",
                Modifier.fillMaxWidth(),
                primary = true,
                enabled = !vm.downloading
            ) { if (vm.downloaded != null) vm.installUpdate() else vm.downloadUpdate() }
        }
    }
    SCard {
        Hint("Pembaruan diambil dari GitHub Releases repo ${Updater.REPO}. Repo harus publik, dan APK baru dibuat otomatis setiap ada perubahan kode.")
    }
}

// ---------------------------------------------------------------- Tentang

@Composable
private fun AboutPage(vm: MainVm) {
    SCard {
        Text("Tweak Launcher", color = Pal.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text("Versi ${BuildConfig.VERSION_NAME}", color = Pal.soft, fontSize = 14.sp)
        Text("Dibuat oleh Mohammad Fauzi", color = Pal.soft, fontSize = 14.sp)
        Text(
            "Shizuku: " + when (vm.shizuku) {
                ShizukuState.READY -> "aktif"
                ShizukuState.NEED_PERM -> "belum diberi izin"
                ShizukuState.OFF -> "belum berjalan"
            },
            color = Pal.sub, fontSize = 13.sp
        )
    }
}
