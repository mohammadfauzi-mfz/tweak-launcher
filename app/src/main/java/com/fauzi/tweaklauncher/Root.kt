package com.fauzi.tweaklauncher

import android.app.Activity
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

@Composable
fun Root(vm: MainVm) {
    val cfg = vm.cfg
    val addWidget = rememberWidgetAdder(vm)

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
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Box(Modifier.weight(1f)) {
                when (vm.screen) {
                    Screen.Home -> HomeScreen(vm)
                    Screen.Care -> CareScreen(vm)
                    Screen.Drawer -> DrawerScreen(vm)
                    Screen.Settings -> SettingsScreen(vm)
                }
            }
            NavBar(vm)
        }
        AppMenu(vm)
        FolderPickDialog(vm)
        ProfilePickDialog(vm)
        FolderDialog(vm)
        WidgetPickerDialog(vm, addWidget)
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
