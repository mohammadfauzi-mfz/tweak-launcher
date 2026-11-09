package com.fauzi.tweaklauncher

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Asisten suara Jarvis: dengar (id-ID, utamakan offline), pahami perintah, jawab dengan suara. */
object Jarvis {
    /** idle, listen, think */
    var state by mutableStateOf("idle")
    var heard by mutableStateOf("")
    var reply by mutableStateOf("")

    private val id = Locale("id", "ID")
    private val scope = CoroutineScope(Dispatchers.Main)
    private var sr: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    private fun ensureTts(ctx: Context) {
        if (tts != null) return
        tts = TextToSpeech(ctx.applicationContext) { st ->
            if (st == TextToSpeech.SUCCESS) {
                val r = tts?.setLanguage(id)
                ttsReady = r != TextToSpeech.LANG_MISSING_DATA && r != TextToSpeech.LANG_NOT_SUPPORTED
            }
        }
    }

    fun say(ctx: Context, vm: MainVm, text: String) {
        reply = text
        if (!vm.cfg.jvSpeak) return
        ensureTts(ctx)
        if (ttsReady) tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jv")
    }

    fun stop() {
        try {
            sr?.cancel()
            sr?.destroy()
        } catch (e: Exception) {
        }
        sr = null
        state = "idle"
    }

    /** Mulai mendengar. Izin mikrofon harus sudah diberikan. */
    fun listen(ctx0: Context, vm: MainVm) {
        val ctx = ctx0.applicationContext
        if (state == "listen") {
            stop()
            return
        }
        tts?.stop()
        if (!SpeechRecognizer.isRecognitionAvailable(ctx)) {
            say(ctx, vm, "Layanan pengenal suara tidak tersedia di HP ini.")
            return
        }
        stop()
        heard = ""
        reply = ""
        val rec = try {
            if (Build.VERSION.SDK_INT >= 33 && vm.cfg.jvOffline && SpeechRecognizer.isOnDeviceRecognitionAvailable(ctx))
                SpeechRecognizer.createOnDeviceSpeechRecognizer(ctx)
            else SpeechRecognizer.createSpeechRecognizer(ctx)
        } catch (e: Exception) {
            null
        }
        if (rec == null) {
            say(ctx, vm, "Pengenal suara gagal dibuat.")
            return
        }
        sr = rec
        rec.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                state = "listen"
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                state = "think"
            }

            override fun onError(error: Int) {
                state = "idle"
                val m = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Saya tidak mendengar apa pun. Coba lagi."
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Izin mikrofon belum diberikan."
                    12, 13 -> "Bahasa Indonesia belum tersedia offline. Unduh paket suara Indonesia di pengaturan HP, atau matikan opsi offline."
                    SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Butuh internet untuk mengenali suara. Unduh paket offline Indonesia."
                    else -> "Pengenalan suara gagal (kode $error)."
                }
                say(ctx, vm, m)
            }

            override fun onResults(results: Bundle?) {
                val t = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                state = "idle"
                heard = t
                if (t.isBlank()) say(ctx, vm, "Saya tidak mendengar apa pun. Coba lagi.")
                else scope.launch { say(ctx, vm, JarvisBrain.handle(ctx, vm, t)) }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let { heard = it }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "id-ID")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "id-ID")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, vm.cfg.jvOffline)
        }
        state = "listen"
        try {
            rec.startListening(i)
        } catch (e: Exception) {
            state = "idle"
            say(ctx, vm, "Gagal mulai mendengar.")
        }
    }
}

/** Pemahaman perintah berbasis aturan (bahasa Indonesia). */
object JarvisBrain {
    private fun norm(s: String) = s.lowercase(Locale("id", "ID")).replace(Regex("[^a-z0-9 ]"), " ").replace(Regex("\\s+"), " ").trim()

    private fun has(t: String, vararg w: String) = w.any { t.contains(it) }

    suspend fun handle(ctx: Context, vm: MainVm, raw: String): String {
        val t = norm(raw)
        val st = vm.stats
        val loc = Locale("id", "ID")

        // ---- Tanya status ----
        if (has(t, "baterai", "batre", "baterei")) return "Baterai ${st.battery} persen."
        if (has(t, "suhu", "panas")) return "Suhu perangkat ${st.tempText}."
        if (has(t, "penyimpanan", "storage", "memori internal")) return "Penyimpanan terpakai ${(st.storagePct * 100).toInt()} persen."
        if (has(t, "ram", "memori")) return "RAM terpakai ${st.ramText}, sekitar ${(st.ramPct * 100).toInt()} persen."
        if (has(t, "jam berapa", "pukul", "waktu")) return "Sekarang pukul " + SimpleDateFormat("HH.mm", loc).format(Date()) + "."
        if (has(t, "tanggal", "hari apa", "hari ini")) return "Hari ini " + SimpleDateFormat("EEEE, d MMMM yyyy", loc).format(Date()) + "."
        if (has(t, "internet", "jaringan", "sinyal", "koneksi")) return netText(ctx)
        if (t == "status" || has(t, "status hp", "kondisi", "laporan")) {
            return hudStatus(st) + " Baterai ${st.battery} persen, RAM ${(st.ramPct * 100).toInt()} persen, suhu ${st.tempText}."
        }

        // ---- Aktifkan fitur ----
        if (has(t, "cache", "sampah")) {
            vm.runTool("Membersihkan cache") { Tweaks.trimCaches() }
            return "Membersihkan cache."
        }
        if (has(t, "optimasi", "optimalkan", "dexopt")) {
            vm.runTool("Mengoptimalkan semua aplikasi") { Tweaks.dexoptAll() }
            return "Mengoptimalkan aplikasi. Ini butuh beberapa menit."
        }
        if (has(t, "kunci", "matikan layar")) {
            vm.runGesture("lock")
            return "Mengunci layar."
        }
        if (has(t, "wifi", "wi fi")) {
            val on = !has(t, "mati", "matikan", "nonaktif")
            if (Shell.state() != ShizukuState.READY) {
                vm.runGesture("settings")
                return "Shizuku belum aktif, jadi saya tidak bisa mengubah Wi-Fi langsung."
            }
            withContext(Dispatchers.IO) { Shell.run("svc wifi " + if (on) "enable" else "disable") }
            return if (on) "Wi-Fi dinyalakan." else "Wi-Fi dimatikan."
        }
        if (has(t, "mode", "profil", "aktifkan")) {
            val p = vm.profiles.firstOrNull { t.contains(norm(it.label)) || t.contains(norm(it.id)) }
                ?: when {
                    has(t, "performa", "game", "kencang") -> vm.profiles.firstOrNull { it.id == "game" || it.id == "boost" }
                    has(t, "hemat") -> vm.profiles.firstOrNull { it.id == "hemat" }
                    has(t, "normal", "biasa") -> vm.profiles.firstOrNull { it.id == "normal" }
                    else -> null
                }
            if (p != null) {
                vm.setProfile(p.id)
                return "Mode ${p.label} diaktifkan."
            }
        }
        if (has(t, "perawatan", "rawat")) {
            vm.runGesture("care")
            return "Membuka perawatan."
        }
        if (has(t, "pengaturan", "setelan", "setting")) {
            vm.runGesture("settings")
            return "Membuka pengaturan."
        }
        if (has(t, "daftar aplikasi", "semua aplikasi")) {
            vm.runGesture("drawer")
            return "Membuka daftar aplikasi."
        }

        // ---- Buka aplikasi ----
        val m = Regex("^(?:tolong |coba )?(?:buka|bukakan|jalankan|nyalakan|open|launch|mulai)(?: aplikasi| app)? (.+)$").find(t)
        val q = (m?.groupValues?.get(1) ?: t).trim()
        val app = findApp(vm, q)
        if (app != null) {
            vm.launchApp(app.pkg)
            return "Membuka ${app.label}."
        }
        return if (m != null) "Aplikasi $q tidak ditemukan." else "Maaf, saya belum paham. Coba: buka WhatsApp, status baterai, atau mode hemat."
    }

    private fun findApp(vm: MainVm, q: String): AppItem? {
        if (q.isBlank()) return null
        val list = vm.visibleApps.map { it to norm(it.label) }.filter { it.second.isNotBlank() }
        list.firstOrNull { it.second == q }?.let { return it.first }
        list.filter { q.contains(it.second) || it.second.contains(q) }.maxByOrNull { it.second.length }?.let { return it.first }
        // Cocokkan kata pertama (mis. "youtube music" -> "YT Music" tidak cocok, tapi "kamera" cocok).
        val words = q.split(" ").filter { it.length > 2 }
        return list.filter { (_, l) -> words.any { w -> l.contains(w) } }
            .maxByOrNull { (_, l) -> words.count { l.contains(it) } }?.first
    }

    private fun netText(ctx: Context): String = try {
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork)
        when {
            caps == null -> "Tidak ada koneksi internet."
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Terhubung lewat Wi-Fi."
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Terhubung lewat data seluler."
            else -> "Terhubung ke jaringan."
        }
    } catch (e: Exception) {
        "Status jaringan tidak diketahui."
    }
}
