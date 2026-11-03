package com.fauzi.tweaklauncher

import android.app.ActivityManager
import android.app.Application
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.util.Locale

enum class Screen { Home, Game, Drawer }

data class AppItem(
    val label: String,
    val pkg: String,
    val icon: ImageBitmap,
    val gameCat: Boolean
)

data class Stats(
    val ramText: String = "-",
    val ramPct: Float = 0f,
    val battery: Int = 0,
    val tempText: String = "-",
    val tempPct: Float = 0f,
    val storagePct: Float = 0f
)

class MainVm(private val app: Application) : AndroidViewModel(app) {

    private val prefs = Prefs(app)

    var screen by mutableStateOf(Screen.Home)
    var profileId by mutableStateOf(prefs.profile)
    var shizuku by mutableStateOf(ShizukuState.OFF)
    var apps by mutableStateOf<List<AppItem>>(emptyList())
    var games by mutableStateOf(prefs.games)
    var boostOff by mutableStateOf(prefs.boostOff)
    var pinned by mutableStateOf(prefs.pinned)
    var lastGame by mutableStateOf(prefs.lastGame)
    var stats by mutableStateOf(Stats())
    var busy by mutableStateOf(false)
    var menuApp by mutableStateOf<AppItem?>(null)

    private var restoreTo: String? = null
    private var loadedAt = 0L

    val homeApps: List<AppItem>
        get() {
            val byPkg = apps.associateBy { it.pkg }
            val p = pinned.mapNotNull { byPkg[it] }
            return if (p.isNotEmpty()) p else apps.filter { it.pkg !in games }.take(8)
        }

    val gameApps: List<AppItem>
        get() = apps.filter { it.pkg in games }

    private fun toast(msg: String) {
        Toast.makeText(app, msg, Toast.LENGTH_LONG).show()
    }

    // ---------- Shizuku ----------

    fun refreshShizuku() {
        shizuku = Shell.state()
    }

    fun requestPerm() {
        try {
            Shizuku.requestPermission(100)
        } catch (e: Throwable) {
            toast("Tidak bisa meminta izin. Pastikan Shizuku sedang berjalan.")
        }
    }

    fun openShizuku() {
        val pm = app.packageManager
        val i = pm.getLaunchIntentForPackage("moe.shizuku.privileged.api")
            ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/RikkaApps/Shizuku/releases"))
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        app.startActivity(i)
    }

    // ---------- Siklus hidup ----------

    fun onResumed() {
        refreshShizuku()
        val back = restoreTo
        if (back != null) {
            restoreTo = null
            setProfile(back)
        }
        if (System.currentTimeMillis() - loadedAt > 60_000) loadApps()
    }

    // ---------- Aplikasi ----------

    fun loadApps() {
        loadedAt = System.currentTimeMillis()
        viewModelScope.launch {
            val list = withContext(Dispatchers.Default) {
                val pm = app.packageManager
                val q = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                pm.queryIntentActivities(q, 0)
                    .filter { it.activityInfo.packageName != app.packageName }
                    .distinctBy { it.activityInfo.packageName }
                    .map { ri ->
                        AppItem(
                            label = ri.loadLabel(pm).toString(),
                            pkg = ri.activityInfo.packageName,
                            icon = ri.loadIcon(pm).toBitmap(108, 108).asImageBitmap(),
                            gameCat = ri.activityInfo.applicationInfo.category == ApplicationInfo.CATEGORY_GAME
                        )
                    }
                    .sortedBy { it.label.lowercase() }
            }
            apps = list
            if (!prefs.gamesInit) {
                games = games + list.filter { it.gameCat }.map { it.pkg }
                prefs.games = games
                prefs.gamesInit = true
            }
        }
    }

    fun launch(pkg: String, boost: Boolean = false) {
        viewModelScope.launch {
            if (pkg in games) {
                lastGame = pkg
                prefs.lastGame = pkg
            }
            if (boost && profileId != "game") {
                val r = withContext(Dispatchers.IO) { Tweaks.apply(PROFILES.first { it.id == "game" }) }
                if (r.ok) {
                    restoreTo = profileId
                    profileId = "game"
                    prefs.profile = "game"
                } else {
                    toast(r.message)
                }
            }
            val i = app.packageManager.getLaunchIntentForPackage(pkg)
            if (i != null) {
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                app.startActivity(i)
            }
        }
    }

    fun toggleGame(pkg: String) {
        games = if (pkg in games) games - pkg else games + pkg
        prefs.games = games
    }

    fun toggleBoost(pkg: String) {
        boostOff = if (pkg in boostOff) boostOff - pkg else boostOff + pkg
        prefs.boostOff = boostOff
    }

    fun togglePin(pkg: String) {
        pinned = if (pkg in pinned) pinned - pkg else pinned + pkg
        prefs.pinned = pinned
    }

    fun openInfo(pkg: String) {
        val i = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$pkg"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        app.startActivity(i)
    }

    fun optimize(pkg: String) {
        viewModelScope.launch {
            toast("Mengoptimalkan, mohon tunggu...")
            val r = withContext(Dispatchers.IO) { Tweaks.optimize(pkg) }
            toast(r.message)
        }
    }

    fun forceStop(pkg: String) {
        viewModelScope.launch {
            val r = withContext(Dispatchers.IO) { Tweaks.forceStop(pkg) }
            toast(r.message)
        }
    }

    // ---------- Profil ----------

    fun setProfile(id: String) {
        viewModelScope.launch {
            val p = PROFILES.first { it.id == id }
            busy = true
            val r = withContext(Dispatchers.IO) { Tweaks.apply(p) }
            busy = false
            if (r.ok) {
                profileId = id
                prefs.profile = id
            }
            toast(r.message)
        }
    }

    // ---------- Statistik ----------

    fun refreshStats() {
        val loc = Locale("id", "ID")
        val am = app.getSystemService(ActivityManager::class.java)
        val mi = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)
        val gb = 1024.0 * 1024.0 * 1024.0
        val used = mi.totalMem - mi.availMem
        val ramPct = if (mi.totalMem > 0) used.toFloat() / mi.totalMem.toFloat() else 0f

        val b = app.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = b?.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) ?: 0
        val scale = b?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val battery = if (scale > 0) level * 100 / scale else 0
        val temp = (b?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10f

        val st = StatFs(Environment.getDataDirectory().path)
        val total = st.totalBytes.toFloat()
        val storagePct = if (total > 0) 1f - st.availableBytes.toFloat() / total else 0f

        stats = Stats(
            ramText = String.format(loc, "%.1f GB", used / gb),
            ramPct = ramPct,
            battery = battery,
            tempText = String.format(loc, "%.0f°C", temp),
            tempPct = temp / 50f,
            storagePct = storagePct
        )
    }
}
