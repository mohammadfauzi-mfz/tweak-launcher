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

// ---------------------------------------------------------------- Game Hub

@Composable
fun GameScreen(vm: MainVm) {
    val st = vm.stats
    val games = vm.gameApps
    val last = games.firstOrNull { it.pkg == vm.cfg.lastGame } ?: games.firstOrNull()

    LaunchedEffect(Unit) {
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
            Text("Game Hub", color = Pal.text, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text("Profil aktif: ${vm.profile.label}", color = Pal.sub, fontSize = 14.sp)
        }

        if (last == null) {
            SCard {
                Text("Belum ada game", color = Pal.text, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Text(
                    "Buka tab Aplikasi, tahan lama sebuah aplikasi, lalu pilih Tandai sebagai game.",
                    color = Pal.soft,
                    fontSize = 13.sp
                )
            }
        } else {
            SCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    AppIcon(last, 64.dp)
                    Column {
                        Text("Terakhir dimainkan", color = Pal.sub, fontSize = 12.sp)
                        Text(
                            last.label,
                            color = Pal.text,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip("Layar", "${st.hz} Hz", Modifier.weight(1f))
                    Chip("Suhu", st.tempText, Modifier.weight(1f))
                    Chip("RAM", st.ramText, Modifier.weight(1f))
                }
                ActionButton("Boost dan Main", Modifier.fillMaxWidth(), primary = true) {
                    vm.launchApp(last.pkg)
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Game kamu", color = Pal.text, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Text("Boost per game", color = Pal.sub, fontSize = 12.sp)
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
    var filter by remember { mutableIntStateOf(0) }
    val games = vm.games
    val sleeping = vm.sleeping
    val hidden = vm.hidden
    val pins = Sys.list(cfg.pinnedCsv)

    val source = if (filter == 4) vm.apps.filter { it.pkg in hidden } else vm.visibleApps
    val list = source.filter { a ->
        (q.isBlank() || a.label.contains(q, ignoreCase = true)) && when (filter) {
            1 -> a.pkg in games
            2 -> a.pkg in pins || "app:${a.pkg}" in pins
            3 -> a.pkg in sleeping
            else -> true
        }
    }
    val searchShape = CircleShape
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    Column(
        Modifier.fillMaxSize().padding(start = 20.dp, end = 20.dp, top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Aplikasi", color = Pal.text, fontSize = 30.sp, fontWeight = FontWeight.Bold)

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

        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Semua", "Game", "Tersemat", "Tidur", "Tersembunyi").forEachIndexed { i, label ->
                Box(
                    Modifier
                        .height(36.dp)
                        .clip(CircleShape)
                        .background(if (filter == i) Pal.accent else Pal.card)
                        .border(1.dp, if (filter == i) Pal.accent else Pal.line, CircleShape)
                        .clickable { filter = i }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        color = if (filter == i) Pal.onAccent else Pal.soft,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

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
                            if (a.pkg in games) Tag("Game")
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
    }
}
