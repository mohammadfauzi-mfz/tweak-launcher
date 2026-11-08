package com.fauzi.tweaklauncher

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

@Composable
fun Root(vm: MainVm) {
    val cfg = vm.cfg
    val addWidget = rememberWidgetAdder(vm)

    // Statistik (baterai di bilah tugas, dll.) diperbarui di semua mode dan layar.
    androidx.compose.runtime.LaunchedEffect(Unit) {
        while (true) {
            vm.refreshStats()
            kotlinx.coroutines.delay(5000)
        }
    }

    BackHandler(enabled = vm.screen != Screen.Home) {
        if (vm.screen == Screen.Settings && vm.settingsPage.isNotEmpty()) {
            vm.settingsPage = ""
        } else {
            vm.screen = Screen.Home
        }
    }

    val dark = Pal.dark
    val view = LocalView.current
    SideEffect {
        val w = (view.context as? Activity)?.window
        if (w != null) {
            val c = WindowCompat.getInsetsController(w, view)
            c.isAppearanceLightStatusBars = !dark
            c.isAppearanceLightNavigationBars = !dark
        }
    }

    val wall = Pal.wallpaper
    Box(Modifier.fillMaxSize().background(if (wall) Color.Transparent else Pal.bg)) {
        if (wall) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background((if (dark) Color.Black else Color.White).copy(alpha = cfg.dim / 100f))
            )
        }
        if (cfg.uiMode == "pda") PdaFrame(vm) else if (cfg.uiMode == "win95") W95Frame(vm) else if (cfg.uiMode == "xp") XpFrame(vm) else if (cfg.uiMode == "dos") DosFrame(vm) else if (cfg.uiMode == "mac") MacFrame(vm) else if (cfg.uiMode == "wp") WpFrame(vm) else if (cfg.uiMode == "sym") SymFrame(vm) else Column(Modifier.fillMaxSize().statusBarsPadding().imePadding()) {
            Box(
                Modifier
                    .weight(1f)
                    .swipes { d, consumed ->
                        // Geser kanan kiri untuk pindah tab: Beranda, Perawatan, Aplikasi.
                        val tabs = listOf(Screen.Home, Screen.Care, Screen.Drawer)
                        val i = tabs.indexOf(vm.screen)
                        if (i >= 0 && !consumed && kotlin.math.abs(d.x) > 120f && kotlin.math.abs(d.x) > kotlin.math.abs(d.y) * 2f) {
                            val n = if (d.x < 0) i + 1 else i - 1
                            if (n in tabs.indices && vm.menuApp == null) vm.screen = tabs[n]
                        }
                    }
            ) {
                AnimatedContent(
                    targetState = vm.screen,
                    transitionSpec = {
                        val fwd = targetState.ordinal > initialState.ordinal
                        (slideInHorizontally(tween(320, easing = FastOutSlowInEasing)) { w -> if (fwd) w / 3 else -w / 3 } +
                            fadeIn(tween(320))) togetherWith
                            (slideOutHorizontally(tween(320, easing = FastOutSlowInEasing)) { w -> if (fwd) -w / 3 else w / 3 } +
                                fadeOut(tween(180)))
                    },
                    label = "tab"
                ) { s ->
                    when (s) {
                        Screen.Home -> HomeScreen(vm)
                        Screen.Care -> CareScreen(vm)
                        Screen.Drawer -> DrawerScreen(vm)
                        Screen.Settings -> SettingsScreen(vm)
                    }
                }
            }
            // Saat keyboard terbuka, bar bawah disembunyikan supaya kolom cari tepat di atas keyboard.
            val imeOpen = WindowInsets.ime.getBottom(LocalDensity.current) > 0
            if (!imeOpen) NavBar(vm)
        }
        AppMenu(vm)
        FolderPickDialog(vm)
        ProfilePickDialog(vm)
        FolderDialog(vm)
        WidgetPickerDialog(vm, addWidget)
        GroupDialogs(vm)
        IconPickerDialog(vm)
        FirewallDialog(vm)
        UpdateDialog(vm)
        RevertDialog(vm)
    }
}

@Composable
fun NavBar(vm: MainVm) {
    Row(
        Modifier
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .fillMaxWidth()
            .height(60.dp)
            .clip(CircleShape)
            .background(Pal.card)
            .border(1.dp, Pal.line, CircleShape)
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        listOf(
            Screen.Home to "Beranda",
            Screen.Care to "Perawatan",
            Screen.Drawer to "Aplikasi"
        ).forEach { (s, label) ->
            Pill(label, vm.screen == s, Modifier.weight(1f).fillMaxHeight()) {
                vm.screen = s
            }
        }
    }
}
