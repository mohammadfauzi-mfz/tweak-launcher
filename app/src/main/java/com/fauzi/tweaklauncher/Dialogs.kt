package com.fauzi.tweaklauncher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AppMenu(vm: MainVm) {
    val a = vm.menuApp ?: return
    val isGame = a.pkg in vm.games
    val inDock = a.pkg in vm.dockPkgs
    val isHidden = a.pkg in vm.hidden
    val isSleeping = a.pkg in vm.sleeping
    val folder = vm.openFolder

    fun close() {
        vm.menuApp = null
    }

    // null = garis pemisah (hanya dipakai gaya Windows 95)
    val items: List<Pair<String, () -> Unit>?> = buildList {
        add((if (isGame) "Hapus dari Boost" else "Tandai sebagai Boost") to { vm.toggleGame(a.pkg); close() })
        add((if (vm.isPinned(a.pkg)) "Lepas dari Beranda" else "Sematkan ke Beranda") to { vm.togglePin(a.pkg); close() })
        add((if (inDock) "Lepas dari dock" else "Taruh di dock") to { vm.toggleDock(a.pkg); close() })
        if (folder != null) {
            add("Keluarkan dari folder $folder" to { vm.removeFromFolder(folder, a.pkg); close() })
        }
        if (vm.cfg.uiMode == "win95" && vm.isPinned(a.pkg)) {
            add(null)
            add("Pindahkan ikon" to { vm.deskMove = "app:" + a.pkg; close() })
            add("Susun ikon: nama" to { vm.arrangeDesk(true); close() })
            add("Susun ikon: bawaan" to { vm.arrangeDesk(false); close() })
            add(null)
        }
        add("Ikon..." to { vm.iconPick = IconTarget(false, a.pkg, a.label); close() })
        add("Grup..." to { vm.groupPickApp = a; close() })
        add("Masukkan ke folder..." to { vm.folderPickApp = a; close() })
        add("Profil khusus untuk aplikasi ini..." to { vm.profilePickApp = a; close() })
        add((if (isHidden) "Tampilkan di daftar" else "Sembunyikan dari daftar") to { vm.toggleHidden(a.pkg); close() })
        add((if (isSleeping) "Bangunkan aplikasi" else "Tidurkan aplikasi") to {
            if (isSleeping) vm.wakeApp(a.pkg) else vm.sleepApp(a.pkg)
            close()
        })
        if (vm.cfg.badgeOn) {
            add((if (a.pkg in Sys.list(vm.cfg.badgeOffCsv)) "Nyalakan badge" else "Matikan badge") to { vm.toggleBadge(a.pkg); close() })
        }
        add("Optimalkan (compile speed)" to { vm.optimize(a.pkg); close() })
        add("Hentikan paksa" to { vm.forceStop(a.pkg); close() })
        add("Info aplikasi" to { vm.openInfo(a.pkg); close() })
        add("Copot pemasangan" to { vm.uninstall(a.pkg); close() })
    }

    if (vm.cfg.uiMode == "win95") {
        W95ContextMenu(a.label, items) { close() }
        return
    }

    TweakDialog(
        onDismissRequest = { close() },
        containerColor = Pal.card,
        title = { Text(a.label, color = Pal.text) },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                items.forEach { it?.let { (label, act) -> MenuItem(label, act) } }
            }
        },
        confirmButton = {
            DlgButton(onClick = { close() }) { Text("Tutup", color = Pal.accent) }
        }
    )
}

@Composable
fun FolderPickDialog(vm: MainVm) {
    val a = vm.folderPickApp ?: return
    var name by remember { mutableStateOf("") }
    val folders = vm.folders
    TweakDialog(
        onDismissRequest = { vm.folderPickApp = null },
        containerColor = Pal.card,
        title = { Text("Folder untuk ${a.label}", color = Pal.text) },
        text = {
            Column(Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
                folders.keys.forEach { f ->
                    MenuItem(f) { vm.addToFolder(f, a.pkg); vm.folderPickApp = null }
                }
                Spacer(Modifier.height(8.dp))
                InputBox(name, { name = it }, "Nama folder baru")
                Spacer(Modifier.height(8.dp))
                ActionButton("Buat folder dan masukkan", Modifier.fillMaxWidth(), primary = true, enabled = name.isNotBlank()) {
                    if (vm.createFolder(name)) {
                        vm.addToFolder(name.trim(), a.pkg)
                        vm.folderPickApp = null
                    }
                }
            }
        },
        confirmButton = {
            DlgButton(onClick = { vm.folderPickApp = null }) { Text("Batal", color = Pal.accent) }
        }
    )
}

@Composable
fun ProfilePickDialog(vm: MainVm) {
    val a = vm.profilePickApp ?: return
    val current = Store.map(vm.cfg.appProfilesJson)[a.pkg]
    TweakDialog(
        onDismissRequest = { vm.profilePickApp = null },
        containerColor = Pal.card,
        title = { Text("Profil untuk ${a.label}", color = Pal.text) },
        text = {
            Column {
                Text(
                    "Profil ini diterapkan saat aplikasi dibuka dari launcher. Aktifkan Otomasi di Setelan agar berlaku juga saat dibuka dari tempat lain.",
                    color = Pal.sub,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(8.dp))
                MenuItem((if (current == null) "✓ " else "") + "Ikuti pengaturan umum") {
                    vm.setAppProfile(a.pkg, null); vm.profilePickApp = null
                }
                vm.profiles.forEach { p ->
                    MenuItem((if (current == p.id) "✓ " else "") + p.label) {
                        vm.setAppProfile(a.pkg, p.id); vm.profilePickApp = null
                    }
                }
            }
        },
        confirmButton = {
            DlgButton(onClick = { vm.profilePickApp = null }) { Text("Tutup", color = Pal.accent) }
        }
    )
}

@Composable
fun FolderDialog(vm: MainVm) {
    val name = vm.openFolder ?: return
    val byPkg = vm.apps.associateBy { it.pkg }
    val members = (vm.folders[name] ?: emptyList()).mapNotNull { byPkg[it] }
    TweakDialog(
        onDismissRequest = { vm.openFolder = null },
        containerColor = Pal.card,
        title = { Text(name, color = Pal.text) },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                if (members.isEmpty()) {
                    Text(
                        "Folder kosong. Tahan lama aplikasi di Daftar Aplikasi, lalu pilih Masukkan ke folder.",
                        color = Pal.sub,
                        fontSize = 13.sp
                    )
                }
                members.chunked(3).forEach { row ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        row.forEach { a ->
                            AppTile(a, Modifier.weight(1f), vm)
                        }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        },
        confirmButton = {
            DlgButton(onClick = { vm.openFolder = null }) { Text("Tutup", color = Pal.accent) }
        }
    )
}

@Composable
fun UpdateDialog(vm: MainVm) {
    val u = vm.update ?: return
    if (!vm.showUpdate) return
    TweakDialog(
        onDismissRequest = { if (!vm.downloading) vm.dismissUpdate() },
        containerColor = Pal.card,
        title = { Text("Pembaruan tersedia", color = Pal.text) },
        text = {
            Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                Text("Versi ${u.tag}", color = Pal.text, fontSize = 15.sp)
                if (u.size > 0) {
                    Text(String.format("%.1f MB", u.size / 1048576.0), color = Pal.sub, fontSize = 12.sp)
                }
                if (u.notes.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(u.notes, color = Pal.soft, fontSize = 13.sp)
                }
                if (vm.downloading) {
                    Spacer(Modifier.height(14.dp))
                    LinearProgressIndicator(progress = { vm.progress }, color = Pal.accent, trackColor = Pal.line)
                }
            }
        },
        dismissButton = {
            DlgButton(enabled = !vm.downloading, onClick = { vm.dismissUpdate() }) { Text("Nanti", color = Pal.sub) }
        },
        confirmButton = {
            DlgButton(
                enabled = !vm.downloading,
                onClick = { if (vm.downloaded != null) vm.installUpdate() else vm.downloadUpdate() }
            ) {
                Text(if (vm.downloaded != null) "Pasang" else "Unduh dan pasang", color = Pal.accent)
            }
        }
    )
}

@Composable
fun RevertDialog(vm: MainVm) {
    val r = vm.revert ?: return
    TweakDialog(
        onDismissRequest = { },
        containerColor = Pal.card,
        title = { Text("Pertahankan ${r.label}?", color = Pal.text) },
        text = {
            Text(
                "Pengaturan layar akan kembali otomatis dalam ${vm.revertLeft} detik jika tidak dikonfirmasi.",
                color = Pal.soft
            )
        },
        dismissButton = {
            DlgButton(onClick = { vm.revertDisplay() }) { Text("Kembalikan", color = Red) }
        },
        confirmButton = {
            DlgButton(onClick = { vm.keepDisplay() }) { Text("Pertahankan", color = Pal.accent) }
        }
    )
}
