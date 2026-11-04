package com.fauzi.tweaklauncher

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val tag: String,
    val code: Int,
    val notes: String,
    val url: String,
    val size: Long
)

/** Cek dan unduh pembaruan dari GitHub Releases (repo harus publik). */
object Updater {
    const val REPO = "mohammadfauzi-mfz/tweak-launcher"

    fun check(): UpdateInfo {
        val c = URL("https://api.github.com/repos/$REPO/releases/latest").openConnection() as HttpURLConnection
        c.connectTimeout = 10000
        c.readTimeout = 15000
        c.setRequestProperty("Accept", "application/vnd.github+json")
        c.setRequestProperty("User-Agent", "TweakLauncher")
        val code = c.responseCode
        if (code == 404) {
            throw IOException("Belum ada rilis, atau repo masih privat. Jadikan repo publik di GitHub.")
        }
        if (code != 200) throw IOException("GitHub membalas HTTP $code")
        val j = JSONObject(c.inputStream.bufferedReader().readText())
        val tag = j.getString("tag_name")
        val vc = tag.substringAfterLast('.').filter { it.isDigit() }.toIntOrNull() ?: 0
        val assets = j.getJSONArray("assets")
        var url = ""
        var size = 0L
        for (i in 0 until assets.length()) {
            val a = assets.getJSONObject(i)
            if (a.getString("name").endsWith(".apk")) {
                url = a.getString("browser_download_url")
                size = a.optLong("size")
                break
            }
        }
        if (url.isEmpty()) throw IOException("Rilis $tag tidak berisi berkas APK.")
        return UpdateInfo(tag, vc, j.optString("body"), url, size)
    }

    fun download(ctx: Context, url: String, onProgress: (Float) -> Unit): File {
        val dir = File(ctx.cacheDir, "updates")
        dir.mkdirs()
        dir.listFiles()?.forEach { it.delete() }
        val f = File(dir, "update.apk")
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 15000
        c.readTimeout = 30000
        c.setRequestProperty("User-Agent", "TweakLauncher")
        if (c.responseCode != 200) throw IOException("Unduhan gagal: HTTP ${c.responseCode}")
        val total = c.contentLengthLong
        c.inputStream.use { ins ->
            f.outputStream().use { out ->
                val buf = ByteArray(32 * 1024)
                var read = 0L
                while (true) {
                    val n = ins.read(buf)
                    if (n < 0) break
                    out.write(buf, 0, n)
                    read += n
                    if (total > 0) onProgress((read.toFloat() / total).coerceIn(0f, 1f))
                }
            }
        }
        return f
    }

    /** Mengembalikan false jika izin pasang dari sumber ini belum diberikan (halaman izin dibuka). */
    fun install(ctx: Context, f: File): Boolean {
        if (!ctx.packageManager.canRequestPackageInstalls()) {
            ctx.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${ctx.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            return false
        }
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.files", f)
        val i = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(i)
        return true
    }
}
