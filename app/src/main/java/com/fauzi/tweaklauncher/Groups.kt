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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Satu tab di daftar aplikasi: tab bawaan (semua, boost, ...) atau grup buatan sendiri (id "g:<nama>"). */
data class AppTab(val id: String, val label: String, val builtin: Boolean)

private val BUILTIN_TABS = listOf(
    "all" to "Semua",
    "boost" to "Boost",
    "pin" to "Tersemat",
    "sleep" to "Tidur",
    "hid" to "Tersembunyi"
)

// ---------------------------------------------------------------- Data

val MainVm.groups: LinkedHashMap<String, List<String>> get() = Store.folders(cfg.groupsJson)

private fun MainVm.saveGroups(m: Map<String, List<String>>) {
    cfg.groupsJson = Store.foldersJson(m)
}

/** Semua tab sesuai urutan pilihan pengguna. Tab yang disembunyikan hanya muncul jika [includeHidden]. */
fun MainVm.allTabs(includeHidden: Boolean = false): List<AppTab> {
    val tabs = BUILTIN_TABS.map { AppTab(it.first, it.second, true) } +
        groups.keys.map { AppTab("g:$it", it, false) }
    val order = Sys.list(cfg.tabOrderCsv)
    val sorted = tabs.sortedBy { t ->
        val i = order.indexOf(t.id)
        if (i < 0) Int.MAX_VALUE else i
    }
    if (includeHidden) return sorted
    val hid = Sys.list(cfg.tabHiddenCsv)
    return sorted.filter { it.id !in hid || it.id == "all" }
}

/** Tab yang dipilih; jika sudah dihapus atau disembunyikan, kembali ke Semua. */
fun MainVm.currentTab(): String =
    if (allTabs().any { it.id == tabSel }) tabSel else "all"

fun MainVm.appsForTab(id: String): List<AppItem> {
    val src = if (id == "hid") apps.filter { it.pkg in hidden } else visibleApps
    return when {
        id == "boost" -> {
            val g = games
            src.filter { it.pkg in g }
        }
        id == "pin" -> {
            val p = Sys.list(cfg.pinnedCsv)
            src.filter { it.pkg in p || "app:${it.pkg}" in p }
        }
        id == "sleep" -> {
            val s = sleeping
            src.filter { it.pkg in s }
        }
        id.startsWith("g:") -> {
            val members = groups[id.removePrefix("g:")] ?: emptyList()
            src.filter { it.pkg in members }
        }
        else -> src
    }
}

fun MainVm.createGroup(name: String): Boolean {
    val n = name.trim()
    if (n.isEmpty()) return false
    val g = LinkedHashMap(groups)
    if (g.keys.any { it.equals(n, ignoreCase = true) }) return false
    g[n] = emptyList()
    saveGroups(g)
    return true
}

fun MainVm.renameGroup(old: String, new: String): Boolean {
    val n = new.trim()
    if (n.isEmpty()) return false
    if (n == old) return true
    val g = groups
    if (g.keys.any { it.equals(n, ignoreCase = true) && it != old }) return false
    val out = LinkedHashMap<String, List<String>>()
    g.forEach { (k, v) -> out[if (k == old) n else k] = v }
    saveGroups(out)
    cfg.tabOrderCsv = Sys.join(Sys.list(cfg.tabOrderCsv).map { if (it == "g:$old") "g:$n" else it })
    cfg.tabHiddenCsv = Sys.join(Sys.list(cfg.tabHiddenCsv).map { if (it == "g:$old") "g:$n" else it })
    if (tabSel == "g:$old") tabSel = "g:$n"
    return true
}

fun MainVm.deleteGroup(name: String) {
    val g = LinkedHashMap(groups)
    g.remove(name)
    saveGroups(g)
    cfg.tabOrderCsv = Sys.join(Sys.list(cfg.tabOrderCsv).filter { it != "g:$name" })
    cfg.tabHiddenCsv = Sys.join(Sys.list(cfg.tabHiddenCsv).filter { it != "g:$name" })
    if (tabSel == "g:$name") tabSel = "all"
}

fun MainVm.toggleInGroup(name: String, pkg: String) {
    val g = LinkedHashMap(groups)
    val l = g[name] ?: return
    g[name] = if (pkg in l) l - pkg else l + pkg
    saveGroups(g)
}

fun MainVm.moveTab(id: String, dir: Int) {
    val ids = allTabs(includeHidden = true).map { it.id }.toMutableList()
    val i = ids.indexOf(id)
    val j = i + dir
    if (i < 0 || j < 0 || j >= ids.size) return
    val t = ids[i]
    ids[i] = ids[j]
    ids[j] = t
    cfg.tabOrderCsv = Sys.join(ids)
}

fun MainVm.toggleTabHidden(id: String) {
    if (id == "all") return
    val h = Sys.list(cfg.tabHiddenCsv)
    cfg.tabHiddenCsv = Sys.join(if (id in h) h - id else h + id)
    if (tabSel == id) tabSel = "all"
}

// ---------------------------------------------------------------- Baris tab

/** Baris tab daftar aplikasi. Ketuk untuk memilih, tahan lama untuk mengatur, + untuk grup baru. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppTabsRow(vm: MainVm, modifier: Modifier = Modifier) {
    val sel = vm.currentTab()
    Row(
        modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        vm.allTabs().forEach { t ->
            val on = t.id == sel
            Box(
                Modifier
                    .height(36.dp)
                    .clip(Pal.pill)
                    .background(if (on) Pal.accent else Pal.card)
                    .border(1.dp, if (on) Pal.accent else Pal.line, Pal.pill)
                    .combinedClickable(
                        onClick = { vm.tabSel = t.id },
                        onLongClick = { vm.tabMenu = t.id }
                    )
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    t.label,
                    color = if (on) Pal.onAccent else Pal.soft,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }
        }
        Box(
            Modifier
                .height(36.dp)
                .clip(Pal.pill)
                .background(Pal.card)
                .border(1.dp, Pal.line, Pal.pill)
                .clickable { vm.groupNewOpen = true }
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("+ Grup", color = Pal.soft, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
        }
    }
}

// ---------------------------------------------------------------- Dialog

@Composable
private fun CheckRow(label: String, checked: Boolean, onToggle: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = Pal.accent,
                uncheckedColor = Pal.sub,
                checkmarkColor = Pal.onAccent
            )
        )
        Text(label, color = Pal.text, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun GroupDialogs(vm: MainVm) {
    NewGroupDialog(vm)
    TabMenuDialog(vm)
    GroupAppsDialog(vm)
    GroupPickDialog(vm)
}

@Composable
private fun NewGroupDialog(vm: MainVm) {
    if (!vm.groupNewOpen) return
    var name by remember { mutableStateOf("") }
    var err by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { vm.groupNewOpen = false },
        containerColor = Pal.card,
        title = { Text("Grup baru", color = Pal.text) },
        text = {
            Column {
                InputBox(name, { name = it; err = "" }, "Nama grup, mis. Kerja")
                if (err.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(err, color = Red, fontSize = 12.sp)
                }
                Spacer(Modifier.height(10.dp))
                ActionButton("Buat dan pilih aplikasi", Modifier.fillMaxWidth(), primary = true, enabled = name.isNotBlank()) {
                    if (vm.createGroup(name)) {
                        val n = name.trim()
                        vm.tabSel = "g:$n"
                        vm.groupNewOpen = false
                        vm.groupEditName = n
                    } else {
                        err = "Nama sudah dipakai."
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { vm.groupNewOpen = false }) { Text("Batal", color = Pal.accent) } }
    )
}

@Composable
private fun TabMenuDialog(vm: MainVm) {
    val id = vm.tabMenu ?: return
    val tab = vm.allTabs(includeHidden = true).firstOrNull { it.id == id }
    if (tab == null) {
        vm.tabMenu = null
        return
    }
    var renaming by remember(id) { mutableStateOf(false) }
    var newName by remember(id) { mutableStateOf(tab.label) }
    var err by remember(id) { mutableStateOf("") }
    val hidden = id in Sys.list(vm.cfg.tabHiddenCsv)
    val name = id.removePrefix("g:")
    AlertDialog(
        onDismissRequest = { vm.tabMenu = null },
        containerColor = Pal.card,
        title = { Text(tab.label, color = Pal.text) },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                if (!tab.builtin) {
                    if (renaming) {
                        InputBox(newName, { newName = it; err = "" }, "Nama baru")
                        if (err.isNotEmpty()) Text(err, color = Red, fontSize = 12.sp)
                        Spacer(Modifier.height(8.dp))
                        ActionButton("Simpan nama", Modifier.fillMaxWidth(), primary = true, enabled = newName.isNotBlank()) {
                            if (vm.renameGroup(name, newName)) vm.tabMenu = null else err = "Nama sudah dipakai."
                        }
                        Spacer(Modifier.height(8.dp))
                    } else {
                        MenuItem("Pilih aplikasi...") {
                            vm.tabMenu = null
                            vm.groupEditName = name
                        }
                        MenuItem("Ganti nama...") { renaming = true }
                    }
                }
                MenuItem("Geser ke kiri") { vm.moveTab(id, -1) }
                MenuItem("Geser ke kanan") { vm.moveTab(id, 1) }
                if (id != "all") {
                    MenuItem(if (hidden) "Tampilkan tab" else "Sembunyikan tab") {
                        vm.toggleTabHidden(id)
                        vm.tabMenu = null
                    }
                }
                if (!tab.builtin) {
                    MenuItem("Hapus grup (aplikasi tidak ikut terhapus)") {
                        vm.deleteGroup(name)
                        vm.tabMenu = null
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { vm.tabMenu = null }) { Text("Tutup", color = Pal.accent) } }
    )
}

/** Centang aplikasi mana saja yang masuk ke satu grup. */
@Composable
private fun GroupAppsDialog(vm: MainVm) {
    val name = vm.groupEditName ?: return
    val members = vm.groups[name] ?: run {
        vm.groupEditName = null
        return
    }
    AlertDialog(
        onDismissRequest = { vm.groupEditName = null },
        containerColor = Pal.card,
        title = { Text("Aplikasi di grup $name", color = Pal.text) },
        text = {
            Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState())) {
                vm.apps.forEach { a ->
                    CheckRow(a.label, a.pkg in members) { vm.toggleInGroup(name, a.pkg) }
                }
            }
        },
        confirmButton = { TextButton(onClick = { vm.groupEditName = null }) { Text("Selesai", color = Pal.accent) } }
    )
}

/** Dari menu tahan lama aplikasi: pilih grup mana saja untuk aplikasi itu. */
@Composable
private fun GroupPickDialog(vm: MainVm) {
    val a = vm.groupPickApp ?: return
    var name by remember { mutableStateOf("") }
    val groups = vm.groups
    AlertDialog(
        onDismissRequest = { vm.groupPickApp = null },
        containerColor = Pal.card,
        title = { Text("Grup untuk ${a.label}", color = Pal.text) },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                if (groups.isEmpty()) {
                    Text("Belum ada grup. Buat satu di bawah.", color = Pal.sub, fontSize = 13.sp)
                }
                groups.forEach { (g, members) ->
                    CheckRow(g, a.pkg in members) { vm.toggleInGroup(g, a.pkg) }
                }
                Spacer(Modifier.height(8.dp))
                InputBox(name, { name = it }, "Nama grup baru")
                Spacer(Modifier.height(8.dp))
                ActionButton("Buat grup dan masukkan", Modifier.fillMaxWidth(), primary = true, enabled = name.isNotBlank()) {
                    if (vm.createGroup(name)) {
                        vm.toggleInGroup(name.trim(), a.pkg)
                        name = ""
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { vm.groupPickApp = null }) { Text("Selesai", color = Pal.accent) } }
    )
}
