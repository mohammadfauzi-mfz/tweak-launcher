package com.fauzi.tweaklauncher

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/** Suara pemberitahuan (pengisian daya). Memakai TextToSpeech sistem. */
object ChargeVoice {
    private var tts: TextToSpeech? = null
    private var ready = false
    private var pending: Pair<String, String>? = null

    private fun locale(lang: String) = if (lang == "en") Locale.US else Locale("id", "ID")

    fun speak(ctx: Context, text: String, lang: String) {
        if (text.isBlank()) return
        val t = tts
        if (t != null && ready) {
            t.setLanguage(locale(lang))
            t.speak(text, TextToSpeech.QUEUE_FLUSH, null, "chg")
            return
        }
        pending = text to lang
        if (t == null) {
            tts = TextToSpeech(ctx.applicationContext) { st ->
                ready = st == TextToSpeech.SUCCESS
                if (ready) pending?.let { (tx, lg) ->
                    pending = null
                    tts?.setLanguage(locale(lg))
                    tts?.speak(tx, TextToSpeech.QUEUE_FLUSH, null, "chg")
                }
            }
        }
    }
}
