package com.fauzi.tweaklauncher

import android.content.pm.PackageManager
import rikka.shizuku.Shizuku

enum class ShizukuState { OFF, NEED_PERM, READY }

/** Menjalankan perintah shell lewat Shizuku (tanpa root). */
object Shell {

    fun state(): ShizukuState = try {
        when {
            !Shizuku.pingBinder() -> ShizukuState.OFF
            Shizuku.isPreV11() -> ShizukuState.OFF
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED -> ShizukuState.READY
            else -> ShizukuState.NEED_PERM
        }
    } catch (e: Throwable) {
        ShizukuState.OFF
    }

    /** Menunggu binder Shizuku tersambung (dipakai saat proses dimulai dari alarm). */
    fun awaitBinder(timeoutMs: Long = 4000) {
        val end = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < end) {
            if (state() != ShizukuState.OFF) return
            Thread.sleep(250)
        }
    }

    /** Mengembalikan (kode keluar, keluaran). Kode -1 berarti gagal dijalankan. */
    fun run(cmd: String): Pair<Int, String> = try {
        // newProcess bersifat private di Shizuku 13.x, tapi masih bisa dipanggil lewat refleksi.
        val m = Shizuku::class.java.getDeclaredMethod(
            "newProcess",
            Array<String>::class.java,
            Array<String>::class.java,
            String::class.java
        )
        m.isAccessible = true
        val p = m.invoke(null, arrayOf("sh", "-c", cmd), null, null) as Process
        val out = p.inputStream.bufferedReader().readText()
        val err = p.errorStream.bufferedReader().readText()
        val code = p.waitFor()
        code to (out + err).trim()
    } catch (e: Throwable) {
        -1 to (e.message ?: "gagal menjalankan perintah")
    }
}
