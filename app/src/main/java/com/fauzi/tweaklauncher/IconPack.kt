package com.fauzi.tweaklauncher

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipInputStream
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
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
        "mac2" to "Panel Kontrol (Mac)",
        "sym1" to "Perawatan (Symbian)",
        "sym2" to "Setelan (Symbian)"
    )

    /** Kata kunci nama berkas untuk menebak ikon sistem dari paket impor, per peran. */
    private val SYS_ROLE = mapOf(
        "computer" to "computer", "mac0" to "computer",
        "folder" to "folder", "mac1" to "folder",
        "panel" to "panel", "mac2" to "panel",
        "start" to "start", "sym1" to "care", "sym2" to "settings"
    )
    private val SYS_KEYS = mapOf(
        "computer" to listOf("mycomputer", "computer", "desktop", "monitor", "pc"),
        "folder" to listOf("folder", "directory", "programs", "program"),
        "panel" to listOf("controlpanel", "control", "panel", "tools", "tool"),
        "start" to listOf("start", "windows", "winlogo", "logo"),
        "care" to listOf("perawatan", "maintenance", "care", "health", "battery", "heart", "tools"),
        "settings" to listOf("settings", "setting", "pengaturan", "setelan", "gear", "config", "preferences", "options")
    )

    /** Tebak ikon untuk satu slot sistem dari daftar nama ikon sebuah paket. */
    fun guessSysIn(slot: String, names: List<String>): String? {
        val keys = SYS_KEYS[SYS_ROLE[slot]] ?: return null
        val flat = names.map { it to it.substringAfter('/').lowercase().replace(Regex("[^a-z0-9]"), "") }
        for (k in keys) flat.firstOrNull { it.second == k }?.let { return it.first }
        for (k in keys) flat.firstOrNull { it.second.contains(k) }?.let { return it.first }
        return null
    }

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
            if (name.contains('/')) {
                if (name.contains("..")) null
                else BitmapFactory.decodeFile(File(packRoot(ctx), "$name.png").path)?.asImageBitmap()
            } else {
                ctx.assets.open("icons/$name.png").use { BitmapFactory.decodeStream(it) }?.asImageBitmap()
            }
        } catch (e: Exception) {
            null
        }
        if (bmp != null) cache[name] = bmp
        return bmp
    }

    // ---- Paket impor (zip)

    private fun packRoot(ctx: Context) = File(ctx.filesDir, "iconpacks")

    fun packs(ctx: Context): List<String> =
        packRoot(ctx).listFiles()?.filter { it.isDirectory }?.map { it.name }?.sorted() ?: emptyList()

    /** Nama ikon dalam satu paket impor, berbentuk "paket/nama". */
    fun packNames(ctx: Context, pack: String): List<String> =
        File(packRoot(ctx), pack).listFiles()?.filter { it.name.endsWith(".png") }
            ?.map { "$pack/" + it.name.removeSuffix(".png") }?.sorted() ?: emptyList()

    fun deletePack(ctx: Context, pack: String) {
        if (pack.contains("..") || pack.contains('/')) return
        File(packRoot(ctx), pack).deleteRecursively()
        cache.keys.removeAll { it.startsWith("$pack/") }
    }

    fun displayName(ctx: Context, uri: Uri): String {
        var n = ""
        try {
            ctx.contentResolver.query(uri, null, null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (i >= 0) n = c.getString(i) ?: ""
                }
            }
        } catch (e: Exception) {
        }
        return n
    }

    private fun clean(s: String) = s.replace(Regex("[^A-Za-z0-9_.-]"), "_").trim('.', '_').ifEmpty { "ikon" }

    /** Impor .zip berisi PNG, WebP, atau JPG. Mengembalikan (nama paket, jumlah ikon) atau null jika gagal. */
    fun importZip(ctx: Context, uri: Uri, fileName: String): Pair<String, Int>? {
        val pack = clean(fileName.substringBeforeLast('.', fileName)).take(40)
        val dir = File(packRoot(ctx), pack)
        val tmp = File(packRoot(ctx), ".tmp_$pack")
        tmp.deleteRecursively()
        tmp.mkdirs()
        // nama dasar -> (lebar terbesar)
        val widths = HashMap<String, Int>()
        var count = 0
        try {
            val ins = ctx.contentResolver.openInputStream(uri) ?: return null
            ZipInputStream(ins.buffered()).use { z ->
                while (true) {
                    val e = z.nextEntry ?: break
                    if (e.isDirectory) continue
                    val path = e.name
                    val base = path.substringAfterLast('/')
                    if (path.startsWith("__MACOSX") || base.startsWith("._") || base.startsWith(".")) continue
                    val ext = base.substringAfterLast('.', "").lowercase()
                    if (ext != "png" && ext != "webp" && ext != "jpg" && ext != "jpeg") continue
                    val bytes = z.readBytes()
                    if (bytes.size > 2_000_000) continue
                    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: continue
                    if (bmp.width > 512 || bmp.height > 512) continue
                    // "nama-2" dan sejenisnya dianggap varian ukuran dari ikon yang sama
                    val stem = base.substringBeforeLast('.')
                    val name = clean(stem.replace(Regex("-\\d+$"), ""))
                    val prev = widths[name]
                    if (prev != null && prev >= bmp.width) continue
                    if (prev == null && count >= 4000) continue
                    if (prev == null) count++
                    widths[name] = bmp.width
                    File(tmp, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
                }
            }
        } catch (e: Exception) {
            tmp.deleteRecursively()
            return null
        }
        if (widths.isEmpty()) {
            tmp.deleteRecursively()
            return null
        }
        dir.deleteRecursively()
        tmp.renameTo(dir)
        cache.keys.removeAll { it.startsWith("$pack/") }
        return pack to widths.size
    }

    /** Tebak ikon dari nama berkas paket impor: kata nama aplikasi dicocokkan dengan nama berkas. */
    fun guessIn(label: String, pkg: String, names: List<String>): String? {
        val words = (label.lowercase().split(Regex("[^a-z0-9]+")) + pkg.lowercase().split('.'))
            .filter { it.length >= 3 && it != "com" && it != "android" && it != "app" }
        if (words.isEmpty()) return null
        val flat = names.map { it to it.substringAfter('/').lowercase() }
        for (w in words) {
            flat.firstOrNull { it.second == w }?.let { return it.first }
        }
        for (w in words) {
            flat.firstOrNull { it.second.contains(w) }?.let { return it.first }
        }
        return null
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

fun MainVm.guessIcons(ctx: Context, pack: String? = null): Int {
    val valid = IconPack.names(ctx).toSet()
    val own = if (pack != null) IconPack.packNames(ctx, pack) else emptyList()
    val m = Store.map(cfg.appIconsJson)
    var n = 0
    apps.forEach { a ->
        if (a.pkg !in m) {
            val g = if (pack != null) IconPack.guessIn(a.label, a.pkg, own) else IconPack.guess(a.label, a.pkg, valid)
            if (g != null) {
                m[a.pkg] = g
                n++
            }
        }
    }
    cfg.appIconsJson = Store.mapJson(m)
    return n
}

fun MainVm.importIconPack(ctx: Context, uri: Uri) {
    packMsg = "Mengimpor paket ikon..."
    viewModelScope.launch {
        val r = withContext(Dispatchers.IO) {
            IconPack.importZip(ctx, uri, IconPack.displayName(ctx, uri).ifEmpty { "paket" })
        }
        packMsg = if (r == null) "Gagal: zip kosong atau tidak berisi gambar PNG, WebP, atau JPG (berkas .ico tidak bisa dibaca Android)."
        else "Paket \"${r.first}\" masuk: ${r.second} ikon."
        packRev++
    }
}

/** Tebak ikon sistem launcher. Paket impor menimpa ikon yang cocok; Windows 98 hanya mengisi yang kosong. */
fun MainVm.guessSysIcons(ctx: Context, pack: String? = null): Int {
    val m = Store.map(cfg.sysIconsJson)
    var n = 0
    if (pack == null) {
        IconPack.DEFAULT_SYS.forEach { (slot, name) ->
            if (slot !in m) {
                m[slot] = name
                n++
            }
        }
    } else {
        val own = IconPack.packNames(ctx, pack)
        IconPack.SYS_SLOTS.forEach { (slot, _) ->
            val g = IconPack.guessSysIn(slot, own)
            if (g != null) {
                m[slot] = g
                n++
            }
        }
    }
    cfg.sysIconsJson = Store.mapJson(m)
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
    val packs = remember(vm.packRev) { IconPack.packs(ctx) }
    var sel by remember { mutableStateOf("") }
    val all = remember(sel, vm.packRev) { if (sel.isEmpty()) IconPack.names(ctx) else IconPack.packNames(ctx, sel) }
    val key = q.trim().replace(' ', '_').lowercase()
    val list = if (key.isEmpty()) all else all.filter { it.substringAfter('/').contains(key) }
    TweakDialog(
        onDismissRequest = { vm.iconPick = null },
        containerColor = Pal.card,
        title = { Text("Ikon: ${t.title}", color = Pal.text, fontSize = 17.sp) },
        text = {
            Column {
                if (packs.isNotEmpty()) {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("" to "Windows 98").plus(packs.map { it to it }).forEach { (id, lab) ->
                            Pill(lab, sel == id) { sel = id }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
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
                                    n.substringAfter('/'),
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
        confirmButton = { DlgButton(onClick = { vm.iconPick = null }) { Text("Tutup", color = Pal.accent) } },
        dismissButton = {
            DlgButton(onClick = {
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
    val packs = remember(vm.packRev) { IconPack.packs(ctx) }
    var guessPack by remember { mutableStateOf("") }
    FoldCard("set_pack", "Paket ikon Windows 98", defaultOpen = false) {
        Hint("Ganti ikon aplikasi dengan menahan lama aplikasinya lalu pilih Ikon. Atau tebak otomatis di bawah.")
        SwitchRow("Ikon paket hitam putih", "Cocok untuk mode Mac OS klasik", cfg.packGray) { cfg.packGray = it }
        if (packs.isNotEmpty()) {
            Text("Paket untuk tebak otomatis", color = Pal.sub, fontSize = 12.sp)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("" to "Windows 98").plus(packs.map { it to it }).forEach { (id, lab) ->
                    Pill(lab, guessPack == id) { guessPack = id }
                }
            }
        }
        ActionButton("Tebak ikon otomatis untuk semua aplikasi", Modifier.fillMaxWidth(), primary = true) {
            val n = vm.guessIcons(ctx, guessPack.ifEmpty { null })
            val k = vm.guessSysIcons(ctx, guessPack.ifEmpty { null })
            msg = "$n aplikasi dan $k ikon sistem diberi ikon. Koreksi yang salah lewat tahan lama > Ikon."
        }
        ActionButton("Hapus semua ikon aplikasi kustom", Modifier.fillMaxWidth()) {
            cfg.appIconsJson = "{}"
            msg = "Ikon aplikasi dikembalikan ke aslinya."
        }
        if (msg.isNotEmpty()) Text(msg, color = Pal.sub, fontSize = 12.sp)
    }
    FoldCard("set_sysicons", "Ikon sistem launcher", defaultOpen = false) {
        Text("Dipakai di mode Windows 95, XP, Mac OS klasik, dan Symbian. Ketuk untuk memilih.", color = Pal.sub, fontSize = 12.sp)
        ActionButton("Tebak ikon sistem dari paket terpilih", Modifier.fillMaxWidth()) {
            val k = vm.guessSysIcons(ctx, guessPack.ifEmpty { null })
            msg = "$k ikon sistem diberi ikon."
        }
        if (msg.isNotEmpty()) Text(msg, color = Pal.sub, fontSize = 12.sp)
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

@Composable
fun ImportPackCard(vm: MainVm) {
    val ctx = LocalContext.current
    val packs = remember(vm.packRev) { IconPack.packs(ctx) }
    val zip = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.importIconPack(ctx, uri)
    }
    FoldCard("set_import", "Impor paket ikon (.zip)", "${packs.size} paket diimpor", defaultOpen = false) {
        Hint("Pilih berkas .zip berisi gambar PNG, WebP, atau JPG. Berkas .ico tidak bisa dibaca Android, ubah dulu ke PNG. Paket tampil di galeri ikon dan bisa dipakai tebak otomatis jika nama berkas mirip nama aplikasi.")
        ActionButton("Impor paket ikon (.zip)", Modifier.fillMaxWidth(), primary = true) {
            zip.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream", "*/*"))
        }
        if (vm.packMsg.isNotEmpty()) Text(vm.packMsg, color = Pal.sub, fontSize = 12.sp)
        packs.forEach { p ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(p, color = Pal.text, fontSize = 14.sp)
                    Text("${IconPack.packNames(ctx, p).size} ikon", color = Pal.sub, fontSize = 11.sp)
                }
                DlgButton(onClick = {
                    IconPack.deletePack(ctx, p)
                    // buang ikon kustom yang menunjuk ke paket ini
                    for (isSys in listOf(false, true)) {
                        val m = Store.map(if (isSys) vm.cfg.sysIconsJson else vm.cfg.appIconsJson)
                        m.entries.removeAll { it.value.startsWith("$p/") }
                        val out = Store.mapJson(m)
                        if (isSys) vm.cfg.sysIconsJson = out else vm.cfg.appIconsJson = out
                    }
                    vm.packRev++
                }) { Text("Hapus", color = Pal.accent) }
            }
        }
    }
}
