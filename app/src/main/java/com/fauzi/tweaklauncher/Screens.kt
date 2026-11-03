package com.fauzi.tweaklauncher

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
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

// ---------------------------------------------------------------- Akar

@Composable
fun Root(vm: MainVm) {
    BackHandler(enabled = vm.screen != Screen.Home) { vm.screen = Screen.Home }

    Box(Modifier.fillMaxSize().background(Bg).statusBarsPadding()) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                when (vm.screen) {
                    Screen.Home -> HomeScreen(vm)
                    Screen.Game -> GameScreen(vm)
                    Screen.Drawer -> DrawerScreen(vm)
                }
            }
            NavBar(vm)
        }
        AppMenu(vm)
    }
}

@Composable
fun NavBar(vm: MainVm) {
    val shape = RoundedCornerShape(30.dp)
    Row(
        Modifier
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .fillMaxWidth()
            .height(60.dp)
            .clip(shape)
            .background(CardBg)
            .border(1.dp, Line, shape)
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        listOf(
            Screen.Home to "Beranda",
            Screen.Game to "Game",
            Screen.Drawer to "Aplikasi"
        ).forEach { (s, label) ->
            Pill(label, vm.screen == s, Modifier.weight(1f).fillMaxHeight()) { vm.screen = s }
        }
    }
}

@Composable
fun Pill(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(24.dp))
            .background(if (selected) Lime else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = if (selected) Ink else TextSoft,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
        )
    }
}

// ---------------------------------------------------------------- Beranda

@Composable
fun HomeScreen(vm: MainVm) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            vm.refreshStats()
            delay(5000)
        }
    }
    val loc = remember { Locale("id", "ID") }
    val time = remember(now) { SimpleDateFormat("HH:mm", loc).format(Date(now)) }
    val date = remember(now) { SimpleDateFormat("EEEE, d MMMM", loc).format(Date(now)) }
    val profile = PROFILES.first { it.id == vm.profileId }
    val st = vm.stats

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Column {
                Text(time, color = TextC, fontSize = 60.sp, fontWeight = FontWeight.Light)
                Text(date, color = Sub, fontSize = 14.sp)
            }
            Row(
                Modifier
                    .height(32.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardBg)
                    .border(1.dp, Line, RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(Lime))
                Text("Mode ${profile.label}", color = TextC, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }

        if (vm.shizuku != ShizukuState.READY) SetupCard(vm)

        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(CardBg)
                .border(1.dp, Line, RoundedCornerShape(28.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Profil performa", color = TextC, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Text(
                    if (vm.shizuku == ShizukuState.READY) "Shizuku aktif" else "Shizuku mati",
                    color = Sub,
                    fontSize = 12.sp
                )
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(Inset)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                PROFILES.forEach { p ->
                    Pill(p.label, p.id == vm.profileId, Modifier.weight(1f).fillMaxHeight()) {
                        if (!vm.busy) vm.setProfile(p.id)
                    }
                }
            }
            Text(
                if (vm.busy) "Menerapkan profil..." else profile.desc,
                color = TextSoft,
                fontSize = 13.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatBar("RAM", st.ramText, st.ramPct, Lime, Modifier.weight(1f))
                StatBar("Baterai", "${st.battery}%", st.battery / 100f, Green, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatBar("Suhu", st.tempText, st.tempPct, Orange, Modifier.weight(1f))
                StatBar("Penyimpanan", "${(st.storagePct * 100).toInt()}%", st.storagePct, Blue, Modifier.weight(1f))
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            vm.homeApps.chunked(4).forEach { row ->
                Row(Modifier.fillMaxWidth()) {
                    row.forEach { app -> AppTile(app, Modifier.weight(1f), vm) }
                    repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
fun StatBar(label: String, value: String, pct: Float, color: Color, modifier: Modifier = Modifier) {
    Column(modifier) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Sub, fontSize = 12.sp)
            Text(value, color = TextC, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(6.dp))
        Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(Line)) {
            Box(
                Modifier
                    .fillMaxWidth(pct.coerceIn(0.02f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp))
                    .background(color)
            )
        }
    }
}

@Composable
fun SetupCard(vm: MainVm) {
    val needPerm = vm.shizuku == ShizukuState.NEED_PERM
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(CardBg)
            .border(1.dp, Lime.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            if (needPerm) "Izin Shizuku diperlukan" else "Shizuku belum aktif",
            color = TextC, fontSize = 16.sp, fontWeight = FontWeight.Medium
        )
        Text(
            if (needPerm) "Beri izin supaya profil performa bisa diterapkan."
            else "Pasang dan jalankan Shizuku lewat Wireless debugging, lalu kembali ke sini.",
            color = TextSoft, fontSize = 13.sp
        )
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier
                .height(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Lime)
                .clickable { if (needPerm) vm.requestPerm() else vm.openShizuku() }
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(if (needPerm) "Beri izin" else "Buka Shizuku", color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
    }
}

// ---------------------------------------------------------------- Ikon aplikasi

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppTile(app: AppItem, modifier: Modifier, vm: MainVm) {
    Column(
        modifier
            .combinedClickable(
                onClick = { vm.launch(app.pkg, boost = app.pkg in vm.games && app.pkg !in vm.boostOff) },
                onLongClick = { vm.menuApp = app }
            )
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(app.icon, app.label, Modifier.size(58.dp))
        Spacer(Modifier.height(6.dp))
        Text(
            app.label,
            color = TextSoft,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun AppMenu(vm: MainVm) {
    val a = vm.menuApp ?: return
    val isGame = a.pkg in vm.games
    val isPinned = a.pkg in vm.pinned
    AlertDialog(
        onDismissRequest = { vm.menuApp = null },
        containerColor = CardBg,
        title = { Text(a.label, color = TextC) },
        text = {
            Column {
                MenuItem(if (isGame) "Hapus dari Game" else "Tandai sebagai game") {
                    vm.toggleGame(a.pkg); vm.menuApp = null
                }
                MenuItem(if (isPinned) "Lepas dari Beranda" else "Sematkan ke Beranda") {
                    vm.togglePin(a.pkg); vm.menuApp = null
                }
                MenuItem("Optimalkan (compile speed)") {
                    vm.optimize(a.pkg); vm.menuApp = null
                }
                MenuItem("Hentikan paksa") {
                    vm.forceStop(a.pkg); vm.menuApp = null
                }
                MenuItem("Info aplikasi") {
                    vm.openInfo(a.pkg); vm.menuApp = null
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { vm.menuApp = null }) { Text("Tutup", color = Lime) }
        }
    )
}

@Composable
fun MenuItem(label: String, onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 14.dp)) {
        Text(label, color = TextSoft, fontSize = 15.sp)
    }
}

// ---------------------------------------------------------------- Game Hub

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GameScreen(vm: MainVm) {
    val st = vm.stats
    val games = vm.gameApps
    val last = games.firstOrNull { it.pkg == vm.lastGame } ?: games.firstOrNull()
    val profile = PROFILES.first { it.id == vm.profileId }

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
            Text("Game Hub", color = TextC, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text("Profil aktif: ${profile.label}", color = Sub, fontSize = 14.sp)
        }

        if (last == null) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(CardBg)
                    .border(1.dp, Line, RoundedCornerShape(28.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Belum ada game", color = TextC, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Text(
                    "Buka tab Aplikasi, tahan lama sebuah aplikasi, lalu pilih Tandai sebagai game.",
                    color = TextSoft, fontSize = 13.sp
                )
            }
        } else {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(CardBg)
                    .border(1.dp, Line, RoundedCornerShape(28.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Image(last.icon, last.label, Modifier.size(64.dp))
                    Column {
                        Text("Terakhir dimainkan", color = Sub, fontSize = 12.sp)
                        Text(last.label, color = TextC, fontSize = 20.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip("Baterai", "${st.battery}%", Modifier.weight(1f))
                    Chip("Suhu", st.tempText, Modifier.weight(1f))
                    Chip("RAM", st.ramText, Modifier.weight(1f))
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(Lime)
                        .clickable { vm.launch(last.pkg, boost = true) },
                    contentAlignment = Alignment.Center
                ) {
                    Text("Boost dan Main", color = Ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Game kamu", color = TextC, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Text("Boost per game", color = Sub, fontSize = 12.sp)
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

@Composable
fun Chip(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Inset)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(label, color = Sub, fontSize = 11.sp)
        Text(value, color = TextC, fontSize = 17.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GameCard(g: AppItem, vm: MainVm, modifier: Modifier) {
    val on = g.pkg !in vm.boostOff
    Column(
        modifier
            .clip(RoundedCornerShape(24.dp))
            .background(CardBg)
            .border(1.dp, Line, RoundedCornerShape(24.dp))
            .combinedClickable(
                onClick = { vm.launch(g.pkg, boost = on) },
                onLongClick = { vm.menuApp = g }
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Image(g.icon, g.label, Modifier.size(48.dp))
        Text(g.label, color = TextC, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Box(
            Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(if (on) Lime else Color.Transparent)
                .border(1.dp, if (on) Lime else Color(0xFF2E3340), RoundedCornerShape(20.dp))
                .clickable { vm.toggleBoost(g.pkg) },
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (on) "Boost aktif" else "Boost mati",
                color = if (on) Ink else TextSoft,
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
    var q by remember { mutableStateOf("") }
    var filter by remember { mutableIntStateOf(0) }
    val list = vm.apps.filter { a ->
        (q.isBlank() || a.label.contains(q, ignoreCase = true)) && when (filter) {
            1 -> a.pkg in vm.games
            2 -> a.pkg in vm.pinned
            else -> true
        }
    }
    val searchShape = RoundedCornerShape(26.dp)

    Column(
        Modifier.fillMaxSize().padding(start = 20.dp, end = 20.dp, top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Aplikasi", color = TextC, fontSize = 30.sp, fontWeight = FontWeight.Bold)

        BasicTextField(
            value = q,
            onValueChange = { q = it },
            singleLine = true,
            textStyle = TextStyle(color = TextC, fontSize = 15.sp),
            cursorBrush = SolidColor(Lime),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(searchShape)
                .background(CardBg)
                .border(1.dp, Line, searchShape)
                .padding(horizontal = 20.dp),
            decorationBox = { inner ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                    if (q.isEmpty()) Text("Cari di ${vm.apps.size} aplikasi", color = Sub, fontSize = 15.sp)
                    inner()
                }
            }
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Semua", "Game", "Tersemat").forEachIndexed { i, label ->
                Box(
                    Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (filter == i) Lime else CardBg)
                        .border(1.dp, if (filter == i) Lime else Line, RoundedCornerShape(18.dp))
                        .clickable { filter = i }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        color = if (filter == i) Ink else TextSoft,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        LazyColumn(Modifier.weight(1f)) {
            items(list, key = { it.pkg }) { a ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .combinedClickable(
                            onClick = { vm.launch(a.pkg, boost = a.pkg in vm.games && a.pkg !in vm.boostOff) },
                            onLongClick = { vm.menuApp = a }
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Image(a.icon, a.label, Modifier.size(48.dp))
                    Column(Modifier.weight(1f)) {
                        Text(a.label, color = TextC, fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(a.pkg, color = Sub, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    if (a.pkg in vm.games) Badge("Game")
                    if (a.pkg in vm.pinned) Badge("Beranda")
                }
            }
        }
    }
}

@Composable
fun Badge(text: String) {
    Box(
        Modifier
            .height(26.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(Blue.copy(alpha = 0.16f))
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Blue, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}
