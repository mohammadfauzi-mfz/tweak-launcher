package com.fauzi.tweaklauncher

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ---------------------------------------------------------------- Perawatan

private fun healthScore(st: Stats): Triple<Int, String, androidx.compose.ui.graphics.Color> {
    val tempC = st.tempPct * 50f
    val pRam = (st.ramPct - 0.65f).coerceAtLeast(0f) / 0.35f * 30f
    val pSto = (st.storagePct - 0.70f).coerceAtLeast(0f) / 0.30f * 35f
    val pTemp = (tempC - 35f).coerceAtLeast(0f) / 15f * 35f
    val score = (100f - pRam - pSto - pTemp).coerceIn(0f, 100f).toInt()
    val worst = maxOf(pRam, pSto, pTemp)
    val hint = when {
        worst < 3f && st.battery in 1..19 -> "Baterai rendah, colokkan charger."
        worst < 3f -> "Semuanya normal."
        worst == pSto -> "Penyimpanan hampir penuh. Bersihkan cache."
        worst == pRam -> "RAM terpakai banyak. Tutup aplikasi yang tidak perlu."
        else -> "HP terasa panas. Istirahatkan sebentar."
    }
    val color = when {
        score >= 80 -> Green
        score >= 55 -> Orange
        else -> Red
    }
    return Triple(score, hint, color)
}

@Composable
fun CareScreen(vm: MainVm) {
    val st = vm.stats
    val games = vm.gameApps
    val sl = vm.sleeping
    val (score, hint, scoreColor) = healthScore(st)
    val label = when {
        score >= 80 -> "Sehat"
        score >= 55 -> "Cukup"
        else -> "Perlu perhatian"
    }

    LaunchedEffect(Unit) {
        vm.loadUsage()
        while (true) {
            vm.refreshStats()
            delay(5000)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Column {
            Text("Perawatan", color = Pal.text, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text("Profil aktif: ${vm.profile.label}", color = Pal.sub, fontSize = 14.sp)
        }

        SCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("$score", color = scoreColor, fontSize = 56.sp, fontWeight = FontWeight.Light)
                Column(Modifier.weight(1f)) {
                    Text(label, color = Pal.text, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                    Text(hint, color = Pal.soft, fontSize = 13.sp)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatBar("RAM", st.ramText, st.ramPct, Pal.accent, Modifier.weight(1f))
                StatBar("Penyimpanan", "${(st.storagePct * 100).toInt()}%", st.storagePct, Blue, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatBar("Suhu", st.tempText, st.tempPct, Orange, Modifier.weight(1f))
                StatBar("Baterai", "${st.battery}%", st.battery / 100f, Green, Modifier.weight(1f))
            }
        }

        SCard {
            Text("Alat cepat", color = Pal.text, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            ActionButton("Bersihkan cache", Modifier.fillMaxWidth(), primary = true) {
                vm.runTool("Membersihkan cache") { Tweaks.trimCaches() }
            }
            ActionButton("Optimalkan semua aplikasi", Modifier.fillMaxWidth()) {
                vm.runTool("Menjalankan optimasi") { Tweaks.dexoptAll() }
            }
            ActionButton("Cari aplikasi jarang dipakai", Modifier.fillMaxWidth()) { vm.loadUnused(14) }
            if (vm.unused.isNotEmpty()) {
                Text(
                    vm.unused.take(8).joinToString(", ") { it.label } + if (vm.unused.size > 8) ", ..." else "",
                    color = Pal.soft,
                    fontSize = 13.sp
                )
                ActionButton("Tidurkan semua (${vm.unused.size})", Modifier.fillMaxWidth(), primary = true) {
                    vm.sleepAll(vm.unused)
                }
            }
            if (sl.isNotEmpty()) {
                ActionButton("Bangunkan semua (${sl.size})", Modifier.fillMaxWidth()) { vm.wakeAll() }
            }
        }

        FirewallCard(vm)

        AdsBlockCard(vm)

        if (!vm.usageOk) {
            SCard {
                Text("Favorit dan terakhir dipakai", color = Pal.text, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Text(
                    "Beri akses penggunaan supaya launcher bisa menampilkan aplikasi yang paling sering dan terakhir kamu pakai.",
                    color = Pal.soft,
                    fontSize = 13.sp
                )
                ActionButton("Beri akses penggunaan", Modifier.fillMaxWidth(), primary = true) { vm.openUsageAccess() }
            }
        } else {
            if (vm.recentApps.isNotEmpty()) {
                SCard {
                    Text("Terakhir dipakai", color = Pal.text, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Row(Modifier.horizontalScroll(rememberScrollState())) {
                        vm.recentApps.forEach { a -> AppTile(a, Modifier.width(76.dp), vm, a.pkg in sl) }
                    }
                }
            }
            if (vm.oftenApps.isNotEmpty()) {
                SCard {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Paling sering dipakai", color = Pal.text, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Text("7 hari", color = Pal.sub, fontSize = 12.sp)
                    }
                    Row(Modifier.horizontalScroll(rememberScrollState())) {
                        vm.oftenApps.forEach { a -> AppTile(a, Modifier.width(76.dp), vm, a.pkg in sl) }
                    }
                }
            }
            if (vm.recentApps.isEmpty() && vm.oftenApps.isEmpty()) {
                SCard {
                    Text("Belum ada data pemakaian", color = Pal.text, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Text("Pakai beberapa aplikasi dulu, daftar akan terisi sendiri.", color = Pal.soft, fontSize = 13.sp)
                }
            }
        }

        if (games.isNotEmpty()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Aplikasi Boost", color = Pal.text, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Text("Profil Boost otomatis", color = Pal.sub, fontSize = 12.sp)
            }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                games.chunked(2).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEach { g -> GameCard(g, vm, Modifier.weight(1f)) }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GameCard(g: AppItem, vm: MainVm, modifier: Modifier) {
    val on = g.pkg !in vm.boostOff
    Column(
        modifier
            .clip(Pal.shape)
            .background(Pal.card)
            .border(1.dp, Pal.line, Pal.shape)
            .combinedClickable(
                onClick = { vm.launchApp(g.pkg) },
                onLongClick = { vm.menuApp = g }
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AppIcon(g, 48.dp)
        Text(
            g.label,
            color = Pal.text,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clip(CircleShape)
                .background(if (on) Pal.accent else Color.Transparent)
                .border(1.dp, if (on) Pal.accent else Pal.line, CircleShape)
                .clickable { vm.toggleBoost(g.pkg) },
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (on) "Boost aktif" else "Boost mati",
                color = if (on) Pal.onAccent else Pal.soft,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ---------------------------------------------------------------- Daftar Aplikasi

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DrawerScreen(vm: MainVm) {
    val cfg = vm.cfg
    var q by remember { mutableStateOf("") }
    val games = vm.games
    val sleeping = vm.sleeping
    val tab = vm.currentTab()
    val list = vm.appsForTab(tab).filter { a -> q.isBlank() || a.label.contains(q, ignoreCase = true) }
    val searchShape = CircleShape
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    Column(
        Modifier.fillMaxSize().padding(start = 20.dp, end = 20.dp, top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Aplikasi",
                color = Pal.text,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Pal.card)
                    .border(1.dp, Pal.line, CircleShape)
                    .clickable {
                        vm.settingsPage = ""
                        vm.screen = Screen.Settings
                    },
                contentAlignment = Alignment.Center
            ) {
                GearIcon(Pal.text, 22.dp)
            }
        }

        // Daftar di tengah; filter dan pencarian di bawah supaya dekat jempol.
        if (cfg.drawerStyle == "grid") {
            LazyVerticalGrid(
                columns = GridCells.Fixed(cfg.drawerCols.coerceIn(3, 6)),
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(list, key = { it.pkg }) { a ->
                    AppTile(a, Modifier, vm, a.pkg in sleeping)
                }
            }
        } else {
            val letters = list.map { it.label.firstOrNull()?.uppercaseChar() ?: '#' }.distinct()
            Box(Modifier.weight(1f)) {
                LazyColumn(Modifier.fillMaxSize(), state = listState) {
                    items(list, key = { it.pkg }) { a ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .combinedClickable(
                                    onClick = { vm.launchApp(a.pkg) },
                                    onLongClick = { vm.menuApp = a }
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            AppIcon(a, 48.dp, dim = a.pkg in sleeping)
                            Column(Modifier.weight(1f)) {
                                Text(
                                    a.label,
                                    color = Pal.text,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(a.pkg, color = Pal.sub, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            if (a.pkg in games) Tag("Boost")
                            if (a.pkg in sleeping) Tag("Tidur", Orange)
                        }
                    }
                }
                if (q.isBlank() && list.size > 25) {
                    Column(
                        Modifier.align(Alignment.CenterEnd).padding(end = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        letters.forEach { ch ->
                            Text(
                                ch.toString(),
                                color = Pal.sub,
                                fontSize = 10.sp,
                                modifier = Modifier
                                    .clickable {
                                        val idx = list.indexOfFirst {
                                            (it.label.firstOrNull()?.uppercaseChar() ?: '#') == ch
                                        }
                                        if (idx >= 0) scope.launch { listState.scrollToItem(idx) }
                                    }
                                    .size(width = 22.dp, height = 15.dp)
                            )
                        }
                    }
                }
            }
        }

        AppTabsRow(vm)

        BasicTextField(
            value = q,
            onValueChange = { q = it },
            singleLine = true,
            textStyle = TextStyle(color = Pal.text, fontSize = 15.sp),
            cursorBrush = SolidColor(Pal.accent),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(searchShape)
                .background(Pal.card)
                .border(1.dp, Pal.line, searchShape)
                .padding(horizontal = 20.dp),
            decorationBox = { inner ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                    if (q.isEmpty()) Text("Cari di ${vm.visibleApps.size} aplikasi", color = Pal.sub, fontSize = 15.sp)
                    inner()
                }
            }
        )
    }
}

@Composable
fun GearIcon(color: androidx.compose.ui.graphics.Color, size: androidx.compose.ui.unit.Dp) {
    androidx.compose.foundation.Canvas(Modifier.size(size)) {
        val c = center
        val r = this.size.minDimension / 2f
        val stroke = r * 0.28f
        for (i in 0 until 8) {
            val ang = Math.toRadians(i * 45.0)
            val dx = Math.cos(ang).toFloat()
            val dy = Math.sin(ang).toFloat()
            drawLine(
                color,
                androidx.compose.ui.geometry.Offset(c.x + dx * r * 0.62f, c.y + dy * r * 0.62f),
                androidx.compose.ui.geometry.Offset(c.x + dx * r * 0.95f, c.y + dy * r * 0.95f),
                strokeWidth = stroke * 1.15f,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }
        drawCircle(color, radius = r * 0.62f - stroke / 2f, center = c, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke))
        drawCircle(color, radius = r * 0.2f, center = c)
    }
}
