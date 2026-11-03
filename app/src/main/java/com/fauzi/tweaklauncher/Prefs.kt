package com.fauzi.tweaklauncher

import android.content.Context

class Prefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("tweak", Context.MODE_PRIVATE)

    var profile: String
        get() = sp.getString("profile", "normal") ?: "normal"
        set(v) {
            sp.edit().putString("profile", v).apply()
        }

    var lastGame: String
        get() = sp.getString("lastGame", "") ?: ""
        set(v) {
            sp.edit().putString("lastGame", v).apply()
        }

    var gamesInit: Boolean
        get() = sp.getBoolean("gamesInit", false)
        set(v) {
            sp.edit().putBoolean("gamesInit", v).apply()
        }

    var games: Set<String>
        get() = sp.getStringSet("games", emptySet())?.toSet() ?: emptySet()
        set(v) {
            sp.edit().putStringSet("games", v).apply()
        }

    /** Game yang boost otomatisnya dimatikan (default: semua game boost aktif). */
    var boostOff: Set<String>
        get() = sp.getStringSet("boostOff", emptySet())?.toSet() ?: emptySet()
        set(v) {
            sp.edit().putStringSet("boostOff", v).apply()
        }

    var pinned: List<String>
        get() = (sp.getString("pinned", "") ?: "").split("\n").filter { it.isNotBlank() }
        set(v) {
            sp.edit().putString("pinned", v.joinToString("\n")).apply()
        }
}
