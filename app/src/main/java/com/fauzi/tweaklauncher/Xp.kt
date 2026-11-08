package com.fauzi.tweaklauncher

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val XP_BAR_H = 40.dp
internal val XP_BEIGE = Color(0xFFECE9D8)
internal val XP_BLUE_DARK = Color(0xFF0A4FC8)
private val XP_BLUE = Color(0xFF2F6FE0)

private fun xpBarBrush() = Brush.verticalGradient(listOf(Color(0xFF3C81F3), Color(0xFF245EDC), Color(0xFF1941A5)))
internal fun xpTitleBrush() = Brush.verticalGradient(listOf(Color(0xFF3D8FF5), Color(0xFF0A5FE0), Color(0xFF0A4FC8)))
private fun xpStartBrush() = Brush.verticalGradient(listOf(Color(0xFF5CB85C), Color(0xFF3A9A3A), Color(0xFF2B7A2B)))

@Composable
fun XpFrame(vm: MainVm) {
    BackHandler(enabled = vm.w95Start || vm.clockOpen || vm.trayPop.isNotEmpty() || vm.deskMove != null) {
        vm.w95Start = false
        vm.clockOpen = false
        vm.trayPop = ""
        vm.deskMove = null
    }
    LaunchedEffect(vm.screen) {
        if (vm.screen != Screen.Home && vm.screen !in vm.w95Windows) vm.w95Windows.add(vm.screen)
    }
    Box(Modifier.fillMaxSize().statusBarsPadding().imePadding()) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                if (vm.screen == Screen.Home) XpDesktop(vm) else XpWindow(vm)
            }
            val imeOpen = WindowInsets.ime.getBottom(LocalDensity.current) > 0
            if (!imeOpen) XpTaskbar(vm)
        }
        GameLayer(vm, xp = true)
        if (vm.w95Start) XpStartMenu(vm)
        if (vm.clockOpen) ClockPopup(vm, 1, XP_BAR_H)
        if (vm.trayPop.isNotEmpty()) W95TrayPopup(vm, XP_BAR_H, xp = true)
    }
}

// ---------------------------------------------------------------- Desktop

@Composable
private fun XpShortcut(label: String, kind: Int, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.clickable(onClick = onClick).padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        W95Icon(kind, 44.dp)
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            color = Color.White,
            fontSize = 12.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            style = LocalTextStyle.current.copy(shadow = Shadow(Color.Black, Offset(1f, 1f), 3f))
        )
    }
}

@Composable
private fun XpDesktop(vm: MainVm) {
    val cfg = vm.cfg
    val white = LocalPal.current.copy(text = Color.White, soft = Color.White, sub = Color.White)
    val spec = cfg.wpXp
    val wp = rememberWallpaper(spec, "xp")

    Box(
        Modifier
            .fillMaxSize()
            .wallpaperBg(spec, wp) {
                drawRect(
                    Brush.verticalGradient(
                        listOf(Color(0xFF2B6FDC), Color(0xFF9CCBF5)),
                        startY = 0f,
                        endY = size.height * 0.75f
                    )
                )
                drawOval(
                    Brush.verticalGradient(listOf(Color(0xFF6DBE2E), Color(0xFF2F7A12))),
                    topLeft = Offset(-size.width * 0.3f, size.height * 0.72f),
                    size = Size(size.width * 1.6f, size.height * 0.6f)
                )
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { vm.runGesture(cfg.gDouble) },
                    onLongPress = { vm.runGesture(cfg.gLong) }
                )
            }
            .swipes(
                onEnd = { d, _ ->
                    if (kotlin.math.abs(d.y) > kotlin.math.abs(d.x) * 1.5f) {
                        if (d.y < -140f) vm.runGesture(cfg.gSwipeUp)
                        else if (d.y > 140f) vm.runGesture(cfg.gSwipeDown)
                    }
                }
            )
    ) {
        CompositionLocalProvider(LocalPal provides white) {
            Row(Modifier.fillMaxSize()) {
                DesktopIcons(
                    vm,
                    listOf(
                        DeskItem.Sys("Komputer Saya", 0) { vm.w95Open(Screen.Care) },
                        DeskItem.Sys("Program", 1) { vm.w95Open(Screen.Drawer) },
                        DeskItem.Sys("Panel Kontrol", 2) {
                            vm.settingsPage = ""
                            vm.w95Open(Screen.Settings)
                        }
                    ),
                    Modifier.weight(1f).fillMaxHeight()
                ) { l, k, m, c -> XpShortcut(l, k, m, c) }
                if (vm.widgetIds.isNotEmpty()) {
                    Box(Modifier.width(190.dp).fillMaxHeight().verticalScroll(rememberScrollState()).padding(8.dp)) {
                        WidgetsBlock(vm, fold = false)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Jendela

@Composable
private fun XpTitleBtn(kind: Int, onClick: () -> Unit) {
    val shape = RoundedCornerShape(4.dp)
    Box(
        Modifier
            .size(22.dp)
            .clip(shape)
            .background(if (kind == 2) Color(0xFFD9502F) else XP_BLUE)
            .border(1.dp, Color.White, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(10.dp)) {
            val w = this.size.width
            val t = 2.dp.toPx()
            when (kind) {
                0 -> drawRect(Color.White, Offset(0f, w - t), Size(w * 0.8f, t))
                1 -> drawRect(Color.White, Offset(0f, 0f), Size(w, w), style = androidx.compose.ui.graphics.drawscope.Stroke(t))
                else -> {
                    drawLine(Color.White, Offset(0f, 0f), Offset(w, w), strokeWidth = t)
                    drawLine(Color.White, Offset(w, 0f), Offset(0f, w), strokeWidth = t)
                }
            }
        }
    }
}

@Composable
private fun XpWindow(vm: MainVm) {
    val s = vm.screen
    Column(
        Modifier
            .fillMaxSize()
            .padding(4.dp)
            .clip(RoundedCornerShape(topStart = 9.dp, topEnd = 9.dp))
            .background(XP_BLUE_DARK)
            .padding(start = 3.dp, end = 3.dp, bottom = 3.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(30.dp)
                .background(xpTitleBrush())
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                w95Title(s),
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                modifier = Modifier.weight(1f).padding(start = 4.dp),
                style = LocalTextStyle.current.copy(shadow = Shadow(Color(0xFF0A246A), Offset(1f, 1f), 2f))
            )
            XpTitleBtn(0) { vm.screen = Screen.Home }
            XpTitleBtn(1) { }
            XpTitleBtn(2) { vm.w95Close(s) }
        }
        Box(Modifier.weight(1f).fillMaxWidth().background(XP_BEIGE)) {
            when (s) {
                Screen.Care -> CareScreen(vm)
                Screen.Drawer -> PdaPrograms(vm)
                Screen.Settings -> SettingsScreen(vm)
                Screen.Home -> {}
            }
        }
    }
}

// ---------------------------------------------------------------- Taskbar

@Composable
private fun XpTaskbar(vm: MainVm) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(15_000)
        }
    }
    val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(now))
    Row(
        Modifier
            .fillMaxWidth()
            .background(xpBarBrush())
            .navigationBarsPadding()
            .height(XP_BAR_H),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            Modifier
                .fillMaxHeight()
                .clip(RoundedCornerShape(topEnd = 18.dp, bottomEnd = 18.dp))
                .background(xpStartBrush())
                .clickable {
                    vm.trayPop = ""
                    vm.w95Start = !vm.w95Start
                }
                .padding(start = 10.dp, end = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            W95Icon(3, 18.dp)
            Text(
                "start",
                color = Color.White,
                fontSize = 18.sp,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Bold,
                style = LocalTextStyle.current.copy(shadow = Shadow(Color(0xFF1B4D1B), Offset(1f, 1f), 2f))
            )
        }
        Row(
            Modifier.weight(1f).fillMaxHeight().padding(horizontal = 6.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            vm.w95Windows.toList().forEach { w ->
                val on = vm.screen == w
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (on) Color(0xFF1E52B3) else Color(0xFF3C81F3))
                        .clickable {
                            vm.w95Start = false
                            vm.screen = if (on) Screen.Home else w
                        }
                        .padding(horizontal = 6.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        w95Title(w),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        Row(
            Modifier
                .fillMaxHeight()
                .background(Color(0xFF0F8CE8))
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TrayWifi(Modifier.clickable {
                vm.w95Start = false
                vm.clockOpen = false
                vm.trayPop = if (vm.trayPop == "wifi") "" else "wifi"
            }, Color.White)
            TraySpeaker(Modifier.clickable {
                vm.w95Start = false
                vm.clockOpen = false
                vm.trayPop = if (vm.trayPop == "vol") "" else "vol"
            }, Color.White)
            Row(
                Modifier.clickable {
                    vm.w95Start = false
                    vm.trayPop = ""
                    vm.clockOpen = !vm.clockOpen
                },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                TrayBattery(vm.stats.battery, tint = Color.White)
                Text("${vm.stats.battery}%  $time", color = Color.White, fontSize = 12.sp)
            }
        }
    }
}

// ---------------------------------------------------------------- Menu Start

@Composable
private fun XpMenuRow(label: String, kind: Int, bold: Boolean = false, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        W95Icon(kind, 24.dp)
        Text(
            label,
            color = Color.Black,
            fontSize = 13.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun XpAppRow(a: AppItem, icon: androidx.compose.ui.unit.Dp, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AppIcon(a, icon, badge = false)
        Text(a.label, color = Color.Black, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun XpFolderRow(label: String, open: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        W95Icon(1, 24.dp)
        Text(label, color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Text(if (open) "\u25BC" else "\u25B6", color = Color(0xFF1E8A1E), fontSize = 11.sp)
    }
}

@Composable
private fun XpStartPanel(vm: MainVm) {
    LaunchedEffect(Unit) { vm.loadUsage() }
    var showAll by remember { mutableStateOf(false) }
    var showGames by remember { mutableStateOf(false) }
    var openTab by remember { mutableStateOf("") }
    val gameSet = remember(vm.cfg.gameApps) { Sys.list(vm.cfg.gameApps).toSet() }
    val allApps = remember(vm.apps) { vm.apps.sortedBy { it.label.lowercase() } }
    val shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)
    Column(
        Modifier
            .width(300.dp)
            .clip(shape)
            .background(XP_BLUE_DARK)
            .padding(2.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().background(xpTitleBrush()).padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF2C230))
                    .border(2.dp, Color.White, CircleShape)
            )
            Text("Pengguna", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Column(Modifier.weight(1f).fillMaxHeight().background(Color.White).padding(vertical = 4.dp)) {
                if (showGames) {
                    Row(
                        Modifier.fillMaxWidth().clickable { vm.openGame("mines") }.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MinesIcon(24.dp)
                        Text("Minesweeper", color = Color.Black, fontSize = 13.sp)
                    }
                    Row(
                        Modifier.fillMaxWidth().clickable { vm.openGame("sol") }.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CardsIcon(24.dp)
                        Text("Solitaire", color = Color.Black, fontSize = 13.sp)
                    }
                    Column(Modifier.heightIn(max = 260.dp).verticalScroll(rememberScrollState())) {
                        allApps.filter { it.pkg in gameSet }.forEach { a -> XpAppRow(a, 24.dp) {
                            vm.w95Start = false
                            vm.launchApp(a.pkg)
                        } }
                    }
                    Box(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp).height(1.dp).background(Color(0xFFD0D0D0)))
                    Text(
                        "\u25C0  Kembali",
                        color = Color.Black,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth().clickable { showGames = false }.padding(horizontal = 10.dp, vertical = 9.dp)
                    )
                } else if (showAll) {
                    Column(Modifier.heightIn(max = 340.dp).verticalScroll(rememberScrollState())) {
                        val tabs = vm.allTabs().filter { it.id != "all" }
                        tabs.forEach { t ->
                            val list = vm.appsForTab(t.id)
                            if (list.isNotEmpty()) {
                                XpFolderRow(t.label, openTab == t.id) { openTab = if (openTab == t.id) "" else t.id }
                                if (openTab == t.id) list.forEach { a ->
                                    Box(Modifier.padding(start = 16.dp)) {
                                        XpAppRow(a, 24.dp) {
                                            vm.w95Start = false
                                            vm.launchApp(a.pkg)
                                        }
                                    }
                                }
                            }
                        }
                        if (tabs.isNotEmpty()) Box(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp).height(1.dp).background(Color(0xFFD0D0D0)))
                        XpFolderRow("Semua (${allApps.size})", openTab == "all") { openTab = if (openTab == "all") "" else "all" }
                        if (openTab == "all") allApps.forEach { a ->
                            Box(Modifier.padding(start = 16.dp)) {
                                XpAppRow(a, 24.dp) {
                                    vm.w95Start = false
                                    vm.launchApp(a.pkg)
                                }
                            }
                        }
                    }
                    Box(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp).height(1.dp).background(Color(0xFFD0D0D0)))
                    Text(
                        "\u25C0  Kembali",
                        color = Color.Black,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth().clickable { showAll = false }.padding(horizontal = 10.dp, vertical = 9.dp)
                    )
                } else {
                    vm.recentApps.take(6).forEach { a -> XpAppRow(a, 28.dp) {
                        vm.w95Start = false
                        vm.launchApp(a.pkg)
                    } }
                    Box(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp).height(1.dp).background(Color(0xFFD0D0D0)))
                    Row(
                        Modifier.fillMaxWidth().clickable { showAll = true }.padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        W95Icon(1, 24.dp)
                        Text("Semua Program", color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text("\u25B6", color = Color(0xFF1E8A1E), fontSize = 12.sp)
                    }
                }
            }
            Column(Modifier.weight(1f).fillMaxHeight().background(Color(0xFFD3E5FA)).padding(vertical = 4.dp)) {
                XpMenuRow("Komputer Saya", 0, bold = true) { vm.w95Open(Screen.Care) }
                XpMenuRow("Panel Kontrol", 2, bold = true) {
                    vm.settingsPage = ""
                    vm.w95Open(Screen.Settings)
                }
                XpMenuRow("Cari", 1, bold = true) { vm.w95Open(Screen.Drawer) }
                XpMenuRow("Game", 1, bold = true) {
                    showAll = false
                    showGames = true
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().background(xpTitleBrush()).padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                "Kunci layar",
                color = Color.White,
                fontSize = 13.sp,
                modifier = Modifier
                    .clickable {
                        vm.w95Start = false
                        vm.runGesture("lock")
                    }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
            Text(
                "Mode Modern",
                color = Color.White,
                fontSize = 13.sp,
                modifier = Modifier
                    .clickable {
                        vm.w95Start = false
                        vm.cfg.uiMode = "modern"
                    }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun XpStartMenu(vm: MainVm) {
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
                .padding(start = 2.dp, bottom = XP_BAR_H)
        ) {
            XpStartPanel(vm)
        }
    }
}


/** Menu tahan-lama bergaya Windows XP: title bar biru bergradasi, isi putih, sudut atas membulat. */
@Composable
fun XpContextMenu(title: String, items: List<Pair<String, () -> Unit>?>, onClose: () -> Unit) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onClose) {
        Column(
            Modifier
                .width(270.dp)
                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .background(XP_BLUE_DARK)
                .padding(3.dp)
        ) {
            Text(
                title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().background(xpTitleBrush()).padding(horizontal = 8.dp, vertical = 6.dp)
            )
            Column(Modifier.fillMaxWidth().background(Color.White).heightIn(max = 440.dp).verticalScroll(rememberScrollState())) {
                items.forEach { it ->
                    if (it == null) {
                        Box(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 3.dp).height(1.dp).background(Color(0xFFD0D0D0)))
                    } else {
                        Text(
                            it.first,
                            color = Color.Black,
                            fontSize = 14.sp,
                            modifier = Modifier.fillMaxWidth().clickable(onClick = it.second).padding(horizontal = 12.dp, vertical = 9.dp)
                        )
                    }
                }
            }
        }
    }
}
