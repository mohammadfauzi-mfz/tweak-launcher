package com.fauzi.tweaklauncher

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private val TITLE_H = 30.dp

/**
 * Mode tampilan PDA: layar LCD abu-hijau, bar judul dan tombol Start ala Pocket PC,
 * serta area tombol keras ala Palm di bawah.
 */
@Composable
fun PdaFrame(vm: MainVm) {
    BackHandler(enabled = vm.pdaStart) { vm.pdaStart = false }
    Box(Modifier.fillMaxSize().statusBarsPadding().imePadding()) {
        Column(Modifier.fillMaxSize()) {
            PdaTitleBar(vm)
            Box(Modifier.weight(1f)) {
                when (vm.screen) {
                    Screen.Home -> PdaToday(vm)
                    Screen.Care -> CareScreen(vm)
                    Screen.Drawer -> PdaPrograms(vm)
                    Screen.Settings -> SettingsScreen(vm)
                }
            }
            val imeOpen = WindowInsets.ime.getBottom(LocalDensity.current) > 0
            if (!imeOpen) PdaSilk(vm)
        }
        if (vm.pdaStart) PdaStartMenu(vm)
    }
}

// ---------------------------------------------------------------- Bar judul

@Composable
private fun PdaTitleBar(vm: MainVm) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(15_000)
        }
    }
    val title = when (vm.screen) {
        Screen.Home -> "Today"
        Screen.Care -> "Perawatan"
        Screen.Drawer -> "Programs"
        Screen.Settings -> "Setelan"
    }
    val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(now))
    Row(
        Modifier
            .fillMaxWidth()
            .height(TITLE_H)
            .background(Pal.accent)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            Modifier
                .fillMaxHeight()
                .clickable { vm.pdaStart = !vm.pdaStart }
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            PdaFlag(16.dp)
            Text("Start", color = Pal.onAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.weight(1f))
        Text(title, color = Pal.onAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f))
        Text("${vm.stats.battery}%  $time", color = Pal.onAccent, fontSize = 13.sp)
    }
}

@Composable
private fun PdaFlag(dp: Dp) {
    Canvas(Modifier.size(dp)) {
        val w = this.size.width
        val g = w * 0.08f
        val s = (w - g) / 2f
        drawRect(Color(0xFFE5483B), Offset(0f, 0f), Size(s, s))
        drawRect(Color(0xFF4CAF50), Offset(s + g, 0f), Size(s, s))
        drawRect(Color(0xFF2F7DE1), Offset(0f, s + g), Size(s, s))
        drawRect(Color(0xFFF2C230), Offset(s + g, s + g), Size(s, s))
    }
}

// ---------------------------------------------------------------- Tombol keras (Palm)

@Composable
private fun PdaGlyph(kind: Int, color: Color, dp: Dp) {
    if (kind == 3) {
        GearIcon(color, dp)
        return
    }
    Canvas(Modifier.size(dp)) {
        val w = this.size.width
        when (kind) {
            0 -> {
                val p = Path().apply {
                    moveTo(w * 0.5f, w * 0.1f)
                    lineTo(w * 0.94f, w * 0.5f)
                    lineTo(w * 0.8f, w * 0.5f)
                    lineTo(w * 0.8f, w * 0.9f)
                    lineTo(w * 0.2f, w * 0.9f)
                    lineTo(w * 0.2f, w * 0.5f)
                    lineTo(w * 0.06f, w * 0.5f)
                    close()
                }
                drawPath(p, color)
            }
            1 -> {
                drawRect(color, Offset(w * 0.4f, w * 0.08f), Size(w * 0.2f, w * 0.84f))
                drawRect(color, Offset(w * 0.08f, w * 0.4f), Size(w * 0.84f, w * 0.2f))
            }
            else -> {
                val s = w * 0.38f
                drawRect(color, Offset(w * 0.08f, w * 0.08f), Size(s, s))
                drawRect(color, Offset(w * 0.54f, w * 0.08f), Size(s, s))
                drawRect(color, Offset(w * 0.08f, w * 0.54f), Size(s, s))
                drawRect(color, Offset(w * 0.54f, w * 0.54f), Size(s, s))
            }
        }
    }
}

@Composable
private fun PdaHwButton(kind: Int, target: Screen, vm: MainVm) {
    val on = vm.screen == target
    Box(
        Modifier
            .size(46.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (on) Pal.accent else Pal.inset)
            .border(2.dp, Pal.text, RoundedCornerShape(8.dp))
            .clickable {
                vm.pdaStart = false
                vm.screen = target
            },
        contentAlignment = Alignment.Center
    ) {
        PdaGlyph(kind, if (on) Pal.onAccent else Pal.text, 22.dp)
    }
}

@Composable
private fun PdaSilk(vm: MainVm) {
    Column(Modifier.fillMaxWidth().background(Pal.card)) {
        Box(Modifier.fillMaxWidth().height(2.dp).background(Pal.text))
        Row(
            Modifier
                .navigationBarsPadding()
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PdaHwButton(0, Screen.Home, vm)
            PdaHwButton(1, Screen.Care, vm)
            // Area "Graffiti": hiasan seperti Palm.
            Box(
                Modifier
                    .weight(1f)
                    .height(46.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Pal.bg)
                    .border(1.dp, Pal.text, RoundedCornerShape(6.dp))
                    .clickable { vm.pdaStart = !vm.pdaStart }
            ) {
                Text("abc", color = Pal.sub, fontSize = 10.sp, modifier = Modifier.align(Alignment.BottomStart).padding(4.dp))
                Text("123", color = Pal.sub, fontSize = 10.sp, modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp))
                Box(
                    Modifier
                        .align(Alignment.Center)
                        .width(1.dp)
                        .fillMaxHeight(0.7f)
                        .background(Pal.sub)
                )
            }
            PdaHwButton(2, Screen.Drawer, vm)
            PdaHwButton(3, Screen.Settings, vm)
        }
    }
}

// ---------------------------------------------------------------- Komponen

@Composable
private fun PdaBox(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .border(1.dp, Pal.text)
            .background(Pal.card)
    ) {
        Text(
            title,
            color = Pal.bg,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth().background(Pal.text).padding(horizontal = 8.dp, vertical = 3.dp)
        )
        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}

@Composable
private fun PdaMeter(label: String, value: String, pct: Float) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, color = Pal.text, fontSize = 12.sp, modifier = Modifier.width(84.dp))
        val n = 16
        val on = (pct.coerceIn(0f, 1f) * n).roundToInt()
        Row(
            Modifier.weight(1f).height(12.dp).border(1.dp, Pal.text).padding(1.dp),
            horizontalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            repeat(n) { i ->
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(if (i < on) Pal.text else Color.Transparent)
                )
            }
        }
        Text(value, color = Pal.text, fontSize = 12.sp, textAlign = TextAlign.End, modifier = Modifier.width(60.dp))
    }
}

// ---------------------------------------------------------------- Today

@Composable
private fun PdaToday(vm: MainVm) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            vm.refreshStats()
            delay(5000)
        }
    }
    val st = vm.stats
    val loc = Locale("id", "ID")
    val time = SimpleDateFormat("HH:mm", loc).format(Date(now))
    val date = SimpleDateFormat("EEEE, d MMMM yyyy", loc).format(Date(now))
    val ready = vm.shizuku == ShizukuState.READY
    val needPerm = vm.shizuku == ShizukuState.NEED_PERM
    val current = vm.profile

    val cfg = vm.cfg
    val scroll = rememberScrollState()
    val canUp = remember { booleanArrayOf(false) }
    val canDown = remember { booleanArrayOf(false) }

    // Gestur sama seperti mode Modern: ketuk dua kali, tahan lama, geser atas/bawah.
    Box(
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
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        PdaBox("HARI INI") {
            Text(time, color = Pal.text, fontSize = 44.sp, fontWeight = FontWeight.Bold)
            Text(date, color = Pal.soft, fontSize = 13.sp)
            Text("Mode: ${current.label}", color = Pal.text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }

        PdaBox("PROFIL PERFORMA") {
            vm.profiles.forEach { p ->
                val sel = p.id == current.id
                Row(
                    Modifier.fillMaxWidth().clickable { vm.setProfile(p.id) }.padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        Modifier.size(16.dp).border(2.dp, Pal.text, RoundedCornerShape(8.dp)).padding(3.dp)
                    ) {
                        if (sel) Box(Modifier.fillMaxSize().background(Pal.text, RoundedCornerShape(8.dp)))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(p.label, color = Pal.text, fontSize = 14.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
                        Text(p.desc(), color = Pal.sub, fontSize = 11.sp)
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Shizuku: ", color = Pal.text, fontSize = 13.sp)
                if (ready) {
                    Text("aktif", color = Pal.text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                } else {
                    Text(
                        if (needPerm) "[beri izin]" else "[mati, ketuk untuk buka]",
                        color = Red,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { if (needPerm) vm.requestPerm() else vm.openShizuku() }
                    )
                }
            }
            if (vm.busy) Text("Menerapkan profil...", color = Pal.soft, fontSize = 12.sp)
        }

        PdaBox("SISTEM") {
            PdaMeter("RAM", st.ramText, st.ramPct)
            PdaMeter("Baterai", "${st.battery}%", st.battery / 100f)
            PdaMeter("Suhu", st.tempText, st.tempPct)
            PdaMeter("Simpanan", "${(st.storagePct * 100).toInt()}%", st.storagePct)
        }

        PdaBox("PROGRAM") {
            AppsGrid(vm, fold = false)
        }

        if (vm.widgetIds.isNotEmpty()) {
            PdaBox("WIDGET") { WidgetsBlock(vm, fold = false) }
        }
    }
    }
}

// ---------------------------------------------------------------- Programs

@Composable
internal fun PdaPrograms(vm: MainVm) {
    val cfg = vm.cfg
    var q by remember { mutableStateOf("") }
    val sleeping = vm.sleeping
    val list = vm.appsForTab(vm.currentTab()).filter { a -> q.isBlank() || a.label.contains(q, ignoreCase = true) }

    Column(
        Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AppTabsRow(vm)

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .border(1.dp, Pal.text)
                .background(Pal.card)
                .padding(4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(list, key = { it.pkg }) { a ->
                AppTile(a, Modifier, vm, a.pkg in sleeping)
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Cari:", color = Pal.text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            BasicTextField(
                value = q,
                onValueChange = { q = it },
                singleLine = true,
                textStyle = TextStyle(color = Pal.text, fontSize = 14.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                cursorBrush = SolidColor(Pal.text),
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .border(2.dp, Pal.text)
                    .background(Pal.bg)
                    .padding(horizontal = 8.dp),
                decorationBox = { inner ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                        if (q.isEmpty()) Text("${vm.visibleApps.size} program", color = Pal.sub, fontSize = 13.sp)
                        inner()
                    }
                }
            )
        }
    }
}

// ---------------------------------------------------------------- Menu Start

@Composable
private fun PdaMenuRow(text: String, onClick: () -> Unit) {
    Text(
        text,
        color = Pal.text,
        fontSize = 14.sp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    )
}

@Composable
private fun PdaMenuLine() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(Pal.text))
}

@Composable
private fun PdaStartMenu(vm: MainVm) {
    LaunchedEffect(Unit) { vm.loadUsage() }
    fun go(s: Screen) {
        vm.screen = s
        vm.pdaStart = false
    }
    Box(
        Modifier
            .fillMaxSize()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                vm.pdaStart = false
            }
    ) {
        Column(
            Modifier
                .padding(top = TITLE_H)
                .width(240.dp)
                .background(Pal.bg)
                .border(2.dp, Pal.text)
        ) {
            PdaMenuRow("Today") { go(Screen.Home) }
            PdaMenuRow("Perawatan") { go(Screen.Care) }
            PdaMenuRow("Programs") { go(Screen.Drawer) }
            PdaMenuRow("Setelan") {
                vm.settingsPage = ""
                go(Screen.Settings)
            }
            val recent = vm.recentApps.take(4)
            if (recent.isNotEmpty()) {
                PdaMenuLine()
                recent.forEach { a ->
                    PdaMenuRow(a.label) {
                        vm.pdaStart = false
                        vm.launchApp(a.pkg)
                    }
                }
            }
            PdaMenuLine()
            PdaMenuRow("Kunci layar") {
                vm.pdaStart = false
                vm.runGesture("lock")
            }
            PdaMenuRow("Mode Modern") {
                vm.pdaStart = false
                vm.cfg.uiMode = "modern"
            }
        }
    }
}
