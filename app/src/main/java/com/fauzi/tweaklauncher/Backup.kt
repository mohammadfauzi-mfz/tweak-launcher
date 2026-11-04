package com.fauzi.tweaklauncher

import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/** Ekspor dan impor seluruh pengaturan sebagai satu berkas JSON. */
object Backup {
    private val skip = setOf("applied", "lastUpdateCheck", "askedNotif")

    fun export(sp: SharedPreferences): String {
        val root = JSONObject()
        for ((k, v) in sp.all) {
            if (k in skip) continue
            val e = JSONObject()
            when (v) {
                is Boolean -> e.put("t", "b").put("v", v)
                is Int -> e.put("t", "i").put("v", v)
                is Long -> e.put("t", "l").put("v", v)
                is Float -> e.put("t", "f").put("v", v.toDouble())
                is String -> e.put("t", "s").put("v", v)
                is Set<*> -> e.put("t", "set").put("v", JSONArray(v.map { it.toString() }))
                else -> continue
            }
            root.put(k, e)
        }
        return JSONObject().put("app", "TweakLauncher").put("version", 1).put("prefs", root).toString(2)
    }

    /** Mengembalikan jumlah pengaturan yang dipulihkan. */
    fun import(sp: SharedPreferences, text: String): Int {
        val root = JSONObject(text).getJSONObject("prefs")
        val ed = sp.edit()
        var n = 0
        for (k in root.keys()) {
            if (k in skip) continue
            val e = root.getJSONObject(k)
            when (e.getString("t")) {
                "b" -> ed.putBoolean(k, e.getBoolean("v"))
                "i" -> ed.putInt(k, e.getInt("v"))
                "l" -> ed.putLong(k, e.getLong("v"))
                "f" -> ed.putFloat(k, e.getDouble("v").toFloat())
                "s" -> ed.putString(k, e.getString("v"))
                "set" -> {
                    val a = e.getJSONArray("v")
                    ed.putStringSet(k, HashSet(List(a.length()) { i -> a.getString(i) }))
                }
                else -> continue
            }
            n++
        }
        ed.apply()
        return n
    }
}
