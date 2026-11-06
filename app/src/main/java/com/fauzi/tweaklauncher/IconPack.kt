package com.fauzi.tweaklauncher

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Sasaran pemilihan ikon: satu aplikasi ([sys] = false, [key] = nama paket) atau satu ikon sistem launcher. */
data class IconTarget(val sys: Boolean, val key: String, val title: String)

object IconPack {
    private var names: List<String>? = null
    private val cache = HashMap<String, ImageBitmap>()

    /** Ikon sistem yang bisa diganti. Dipakai oleh mode Windows 95, XP, dan Mac OS klasik. */
    val SYS_SLOTS = listOf(
        "computer" to "Komputer Saya (Win95 dan XP)",
        "folder" to "Program (Win95 dan XP)",
        "panel" to "Panel Kontrol (Win95 dan XP)",
        "start" to "Logo Start (Win95 dan XP)",
        "mac0" to "Macintosh HD (Mac)",
        "mac1" to "Program (Mac)",
        "mac2" to "Panel Kontrol (Mac)"
    )

    val DEFAULT_SYS = mapOf(
        "computer" to "computer_explorer",
        "folder" to "directory_closed",
        "panel" to "directory_control_panel",
        "start" to "windows",
        "mac0" to "computer_explorer",
        "mac1" to "directory_closed",
        "mac2" to "directory_control_panel"
    )

    /** Tebakan ikon dari nama aplikasi: kata kunci ke nama ikon. */
    private val RULES: List<Pair<List<String>, String>> = listOf(
        listOf("kalkulator", "calculator", "calc") to "calculator",
        listOf("kamera", "camera") to "camera3",
        listOf("foto", "photos", "galeri", "gallery", "gambar") to "image_old_jpeg",
        listOf("telepon", "phone", "dialer") to "world_phonereceiver",
        listOf("kontak", "contacts") to "address_book",
        listOf("whatsapp", "telegram", "messenger", "signal", "chat", "discord") to "msn",
        listOf("pesan", "message", "sms") to "message_envelope_open",
        listOf("gmail", "email", "outlook", "mail") to "outlook_express",
        listOf("chrome", "brave", "browser", "firefox", "opera", "safari", "internet") to "world",
        listOf("maps", "peta", "gps", "navigasi", "waze") to "globe_map",
        listOf("musik", "music", "spotify", "podcast", "audio") to "cd_audio_cd",
        listOf("video", "youtube", "vlc", "netflix", "film", "player") to "media_player",
        listOf("catatan", "notes", "memo", "keep") to "notepad",
        listOf("jam", "clock", "alarm", "timer") to "clock",
        listOf("kalender", "calendar") to "calendar",
        listOf("berkas", "files", "manager") to "directory_open_file_mydocs",
        listOf("setelan", "settings", "pengaturan") to "settings_gear",
        listOf("store", "toko", "update") to "windows_update_large",
        listOf("drive", "cloud", "dropbox", "onedrive", "backup") to "network_drive_world",
        listOf("game", "permainan", "solitaire") to "game_solitaire",
        listOf("pdf", "acrobat", "docs", "word", "dokumen", "document") to "document",
        listOf("scan", "scanner") to "scanner",
        listOf("vpn", "warp", "wifi", "network", "jaringan") to "network_normal_two_pcs",
        listOf("bank", "wallet", "dompet", "gopay", "ovo", "dana", "pay") to "key_gray",
        listOf("shizuku", "tweak") to "computer_gear",
        listOf("bantuan", "help", "asisten", "assistant") to "help_book_big",
        listOf("baterai", "battery") to "battery",
        listOf("canva", "paint", "capcut", "editor", "desain") to "paint_file",
        listOf("printer", "cetak") to "printer"
    )

    fun names(ctx: Context): List<String> {
        names?.let { return it }
        val l = try {
            ctx.assets.list("icons")?.filter { it.endsWith(".png") }?.map { it.removeSuffix(".png") }?.sorted() ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
        names = l
        return l
    }

    fun get(ctx: Context, name: String): ImageBitmap? {
        cache[name]?.let { return it }
        val bmp = try {
            ctx.assets.open("icons/$name.png").use { BitmapFactory.decodeStream(it) }?.asImageBitmap()
        } catch (e: Exception) {
            null
        }
        if (bmp != null) cache[name] = bmp
        return bmp
    }

    /** Cari nama ikon yang cocok untuk satu aplikasi, atau null jika tidak ada tebakan. */
    fun guess(label: String, pkg: String, valid: Set<String>): String? {
        val l = label.lowercase()
        val p = pkg.lowercase()
        val words = l.split(Regex("[^a-z0-9]+")).filter { it.isNotEmpty() }
        for ((keys, icon) in RULES) {
            if (icon !in valid) continue
            for (k in keys) {
                val hit = if (k.length <= 4) k in words else (l.contains(k) || p.contains(k))
                if (hit) return icon
            }
        }
        return null
    }

    val GRAY: ColorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
}

// ---------------------------------------------------------------- Operasi

fun MainVm.setIcon(t: IconTarget, name: String?) {
    val json = if (t.sys) cfg.sysIconsJson else cfg.appIconsJson
    val m = Store.map(json)
    if (name == null) m.remove(t.key) else m[t.key] = name
    val out = Store.mapJson(m)
    if (t.sys) cfg.sysIconsJson = out else cfg.appIconsJson = out
}

fun MainVm.guessIcons(ctx: Context): Int {
    val valid = IconPack.names(ctx).toSet()
    val m = Store.map(cfg.appIconsJson)
    var n = 0
    apps.forEach { a ->
        if (a.pkg !in m) {
            val g = IconPack.guess(a.label, a.pkg, valid)
            if (g != null) {
                m[a.pkg] = g
                n++
            }
        }
    }
    cfg.appIconsJson = Store.mapJson(m)
    return n
}

fun MainVm.applyDefaultSys() {
    cfg.sysIconsJson = Store.mapJson(IconPack.DEFAULT_SYS)
}

// ---------------------------------------------------------------- Galeri

@Composable
fun IconPickerDialog(vm: MainVm) {
    val t = vm.iconPick ?: return
    val ctx = LocalContext.current
    var q by remember { mutableStateOf("") }
    val all = remember { IconPack.names(ctx) }
    val key = q.trim().replace(' ', '_').lowercase()
    val list = if (key.isEmpty()) all else all.filter { it.contains(key) }
    AlertDialog(
        onDismissRequest = { vm.iconPick = null },
        containerColor = Pal.card,
        title = { Text("Ikon: ${t.title}", color = Pal.text, fontSize = 17.sp) },
        text = {
            Column {
                InputBox(q, { q = it }, "Cari ikon, mis. folder, computer, paint")
                Spacer(Modifier.height(8.dp))
                Text("${list.size} ikon", color = Pal.sub, fontSize = 11.sp)
                Spacer(Modifier.height(4.dp))
                Box(Modifier.height(360.dp)) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(list, key = { it }) { n ->
                            val bmp = IconPack.get(ctx, n)
                            Column(
                                Modifier
                                    .clickable {
                                        vm.setIcon(t, n)
                                        vm.iconPick = null
                                    }
                                    .padding(2.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (bmp != null) {
                                    Image(
                                        bitmap = bmp,
                                        contentDescription = n,
                                        filterQuality = FilterQuality.None,
                                        modifier = Modifier.size(44.dp).background(Pal.inset).padding(4.dp)
                                    )
                                }
                                Text(
                                    n,
                                    color = Pal.sub,
                                    fontSize = 8.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { vm.iconPick = null }) { Text("Tutup", color = Pal.accent) } },
        dismissButton = {
            TextButton(onClick = {
                vm.setIcon(t, null)
                vm.iconPick = null
            }) { Text("Kembalikan asli", color = Pal.accent) }
        }
    )
}

// ---------------------------------------------------------------- Setelan

@Composable
fun IconPackCard(vm: MainVm) {
    val cfg = vm.cfg
    val ctx = LocalContext.current
    var msg by remember { mutableStateOf("") }
    SCard {
        Text("Paket ikon Windows 98", color = Pal.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(
            "Ganti ikon aplikasi dengan menahan lama aplikasinya lalu pilih Ikon. Atau tebak otomatis di bawah.",
            color = Pal.sub, fontSize = 12.sp
        )
        SwitchRow("Ikon paket hitam putih", "Cocok untuk mode Mac OS klasik", cfg.packGray) { cfg.packGray = it }
        ActionButton("Tebak ikon otomatis untuk semua aplikasi", Modifier.fillMaxWidth(), primary = true) {
            val n = vm.guessIcons(ctx)
            msg = "$n aplikasi diberi ikon. Koreksi yang salah lewat tahan lama > Ikon."
        }
        ActionButton("Hapus semua ikon aplikasi kustom", Modifier.fillMaxWidth()) {
            cfg.appIconsJson = "{}"
            msg = "Ikon aplikasi dikembalikan ke aslinya."
        }
        if (msg.isNotEmpty()) Text(msg, color = Pal.sub, fontSize = 12.sp)
    }
    SCard {
        Text("Ikon sistem launcher", color = Pal.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text("Dipakai di mode Windows 95, XP, dan Mac OS klasik. Ketuk untuk memilih.", color = Pal.sub, fontSize = 12.sp)
        IconPack.SYS_SLOTS.forEach { (slot, label) ->
            val cur = cfg.sysIcon(slot)
            val bmp = cur?.let { IconPack.get(ctx, it) }
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { vm.iconPick = IconTarget(true, slot, label) }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(Modifier.size(36.dp).background(Pal.inset), contentAlignment = Alignment.Center) {
                    if (bmp != null) {
                        Image(bmp, null, Modifier.size(32.dp), filterQuality = FilterQuality.None)
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text(label, color = Pal.text, fontSize = 14.sp)
                    Text(cur ?: "gambar bawaan", color = Pal.sub, fontSize = 11.sp)
                }
            }
        }
        ActionButton("Pakai pilihan Windows 98 yang disarankan", Modifier.fillMaxWidth(), primary = true) { vm.applyDefaultSys() }
        ActionButton("Kembalikan ikon sistem ke gambar bawaan", Modifier.fillMaxWidth()) { cfg.sysIconsJson = "{}" }
    }
}
