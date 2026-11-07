package com.fauzi.tweaklauncher

import android.content.Intent
import android.provider.ContactsContract
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Palet Symbian S60 (Nokia): latar biru muda, aksen biru terang, teks hitam. */
fun symPalette() = Palette(
    bg = Color(0xFFEAF0F8), card = Color(0xFFFFFFFF), inset = Color(0xFFDDE6F2),
    line = Color(0xFF9FB4CC), text = Color(0xFF000000), soft = Color(0xFF1A1A1A), sub = Color(0xFF55657A),
    accent = Color(0xFF1E88E5), onAccent = Color(0xFFFFFFFF), radius = 6.dp, dark = false, wallpaper = false
)

internal fun symDarkBrush() = Brush.verticalGradient(listOf(Color(0xFF454B55), Color(0xFF1A1D22), Color(0xFF0C0E11)))
internal fun symTitleBrush() = Brush.verticalGradient(listOf(Color(0xFFD7E6F8), Color(0xFFA8C6EA)))
private fun symStatusBrush() = Brush.verticalGradient(listOf(Color(0xFF2B4F86), Color(0xFF0E2547)))
private fun symSelBrush() = Brush.verticalGradient(listOf(Color(0xFF8FD0FF), Color(0xFF2B8DE8)))

private class SymItem(val key: String, val label: String, val app: AppItem?, val sys: Int)

private fun symItems(vm: MainVm): List<SymItem> =
    (vm.appsForTab("all").map { SymItem("app:" + it.pkg, it.label, it, 0) } +
        SymItem("sys:care", "Perawatan", null, 1) + SymItem("sys:set", "Setelan", null, 2))
        .sortedBy { it.label.lowercase() }

private fun symBack(vm: MainVm) {
    if (vm.symOpts) {
        vm.symOpts = false
    } else if (vm.symSel != null) {
        vm.symSel = null
    } else when (vm.screen) {
        Screen.Settings -> if (vm.settingsPage.isNotEmpty()) vm.settingsPage = "" else vm.screen = Screen.Drawer
        Screen.Care -> vm.screen = Screen.Drawer
        Screen.Drawer -> vm.screen = Screen.Home
        Screen.Home -> {}
    }
}

private fun symOpen(vm: MainVm, it: SymItem) {
    vm.symSel = null
    when {
        it.app != null -> vm.launchApp(it.app.pkg)
        it.sys == 1 -> vm.screen = Screen.Care
        else -> {
            vm.settingsPage = ""
            vm.screen = Screen.Settings
        }
    }
}

// ---------------------------------------------------------------- Bingkai

@Composable
fun SymFrame(vm: MainVm) {
    val ctx = LocalContext.current
    BackHandler(enabled = vm.screen != Screen.Home || vm.symOpts || vm.symSel != null) { symBack(vm) }
    LaunchedEffect(vm.screen) { vm.symSel = null }

    val title = when (vm.screen) {
        Screen.Home -> ""
        Screen.Drawer -> "Menu"
        Screen.Care -> "Perawatan"
        Screen.Settings -> "Setelan"
    }
    val left = if (vm.screen == Screen.Home) "Menu" else "Pilihan"
    val right = when (vm.screen) {
        Screen.Home -> "Kontak"
        Screen.Drawer -> "Keluar"
        else -> "Kembali"
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
            SymStatus(vm)
            if (title.isNotEmpty()) {
                Box(
                    Modifier.fillMaxWidth().height(30.dp).background(symTitleBrush()).padding(horizontal = 10.dp),
                    contentAlignment = Alignment.CenterStart
                ) { Text(title, color = Color.Black, fontSize = 15.sp, fontWeight = FontWeight.Bold) }
            }
            Box(Modifier.weight(1f)) {
                when (vm.screen) {
                    Screen.Home -> SymStandby(vm)
                    Screen.Drawer -> SymMenu(vm)
                    Screen.Care -> CareScreen(vm)
                    Screen.Settings -> SettingsScreen(vm)
                }
            }
            SymSoftkeys(
                left, right,
                onLeft = {
                    if (vm.screen == Screen.Home) vm.screen = Screen.Drawer else vm.symOpts = true
                },
                onRight = {
                    if (vm.screen == Screen.Home) {
                        try {
                            ctx.startActivity(
                                Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.CONTENT_URI).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        } catch (e: Exception) {
                        }
                    } else symBack(vm)
                }
            )
        }
        if (vm.symOpts) SymOptions(vm)
    }
}

@Composable
private fun SymStatus(vm: MainVm) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(15_000)
        }
    }
    Row(
        Modifier.fillMaxWidth().height(28.dp).background(symStatusBrush()).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Canvas(Modifier.size(width = 22.dp, height = 14.dp)) {
            val w = size.width
            val h = size.height
            for (i in 0 until 5) {
                val bh = h * (0.3f + 0.7f * i / 4f)
                drawRect(Color.White, Offset(i * (w / 5f), h - bh), Size(w / 5f - 2.dp.toPx(), bh))
            }
        }
        Text("3G", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f))
        Text(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(now)), color = Color.White, fontSize = 12.sp)
        // Baterai bersegmen tujuh
        val seg = ((vm.stats.battery.coerceIn(0, 100) * 7 + 99) / 100).coerceIn(0, 7)
        Canvas(Modifier.size(width = 24.dp, height = 12.dp)) {
            val w = size.width
            val h = size.height
            for (i in 0 until 7) {
                val bh = h * (0.35f + 0.65f * i / 6f)
                drawRect(
                    if (i < seg) Color(0xFF7CFC4A) else Color(0x55FFFFFF),
                    Offset(i * (w / 7f), h - bh), Size(w / 7f - 1.5.dp.toPx(), bh)
                )
            }
        }
    }
}

@Composable
private fun SymSoftkeys(left: String, right: String, onLeft: () -> Unit, onRight: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(46.dp).background(symDarkBrush()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            left, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f).fillMaxHeight().clickable(onClick = onLeft).padding(start = 16.dp, top = 13.dp)
        )
        Text(
            right, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End,
            modifier = Modifier.weight(1f).fillMaxHeight().clickable(onClick = onRight).padding(end = 16.dp, top = 13.dp)
        )
    }
}

// ---------------------------------------------------------------- Layar siaga (standby)

@Composable
private fun SymStandby(vm: MainVm) {
    val cfg = vm.cfg
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(10_000)
        }
    }
    val spec = cfg.wpSym
    val wp = rememberWallpaper(spec, "sym")
    val white = LocalPal.current.copy(text = Color.White, soft = Color.White, sub = Color.White)
    val shadow = TextStyle(shadow = androidx.compose.ui.graphics.Shadow(Color(0x99000000), Offset(2f, 2f), 6f))

    Box(
        Modifier
            .fillMaxSize()
            .wallpaperBg(spec, wp) {
                drawRect(Brush.verticalGradient(listOf(Color(0xFF0B2C5C), Color(0xFF1F63B5), Color(0xFF58A8F0))))
                drawCircle(Color(0x22FFFFFF), size.width * 0.55f, Offset(size.width * 0.85f, size.height * 0.2f))
                drawCircle(Color(0x18FFFFFF), size.width * 0.4f, Offset(size.width * 0.1f, size.height * 0.75f))
                drawCircle(Color(0x14FFFFFF), size.width * 0.25f, Offset(size.width * 0.6f, size.height * 0.9f))
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
        Column(Modifier.align(Alignment.TopCenter).padding(top = 36.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(now)),
                color = Color.White, fontSize = 68.sp, fontWeight = FontWeight.Light, style = shadow
            )
            Text(
                SimpleDateFormat("EEEE, d MMMM", Locale("id", "ID")).format(Date(now)),
                color = Color.White, fontSize = 15.sp, style = shadow
            )
            Text("Baterai ${vm.stats.battery}%", color = Color(0xFFD9ECFF), fontSize = 13.sp, style = shadow)
        }
        // Bilah pintasan siaga aktif
        CompositionLocalProvider(LocalPal provides white) {
            Row(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 10.dp, vertical = 12.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0x66000000))
                    .horizontalScroll(rememberScrollState())
                    .padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val sl = vm.sleeping
                vm.homeEntries.forEach { e ->
                    when (e) {
                        is HomeEntry.A -> AppTile(e.app, Modifier.width(76.dp), vm, e.app.pkg in sl)
                        is HomeEntry.F -> FolderTile(e.name, e.apps, Modifier.width(76.dp), vm)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Menu utama

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SymMenu(vm: MainVm) {
    val items = symItems(vm)
    val sleeping = vm.sleeping
    Box(Modifier.fillMaxSize().background(Color(0xFFF4F8FD))) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(items, key = { it.key }) { it ->
                val sel = vm.symSel == it.key
                Column(
                    Modifier
                        .padding(2.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .then(if (sel) Modifier.background(symSelBrush()) else Modifier)
                        .combinedClickable(
                            onClick = { symOpen(vm, it) },
                            onLongClick = { vm.symSel = it.key }
                        )
                        .padding(vertical = 10.dp, horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(Modifier.size(46.dp), contentAlignment = Alignment.Center) {
                        if (it.app != null) AppIcon(it.app, 44.dp, dim = it.app.pkg in sleeping)
                        else SymSysIcon(it.sys, 40)
                    }
                    Spacer(Modifier.height(5.dp))
                    Text(
                        it.label,
                        color = if (sel) Color.White else Color.Black,
                        fontSize = 11.sp,
                        fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun SymSysIcon(kind: Int, dp: Int) {
    Box(
        Modifier.size(dp.dp).clip(RoundedCornerShape(8.dp)).background(
            Brush.verticalGradient(if (kind == 1) listOf(Color(0xFF7CD65A), Color(0xFF2E8B1E)) else listOf(Color(0xFF9AA9BC), Color(0xFF4D5E75)))
        ),
        contentAlignment = Alignment.Center
    ) {
        if (kind == 2) GearIcon(Color.White, (dp * 0.62f).dp)
        else Canvas(Modifier.size((dp * 0.55f).dp)) {
            val w = size.width
            val t = w * 0.26f
            drawRect(Color.White, Offset(w * 0.5f - t / 2, 0f), Size(t, w))
            drawRect(Color.White, Offset(0f, w * 0.5f - t / 2), Size(w, t))
        }
    }
}

// ---------------------------------------------------------------- Menu Pilihan

@Composable
private fun SymOptions(vm: MainVm) {
    val sel = vm.symSel
    val items = symItems(vm)
    val cur = items.firstOrNull { it.key == sel }
    val list = buildList<Pair<String, () -> Unit>> {
        if (cur != null) {
            add("Buka" to { symOpen(vm, cur); vm.symOpts = false })
            if (cur.app != null) add("Opsi aplikasi..." to { vm.menuApp = cur.app; vm.symOpts = false; vm.symSel = null })
        }
        add("Perawatan" to { vm.symOpts = false; vm.screen = Screen.Care })
        add("Setelan" to { vm.symOpts = false; vm.settingsPage = ""; vm.screen = Screen.Settings })
        add("Kunci layar" to { vm.symOpts = false; vm.runGesture("lock") })
        add("Mode Modern" to { vm.symOpts = false; vm.cfg.uiMode = "modern" })
        add("Keluar" to { vm.symOpts = false; vm.screen = Screen.Home })
    }
    Box(
        Modifier
            .fillMaxSize()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { vm.symOpts = false }
    ) {
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(start = 4.dp, bottom = 46.dp)
                .width(230.dp)
                .border(2.dp, Color(0xFF3E5E8F), RoundedCornerShape(6.dp))
                .clip(RoundedCornerShape(6.dp))
                .background(Color.White)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }
        ) {
            list.forEachIndexed { i, (label, act) ->
                if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFD5DEEA)))
                Text(
                    label, color = Color.Black, fontSize = 15.sp,
                    modifier = Modifier.fillMaxWidth().clickable(onClick = act).padding(horizontal = 14.dp, vertical = 12.dp)
                )
            }
        }
    }
}
