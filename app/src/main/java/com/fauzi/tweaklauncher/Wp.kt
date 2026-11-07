package com.fauzi.tweaklauncher

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.Canvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Warna aksen Windows Phone (20 warna bawaan). */
val WP_COLORS = listOf(
    "A4C400", "60A917", "008A00", "00ABA9", "1BA1E2", "0050EF", "6A00FF", "AA00FF", "F472D0", "D80073",
    "A20025", "E51400", "FA6800", "F0A30A", "E3C800", "825A2C", "6D8764", "647687", "76608A", "87794E"
)

fun wpPalette(accentHex: String) = Palette(
    bg = Color(0xFF000000), card = Color(0xFF1F1F1F), inset = Color(0xFF2B2B2B),
    line = Color(0xFF3A3A3A), text = Color(0xFFFFFFFF), soft = Color(0xFFE0E0E0), sub = Color(0xFF9A9A9A),
    accent = parseColor(accentHex, Color(0xFF1BA1E2)), onAccent = Color.White,
    radius = 0.dp, dark = true, wallpaper = false
)

// ---------------------------------------------------------------- Bingkai

@Composable
fun WpFrame(vm: MainVm) {
    BackHandler(enabled = vm.wpEdit != null) { vm.wpEdit = null }
    Box(Modifier.fillMaxSize().statusBarsPadding()) {
        when (vm.screen) {
            Screen.Home, Screen.Drawer -> WpPager(vm)
            Screen.Care -> Box(Modifier.fillMaxSize().navigationBarsPadding().imePadding()) { CareScreen(vm) }
            Screen.Settings -> Box(Modifier.fillMaxSize().navigationBarsPadding().imePadding()) { SettingsScreen(vm) }
        }
    }
}

@Composable
private fun WpPager(vm: MainVm) {
    val pager = rememberPagerState(initialPage = if (vm.screen == Screen.Drawer) 1 else 0) { 2 }
    LaunchedEffect(vm.screen) {
        val t = if (vm.screen == Screen.Drawer) 1 else 0
        if (pager.currentPage != t) pager.animateScrollToPage(t)
    }
    LaunchedEffect(pager.currentPage) {
        vm.wpEdit = null
        vm.screen = if (pager.currentPage == 1) Screen.Drawer else Screen.Home
    }
    HorizontalPager(state = pager, modifier = Modifier.fillMaxSize().navigationBarsPadding()) { page ->
        if (page == 0) WpStart(vm) else WpApps(vm)
    }
}

// ---------------------------------------------------------------- Ubin

private class WpTile(val key: String, val label: String, val app: AppItem?, val folder: List<AppItem>?, val sys: Int)

private sealed class WpSlot {
    class W(val t: WpTile) : WpSlot()
    class M(val t: WpTile) : WpSlot()
    class S(val ts: MutableList<WpTile>) : WpSlot()
}

private fun wpTiles(vm: MainVm): List<WpTile> {
    val base = mutableListOf<WpTile>()
    base.add(WpTile("sys:care", "perawatan", null, null, 1))
    base.add(WpTile("sys:set", "setelan", null, null, 2))
    vm.homeEntries.forEach { e ->
        when (e) {
            is HomeEntry.A -> base.add(WpTile("app:" + e.app.pkg, e.app.label, e.app, null, 0))
            is HomeEntry.F -> base.add(WpTile("dir:" + e.name, e.name, null, e.apps, 0))
        }
    }
    val idx = HashMap<String, Int>()
    vm.cfg.wpOrder.split("\n").filter { it.isNotEmpty() }.forEachIndexed { n, k -> idx[k] = n }
    return base.sortedBy { idx[it.key] ?: Int.MAX_VALUE }
}

private fun wpMove(vm: MainVm, all: List<String>, moving: String, before: String) {
    val l = all.toMutableList()
    if (!l.remove(moving)) return
    val at = l.indexOf(before).let { if (it < 0) l.size else it }
    l.add(at, moving)
    vm.cfg.wpOrder = l.joinToString("\n")
}

private fun wpSetSize(vm: MainVm, key: String, size: String) {
    val m = Store.map(vm.cfg.wpSizes)
    m[key] = size
    vm.cfg.wpSizes = Store.mapJson(m)
}

@Composable
private fun WpGlyph(kind: Int, size: Dp) {
    if (kind == 2) {
        GearIcon(Color.White, size)
    } else {
        Canvas(Modifier.size(size)) {
            val w = this.size.width
            val t = w * 0.22f
            drawRect(Color.White, Offset(w * 0.5f - t / 2, w * 0.1f), Size(t, w * 0.8f))
            drawRect(Color.White, Offset(w * 0.1f, w * 0.5f - t / 2), Size(w * 0.8f, t))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WpTileView(vm: MainVm, t: WpTile, sz: String, w: Dp, h: Dp, all: List<String>) {
    val cfg = vm.cfg
    val edit = vm.wpEdit
    val sel = edit == t.key
    val sleeping = vm.sleeping
    Box(
        Modifier
            .size(w, h)
            .then(if (sel) Modifier.scale(0.93f) else Modifier)
            .alpha(if (edit != null && !sel) 0.55f else 1f)
            .background(Pal.accent)
            .then(if (sel) Modifier.border(1.dp, Color.White) else Modifier)
            .combinedClickable(
                onClick = {
                    if (edit != null) {
                        if (!sel) wpMove(vm, all, edit, t.key)
                        vm.wpEdit = null
                    } else if (t.sys == 1) {
                        vm.screen = Screen.Care
                    } else if (t.sys == 2) {
                        vm.settingsPage = ""
                        vm.screen = Screen.Settings
                    } else if (t.app != null) {
                        vm.launchApp(t.app.pkg)
                    } else if (t.folder != null) {
                        vm.openFolder = t.label
                    }
                },
                onLongClick = { vm.wpEdit = t.key }
            )
    ) {
        val ic = minOf(w, h) * (if (sz == "s") 0.6f else 0.46f)
        Box(Modifier.align(Alignment.Center)) {
            when {
                t.sys != 0 -> WpGlyph(t.sys, ic)
                t.app != null -> AppIcon(t.app, ic, badge = false, dim = t.app.pkg in sleeping)
                t.folder != null -> {
                    val mini = ic / 2.2f
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        t.folder.take(4).chunked(2).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                row.forEach { a -> AppIcon(a, mini, badge = false) }
                            }
                        }
                    }
                }
            }
        }
        if (sz != "s") {
            Text(
                t.label,
                color = Color.White,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.BottomStart).padding(start = 8.dp, bottom = 6.dp, end = 30.dp)
            )
        }
        if (t.app != null && cfg.badgeOn && sz != "s") {
            val n = Badges.counts[t.app.pkg] ?: 0
            if (n > 0 && t.app.pkg !in Sys.list(cfg.badgeOffCsv)) {
                Text(
                    if (n > 99) "99+" else n.toString(),
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Light,
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                )
            }
        }
        if (sel) {
            if (t.sys == 0) {
                WpRound("\u2715", Modifier.align(Alignment.TopEnd).padding(4.dp)) {
                    if (t.app != null) vm.togglePin(t.app.pkg)
                    else if (t.folder != null) {
                        cfg.pinnedCsv = Sys.join(Sys.list(cfg.pinnedCsv).filter { it != "folder:" + t.label })
                    }
                    vm.wpEdit = null
                }
            }
            WpRound("\u2194", Modifier.align(Alignment.BottomEnd).padding(4.dp)) {
                val cur = Store.map(cfg.wpSizes)[t.key] ?: "m"
                wpSetSize(vm, t.key, when (cur) { "m" -> "w"; "w" -> "s"; else -> "m" })
            }
            if (t.app != null) {
                WpRound("\u2026", Modifier.align(Alignment.BottomStart).padding(4.dp)) {
                    vm.menuApp = t.app
                    vm.wpEdit = null
                }
            }
        }
    }
}

@Composable
private fun WpRound(glyph: String, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(Color(0xCC000000))
            .border(1.dp, Color.White, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Text(glyph, color = Color.White, fontSize = 13.sp) }
}

@Composable
private fun WpSlotView(vm: MainVm, slot: WpSlot, half: Dp, u: Dp, gap: Dp, all: List<String>) {
    when (slot) {
        is WpSlot.W -> WpTileView(vm, slot.t, "w", half * 2 + gap, half, all)
        is WpSlot.M -> WpTileView(vm, slot.t, "m", half, half, all)
        is WpSlot.S -> Box(Modifier.size(half)) {
            slot.ts.chunked(2).forEachIndexed { r, pair ->
                pair.forEachIndexed { c, t ->
                    Box(Modifier.offset(x = (u + gap) * c, y = (u + gap) * r)) { WpTileView(vm, t, "s", u, u, all) }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Layar Start

@Composable
private fun WpStart(vm: MainVm) {
    val cfg = vm.cfg
    val tiles = wpTiles(vm)
    val sizes = Store.map(cfg.wpSizes)
    val all = tiles.map { it.key }

    val slots = mutableListOf<WpSlot>()
    tiles.forEach { t ->
        when (sizes[t.key]) {
            "w" -> slots.add(WpSlot.W(t))
            "s" -> {
                val last = slots.lastOrNull()
                if (last is WpSlot.S && last.ts.size < 4) last.ts.add(t) else slots.add(WpSlot.S(mutableListOf(t)))
            }
            else -> slots.add(WpSlot.M(t))
        }
    }
    val rows = mutableListOf<List<WpSlot>>()
    var pend: WpSlot? = null
    slots.forEach { s ->
        val p = pend
        if (s is WpSlot.W) {
            if (p != null) rows.add(listOf(p))
            pend = null
            rows.add(listOf(s))
        } else if (p == null) {
            pend = s
        } else {
            rows.add(listOf(p, s))
            pend = null
        }
    }
    pend?.let { rows.add(listOf(it)) }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val gap = 4.dp
        val avail = maxWidth - 16.dp
        val half = (avail - gap) / 2
        val u = (half - gap) / 2
        LazyColumn(
            Modifier
                .fillMaxSize()
                .then(
                    if (vm.wpEdit != null) Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { vm.wpEdit = null }
                    else Modifier
                )
                .pointerInput(Unit) { detectTapGestures(onDoubleTap = { vm.runGesture(cfg.gDouble) }) },
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(gap)
        ) {
            items(rows) { row ->
                if (row.size == 1 && row[0] is WpSlot.W) {
                    WpSlotView(vm, row[0], half, u, gap, all)
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                        row.forEach { WpSlotView(vm, it, half, u, gap, all) }
                    }
                }
            }
            item {
                Box(Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.CenterEnd) {
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .border(2.dp, Color.White, CircleShape)
                            .clickable { vm.screen = Screen.Drawer },
                        contentAlignment = Alignment.Center
                    ) { Text("\u2192", color = Color.White, fontSize = 22.sp) }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Daftar aplikasi

private class WpRow(val label: String, val app: AppItem?, val sys: Int)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WpApps(vm: MainVm) {
    var q by remember { mutableStateOf("") }
    val sleeping = vm.sleeping
    val src = vm.appsForTab("all")
    val rows = (src.map { WpRow(it.label, it, 0) } + WpRow("Perawatan", null, 1) + WpRow("Setelan", null, 2))
        .filter { q.isBlank() || it.label.contains(q, ignoreCase = true) }
        .sortedBy { it.label.lowercase() }
    val grouped = rows.groupBy { r ->
        val c = r.label.firstOrNull()?.uppercaseChar() ?: '#'
        if (c.isLetter()) c else '#'
    }.entries.sortedBy { if (it.key == '#') '{' else it.key }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(8.dp))
        InputBox(q, { q = it }, "search", Modifier.fillMaxWidth())
        Spacer(Modifier.height(6.dp))
        LazyColumn(Modifier.weight(1f)) {
            grouped.forEach { (letter, list) ->
                item(key = "h$letter") {
                    Box(
                        Modifier.padding(top = 10.dp, bottom = 4.dp).size(44.dp).background(Pal.accent),
                        contentAlignment = Alignment.Center
                    ) { Text(letter.lowercase(), color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Light) }
                }
                items(list, key = { it.app?.pkg ?: "sys" + it.sys }) { r ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {
                                    if (r.app != null) vm.launchApp(r.app.pkg)
                                    else if (r.sys == 1) vm.screen = Screen.Care
                                    else {
                                        vm.settingsPage = ""
                                        vm.screen = Screen.Settings
                                    }
                                },
                                onLongClick = { r.app?.let { vm.menuApp = it } }
                            )
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(Modifier.size(52.dp).background(Pal.accent), contentAlignment = Alignment.Center) {
                            if (r.app != null) AppIcon(r.app, 40.dp, dim = r.app.pkg in sleeping) else WpGlyph(r.sys, 30.dp)
                        }
                        Text(
                            r.label,
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Light,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Setelan

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WpAccentCard(vm: MainVm) {
    val cfg = vm.cfg
    if (cfg.uiMode != "wp") return
    SCard {
        Text("Warna aksen", color = Pal.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text("Dipakai untuk ubin, huruf grup, dan tombol di mode Windows Phone.", color = Pal.sub, fontSize = 12.sp)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            WP_COLORS.forEach { hex ->
                Box(
                    Modifier
                        .size(44.dp)
                        .background(parseColor(hex))
                        .then(if (cfg.wpAccent.equals(hex, true)) Modifier.border(3.dp, Color.White) else Modifier)
                        .clickable { cfg.wpAccent = hex }
                )
            }
        }
        Text("Di ubin: tahan lama untuk mengedit. Ketuk ubin lain untuk memindahkan ke posisi itu, \u2194 untuk mengubah ukuran.", color = Pal.sub, fontSize = 12.sp)
    }
}
