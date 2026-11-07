package com.fauzi.tweaklauncher

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

/** Penampung widget Android asli (jam, cuaca, dll) di Beranda. */
object WidgetHost {
    lateinit var host: AppWidgetHost
    lateinit var mgr: AppWidgetManager
    private const val HOST_ID = 1024

    fun init(ctx: Context) {
        if (!::host.isInitialized) {
            mgr = AppWidgetManager.getInstance(ctx)
            host = AppWidgetHost(ctx, HOST_ID)
        }
    }
}

@Composable
fun WidgetView(id: Int) {
    val info = remember(id) {
        try {
            WidgetHost.mgr.getAppWidgetInfo(id)
        } catch (e: Exception) {
            null
        }
    } ?: return
    val density = LocalDensity.current
    val conf = LocalConfiguration.current
    val hDp = with(density) { info.minHeight.toDp() }.coerceAtLeast(70.dp)
    val wDp = conf.screenWidthDp - 40
    val hInt = hDp.value.toInt()
    AndroidView(
        factory = { c ->
            WidgetHost.host.createView(c, id, info).apply {
                updateAppWidgetSize(Bundle(), wDp, hInt, wDp, hInt)
            }
        },
        modifier = Modifier.fillMaxWidth().height(hDp).clip(Pal.shape)
    )
}

private fun configIntent(info: AppWidgetProviderInfo, id: Int): Intent =
    Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE)
        .setComponent(info.configure)
        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)

/** Mengembalikan fungsi yang menjalankan alur tambah widget: izin bind, konfigurasi, lalu simpan. */
@Composable
fun rememberWidgetAdder(vm: MainVm): (AppWidgetProviderInfo) -> Unit {
    val cfgLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        val id = vm.pendingWidget
        vm.pendingWidget = -1
        if (id != -1) {
            if (r.resultCode == Activity.RESULT_OK) vm.addWidget(id) else vm.dropWidgetId(id)
        }
    }
    val bindLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        val id = vm.pendingWidget
        if (id != -1) {
            if (r.resultCode == Activity.RESULT_OK) {
                val info = WidgetHost.mgr.getAppWidgetInfo(id)
                if (info != null && info.configure != null) {
                    cfgLauncher.launch(configIntent(info, id))
                } else {
                    vm.pendingWidget = -1
                    vm.addWidget(id)
                }
            } else {
                vm.pendingWidget = -1
                vm.dropWidgetId(id)
            }
        }
    }
    return { p: AppWidgetProviderInfo ->
        val id = WidgetHost.host.allocateAppWidgetId()
        vm.pendingWidget = id
        if (WidgetHost.mgr.bindAppWidgetIdIfAllowed(id, p.provider)) {
            if (p.configure != null) {
                cfgLauncher.launch(configIntent(p, id))
            } else {
                vm.pendingWidget = -1
                vm.addWidget(id)
            }
        } else {
            bindLauncher.launch(
                Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, p.provider)
            )
        }
    }
}

@Composable
fun WidgetPickerDialog(vm: MainVm, onPick: (AppWidgetProviderInfo) -> Unit) {
    if (!vm.showWidgetPicker) return
    val ctx = LocalContext.current
    val pm = ctx.packageManager
    val list = remember {
        WidgetHost.mgr.installedProviders
            .map { Pair(it, it.loadLabel(pm)) }
            .sortedBy { it.second.lowercase() }
    }
    TweakDialog(
        onDismissRequest = { vm.showWidgetPicker = false },
        containerColor = Pal.card,
        title = { Text("Pilih widget", color = Pal.text) },
        text = {
            LazyColumn(Modifier.heightIn(max = 420.dp)) {
                items(list) { (info, label) ->
                    val appName = try {
                        pm.getApplicationInfo(info.provider.packageName, 0).loadLabel(pm).toString()
                    } catch (e: Exception) {
                        info.provider.packageName
                    }
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                vm.showWidgetPicker = false
                                onPick(info)
                            }
                            .padding(vertical = 12.dp)
                    ) {
                        Text(label, color = Pal.text, fontSize = 15.sp)
                        Text(appName, color = Pal.sub, fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            DlgButton(onClick = { vm.showWidgetPicker = false }) { Text("Batal", color = Pal.accent) }
        }
    )
}
