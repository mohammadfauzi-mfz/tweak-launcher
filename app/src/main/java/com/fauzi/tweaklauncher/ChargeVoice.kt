package com.fauzi.tweaklauncher

import android.content.Context
import android.media.AudioAttributes
import android.speech.tts.TextToSpeech
import java.util.Locale

/** Suara pemberitahuan (pengisian daya). Memakai TextToSpeech sistem. */
object ChargeVoice {
    private var tts: TextToSpeech? = null
    private var ready = false
    private var pending: Pair<String, String>? = null
    private var pendingNotif = true

    private fun locale(lang: String) = if (lang == "en") Locale.US else Locale("id", "ID")

    private fun route(t: TextToSpeech, notif: Boolean) {
        t.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(if (notif) AudioAttributes.USAGE_NOTIFICATION else AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
        )
    }

    fun speak(ctx: Context, text: String, lang: String, notif: Boolean = true) {
        if (text.isBlank()) return
        val t = tts
        if (t != null && ready) {
            t.setLanguage(locale(lang))
            route(t, notif)
            t.speak(text, TextToSpeech.QUEUE_FLUSH, null, "chg")
            return
        }
        pending = text to lang
        pendingNotif = notif
        if (t == null) {
            tts = TextToSpeech(ctx.applicationContext) { st ->
                ready = st == TextToSpeech.SUCCESS
                if (ready) pending?.let { (tx, lg) ->
                    pending = null
                    tts?.setLanguage(locale(lg))
                    tts?.let { route(it, pendingNotif) }
                    tts?.speak(tx, TextToSpeech.QUEUE_FLUSH, null, "chg")
                }
            }
        }
    }
}
