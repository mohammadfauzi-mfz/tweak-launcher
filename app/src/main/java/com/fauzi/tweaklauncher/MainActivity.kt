package com.fauzi.tweaklauncher

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import rikka.shizuku.Shizuku

class MainActivity : ComponentActivity() {

    private val vm: MainVm by viewModels()

    private val onBinder = Shizuku.OnBinderReceivedListener { vm.refreshShizuku() }
    private val onDead = Shizuku.OnBinderDeadListener { vm.refreshShizuku() }
    private val onPerm = Shizuku.OnRequestPermissionResultListener { _, _ -> vm.refreshShizuku() }

    private val notifPerm = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WidgetHost.init(applicationContext)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        Shizuku.addBinderReceivedListenerSticky(onBinder)
        Shizuku.addBinderDeadListener(onDead)
        Shizuku.addRequestPermissionResultListener(onPerm)

        // Notifikasi dipakai oleh layanan latar (overlay dan profil otomatis). Ditanya sekali saja.
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED &&
            !vm.cfg.askedNotif
        ) {
            vm.cfg.askedNotif = true
            notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            TweakTheme(vm.cfg) { Root(vm) }
        }
    }

    override fun onStart() {
        super.onStart()
        try {
            WidgetHost.host.startListening()
        } catch (e: Exception) {
        }
    }

    override fun onStop() {
        try {
            WidgetHost.host.stopListening()
        } catch (e: Exception) {
        }
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        vm.onResumed()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Tombol Home ditekan: kembali ke Beranda.
        vm.screen = Screen.Home
        vm.menuApp = null
        vm.pdaStart = false
        vm.w95Start = false
    }

    override fun onDestroy() {
        Shizuku.removeBinderReceivedListener(onBinder)
        Shizuku.removeBinderDeadListener(onDead)
        Shizuku.removeRequestPermissionResultListener(onPerm)
        super.onDestroy()
    }
}
