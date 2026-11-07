package com.fauzi.tweaklauncher

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Isi desktop: ikon sistem (Komputer Saya dan sebagainya) atau satu entri Beranda (aplikasi atau folder). */
sealed class DeskItem {
    class Sys(val label: String, val kind: Int, val onClick: () -> Unit) : DeskItem()
    class Ent(val e: HomeEntry) : DeskItem()
}

/**
 * Ikon desktop tersusun ke bawah di sisi kiri, seperti desktop Windows dan Mac asli.
 * Jika kolom penuh, ikon berikutnya pindah ke kolom di kanannya.
 */
@Composable
fun DesktopIcons(
    vm: MainVm,
    system: List<DeskItem.Sys>,
    modifier: Modifier,
    shortcut: @Composable (String, Int, Modifier, () -> Unit) -> Unit
) {
    val sleeping = vm.sleeping
    val items: List<DeskItem> = system + vm.homeEntries.map { DeskItem.Ent(it) }
    val itemH = vm.cfg.iconSize.dp + 58.dp
    BoxWithConstraints(modifier) {
        val rows = ((maxHeight - 16.dp) / itemH).toInt().coerceAtLeast(1)
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items.chunked(rows).forEach { col ->
                Column(Modifier.width(86.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    col.forEach { item ->
                        when (item) {
                            is DeskItem.Sys -> shortcut(item.label, item.kind, Modifier.fillMaxWidth(), item.onClick)
                            is DeskItem.Ent -> when (val e = item.e) {
                                is HomeEntry.A -> AppTile(e.app, Modifier.fillMaxWidth(), vm, e.app.pkg in sleeping)
                                is HomeEntry.F -> FolderTile(e.name, e.apps, Modifier.fillMaxWidth(), vm)
                            }
                        }
                    }
                }
            }
        }
    }
}
