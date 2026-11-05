package com.fauzi.tweaklauncher

import org.json.JSONArray
import org.json.JSONObject

/** Profil performa. refresh = 0 berarti pakai bawaan sistem. */
data class Profile(
    val id: String,
    val label: String,
    val anim: String,
    val refresh: Int,
    val saver: Boolean
) {
    fun desc(): String {
        val a = if (anim == "0") "mati" else "${anim}x"
        val r = if (refresh == 0) "bawaan" else "$refresh Hz"
        val s = if (saver) "aktif" else "mati"
        return "Animasi $a, refresh rate $r, hemat daya $s."
    }
}

object ProfileStore {
    val DEFAULT = listOf(
        Profile("game", "Boost", "0.5", 120, false),
        Profile("hemat", "Hemat", "0", 60, true),
        Profile("normal", "Normal", "1", 0, false)
    )

    fun parse(json: String): List<Profile> {
        if (json.isBlank()) return DEFAULT
        return try {
            val a = JSONArray(json)
            val l = List(a.length()) { i ->
                val o = a.getJSONObject(i)
                val id = o.getString("id")
                val label = o.getString("label")
                Profile(
                    id,
                    // Nama lama "Game" diganti "Boost" untuk profil bawaan.
                    if (id == "game" && label == "Game") "Boost" else label,
                    o.getString("anim"),
                    o.getInt("refresh"),
                    o.getBoolean("saver")
                )
            }
            if (l.isEmpty()) DEFAULT else l
        } catch (e: Exception) {
            DEFAULT
        }
    }

    fun toJson(l: List<Profile>): String {
        val a = JSONArray()
        l.forEach { p ->
            a.put(
                JSONObject()
                    .put("id", p.id)
                    .put("label", p.label)
                    .put("anim", p.anim)
                    .put("refresh", p.refresh)
                    .put("saver", p.saver)
            )
        }
        return a.toString()
    }
}
