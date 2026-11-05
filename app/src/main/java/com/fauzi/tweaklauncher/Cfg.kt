package com.fauzi.tweaklauncher

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import kotlin.reflect.KProperty

/** Satu pengaturan yang tersimpan di SharedPreferences dan bisa diamati Compose. */
class Setting<T : Any>(
    private val sp: SharedPreferences,
    private val key: String,
    private val def: T
) {
    private var state by mutableStateOf(load())

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T = state

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        state = value
        save(value)
    }

    fun reload() {
        state = load()
    }

    @Suppress("UNCHECKED_CAST")
    private fun load(): T = try {
        when (def) {
            is Boolean -> sp.getBoolean(key, def)
            is Int -> sp.getInt(key, def)
            is Float -> sp.getFloat(key, def)
            is String -> sp.getString(key, def) ?: def
            else -> def
        } as T
    } catch (e: Exception) {
        def
    }

    private fun save(v: T) {
        val e = sp.edit()
        when (v) {
            is Boolean -> e.putBoolean(key, v)
            is Int -> e.putInt(key, v)
            is Float -> e.putFloat(key, v)
            is String -> e.putString(key, v)
        }
        e.apply()
    }
}

/** Seluruh pengaturan launcher. Dipakai bersama oleh UI dan layanan latar. */
class Cfg(ctx: Context) {
    val sp: SharedPreferences = ctx.getSharedPreferences("tweak", Context.MODE_PRIVATE)
    private val all = mutableListOf<Setting<*>>()

    private fun <T : Any> s(key: String, def: T): Setting<T> = Setting(sp, key, def).also { all += it }

    fun reload() {
        all.forEach { it.reload() }
    }

    // ---- Tampilan ----
    var uiMode by s("uiMode", "modern")
    var pdaMono by s("pdaMono", true)
    var dosColor by s("dosColor", "green")
    var theme by s("theme", "amoled")
    var accent by s("accent", "#C6F432")
    var iconShape by s("iconShape", "squircle")
    var iconSize by s("iconSize", 56)
    var radius by s("radius", 28)
    var cardAlpha by s("cardAlpha", 100)
    var font by s("font", "sans")
    var background by s("bg", "solid")
    var dim by s("dim", 40)

    // ---- Beranda ----
    var clockStyle by s("clockStyle", "large")
    var clock24 by s("clock24", true)
    var showDate by s("showDate", true)
    var homeCols by s("homeCols", 4)
    var badgeOn by s("badgeOn", false)
    var badgeStyle by s("badgeStyle", "count")
    var badgeColor by s("badgeColor", "FF5252")
    var badgeOffCsv by s("badgeOff", "")
    var showLabels by s("showLabels", true)
    var labelSize by s("labelSize", 12)
    var homeSections by s("homeSections", "clock,profile,stats,apps,widgets")
    var dockOn by s("dockOn", true)

    // ---- Daftar aplikasi ----
    var drawerStyle by s("drawerStyle", "list")
    var drawerCols by s("drawerCols", 4)
    var showHidden by s("showHidden", false)

    // ---- Gestur ----
    var gSwipeUp by s("gSwipeUp", "drawer")
    var gSwipeDown by s("gSwipeDown", "notif")
    var gDouble by s("gDouble", "lock")
    var gLong by s("gLong", "settings")

    // ---- Otomasi dan overlay ----
    var autoApp by s("autoApp", false)
    var autoSched by s("autoSched", false)
    var schedStart by s("schedStart", 22 * 60)
    var schedEnd by s("schedEnd", 6 * 60)
    var schedProfile by s("schedProfile", "hemat")
    var overlayOn by s("overlayOn", false)
    var overlayPos by s("overlayPos", "topright")
    var overlayAlpha by s("overlayAlpha", 80)

    // ---- Pembaruan ----
    var autoUpdate by s("autoUpdate", true)
    var lastUpdateCheck by s("lastUpdateCheck", "0")
    var askedNotif by s("askedNotif", false)

    // ---- Data (daftar disimpan sebagai teks) ----
    var baseProfile by s("profile", "normal")
    private val appliedS = s("applied", "normal")
    var applied by appliedS
    var lastGame by s("lastGame", "")
    var gamesInit by s("gamesInit", false)
    var gamesCsv by s("gamesL", "")
    var boostOffCsv by s("boostOffL", "")
    var pinnedCsv by s("pinned", "")
    var dockCsv by s("dockL", "")
    var hiddenCsv by s("hiddenL", "")
    var sleepingCsv by s("sleepingL", "")
    var widgetsCsv by s("widgetsL", "")
    var foldersJson by s("foldersJ", "{}")
    var appProfilesJson by s("appProfilesJ", "{}")
    var profilesJson by s("profilesJ", "")

    /** Dipanggil VM untuk menyamakan profil yang sedang aktif jika diubah oleh layanan latar. */
    fun syncApplied() {
        appliedS.reload()
    }

    /** Profil yang harus dipakai untuk aplikasi ini, atau null jika tidak ada aturan khusus. */
    fun ruleFor(pkg: String): String? {
        Store.map(appProfilesJson)[pkg]?.let { return it }
        if (pkg in Sys.list(gamesCsv) && pkg !in Sys.list(boostOffCsv)) return "game"
        return null
    }
}

/** Pembantu baca-tulis JSON sederhana. */
object Store {
    fun map(json: String): LinkedHashMap<String, String> {
        val m = LinkedHashMap<String, String>()
        try {
            val o = JSONObject(if (json.isBlank()) "{}" else json)
            for (k in o.keys()) m[k] = o.getString(k)
        } catch (e: Exception) {
        }
        return m
    }

    fun mapJson(m: Map<String, String>): String {
        val o = JSONObject()
        m.forEach { (k, v) -> o.put(k, v) }
        return o.toString()
    }

    fun folders(json: String): LinkedHashMap<String, List<String>> {
        val m = LinkedHashMap<String, List<String>>()
        try {
            val o = JSONObject(if (json.isBlank()) "{}" else json)
            for (k in o.keys()) {
                val a = o.getJSONArray(k)
                m[k] = List(a.length()) { i -> a.getString(i) }
            }
        } catch (e: Exception) {
        }
        return m
    }

    fun foldersJson(m: Map<String, List<String>>): String {
        val o = JSONObject()
        m.forEach { (k, v) -> o.put(k, JSONArray(v)) }
        return o.toString()
    }
}

/** Susunan bagian Beranda: urutan dan apakah aktif. Format teks: "clock,!stats,apps". */
object Layout {
    val NAMES = linkedMapOf(
        "clock" to "Jam dan tanggal",
        "profile" to "Kartu profil",
        "stats" to "Statistik",
        "apps" to "Aplikasi",
        "widgets" to "Widget"
    )

    fun parse(s: String): List<Pair<String, Boolean>> {
        val out = mutableListOf<Pair<String, Boolean>>()
        s.split(",").filter { it.isNotBlank() }.forEach { t ->
            val on = !t.startsWith("!")
            val k = t.removePrefix("!")
            if (k in NAMES && out.none { it.first == k }) out.add(Pair(k, on))
        }
        NAMES.keys.forEach { k ->
            if (out.none { it.first == k }) out.add(Pair(k, true))
        }
        return out
    }

    fun join(l: List<Pair<String, Boolean>>): String =
        l.joinToString(",") { (if (it.second) "" else "!") + it.first }
}

val GESTURE_ACTIONS = listOf(
    "none" to "Tidak ada",
    "drawer" to "Daftar aplikasi",
    "care" to "Perawatan",
    "notif" to "Panel notifikasi",
    "lock" to "Kunci layar",
    "settings" to "Setelan launcher",
    "cycle" to "Ganti profil berikutnya"
)
