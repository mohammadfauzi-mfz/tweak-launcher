package com.fauzi.tweaklauncher

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import rikka.shizuku.Shizuku

class MainActivity : ComponentActivity() {

    private val vm: MainVm by viewModels()

    private val onBinder = Shizuku.OnBinderReceivedListener { vm.refreshShizuku() }
    private val onDead = Shizuku.OnBinderDeadListener { vm.refreshShizuku() }
    private val onPerm = Shizuku.OnRequestPermissionResultListener { _, _ -> vm.refreshShizuku() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        Shizuku.addBinderReceivedListenerSticky(onBinder)
        Shizuku.addBinderDeadListener(onDead)
        Shizuku.addRequestPermissionResultListener(onPerm)
        setContent {
            TweakTheme { Root(vm) }
        }
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
    }

    override fun onDestroy() {
        Shizuku.removeBinderReceivedListener(onBinder)
        Shizuku.removeBinderDeadListener(onDead)
        Shizuku.removeRequestPermissionResultListener(onPerm)
        super.onDestroy()
    }
}
