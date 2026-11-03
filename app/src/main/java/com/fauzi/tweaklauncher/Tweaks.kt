package com.fauzi.tweaklauncher

data class Profile(
    val id: String,
    val label: String,
    val desc: String,
    val anim: String,
    val refresh: Int?,
    val saver: Boolean
)

val PROFILES = listOf(
    Profile("game", "Game", "Animasi 0.5x, refresh rate 120 Hz, mode hemat daya mati.", "0.5", 120, false),
    Profile("hemat", "Hemat", "Animasi mati, refresh rate 60 Hz, mode hemat daya aktif.", "0", 60, true),
    Profile("normal", "Normal", "Pengaturan bawaan sistem, tanpa pembatasan.", "1", null, false)
)

data class TweakResult(val ok: Boolean, val message: String)

object Tweaks {

    fun apply(p: Profile): TweakResult {
        if (Shell.state() != ShizukuState.READY) {
            return TweakResult(false, "Shizuku belum siap. Jalankan Shizuku dan beri izin dulu.")
        }

        val steps = mutableListOf<Pair<String, String>>()
        steps += "Animasi" to "settings put global window_animation_scale ${p.anim}"
        steps += "Animasi" to "settings put global transition_animation_scale ${p.anim}"
        steps += "Animasi" to "settings put global animator_duration_scale ${p.anim}"
        if (p.refresh != null) {
            steps += "Refresh rate" to "settings put system min_refresh_rate ${p.refresh}"
            steps += "Refresh rate" to "settings put system peak_refresh_rate ${p.refresh}"
        } else {
            steps += "Refresh rate" to "settings delete system min_refresh_rate"
            steps += "Refresh rate" to "settings delete system peak_refresh_rate"
        }
        steps += "Mode hemat daya" to "settings put global low_power ${if (p.saver) 1 else 0}"

        val failed = steps.filter { Shell.run(it.second).first != 0 }
        return when {
            failed.isEmpty() -> TweakResult(true, "Profil ${p.label} aktif")
            failed.size == steps.size -> TweakResult(false, "Gagal menerapkan profil ${p.label}")
            else -> {
                val names = failed.map { it.first }.distinct().joinToString(", ")
                TweakResult(true, "Profil ${p.label} aktif, tapi gagal di: $names. Mungkin tidak didukung HP ini.")
            }
        }
    }

    fun optimize(pkg: String): TweakResult {
        if (Shell.state() != ShizukuState.READY) {
            return TweakResult(false, "Shizuku belum siap.")
        }
        val (code, out) = Shell.run("cmd package compile -m speed -f $pkg")
        return if (code == 0) TweakResult(true, "Aplikasi selesai dioptimalkan")
        else TweakResult(false, "Gagal mengoptimalkan: $out")
    }

    fun forceStop(pkg: String): TweakResult {
        if (Shell.state() != ShizukuState.READY) {
            return TweakResult(false, "Shizuku belum siap.")
        }
        val (code, out) = Shell.run("am force-stop $pkg")
        return if (code == 0) TweakResult(true, "Aplikasi dihentikan")
        else TweakResult(false, "Gagal menghentikan: $out")
    }
}
