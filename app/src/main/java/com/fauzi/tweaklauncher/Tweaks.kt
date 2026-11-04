package com.fauzi.tweaklauncher

data class TweakResult(val ok: Boolean, val message: String)

data class DisplayInfo(
    val physSize: String = "",
    val overSize: String = "",
    val physDpi: Int = 0,
    val overDpi: Int = 0
) {
    val curDpi: Int get() = if (overDpi > 0) overDpi else physDpi
    val curSize: String get() = if (overSize.isNotEmpty()) overSize else physSize
}

/** Semua perintah yang berjalan lewat Shizuku. */
object Tweaks {

    private const val NO_SHIZUKU = "Shizuku belum siap. Jalankan Shizuku dan beri izin dulu."

    private fun ready() = Shell.state() == ShizukuState.READY

    private fun run(cmd: String, okMsg: String, failMsg: String): TweakResult {
        if (!ready()) return TweakResult(false, NO_SHIZUKU)
        val (code, out) = Shell.run(cmd)
        return if (code == 0) TweakResult(true, okMsg) else TweakResult(false, "$failMsg: $out")
    }

    fun apply(cfg: Cfg, p: Profile): TweakResult {
        if (!ready()) return TweakResult(false, NO_SHIZUKU)

        // Semua perintah digabung jadi satu panggilan supaya cepat.
        val sb = StringBuilder()
        fun add(label: String, cmd: String) {
            sb.append("(").append(cmd).append(") >/dev/null 2>&1 || echo FAIL:").append(label).append("; ")
        }
        add("Animasi", "settings put global window_animation_scale ${p.anim}")
        add("Animasi", "settings put global transition_animation_scale ${p.anim}")
        add("Animasi", "settings put global animator_duration_scale ${p.anim}")
        if (p.refresh > 0) {
            add("Refresh rate", "settings put system min_refresh_rate ${p.refresh}")
            add("Refresh rate", "settings put system peak_refresh_rate ${p.refresh}")
        } else {
            add("Refresh rate", "settings delete system min_refresh_rate")
            add("Refresh rate", "settings delete system peak_refresh_rate")
        }
        add("Mode hemat daya", "settings put global low_power ${if (p.saver) 1 else 0}")

        val (code, out) = Shell.run(sb.toString())
        if (code == -1) return TweakResult(false, "Gagal menerapkan profil ${p.label}: $out")
        val failed = out.lines()
            .filter { it.startsWith("FAIL:") }
            .map { it.removePrefix("FAIL:").trim() }
            .distinct()
        return when {
            failed.isEmpty() -> {
                cfg.applied = p.id
                TweakResult(true, "Profil ${p.label} aktif")
            }
            failed.size >= 3 -> TweakResult(false, "Gagal menerapkan profil ${p.label}")
            else -> {
                cfg.applied = p.id
                TweakResult(
                    true,
                    "Profil ${p.label} aktif, tapi gagal di: ${failed.joinToString(", ")}. Mungkin tidak didukung HP ini."
                )
            }
        }
    }

    fun optimize(pkg: String): TweakResult =
        run("cmd package compile -m speed -f $pkg", "Aplikasi selesai dioptimalkan", "Gagal mengoptimalkan")

    fun forceStop(pkg: String): TweakResult =
        run("am force-stop $pkg", "Aplikasi dihentikan", "Gagal menghentikan")

    fun sleep(pkg: String): TweakResult = run(
        "am force-stop $pkg; cmd appops set $pkg RUN_IN_BACKGROUND ignore; " +
            "cmd appops set $pkg RUN_ANY_IN_BACKGROUND ignore; am set-standby-bucket $pkg restricted",
        "Aplikasi ditidurkan",
        "Gagal menidurkan"
    )

    fun wake(pkg: String): TweakResult = run(
        "cmd appops set $pkg RUN_IN_BACKGROUND allow; cmd appops set $pkg RUN_ANY_IN_BACKGROUND allow; " +
            "am set-standby-bucket $pkg active",
        "Aplikasi dibangunkan",
        "Gagal membangunkan"
    )

    fun trimCaches(): TweakResult =
        run("pm trim-caches 999G", "Cache aplikasi dibersihkan", "Gagal membersihkan cache")

    fun dexoptAll(): TweakResult =
        run("cmd package bg-dexopt-job", "Optimasi aplikasi dijalankan di latar", "Gagal menjalankan optimasi")

    fun lockScreen(): TweakResult =
        run("input keyevent 223", "Layar dikunci", "Gagal mengunci layar")

    fun expandNotif(): TweakResult =
        run("cmd statusbar expand-notifications", "Panel dibuka", "Gagal membuka panel notifikasi")

    fun readDisplay(): DisplayInfo {
        if (!ready()) return DisplayInfo()
        val out = Shell.run("wm size; wm density").second
        fun pick(re: String): String? = Regex(re).find(out)?.groupValues?.get(1)
        return DisplayInfo(
            physSize = pick("Physical size: (\\d+x\\d+)") ?: "",
            overSize = pick("Override size: (\\d+x\\d+)") ?: "",
            physDpi = pick("Physical density: (\\d+)")?.toIntOrNull() ?: 0,
            overDpi = pick("Override density: (\\d+)")?.toIntOrNull() ?: 0
        )
    }
}
