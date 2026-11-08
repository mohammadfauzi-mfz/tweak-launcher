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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val MENU_H = 30.dp

private fun macTitle(s: Screen) = when (s) {
    Screen.Home -> "Finder"
    Screen.Care -> "Macintosh HD"
    Screen.Drawer -> "Program"
    Screen.Settings -> "Panel Kontrol"
}

/** Garis hitam tipis berselang untuk bar judul jendela klasik. */
internal fun Modifier.stripes(): Modifier = this.drawBehind {
    val step = 3.dp.toPx()
    var y = 2.dp.toPx()
    while (y < size.height - 2.dp.toPx()) {
        drawRect(Color.Black, Offset(0f, y), Size(size.width, 1.dp.toPx()))
        y += step
    }
}

internal fun Modifier.macShadow(): Modifier = this.drawBehind {
    val o = 3.dp.toPx()
    drawRect(Color.Black, Offset(o, o), size)
}

@Composable
internal fun MacIcon(kind: Int, dp: Dp) {
    val cfg = LocalCfg.current
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val packBmp = cfg.sysIcon("mac$kind")?.let { IconPack.get(ctx, it) }
    if (packBmp != null) {
        androidx.compose.foundation.Image(
            bitmap = packBmp,
            contentDescription = null,
            filterQuality = androidx.compose.ui.graphics.FilterQuality.None,
            colorFilter = if (cfg.packGray) IconPack.GRAY else null,
            modifier = Modifier.size(dp)
        )
        return
    }
    Canvas(Modifier.size(dp)) {
        val w = this.size.width
        val st = Stroke(width = w * 0.05f)
        when (kind) {
            0 -> {
                drawRoundRect(Color.White, Offset(w * 0.12f, w * 0.28f), Size(w * 0.76f, w * 0.44f), CornerRadius(w * 0.06f))
                drawRoundRect(Color.Black, Offset(w * 0.12f, w * 0.28f), Size(w * 0.76f, w * 0.44f), CornerRadius(w * 0.06f), style = st)
                drawRect(Color.Black, Offset(w * 0.2f, w * 0.58f), Size(w * 0.4f, w * 0.04f))
                drawCircle(Color.Black, w * 0.035f, Offset(w * 0.78f, w * 0.6f))
            }
            1 -> {
                drawRect(Color.White, Offset(w * 0.1f, w * 0.2f), Size(w * 0.35f, w * 0.14f))
                drawRect(Color.Black, Offset(w * 0.1f, w * 0.2f), Size(w * 0.35f, w * 0.14f), style = st)
                drawRect(Color.White, Offset(w * 0.1f, w * 0.32f), Size(w * 0.8f, w * 0.5f))
                drawRect(Color.Black, Offset(w * 0.1f, w * 0.32f), Size(w * 0.8f, w * 0.5f), style = st)
            }
            else -> {
                drawRect(Color.White, Offset(w * 0.12f, w * 0.18f), Size(w * 0.76f, w * 0.64f))
                drawRect(Color.Black, Offset(w * 0.12f, w * 0.18f), Size(w * 0.76f, w * 0.64f), style = st)
                drawRect(Color.Black, Offset(w * 0.12f, w * 0.18f), Size(w * 0.76f, w * 0.12f))
                drawRect(Color.Black, Offset(w * 0.22f, w * 0.46f), Size(w * 0.56f, w * 0.03f))
                drawRect(Color.Black, Offset(w * 0.4f, w * 0.41f), Size(w * 0.07f, w * 0.13f))
                drawRect(Color.Black, Offset(w * 0.22f, w * 0.64f), Size(w * 0.56f, w * 0.03f))
                drawRect(Color.Black, Offset(w * 0.6f, w * 0.59f), Size(w * 0.07f, w * 0.13f))
            }
        }
    }
}

@Composable
fun MacFrame(vm: MainVm) {
    BackHandler(enabled = vm.w95Start) { vm.w95Start = false }
    Box(Modifier.fillMaxSize().statusBarsPadding().imePadding()) {
        Column(Modifier.fillMaxSize()) {
            MacMenuBar(vm)
            Box(Modifier.weight(1f).navigationBarsPadding()) {
                if (vm.screen == Screen.Home) MacDesktop(vm) else MacWindow(vm)
            }
        }
        if (vm.w95Start) MacMenu(vm)
    }
}

// ---------------------------------------------------------------- Bar menu

@Composable
private fun MacMenuBar(vm: MainVm) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(15_000)
        }
    }
    val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(now))
    Column(Modifier.fillMaxWidth().background(Color.White)) {
        Row(
            Modifier.fillMaxWidth().height(MENU_H).padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .background(if (vm.w95Start) Color.Black else Color.White)
                    .clickable { vm.w95Start = !vm.w95Start }
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Tweak", color = if (vm.w95Start) Color.White else Color.Black, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            Text(macTitle(vm.screen), color = Color.Black, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 10.dp).weight(1f), maxLines = 1)
            Text("${vm.stats.battery}%  $time", color = Color.Black, fontSize = 14.sp)
        }
        Box(Modifier.fillMaxWidth().height(2.dp).background(Color.Black))
    }
}

@Composable
private fun MacMenuRow(label: String, onClick: () -> Unit) {
    Text(
        label,
        color = Color.Black,
        fontSize = 15.sp,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 11.dp)
    )
}

@Composable
private fun MacMenu(vm: MainVm) {
    fun go(s: Screen) {
        vm.w95Start = false
        vm.screen = s
    }
    Box(
        Modifier
            .fillMaxSize()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { vm.w95Start = false }
    ) {
        Column(
            Modifier
                .padding(top = MENU_H + 2.dp, start = 6.dp)
                .width(250.dp)
                .macShadow()
                .background(Color.White)
                .border(2.dp, Color.Black)
        ) {
            MacMenuRow("Tentang Tweak Launcher...") {
                vm.settingsPage = "tentang"
                go(Screen.Settings)
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color.Black))
            MacMenuRow("Macintosh HD (Perawatan)") { go(Screen.Care) }
            MacMenuRow("Program") { go(Screen.Drawer) }
            MacMenuRow("Panel Kontrol (Setelan)") {
                vm.settingsPage = ""
                go(Screen.Settings)
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color.Black))
            MacMenuRow("Kunci layar") {
                vm.w95Start = false
                vm.runGesture("lock")
            }
            MacMenuRow("Mode Modern") {
                vm.w95Start = false
                vm.cfg.uiMode = "modern"
            }
        }
    }
}

// ---------------------------------------------------------------- Desktop

@Composable
private fun MacShortcut(label: String, kind: Int, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.clickable(onClick = onClick).padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        MacIcon(kind, 44.dp)
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            color = Color.Black,
            fontSize = 12.sp,
            maxLines = 2,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.background(Color.White).padding(horizontal = 3.dp)
        )
    }
}

@Composable
private fun MacDesktop(vm: MainVm) {
    val cfg = vm.cfg
    // Label ikon di desktop: teks hitam di atas kotak putih, seperti Finder lama.
    val labelPal = LocalPal.current.copy(text = Color.Black, soft = Color.Black, sub = Color.Black)

    Box(
        Modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(Color(0xFFA6A6A6), Offset(0f, 0f), size)
                val step = 4.dp.toPx()
                var y = 0f
                while (y < size.height) {
                    drawRect(Color(0xFF8E8E8E), Offset(0f, y), Size(size.width, 1.dp.toPx()))
                    y += step
                }
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
        CompositionLocalProvider(LocalPal provides labelPal) {
            Row(Modifier.fillMaxSize()) {
                DesktopIcons(
                    vm,
                    listOf(
                        DeskItem.Sys("Macintosh HD", 0) { vm.screen = Screen.Care },
                        DeskItem.Sys("Program", 1) { vm.screen = Screen.Drawer },
                        DeskItem.Sys("Panel Kontrol", 2) {
                            vm.settingsPage = ""
                            vm.screen = Screen.Settings
                        }
                    ),
                    Modifier.weight(1f).fillMaxHeight()
                ) { l, k, mod, c -> MacShortcut(l, k, mod, c) }
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
private fun MacWindow(vm: MainVm) {
    val s = vm.screen
    Box(
        Modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(Color(0xFFA6A6A6), Offset(0f, 0f), size)
            }
            .padding(start = 10.dp, top = 10.dp, end = 13.dp, bottom = 13.dp)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .macShadow()
                .background(Color(0xFFDDDDDD))
                .border(2.dp, Color.Black)
        ) {
            Box(
                Modifier.fillMaxWidth().height(26.dp).background(Color.White).stripes(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 8.dp)
                        .size(15.dp)
                        .background(Color.White)
                        .border(2.dp, Color.Black)
                        .clickable { vm.screen = Screen.Home }
                )
                Text(
                    macTitle(s),
                    color = Color.Black,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.background(Color.White).padding(horizontal = 10.dp),
                    maxLines = 1
                )
                Box(
                    Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 8.dp)
                        .size(15.dp)
                        .background(Color.White)
                        .border(2.dp, Color.Black)
                        .clickable { vm.screen = Screen.Home }
                        .padding(3.dp)
                        .border(1.dp, Color.Black)
                )
            }
            Box(Modifier.fillMaxWidth().height(2.dp).background(Color.Black))
            Box(Modifier.weight(1f).fillMaxWidth()) {
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
