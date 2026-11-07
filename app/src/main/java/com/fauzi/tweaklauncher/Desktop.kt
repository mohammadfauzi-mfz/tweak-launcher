package com.fauzi.tweaklauncher

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Isi desktop: ikon sistem (Komputer Saya dan sebagainya) atau satu entri Beranda (aplikasi atau folder). */
sealed class DeskItem {
    class Sys(val label: String, val kind: Int, val onClick: () -> Unit) : DeskItem()
    class Ent(val e: HomeEntry) : DeskItem()
}

private const val SEP = "\n"

fun deskKey(i: DeskItem): String = when (i) {
    is DeskItem.Sys -> "sys:${i.kind}"
    is DeskItem.Ent -> when (val e = i.e) {
        is HomeEntry.A -> "app:${e.app.pkg}"
        is HomeEntry.F -> "dir:${e.name}"
    }
}

private fun deskLabel(i: DeskItem): String = when (i) {
    is DeskItem.Sys -> i.label
    is DeskItem.Ent -> when (val e = i.e) {
        is HomeEntry.A -> e.app.label
        is HomeEntry.F -> e.name
    }
}

/** Urutan tampil: sesuai urutan tersimpan (khusus Windows 95), sisanya mengikuti urutan bawaan. */
private fun ordered(vm: MainVm, items: List<DeskItem>): List<DeskItem> {
    if (vm.cfg.uiMode != "win95") return items
    val order = vm.cfg.deskOrder.split(SEP).filter { it.isNotEmpty() }
    if (order.isEmpty()) return items
    val idx = HashMap<String, Int>()
    order.forEachIndexed { n, k -> idx[k] = n }
    return items.sortedBy { idx[deskKey(it)] ?: Int.MAX_VALUE }
}


/** Simpan urutan: [moving] dipindah ke sebelum [before] (atau ke paling akhir bila null). */
fun MainVm.moveDeskItem(all: List<String>, moving: String, before: String?) {
    val l = all.toMutableList()
    if (!l.remove(moving)) return
    val at = if (before == null) l.size else l.indexOf(before).let { if (it < 0) l.size else it }
    l.add(at, moving)
    cfg.deskOrder = l.joinToString(SEP)
}

/** Susun ikon desktop berdasarkan nama, atau kembali ke urutan bawaan. */
fun MainVm.arrangeDesk(byName: Boolean) {
    if (!byName) {
        cfg.deskOrder = ""
        return
    }
    val sys = (0..2).map { "sys:$it" }
    val rest = homeEntries.map { e ->
        val item = DeskItem.Ent(e)
        deskKey(item) to deskLabel(item).lowercase()
    }.sortedBy { it.second }.map { it.first }
    cfg.deskOrder = (sys + rest).joinToString(SEP)
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
    val items: List<DeskItem> = ordered(vm, system + vm.homeEntries.map { DeskItem.Ent(it) })
    val allKeys = items.map { deskKey(it) }
    val moving = vm.deskMove
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
                        val key = deskKey(item)
                        Box(Modifier.fillMaxWidth()) {
                            when (item) {
                                is DeskItem.Sys -> shortcut(item.label, item.kind, Modifier.fillMaxWidth(), item.onClick)
                                is DeskItem.Ent -> when (val e = item.e) {
                                    is HomeEntry.A -> AppTile(e.app, Modifier.fillMaxWidth(), vm, e.app.pkg in sleeping)
                                    is HomeEntry.F -> FolderTile(e.name, e.apps, Modifier.fillMaxWidth(), vm)
                                }
                            }
                            if (moving != null) {
                                Box(
                                    Modifier
                                        .matchParentSize()
                                        .background(if (key == moving) Color(0x66000080) else Color.Transparent)
                                        .clickable {
                                            if (key != moving) vm.moveDeskItem(allKeys, moving, key)
                                            vm.deskMove = null
                                        }
                                )
                            }
                        }
                    }
                }
            }
        }
        if (moving != null) {
            Row(
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 6.dp)
                    .background(Color(0xFFFFFFE1))
                    .bevel(true)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Ketuk ikon tujuan", color = Color.Black, fontSize = 12.sp)
                Text(
                    "Ke akhir",
                    color = Color(0xFF000080),
                    fontSize = 12.sp,
                    modifier = Modifier.clickable {
                        vm.moveDeskItem(allKeys, moving, null)
                        vm.deskMove = null
                    }
                )
                Text("Batal", color = Color(0xFF000080), fontSize = 12.sp, modifier = Modifier.clickable { vm.deskMove = null })
            }
        }
    }
}
