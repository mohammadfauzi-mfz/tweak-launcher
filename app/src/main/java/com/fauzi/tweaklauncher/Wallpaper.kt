package com.fauzi.tweaklauncher

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import java.io.File

object WallpaperStore {
    private fun file(ctx: Context, mode: String): File {
        val d = File(ctx.filesDir, "wallpapers")
        d.mkdirs()
        return File(d, "$mode.img")
    }

    fun save(ctx: Context, mode: String, uri: Uri): Boolean = try {
        val ins = ctx.contentResolver.openInputStream(uri)
        if (ins == null) {
            false
        } else {
            ins.use { i -> file(ctx, mode).outputStream().use { o -> i.copyTo(o) } }
            true
        }
    } catch (e: Exception) {
        false
    }

    fun load(ctx: Context, mode: String): ImageBitmap? = try {
        val f = file(ctx, mode)
        if (!f.exists()) {
            null
        } else {
            val b = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(f.path, b)
            var s = 1
            while (b.outWidth / s > 2000 || b.outHeight / s > 2000) s *= 2
            val o = BitmapFactory.Options().apply { inSampleSize = s }
            BitmapFactory.decodeFile(f.path, o)?.asImageBitmap()
        }
    } catch (e: Exception) {
        null
    }
}

fun Cfg.wallpaper(mode: String): String = when (mode) {
    "xp" -> wpXp
    "sym" -> wpSym
    else -> wp95
}

fun Cfg.setWallpaper(mode: String, v: String) {
    when (mode) {
        "xp" -> wpXp = v
        "sym" -> wpSym = v
        else -> wp95 = v
    }
}

@Composable
fun rememberWallpaper(spec: String, mode: String): ImageBitmap? {
    val ctx = LocalContext.current
    return remember(spec) { if (spec.startsWith("image:")) WallpaperStore.load(ctx, mode) else null }
}

/** Menggambar latar desktop: warna polos, foto (dipotong penuh), wallpaper sistem, atau bawaan mode. */
fun Modifier.wallpaperBg(spec: String, img: ImageBitmap?, default: DrawScope.() -> Unit): Modifier =
    this.drawBehind {
        when {
            spec == "system" -> {}
            spec.startsWith("color:") -> {
                val c = try {
                    Color(android.graphics.Color.parseColor("#" + spec.removePrefix("color:")))
                } catch (e: Exception) {
                    Color.Black
                }
                drawRect(c)
            }
            img != null -> {
                val bw = img.width
                val bh = img.height
                val w = size.width
                val h = size.height
                val scale = maxOf(w / bw, h / bh)
                val sw = (w / scale).toInt().coerceIn(1, bw)
                val sh = (h / scale).toInt().coerceIn(1, bh)
                drawImage(
                    img,
                    srcOffset = IntOffset((bw - sw) / 2, (bh - sh) / 2),
                    srcSize = IntSize(sw, sh),
                    dstSize = IntSize(w.toInt(), h.toInt())
                )
            }
            else -> default(this)
        }
    }

/** Pengaturan wallpaper desktop untuk mode Windows 95 dan XP (masing-masing punya sendiri). */
@Composable
fun WallpaperCard(vm: MainVm) {
    val cfg = vm.cfg
    val mode = cfg.uiMode
    if (mode != "win95" && mode != "xp" && mode != "sym") return
    val ctx = LocalContext.current
    val spec = cfg.wallpaper(mode)
    val kind = when {
        spec == "system" -> "system"
        spec.startsWith("color:") -> "color"
        spec.startsWith("image:") -> "image"
        else -> "default"
    }
    var hex by remember(mode) {
        mutableStateOf(if (spec.startsWith("color:")) spec.removePrefix("color:") else if (mode == "xp") "3A78D8" else if (mode == "sym") "1F63B5" else "008080")
    }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null && WallpaperStore.save(ctx, mode, uri)) {
            cfg.setWallpaper(mode, "image:" + System.currentTimeMillis())
        }
    }
    SCard {
        Text("Wallpaper desktop", color = Pal.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        ChoiceRow(
            "Latar",
            listOf("default" to "Bawaan", "color" to "Warna", "image" to "Foto", "system" to "Wallpaper HP"),
            kind
        ) { k ->
            when (k) {
                "default" -> cfg.setWallpaper(mode, "")
                "system" -> cfg.setWallpaper(mode, "system")
                "color" -> cfg.setWallpaper(mode, "color:" + hex.trim().trimStart('#').ifEmpty { "000000" })
                else -> pick.launch("image/*")
            }
        }
        if (kind == "color") {
            InputBox(hex, {
                hex = it
                val h = it.trim().trimStart('#')
                if (Regex("[0-9a-fA-F]{6}").matches(h)) cfg.setWallpaper(mode, "color:$h")
            }, "Kode hex 6 digit, mis. 008080")
        }
        if (kind == "image") {
            ActionButton("Ganti foto", Modifier.fillMaxWidth()) { pick.launch("image/*") }
        }
        if (kind == "system") {
            Text("Wallpaper HP tampil di belakang desktop. Atur peredupan di Setelan > Tampilan.", color = Pal.sub, fontSize = 12.sp)
        }
    }
}
