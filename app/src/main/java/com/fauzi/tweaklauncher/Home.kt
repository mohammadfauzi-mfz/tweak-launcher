package com.fauzi.tweaklauncher

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(vm: MainVm) {
    val cfg = vm.cfg
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            vm.refreshStats()
            delay(5000)
        }
    }
    val scroll = rememberScrollState()
    val canUp = remember { booleanArrayOf(false) }
    val canDown = remember { booleanArrayOf(false) }
    val sections = Layout.parse(cfg.homeSections).filter { it.second }.map { it.first }

    Column(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { vm.runGesture(cfg.gDouble) },
                    onLongPress = { vm.runGesture(cfg.gLong) }
                )
            }
            .swipes(
                onStart = { canUp[0] = scroll.canScrollForward; canDown[0] = scroll.canScrollBackward },
                onEnd = { d, _ ->
                    if (kotlin.math.abs(d.y) > kotlin.math.abs(d.x) * 1.5f) {
                        if (d.y < -140f && !canUp[0]) vm.runGesture(cfg.gSwipeUp)
                        else if (d.y > 140f && !canDown[0]) vm.runGesture(cfg.gSwipeDown)
                    }
                }
            )
    ) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(scroll)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            sections.forEach { s ->
                when (s) {
                    "clock" -> ClockBlock(vm, now)
                    "profile" -> ProfileCard(vm)
                    "stats" -> StatsCard(vm)
                    "apps" -> AppsGrid(vm)
                    "widgets" -> WidgetsBlock(vm)
                }
            }
        }
        if (cfg.dockOn) Dock(vm)
    }
}

@Composable
fun ClockBlock(vm: MainVm, now: Long) {
    val cfg = vm.cfg
    if (cfg.clockStyle == "hidden") return
    val loc = Locale("id", "ID")
    val time = SimpleDateFormat(if (cfg.clock24) "HH:mm" else "h:mm a", loc).format(Date(now))
    val date = SimpleDateFormat("EEEE, d MMMM", loc).format(Date(now))
    val big = cfg.clockStyle == "large"
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
        Column {
            Text(time, color = Pal.text, fontSize = if (big) 60.sp else 34.sp, fontWeight = FontWeight.Light)
            if (cfg.showDate) Text(date, color = Pal.sub, fontSize = 14.sp)
        }
        Row(
            Modifier
                .height(32.dp)
                .clip(CircleShape)
                .background(Pal.card)
                .border(1.dp, Pal.line, CircleShape)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(Pal.accent))
            Text("Mode ${vm.profile.label}", color = Pal.text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun ProfileCard(vm: MainVm) {
    val profiles = vm.profiles
    val current = vm.profile
    val many = profiles.size > 3
    val ready = vm.shizuku == ShizukuState.READY
    val needPerm = vm.shizuku == ShizukuState.NEED_PERM
    FoldCard("home_profile", "Profil performa", "Mode ${current.label}") {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Status Shizuku", color = Pal.sub, fontSize = 12.sp)
            if (ready) {
                Text("Shizuku aktif", color = Green, fontSize = 12.sp)
            } else {
                // Ketuk untuk membuka Shizuku (atau meminta izin jika Shizuku sudah jalan).
                Text(
                    if (needPerm) "Beri izin Shizuku" else "Shizuku mati",
                    color = Red,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { if (needPerm) vm.requestPerm() else vm.openShizuku() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .clip(CircleShape)
                .background(Pal.inset)
                .padding(4.dp)
                .then(if (many) Modifier.horizontalScroll(rememberScrollState()) else Modifier),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            profiles.forEach { p ->
                Pill(
                    p.label,
                    p.id == current.id,
                    (if (many) Modifier.widthIn(min = 88.dp) else Modifier.weight(1f)).height(44.dp)
                ) {
                    if (!vm.busy) vm.setProfile(p.id)
                }
            }
        }
        Text(if (vm.busy) "Menerapkan profil..." else current.desc(), color = Pal.soft, fontSize = 13.sp)
    }
}

@Composable
fun StatsCard(vm: MainVm) {
    val st = vm.stats
    FoldCard("home_stats", "Status HP", "RAM ${st.ramText} · Baterai ${st.battery}%") {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StatBar("RAM", st.ramText, st.ramPct, Pal.accent, Modifier.weight(1f))
            StatBar("Baterai", "${st.battery}%", st.battery / 100f, Green, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StatBar("Suhu", st.tempText, st.tempPct, Orange, Modifier.weight(1f))
            StatBar("Penyimpanan", "${(st.storagePct * 100).toInt()}%", st.storagePct, Blue, Modifier.weight(1f))
        }
    }
}

@Composable
fun AppsGrid(vm: MainVm, fold: Boolean = true) {
    val cols = vm.cfg.homeCols.coerceIn(3, 6)
    val sl = vm.sleeping
    val (open0, toggle) = rememberFold("home_apps", true)
    val open = open0 || !fold
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (fold) FoldHeader("Aplikasi", "${vm.homeEntries.size} item", open, toggle)
        if (open) vm.homeEntries.chunked(cols).forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEach { e ->
                    when (e) {
                        is HomeEntry.A -> AppTile(e.app, Modifier.weight(1f), vm, e.app.pkg in sl)
                        is HomeEntry.F -> FolderTile(e.name, e.apps, Modifier.weight(1f), vm)
                    }
                }
                repeat(cols - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
fun WidgetsBlock(vm: MainVm, fold: Boolean = true) {
    val ids = vm.widgetIds
    val (open0, toggle) = rememberFold("home_widgets", true)
    val open = open0 || !fold
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (fold) FoldHeader("Widget", "${ids.size} widget", open, toggle)
        if (open) ids.forEach { id -> WidgetView(id) }
        if (open && ids.isEmpty()) {
            ActionButton("Tambah widget", Modifier.fillMaxWidth()) { vm.showWidgetPicker = true }
        }
    }
}

@Composable
fun Dock(vm: MainVm) {
    val apps = vm.dockApps
    if (apps.isEmpty()) return
    Row(
        Modifier
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .fillMaxWidth()
            .clip(Pal.shape)
            .background(Pal.card)
            .border(1.dp, Pal.line, Pal.shape)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        apps.forEach { a ->
            Box(Modifier.clickable { vm.launchApp(a.pkg) }.padding(4.dp)) {
                AppIcon(a, vm.cfg.iconSize.dp)
            }
        }
    }
}
