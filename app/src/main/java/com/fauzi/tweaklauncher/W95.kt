package com.fauzi.tweaklauncher

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val TASKBAR_H = 38.dp
private val W95_GRAY = Color(0xFFC0C0C0)

/** Tepi timbul (raised) atau tenggelam (sunken) 3D ala Windows 95. */
fun Modifier.bevel(raised: Boolean = true): Modifier = this.drawBehind {
    val w = size.width
    val h = size.height
    val t = 1.dp.toPx()
    val white = Color.White
    val black = Color.Black
    val mid = Color(0xFF808080)
    val soft = Color(0xFFDFDFDF)
    val tl1 = if (raised) white else mid
    val br1 = if (raised) black else white
    val tl2 = if (raised) soft else black
    val br2 = if (raised) mid else soft
    drawRect(tl1, Offset(0f, 0f), Size(w, t))
    drawRect(tl1, Offset(0f, 0f), Size(t, h))
    drawRect(br1, Offset(0f, h - t), Size(w, t))
    drawRect(br1, Offset(w - t, 0f), Size(t, h))
    drawRect(tl2, Offset(t, t), Size(w - 2 * t, t))
    drawRect(tl2, Offset(t, t), Size(t, h - 2 * t))
    drawRect(br2, Offset(t, h - 2 * t), Size(w - 2 * t, t))
    drawRect(br2, Offset(w - 2 * t, t), Size(t, h - 2 * t))
}

private fun w95Title(s: Screen) = when (s) {
    Screen.Home -> "Desktop"
    Screen.Care -> "Komputer Saya"
    Screen.Drawer -> "Program"
    Screen.Settings -> "Panel Kontrol"
}

@Composable
fun W95Frame(vm: MainVm) {
    BackHandler(enabled = vm.w95Start) { vm.w95Start = false }
    // Jendela yang dibuka lewat gestur atau tombol lain tetap masuk ke taskbar.
    LaunchedEffect(vm.screen) {
        if (vm.screen != Screen.Home && vm.screen !in vm.w95Windows) vm.w95Windows.add(vm.screen)
    }
    Box(Modifier.fillMaxSize().statusBarsPadding().imePadding()) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                if (vm.screen == Screen.Home) W95Desktop(vm) else W95Window(vm)
            }
            val imeOpen = WindowInsets.ime.getBottom(LocalDensity.current) > 0
            if (!imeOpen) W95Taskbar(vm)
        }
        if (vm.w95Start) W95StartMenu(vm)
    }
}

// ---------------------------------------------------------------- Ikon gambar

@Composable
private fun W95Icon(kind: Int, dp: Dp) {
    Canvas(Modifier.size(dp)) {
        val w = this.size.width
        when (kind) {
            0 -> {
                drawRect(W95_GRAY, Offset(w * 0.1f, w * 0.1f), Size(w * 0.8f, w * 0.55f))
                drawRect(Color(0xFF000080), Offset(w * 0.18f, w * 0.18f), Size(w * 0.64f, w * 0.39f))
                drawRect(W95_GRAY, Offset(w * 0.3f, w * 0.68f), Size(w * 0.4f, w * 0.1f))
                drawRect(Color(0xFF808080), Offset(w * 0.2f, w * 0.78f), Size(w * 0.6f, w * 0.12f))
            }
            1 -> {
                drawRect(Color(0xFFD9A800), Offset(w * 0.08f, w * 0.18f), Size(w * 0.36f, w * 0.14f))
                drawRect(Color(0xFFFFE066), Offset(w * 0.08f, w * 0.3f), Size(w * 0.84f, w * 0.56f))
                drawRect(Color(0xFF808000), Offset(w * 0.08f, w * 0.3f), Size(w * 0.84f, w * 0.56f), style = Stroke(w * 0.04f))
            }
            2 -> {
                drawRect(W95_GRAY, Offset(w * 0.1f, w * 0.15f), Size(w * 0.8f, w * 0.7f))
                drawRect(Color(0xFF000080), Offset(w * 0.1f, w * 0.15f), Size(w * 0.8f, w * 0.16f))
                drawRect(Color.White, Offset(w * 0.18f, w * 0.42f), Size(w * 0.64f, w * 0.34f))
                drawRect(Color(0xFF808080), Offset(w * 0.1f, w * 0.15f), Size(w * 0.8f, w * 0.7f), style = Stroke(w * 0.04f))
            }
            else -> {
                drawRect(Color(0xFFE5483B), Offset(0f, 0f), Size(w * 0.46f, w * 0.46f))
                drawRect(Color(0xFF4CAF50), Offset(w * 0.54f, 0f), Size(w * 0.46f, w * 0.46f))
                drawRect(Color(0xFF2F7DE1), Offset(0f, w * 0.54f), Size(w * 0.46f, w * 0.46f))
                drawRect(Color(0xFFF2C230), Offset(w * 0.54f, w * 0.54f), Size(w * 0.46f, w * 0.46f))
            }
        }
    }
}

@Composable
private fun W95TitleBtn(kind: Int, onClick: () -> Unit) {
    Box(
        Modifier
            .size(width = 20.dp, height = 18.dp)
            .background(W95_GRAY)
            .bevel(true)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(9.dp)) {
            val w = this.size.width
            val t = 2.dp.toPx()
            when (kind) {
                0 -> drawRect(Color.Black, Offset(0f, w - t), Size(w * 0.8f, t))
                1 -> drawRect(Color.Black, Offset(0f, 0f), Size(w, w), style = Stroke(t))
                else -> {
                    drawLine(Color.Black, Offset(0f, 0f), Offset(w, w), strokeWidth = t)
                    drawLine(Color.Black, Offset(w, 0f), Offset(0f, w), strokeWidth = t)
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Desktop

@Composable
private fun W95Shortcut(label: String, kind: Int, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.clickable(onClick = onClick).padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        W95Icon(kind, 44.dp)
        Spacer(Modifier.height(6.dp))
        Text(label, color = Color.White, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
    }
}

@Composable
private fun W95Desktop(vm: MainVm) {
    val cfg = vm.cfg
    val scroll = rememberScrollState()
    val canUp = remember { booleanArrayOf(false) }
    val canDown = remember { booleanArrayOf(false) }
    val cols = cfg.homeCols.coerceIn(3, 6)
    val white = LocalPal.current.copy(text = Color.White, soft = Color.White, sub = Color.White)

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
        CompositionLocalProvider(LocalPal provides white) {
            Column(
                Modifier.fillMaxSize().verticalScroll(scroll).padding(horizontal = 10.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(Modifier.fillMaxWidth()) {
                    W95Shortcut("Komputer Saya", 0, Modifier.weight(1f)) { vm.w95Open(Screen.Care) }
                    W95Shortcut("Program", 1, Modifier.weight(1f)) { vm.w95Open(Screen.Drawer) }
                    W95Shortcut("Panel Kontrol", 2, Modifier.weight(1f)) {
                        vm.settingsPage = ""
                        vm.w95Open(Screen.Settings)
                    }
                    repeat(cols - 3) { Spacer(Modifier.weight(1f)) }
                }
                AppsGrid(vm)
                if (vm.widgetIds.isNotEmpty()) WidgetsBlock(vm)
            }
        }
    }
}

// ---------------------------------------------------------------- Jendela

@Composable
private fun W95Window(vm: MainVm) {
    val s = vm.screen
    Box(
        Modifier
            .fillMaxSize()
            .padding(4.dp)
            .background(W95_GRAY)
            .bevel(true)
            .padding(4.dp)
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .background(Color(0xFF000080))
                    .padding(horizontal = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    w95Title(s),
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f).padding(start = 3.dp),
                    maxLines = 1
                )
                W95TitleBtn(0) { vm.screen = Screen.Home }
                W95TitleBtn(1) { }
                Spacer(Modifier.width(2.dp))
                W95TitleBtn(2) { vm.w95Close(s) }
            }
            Box(Modifier.weight(1f).fillMaxWidth().padding(top = 3.dp)) {
                when (s) {
                    Screen.Care -> CareScreen(vm)
                    Screen.Drawer -> PdaPrograms(vm)
                    Screen.Settings -> SettingsScreen(vm)
                    Screen.Home -> {}
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Taskbar

@Composable
private fun W95Taskbar(vm: MainVm) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(15_000)
        }
    }
    val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(now))
    Column(Modifier.fillMaxWidth().background(W95_GRAY).navigationBarsPadding()) {
        Box(Modifier.fillMaxWidth().height(2.dp).background(Color.White))
        Row(
            Modifier.fillMaxWidth().height(TASKBAR_H).padding(horizontal = 3.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                Modifier
                    .fillMaxHeight()
                    .background(W95_GRAY)
                    .bevel(!vm.w95Start)
                    .clickable { vm.w95Start = !vm.w95Start }
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                W95Icon(3, 16.dp)
                Text("Start", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Row(
                Modifier.weight(1f).fillMaxHeight(),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                vm.w95Windows.toList().forEach { w ->
                    val on = vm.screen == w
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(if (on) Color(0xFFE0E0E0) else W95_GRAY)
                            .bevel(!on)
                            .clickable {
                                vm.w95Start = false
                                vm.screen = if (on) Screen.Home else w
                            }
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            w95Title(w),
                            color = Color.Black,
                            fontSize = 11.sp,
                            fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            Box(
                Modifier
                    .fillMaxHeight()
                    .background(W95_GRAY)
                    .bevel(false)
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("${vm.stats.battery}%  $time", color = Color.Black, fontSize = 12.sp)
            }
        }
    }
}

// ---------------------------------------------------------------- Menu Start

@Composable
private fun W95MenuRow(label: String, kind: Int, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        W95Icon(kind, 24.dp)
        Text(label, color = Color.Black, fontSize = 14.sp)
    }
}

@Composable
private fun W95MenuLine() {
    Box(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp).height(2.dp).background(Color(0xFF808080)))
}

@Composable
private fun BoxScopeStart(vm: MainVm) {
    var showProg by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { vm.loadUsage() }
    Row(
        Modifier
            .width(250.dp)
            .height(IntrinsicSize.Min)
            .background(W95_GRAY)
            .bevel(true)
            .padding(3.dp)
    ) {
        Box(Modifier.width(26.dp).fillMaxHeight().background(Color(0xFF000080)))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            W95MenuRow(if (showProg) "Program  ▾" else "Program  ▸", 1) { showProg = !showProg }
            if (showProg) {
                vm.recentApps.take(5).forEach { a ->
                    Text(
                        a.label,
                        color = Color.Black,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                vm.w95Start = false
                                vm.launchApp(a.pkg)
                            }
                            .padding(start = 42.dp, top = 7.dp, bottom = 7.dp, end = 6.dp)
                    )
                }
                Text(
                    "Semua program...",
                    color = Color.Black,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { vm.w95Open(Screen.Drawer) }
                        .padding(start = 42.dp, top = 7.dp, bottom = 7.dp)
                )
            }
            W95MenuRow("Komputer Saya", 0) { vm.w95Open(Screen.Care) }
            W95MenuRow("Panel Kontrol", 2) {
                vm.settingsPage = ""
                vm.w95Open(Screen.Settings)
            }
            W95MenuRow("Cari...", 1) { vm.w95Open(Screen.Drawer) }
            W95MenuLine()
            W95MenuRow("Kunci layar...", 3) {
                vm.w95Start = false
                vm.runGesture("lock")
            }
            W95MenuRow("Mode Modern", 3) {
                vm.w95Start = false
                vm.cfg.uiMode = "modern"
            }
        }
    }
}

@Composable
private fun W95StartMenu(vm: MainVm) {
    Box(
        Modifier
            .fillMaxSize()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                vm.w95Start = false
            }
    ) {
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(start = 3.dp, bottom = TASKBAR_H + 2.dp)
        ) {
            BoxScopeStart(vm)
        }
    }
}
