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
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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

fun wpPalette(accentHex: String, light: Boolean = false) = if (light) Palette(
    bg = Color(0xFFFFFFFF), card = Color(0xFFF2F2F2), inset = Color(0xFFE6E6E6),
    line = Color(0xFFCCCCCC), text = Color(0xFF000000), soft = Color(0xFF222222), sub = Color(0xFF666666),
    accent = parseColor(accentHex, Color(0xFF1BA1E2)), onAccent = Color.White,
    radius = 0.dp, dark = false, wallpaper = false
) else Palette(
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
            Screen.Care -> WpPage("perawatan") { CareScreen(vm) }
            Screen.Settings -> WpPage("setelan") { SettingsScreen(vm) }
        }
    }
}

/** Halaman gaya pivot Windows Phone: nama aplikasi kecil huruf besar, judul besar huruf kecil. */
@Composable
private fun WpPage(title: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize().navigationBarsPadding().imePadding()) {
        Column(Modifier.padding(start = 20.dp, top = 14.dp, bottom = 2.dp)) {
            Text("TWEAK LAUNCHER", color = Pal.text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(title, color = Pal.text, fontSize = 50.sp, fontWeight = FontWeight.Light)
        }
        Box(Modifier.weight(1f)) { content() }
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

@Composable
private fun WpTileView(vm: MainVm, t: WpTile, sz: String, w: Dp, h: Dp, all: List<String>, bounds: MutableMap<String, Rect>) {
    val cfg = vm.cfg
    val ctx = LocalContext.current
    val density = LocalDensity.current.density
    val edit = vm.wpEdit
    val sel = edit == t.key
    val sleeping = vm.sleeping

    // Data hidup: jumlah notifikasi untuk aplikasi, baterai untuk Perawatan.
    val count = if (t.app != null && cfg.badgeOn && t.app.pkg !in Sys.list(cfg.badgeOffCsv)) (Badges.counts[t.app.pkg] ?: 0) else 0
    val live: String? = when {
        t.sys == 1 -> "baterai ${vm.stats.battery}%"
        count > 0 -> count.toString()
        else -> null
    }
    val flip = remember { Animatable(0f) }
    var back by remember { mutableStateOf(false) }
    LaunchedEffect(live != null, sz) {
        if (live == null || sz == "s") {
            back = false
            flip.snapTo(0f)
        } else {
            val seed = kotlin.math.abs(t.key.hashCode())
            delay(2000L + seed % 4000)
            while (true) {
                flip.animateTo(90f, tween(300))
                back = !back
                flip.snapTo(-90f)
                flip.animateTo(0f, tween(300))
                delay(5000L + seed % 3000)
            }
        }
    }

    // Efek miring saat ditekan, mengikuti titik sentuh.
    var pressed by remember { mutableStateOf(false) }
    var px by remember { mutableFloatStateOf(0f) }
    var py by remember { mutableFloatStateOf(0f) }
    val tx by animateFloatAsState(if (pressed) py * -9f else 0f, tween(120), label = "tiltX")
    val ty by animateFloatAsState(if (pressed) px * 9f else 0f, tween(120), label = "tiltY")
    val sc by animateFloatAsState(if (pressed) 0.96f else if (sel) 0.93f else 1f, tween(120), label = "scale")

    // Seret setelah tahan lama untuk memindahkan ubin.
    var dx by remember { mutableFloatStateOf(0f) }
    var dy by remember { mutableFloatStateOf(0f) }
    var drag by remember { mutableStateOf(false) }
    var start by remember { mutableStateOf(Offset.Zero) }

    Box(
        Modifier
            .size(w, h)
            .onGloballyPositioned { bounds[t.key] = it.boundsInRoot() }
            .zIndex(if (drag) 10f else 0f)
            .graphicsLayer {
                rotationX = flip.value + tx
                rotationY = ty
                scaleX = sc
                scaleY = sc
                translationX = dx
                translationY = dy
                cameraDistance = 14f * density
                alpha = if (edit != null && !sel) 0.55f else 1f
            }
            .background(Pal.accent)
            .then(if (sel) Modifier.border(1.dp, Color.White) else Modifier)
            .pointerInput(edit, t.key) {
                detectTapGestures(
                    onPress = { o ->
                        px = (o.x / size.width - 0.5f) * 2f
                        py = (o.y / size.height - 0.5f) * 2f
                        pressed = true
                        tryAwaitRelease()
                        pressed = false
                    },
                    onTap = {
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
                    }
                )
            }
            .pointerInput(t.key) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { o ->
                        vm.wpEdit = t.key
                        drag = true
                        dx = 0f
                        dy = 0f
                        val r = bounds[t.key]
                        start = if (r != null) Offset(r.left + o.x, r.top + o.y) else Offset.Zero
                    },
                    onDrag = { ch, d ->
                        ch.consume()
                        dx += d.x
                        dy += d.y
                    },
                    onDragEnd = {
                        val pos = Offset(start.x + dx, start.y + dy)
                        val target = bounds.entries.firstOrNull { it.key != t.key && it.value.contains(pos) }?.key
                        if (target != null) wpMove(vm, all, t.key, target)
                        dx = 0f
                        dy = 0f
                        drag = false
                    },
                    onDragCancel = {
                        dx = 0f
                        dy = 0f
                        drag = false
                    }
                )
            }
    ) {
        // Isi ubin; sisi belakang ikut terbalik agar tidak tampak cermin.
        val ic = minOf(w, h) * (if (sz == "s") 0.6f else 0.46f)
        Box(Modifier.fillMaxSize().graphicsLayer { rotationX = 0f }) {
            if (back && live != null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            if (t.sys == 1) "${vm.stats.battery}%" else live,
                            color = Color.White,
                            fontSize = if (sz == "w") 40.sp else 34.sp,
                            fontWeight = FontWeight.Light
                        )
                        Text(
                            if (t.sys == 1) "baterai" else if (count == 1) "notifikasi" else "notifikasi",
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                Box(Modifier.align(Alignment.Center)) {
                    when {
                        t.sys != 0 -> WpGlyph(t.sys, ic)
                        t.app != null -> WpAppIcon(vm, t.app, ic, t.app.pkg in sleeping)
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
        if (count > 0 && sz != "s" && !back) {
            Text(
                if (count > 99) "99+" else count.toString(),
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Light,
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
            )
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

/** Ikon aplikasi di ubin: siluet putih jika tersedia, jika tidak ikon aslinya (atau ikon pilihan Anda). */
@Composable
private fun WpAppIcon(vm: MainVm, app: AppItem, size: Dp, dim: Boolean) {
    val ctx = LocalContext.current
    val density = LocalDensity.current.density
    val custom = vm.cfg.appIcon(app.pkg)
    val mono = remember(app.pkg, size, custom) {
        if (custom == null) WpIcons.mono(ctx, app.pkg, (size.value * density).toInt().coerceAtLeast(16)) else null
    }
    if (mono != null) {
        Image(mono, null, Modifier.size(size).alpha(if (dim) 0.45f else 1f))
    } else {
        AppIcon(app, size, badge = false, dim = dim)
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
private fun WpSlotView(vm: MainVm, slot: WpSlot, half: Dp, u: Dp, gap: Dp, all: List<String>, bounds: MutableMap<String, Rect>) {
    when (slot) {
        is WpSlot.W -> WpTileView(vm, slot.t, "w", half * 2 + gap, half, all, bounds)
        is WpSlot.M -> WpTileView(vm, slot.t, "m", half, half, all, bounds)
        is WpSlot.S -> Box(Modifier.size(half)) {
            slot.ts.chunked(2).forEachIndexed { r, pair ->
                pair.forEachIndexed { c, t ->
                    Box(Modifier.offset(x = (u + gap) * c, y = (u + gap) * r)) { WpTileView(vm, t, "s", u, u, all, bounds) }
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
    val bounds = remember { mutableMapOf<String, Rect>() }

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
                val rowKeys = row.flatMap { sl ->
                    when (sl) {
                        is WpSlot.W -> listOf(sl.t.key)
                        is WpSlot.M -> listOf(sl.t.key)
                        is WpSlot.S -> sl.ts.map { it.key }
                    }
                }
                Box(Modifier.zIndex(if (vm.wpEdit != null && vm.wpEdit in rowKeys) 5f else 0f)) {
                if (row.size == 1 && row[0] is WpSlot.W) {
                    WpSlotView(vm, row[0], half, u, gap, all, bounds)
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                        row.forEach { WpSlotView(vm, it, half, u, gap, all, bounds) }
                    }
                }
                }
            }
            item {
                Box(Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.CenterEnd) {
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .border(2.dp, Pal.text, CircleShape)
                            .clickable { vm.screen = Screen.Drawer },
                        contentAlignment = Alignment.Center
                    ) { Text("\u2192", color = Pal.text, fontSize = 22.sp) }
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
    var jump by remember { mutableStateOf(false) }
    val sleeping = vm.sleeping
    val src = vm.appsForTab("all")
    val rows = (src.map { WpRow(it.label, it, 0) } + WpRow("Perawatan", null, 1) + WpRow("Setelan", null, 2))
        .filter { q.isBlank() || it.label.contains(q, ignoreCase = true) }
        .sortedBy { it.label.lowercase() }
    val grouped = rows.groupBy { r ->
        val c = r.label.firstOrNull()?.uppercaseChar() ?: '#'
        if (c.isLetter()) c else '#'
    }.entries.sortedBy { if (it.key == '#') '{' else it.key }

    // posisi header tiap huruf di dalam daftar
    val headIdx = HashMap<Char, Int>()
    var run = 0
    grouped.forEach { (l, list) ->
        headIdx[l] = run
        run += 1 + list.size
    }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(8.dp))
            InputBox(q, { q = it }, "search", Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))
            LazyColumn(Modifier.weight(1f), state = listState) {
                grouped.forEach { (letter, list) ->
                    item(key = "h$letter") {
                        Box(
                            Modifier
                                .padding(top = 10.dp, bottom = 4.dp)
                                .size(44.dp)
                                .background(Pal.accent)
                                .clickable { jump = true },
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
                                if (r.app != null) WpAppIcon(vm, r.app, 36.dp, r.app.pkg in sleeping) else WpGlyph(r.sys, 30.dp)
                            }
                            Text(
                                r.label,
                                color = Pal.text,
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
        if (jump) {
            // Papan lompat huruf: huruf yang ada berwarna aksen, yang tidak ada redup.
            val letters = ('A'..'Z').toList() + '#'
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Pal.bg.copy(alpha = 0.96f))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { jump = false }
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(letters) { c ->
                        val has = headIdx.containsKey(c)
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .then(if (has) Modifier.background(Pal.accent) else Modifier.background(Pal.inset))
                                .then(
                                    if (has) Modifier.clickable {
                                        jump = false
                                        scope.launch { listState.animateScrollToItem(headIdx[c] ?: 0) }
                                    } else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                c.lowercase(),
                                color = if (has) Color.White else Pal.sub,
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Light
                            )
                        }
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
        Text("Tema Windows Phone", color = Pal.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        ChoiceRow("Latar", listOf("dark" to "Gelap", "light" to "Terang"), if (cfg.wpLight) "light" else "dark") { cfg.wpLight = it == "light" }
        Text("Warna aksen", color = Pal.text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
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
