package com.fauzi.tweaklauncher

import android.app.Application
import android.app.usage.UsageStatsManager
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.io.File

enum class Screen { Home, Care, Drawer, Settings }

data class AppItem(
    val label: String,
    val pkg: String,
    val icon: ImageBitmap,
    val gameCat: Boolean,
    val system: Boolean
)

sealed class HomeEntry {
    data class A(val app: AppItem) : HomeEntry()
    data class F(val name: String, val apps: List<AppItem>) : HomeEntry()
}

data class Revert(val label: String, val cmd: String)

class MainVm(private val app: Application) : AndroidViewModel(app) {

    val cfg = Cfg(app)

    var screen by mutableStateOf(Screen.Home)
    var pdaStart by mutableStateOf(false)
    var iconPick by mutableStateOf<IconTarget?>(null)
    var clockOpen by mutableStateOf(false)
    var wpEdit by mutableStateOf<String?>(null)
    var fwOpen by mutableStateOf(false)
    var adsMsg by mutableStateOf("")
    var dnsBusy by mutableStateOf(false)
    var dnsRes by mutableStateOf<List<DnsResult>>(emptyList())
    var adsRev by mutableIntStateOf(0)
    var fwMsg by mutableStateOf("")
    var trayPop by mutableStateOf("")
    var deskMove by mutableStateOf<String?>(null)
    var packMsg by mutableStateOf("")
    var packRev by mutableIntStateOf(0)

    // Grup dan tab daftar aplikasi (dipakai semua mode tampilan).
    var tabSel by mutableStateOf("all")
    var tabMenu by mutableStateOf<String?>(null)
    var groupNewOpen by mutableStateOf(false)
    var groupEditName by mutableStateOf<String?>(null)
    var groupPickApp by mutableStateOf<AppItem?>(null)

    // Mode DOS: keluaran terminal dan daftar terakhir yang ditampilkan (untuk perintah nomor).
    val dosLines = mutableStateListOf<DosLine>()
    var dosListed: List<AppItem> = emptyList()

    fun dosPrint(text: String, pkg: String? = null) {
        dosLines.add(DosLine(text, pkg))
        while (dosLines.size > 600) dosLines.removeAt(0)
    }

    fun dosShell(cmd: String) {
        if (shizuku != ShizukuState.READY) {
            dosPrint("Shizuku belum aktif.")
            return
        }
        viewModelScope.launch {
            val r = withContext(Dispatchers.IO) { Shell.run(cmd) }
            val out = r.second.trimEnd()
            if (out.isNotEmpty()) out.lines().take(300).forEach { dosPrint(it) }
            if (r.first != 0) dosPrint("(kode keluar ${r.first})")
        }
    }

    // Mode Windows 95: jendela yang terbuka (tombol di taskbar) dan menu Start.
    val w95Windows = mutableStateListOf<Screen>()
    var w95Start by mutableStateOf(false)

    fun w95Open(s: Screen) {
        if (s != Screen.Home && s !in w95Windows) w95Windows.add(s)
        screen = s
        w95Start = false
    }

    fun w95Close(s: Screen) {
        w95Windows.remove(s)
        screen = Screen.Home
    }
    var settingsPage by mutableStateOf("")
    var shizuku by mutableStateOf(ShizukuState.OFF)
    var apps by mutableStateOf<List<AppItem>>(emptyList())
    var stats by mutableStateOf(Stats())
    var busy by mutableStateOf(false)

    // Dialog
    var menuApp by mutableStateOf<AppItem?>(null)
    var folderPickApp by mutableStateOf<AppItem?>(null)
    var profilePickApp by mutableStateOf<AppItem?>(null)
    var openFolder by mutableStateOf<String?>(null)
    var showWidgetPicker by mutableStateOf(false)
    var showUpdate by mutableStateOf(false)

    // Pembaruan
    var update by mutableStateOf<UpdateInfo?>(null)
    var updateStatus by mutableStateOf("")
    var checking by mutableStateOf(false)
    var downloading by mutableStateOf(false)
    var progress by mutableFloatStateOf(0f)
    var downloaded by mutableStateOf<File?>(null)
    private var dismissedTag = ""

    // Layar (DPI dan resolusi)
    var display by mutableStateOf(DisplayInfo())
    var revert by mutableStateOf<Revert?>(null)
    var revertLeft by mutableIntStateOf(0)
    private var revertJob: Job? = null

    // Alat
    var unused by mutableStateOf<List<AppItem>>(emptyList())

    // Favorit dan terakhir dipakai (dari statistik penggunaan)
    var recentApps by mutableStateOf<List<AppItem>>(emptyList())
    var oftenApps by mutableStateOf<List<AppItem>>(emptyList())
    var usageOk by mutableStateOf(false)

    var pendingWidget = -1
    private var autoRestore = false
    private var loadedAt = 0L

    // ---------------- Data turunan ----------------

    val profiles: List<Profile> get() = ProfileStore.parse(cfg.profilesJson)

    val baseProfile: Profile
        get() = profiles.firstOrNull { it.id == cfg.baseProfile } ?: profiles.first()

    val profile: Profile
        get() = profiles.firstOrNull { it.id == cfg.applied } ?: baseProfile

    val games: Set<String> get() = Sys.list(cfg.gamesCsv).toSet()
    val boostOff: Set<String> get() = Sys.list(cfg.boostOffCsv).toSet()
    val hidden: Set<String> get() = Sys.list(cfg.hiddenCsv).toSet()
    val sleeping: Set<String> get() = Sys.list(cfg.sleepingCsv).toSet()
    val dockPkgs: List<String> get() = Sys.list(cfg.dockCsv)
    val widgetIds: List<Int> get() = cfg.widgetsCsv.split(",").mapNotNull { it.toIntOrNull() }

    val visibleApps: List<AppItem>
        get() {
            val h = hidden
            return if (cfg.showHidden) apps else apps.filter { it.pkg !in h }
        }

    val gameApps: List<AppItem> get() = apps.filter { it.pkg in games }

    val dockApps: List<AppItem>
        get() {
            val byPkg = apps.associateBy { it.pkg }
            return dockPkgs.mapNotNull { byPkg[it] }
        }

    val homeEntries: List<HomeEntry>
        get() {
            val byPkg = apps.associateBy { it.pkg }
            val fs = Store.folders(cfg.foldersJson)
            val out = mutableListOf<HomeEntry>()
            Sys.list(cfg.pinnedCsv).forEach { raw ->
                if (raw.startsWith("folder:")) {
                    val n = raw.removePrefix("folder:")
                    val members = fs[n]
                    if (members != null) out.add(HomeEntry.F(n, members.mapNotNull { byPkg[it] }))
                } else {
                    byPkg[raw.removePrefix("app:")]?.let { out.add(HomeEntry.A(it)) }
                }
            }
            if (out.isEmpty()) {
                val g = games
                apps.filter { it.pkg !in g }.take(8).forEach { out.add(HomeEntry.A(it)) }
            }
            return out
        }

    private fun toast(msg: String) {
        Toast.makeText(app, msg, Toast.LENGTH_LONG).show()
    }

    private fun startSettings(i: Intent) {
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        app.startActivity(i)
    }

    // ---------------- Shizuku ----------------

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
        val i = app.packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
            ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/RikkaApps/Shizuku/releases"))
        startSettings(i)
    }

    // ---------------- Siklus hidup ----------------

    fun onResumed() {
        refreshShizuku()
        refreshNotifAccess()
        if (autoRestore) {
            autoRestore = false
            val base = baseProfile
            if (base.id != cfg.applied) {
                viewModelScope.launch { withContext(Dispatchers.IO) { Tweaks.apply(cfg, base) } }
            }
        }
        if (System.currentTimeMillis() - loadedAt > 60_000L) loadApps()
        syncService()
        maybeAutoCheckUpdate()
    }

    // ---------------- Aplikasi ----------------

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
                        val ai = ri.activityInfo.applicationInfo
                        AppItem(
                            label = ri.loadLabel(pm).toString(),
                            pkg = ri.activityInfo.packageName,
                            icon = Sys.renderIcon(ri.loadIcon(pm)).asImageBitmap(),
                            gameCat = ai.category == ApplicationInfo.CATEGORY_GAME,
                            system = (ai.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                        )
                    }
                    .sortedBy { it.label.lowercase() }
            }
            apps = list
            if (!cfg.gamesInit) {
                val merged = games + list.filter { it.gameCat }.map { it.pkg }
                cfg.gamesCsv = Sys.join(merged)
                cfg.gamesInit = true
            }
        }
    }

    fun launchApp(pkg: String) {
        viewModelScope.launch {
            if (pkg in games) cfg.lastGame = pkg
            if (pkg in sleeping) {
                withContext(Dispatchers.IO) { Tweaks.wake(pkg) }
                cfg.sleepingCsv = Sys.join(sleeping - pkg)
            }
            val target = cfg.ruleFor(pkg)
            if (target != null && target != cfg.applied) {
                val p = profiles.firstOrNull { it.id == target }
                if (p != null) {
                    val r = withContext(Dispatchers.IO) { Tweaks.apply(cfg, p) }
                    if (r.ok) autoRestore = true else toast(r.message)
                }
            }
            val i = app.packageManager.getLaunchIntentForPackage(pkg)
            if (i != null) startSettings(i)
        }
    }

    fun toggleGame(pkg: String) {
        cfg.gamesCsv = Sys.join(if (pkg in games) games - pkg else games + pkg)
    }

    fun toggleBoost(pkg: String) {
        cfg.boostOffCsv = Sys.join(if (pkg in boostOff) boostOff - pkg else boostOff + pkg)
    }

    var notifAccess by mutableStateOf(false)

    fun refreshNotifAccess() {
        val l = Settings.Secure.getString(app.contentResolver, "enabled_notification_listeners") ?: ""
        notifAccess = l.contains(app.packageName)
    }

    fun openNotifAccess() {
        startSettings(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }

    fun toggleBadge(pkg: String) {
        val l = Sys.list(cfg.badgeOffCsv)
        cfg.badgeOffCsv = Sys.join(if (pkg in l) l - pkg else l + pkg)
    }

    fun toggleHidden(pkg: String) {
        cfg.hiddenCsv = Sys.join(if (pkg in hidden) hidden - pkg else hidden + pkg)
    }

    fun isPinned(pkg: String): Boolean {
        val l = Sys.list(cfg.pinnedCsv)
        return "app:$pkg" in l || pkg in l
    }

    fun togglePin(pkg: String) {
        val l = Sys.list(cfg.pinnedCsv).filter { it != "app:$pkg" && it != pkg }
        cfg.pinnedCsv = if (isPinned(pkg)) Sys.join(l) else Sys.join(l + "app:$pkg")
    }

    fun toggleDock(pkg: String) {
        val l = dockPkgs
        if (pkg in l) {
            cfg.dockCsv = Sys.join(l - pkg)
        } else if (l.size >= 5) {
            toast("Dock penuh, maksimal 5 aplikasi.")
        } else {
            cfg.dockCsv = Sys.join(l + pkg)
        }
    }

    fun openInfo(pkg: String) {
        startSettings(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$pkg")))
    }

    fun uninstall(pkg: String) {
        startSettings(Intent(Intent.ACTION_DELETE, Uri.parse("package:$pkg")))
    }

    fun optimize(pkg: String) = runTool("Mengoptimalkan, mohon tunggu") { Tweaks.optimize(pkg) }

    fun forceStop(pkg: String) = runTool(null) { Tweaks.forceStop(pkg) }

    fun runTool(startMsg: String?, block: () -> TweakResult) {
        viewModelScope.launch {
            if (startMsg != null) toast("$startMsg...")
            val r = withContext(Dispatchers.IO) { block() }
            toast(r.message)
        }
    }

    // ---------------- Tidurkan aplikasi ----------------

    fun sleepApp(pkg: String) {
        viewModelScope.launch {
            val r = withContext(Dispatchers.IO) { Tweaks.sleep(pkg) }
            if (r.ok) cfg.sleepingCsv = Sys.join(sleeping + pkg)
            toast(r.message)
        }
    }

    fun wakeApp(pkg: String) {
        viewModelScope.launch {
            val r = withContext(Dispatchers.IO) { Tweaks.wake(pkg) }
            if (r.ok) cfg.sleepingCsv = Sys.join(sleeping - pkg)
            toast(r.message)
        }
    }

    fun sleepAll(list: List<AppItem>) {
        viewModelScope.launch {
            val done = withContext(Dispatchers.IO) { list.filter { Tweaks.sleep(it.pkg).ok } }
            cfg.sleepingCsv = Sys.join(sleeping + done.map { it.pkg })
            unused = emptyList()
            toast("${done.size} dari ${list.size} aplikasi ditidurkan")
        }
    }

    fun wakeAll() {
        viewModelScope.launch {
            val l = sleeping.toList()
            withContext(Dispatchers.IO) { l.forEach { Tweaks.wake(it) } }
            cfg.sleepingCsv = ""
            toast("${l.size} aplikasi dibangunkan")
        }
    }

    fun loadUnused(days: Int = 14) {
        if (!Sys.hasUsageAccess(app)) {
            toast("Beri izin Akses penggunaan dulu.")
            startSettings(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            return
        }
        viewModelScope.launch {
            val sl = sleeping
            val list = withContext(Dispatchers.Default) {
                val usm = app.getSystemService(UsageStatsManager::class.java)
                val now = System.currentTimeMillis()
                val st = usm.queryUsageStats(UsageStatsManager.INTERVAL_BEST, now - 60L * 86_400_000L, now)
                val last = HashMap<String, Long>()
                st.forEach { s -> last[s.packageName] = maxOf(last[s.packageName] ?: 0L, s.lastTimeUsed) }
                val cutoff = now - days * 86_400_000L
                apps.filter { a ->
                    !a.system && a.pkg !in sl && a.pkg != "moe.shizuku.privileged.api" &&
                        (last[a.pkg] ?: 0L) < cutoff
                }
            }
            unused = list
            if (list.isEmpty()) toast("Tidak ada aplikasi yang jarang dipakai")
        }
    }

    fun openUsageAccess() {
        startSettings(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
    }

    /** Memuat aplikasi yang terakhir dan paling sering dipakai (7 hari terakhir). */
    fun loadUsage() {
        usageOk = Sys.hasUsageAccess(app)
        if (!usageOk) {
            recentApps = emptyList()
            oftenApps = emptyList()
            return
        }
        viewModelScope.launch {
            val all = apps
            val hid = hidden
            val (recent, often) = withContext(Dispatchers.Default) {
                val usm = app.getSystemService(UsageStatsManager::class.java)
                val now = System.currentTimeMillis()
                val st = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, now - 7L * 86_400_000L, now)
                val last = HashMap<String, Long>()
                val total = HashMap<String, Long>()
                st.forEach { s ->
                    last[s.packageName] = maxOf(last[s.packageName] ?: 0L, s.lastTimeUsed)
                    total[s.packageName] = (total[s.packageName] ?: 0L) + s.totalTimeInForeground
                }
                val usable = all.filter { it.pkg !in hid }
                val r = usable.filter { (last[it.pkg] ?: 0L) > 0L }
                    .sortedByDescending { last[it.pkg] ?: 0L }
                    .take(8)
                val o = usable.filter { (total[it.pkg] ?: 0L) > 60_000L }
                    .sortedByDescending { total[it.pkg] ?: 0L }
                    .take(8)
                Pair(r, o)
            }
            recentApps = recent
            oftenApps = often
        }
    }

    // ---------------- Folder ----------------

    val folders: Map<String, List<String>> get() = Store.folders(cfg.foldersJson)

    private fun saveFolders(m: Map<String, List<String>>) {
        cfg.foldersJson = Store.foldersJson(m)
    }

    fun createFolder(name: String): Boolean {
        val n = name.trim()
        if (n.isEmpty() || n.contains(":")) return false
        val m = LinkedHashMap(folders)
        if (m.containsKey(n)) return false
        m[n] = emptyList()
        saveFolders(m)
        cfg.pinnedCsv = Sys.join(Sys.list(cfg.pinnedCsv) + "folder:$n")
        return true
    }

    fun addToFolder(folder: String, pkg: String) {
        val m = LinkedHashMap(folders)
        val cur = m[folder] ?: return
        if (pkg !in cur) m[folder] = cur + pkg
        saveFolders(m)
        // Aplikasi yang sudah di folder tidak perlu muncul lagi di grid Beranda.
        cfg.pinnedCsv = Sys.join(Sys.list(cfg.pinnedCsv).filter { it != "app:$pkg" && it != pkg })
    }

    fun removeFromFolder(folder: String, pkg: String) {
        val m = LinkedHashMap(folders)
        val cur = m[folder] ?: return
        m[folder] = cur - pkg
        saveFolders(m)
    }

    fun deleteFolder(name: String) {
        val m = LinkedHashMap(folders)
        m.remove(name)
        saveFolders(m)
        cfg.pinnedCsv = Sys.join(Sys.list(cfg.pinnedCsv).filter { it != "folder:$name" })
        openFolder = null
    }

    // ---------------- Profil performa ----------------

    fun setProfile(id: String) {
        viewModelScope.launch {
            val p = profiles.firstOrNull { it.id == id } ?: return@launch
            busy = true
            val r = withContext(Dispatchers.IO) { Tweaks.apply(cfg, p) }
            busy = false
            if (r.ok) cfg.baseProfile = id
            toast(r.message)
        }
    }

    fun cycleProfile() {
        val l = profiles
        val i = l.indexOfFirst { it.id == cfg.baseProfile }
        setProfile(l[(i + 1).mod(l.size)].id)
    }

    fun saveProfiles(l: List<Profile>) {
        cfg.profilesJson = ProfileStore.toJson(l)
    }

    fun setAppProfile(pkg: String, profileId: String?) {
        val m = Store.map(cfg.appProfilesJson)
        if (profileId == null) m.remove(pkg) else m[pkg] = profileId
        cfg.appProfilesJson = Store.mapJson(m)
    }

    // ---------------- Layanan latar ----------------

    fun syncService() {
        if (cfg.overlayOn || cfg.autoApp || cfg.autoSched) {
            try {
                ContextCompat.startForegroundService(app, Intent(app, TweakService::class.java))
            } catch (e: Exception) {
                toast("Gagal memulai layanan: ${e.message}")
            }
        } else {
            app.stopService(Intent(app, TweakService::class.java))
        }
    }

    fun setOverlay(on: Boolean) {
        if (on && !Settings.canDrawOverlays(app)) {
            toast("Izinkan 'Tampil di atas aplikasi lain', lalu aktifkan lagi.")
            startSettings(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${app.packageName}")))
            return
        }
        cfg.overlayOn = on
        syncService()
    }

    fun setAutoApp(on: Boolean) {
        if (on && !Sys.hasUsageAccess(app)) {
            toast("Izinkan 'Akses penggunaan' untuk Tweak Launcher, lalu aktifkan lagi.")
            startSettings(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            return
        }
        cfg.autoApp = on
        syncService()
    }

    fun setAutoSched(on: Boolean) {
        cfg.autoSched = on
        syncService()
    }

    // ---------------- Gestur ----------------

    fun runGesture(action: String) {
        when (action) {
            "drawer" -> screen = Screen.Drawer
            "care", "game" -> screen = Screen.Care
            "settings" -> {
                settingsPage = ""
                screen = Screen.Settings
            }
            "cycle" -> cycleProfile()
            "notif" -> runTool(null) { Tweaks.expandNotif() }
            "lock" -> runTool(null) { Tweaks.lockScreen() }
        }
    }

    // ---------------- Statistik ----------------

    fun refreshStats() {
        cfg.syncApplied()
        stats = Sys.stats(app)
    }

    // ---------------- Widget ----------------

    fun addWidget(id: Int) {
        val l = widgetIds
        if (id !in l) cfg.widgetsCsv = (l + id).joinToString(",")
    }

    fun removeWidget(id: Int) {
        cfg.widgetsCsv = widgetIds.filter { it != id }.joinToString(",")
        try {
            WidgetHost.host.deleteAppWidgetId(id)
        } catch (e: Exception) {
        }
    }

    fun dropWidgetId(id: Int) {
        try {
            WidgetHost.host.deleteAppWidgetId(id)
        } catch (e: Exception) {
        }
    }

    // ---------------- Pembaruan ----------------

    fun maybeAutoCheckUpdate() {
        if (!cfg.autoUpdate || checking) return
        val last = cfg.lastUpdateCheck.toLongOrNull() ?: 0L
        if (System.currentTimeMillis() - last > 12L * 3_600_000L) checkUpdate(false)
    }

    fun checkUpdate(manual: Boolean) {
        if (checking) return
        viewModelScope.launch {
            checking = true
            if (manual) updateStatus = "Memeriksa..."
            try {
                val info = withContext(Dispatchers.IO) { Updater.check() }
                cfg.lastUpdateCheck = System.currentTimeMillis().toString()
                if (info.code > BuildConfig.VERSION_CODE) {
                    update = info
                    updateStatus = "Versi baru tersedia: ${info.tag}"
                    if (manual || info.tag != dismissedTag) showUpdate = true
                } else {
                    update = null
                    updateStatus = "Kamu sudah memakai versi terbaru (${BuildConfig.VERSION_NAME})"
                }
            } catch (e: Exception) {
                if (manual) updateStatus = "Gagal memeriksa: ${e.message}"
            } finally {
                checking = false
            }
        }
    }

    fun dismissUpdate() {
        dismissedTag = update?.tag ?: ""
        showUpdate = false
    }

    fun downloadUpdate() {
        val u = update ?: return
        if (downloading) return
        viewModelScope.launch {
            downloading = true
            progress = 0f
            try {
                val f = withContext(Dispatchers.IO) { Updater.download(app, u.url) { progress = it } }
                downloaded = f
                installUpdate()
            } catch (e: Exception) {
                toast("Gagal mengunduh: ${e.message}")
            } finally {
                downloading = false
            }
        }
    }

    fun installUpdate() {
        val f = downloaded ?: return
        if (!Updater.install(app, f)) {
            toast("Izinkan Tweak Launcher memasang aplikasi, lalu tekan Pasang lagi.")
        }
    }

    // ---------------- Layar: DPI dan resolusi ----------------

    fun refreshDisplay() {
        viewModelScope.launch { display = withContext(Dispatchers.IO) { Tweaks.readDisplay() } }
    }

    private fun applyDisplay(applyCmd: String, revertCmd: String, label: String) {
        viewModelScope.launch {
            val (code, out) = withContext(Dispatchers.IO) {
                if (Shell.state() == ShizukuState.READY) Shell.run(applyCmd) else Pair(-1, "Shizuku belum siap")
            }
            if (code != 0) {
                toast("Gagal: $out")
                return@launch
            }
            display = withContext(Dispatchers.IO) { Tweaks.readDisplay() }
            revert = Revert(label, revertCmd)
            revertLeft = 15
            revertJob?.cancel()
            revertJob = launch {
                while (revertLeft > 0) {
                    delay(1000)
                    revertLeft -= 1
                }
                revertNow()
            }
        }
    }

    private suspend fun revertNow() {
        val r = revert ?: return
        revert = null
        withContext(Dispatchers.IO) { Shell.run(r.cmd) }
        display = withContext(Dispatchers.IO) { Tweaks.readDisplay() }
    }

    fun keepDisplay() {
        revertJob?.cancel()
        revert = null
    }

    fun revertDisplay() {
        revertJob?.cancel()
        viewModelScope.launch { revertNow() }
    }

    fun setDensity(value: Int) {
        val old = display.overDpi
        applyDisplay(
            "wm density $value",
            if (old > 0) "wm density $old" else "wm density reset",
            "DPI $value"
        )
    }

    fun resetDensity() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { Shell.run("wm density reset") }
            display = withContext(Dispatchers.IO) { Tweaks.readDisplay() }
        }
    }

    fun setScale(percent: Int) {
        val parts = display.physSize.split("x")
        val w = parts.getOrNull(0)?.toIntOrNull() ?: return
        val h = parts.getOrNull(1)?.toIntOrNull() ?: return
        val nw = (w * percent / 100) / 2 * 2
        val nh = (h * percent / 100) / 2 * 2
        val old = display.overSize
        applyDisplay(
            "wm size ${nw}x$nh",
            if (old.isNotEmpty()) "wm size $old" else "wm size reset",
            "Resolusi ${nw}x$nh"
        )
    }

    fun resetSize() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { Shell.run("wm size reset") }
            display = withContext(Dispatchers.IO) { Tweaks.readDisplay() }
        }
    }

    // ---------------- Cadangan ----------------

    fun exportTo(uri: Uri) {
        try {
            app.contentResolver.openOutputStream(uri)?.use { it.write(Backup.export(cfg.sp).toByteArray()) }
            toast("Pengaturan diekspor")
        } catch (e: Exception) {
            toast("Gagal ekspor: ${e.message}")
        }
    }

    fun importFrom(uri: Uri) {
        try {
            val text = app.contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: ""
            val n = Backup.import(cfg.sp, text)
            cfg.reload()
            syncService()
            toast("$n pengaturan dipulihkan")
        } catch (e: Exception) {
            toast("Berkas tidak valid: ${e.message}")
        }
    }
}
