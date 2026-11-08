package com.fauzi.tweaklauncher

import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val L_GRAY = Color(0xFFC0C0C0)
private val L_NAVY = Color(0xFF000080)

/**
 * Layar kunci gaya Windows 95 yang tampil di atas lockscreen asli HP.
 * Keamanan tetap dari lockscreen sistem: tombol OK memintanya (PIN, pola, atau sidik jari) dibuka.
 */
class LockActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(false)
        val cfg = Cfg(this)
        setContent {
            TweakTheme(cfg) {
                LockUi(cfg, onUnlock = { unlock() }, onCancel = { finish() })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setShowWhenLocked(true)
    }

    private fun unlock() {
        val km = getSystemService(KeyguardManager::class.java)
        if (km == null) {
            finish()
            return
        }
        km.requestDismissKeyguard(this, object : KeyguardManager.KeyguardDismissCallback() {
            override fun onDismissSucceeded() {
                finish()
            }
        })
    }
}

@androidx.compose.runtime.Composable
private fun LockUi(cfg: Cfg, onUnlock: () -> Unit, onCancel: () -> Unit) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }
    val loc = Locale("id", "ID")
    val time = SimpleDateFormat(if (cfg.clock24) "HH:mm" else "h:mm a", loc).format(Date(now))
    val date = SimpleDateFormat("EEEE, d MMMM yyyy", loc).format(Date(now))
    val spec = cfg.wp95.takeIf { it != "system" } ?: ""
    val wp = rememberWallpaper(spec, "win95")

    Box(
        Modifier
            .fillMaxSize()
            .wallpaperBg(spec, wp) { drawRect(Color(0xFF008080)) }
            .swipes(onEnd = { d, _ ->
                if (d.y < -220f && kotlin.math.abs(d.y) > kotlin.math.abs(d.x)) onUnlock()
            })
    ) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f).statusBarsPadding(), contentAlignment = Alignment.Center) {
                Column(
                    Modifier
                        .padding(horizontal = 24.dp)
                        .widthIn(max = 340.dp)
                        .fillMaxWidth()
                        .background(L_GRAY)
                        .bevel(true)
                        .padding(3.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth().background(L_NAVY).padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Selamat Datang di Windows", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            W95Icon(0, 44.dp)
                            Column {
                                Text(time, color = Color.Black, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                                Text(date, color = Color.Black, fontSize = 12.sp)
                            }
                        }
                        Text("Tekan OK atau geser ke atas untuk membuka kunci.", color = Color.Black, fontSize = 13.sp)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            LockButton("OK", true, onUnlock)
                            Spacer(Modifier.width(8.dp))
                            LockButton("Batal", false, onCancel)
                        }
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth().background(L_GRAY).navigationBarsPadding().padding(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    Modifier.background(L_GRAY).bevel(true).padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    W95Icon(3, 18.dp)
                    Text("Start", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.weight(1f))
                Box(Modifier.background(L_GRAY).bevel(false).padding(horizontal = 10.dp, vertical = 5.dp)) {
                    Text(time, color = Color.Black, fontSize = 12.sp)
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun LockButton(text: String, bold: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .width(84.dp)
            .height(30.dp)
            .background(L_GRAY)
            .bevel(true)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.Black, fontSize = 13.sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
    }
}

/** Layanan latar: memantau layar mati lalu menampilkan layar kunci buatan launcher. */
class LockService : Service() {

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_SCREEN_OFF) LockLauncher.show(context)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL, "Layar kunci", NotificationManager.IMPORTANCE_MIN))
        registerReceiver(receiver, IntentFilter(Intent.ACTION_SCREEN_OFF))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val n = Notification.Builder(this, CHANNEL)
            .setContentTitle("Layar kunci Windows 95 aktif")
            .setSmallIcon(R.drawable.ic_stat)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NID, n)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(receiver)
        } catch (e: Exception) {
        }
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL = "lock_service"
        private const val NID = 43
    }
}

object LockLauncher {
    /** Buka layar kunci. Android membatasi aplikasi latar, jadi dipakai izin overlay atau Shizuku. */
    fun show(ctx: Context) {
        val audio = ctx.getSystemService(AudioManager::class.java)
        // Jangan menutupi layar telepon saat layar mati karena sensor jarak.
        if (audio != null && (audio.mode == AudioManager.MODE_IN_CALL || audio.mode == AudioManager.MODE_IN_COMMUNICATION ||
                audio.mode == AudioManager.MODE_RINGTONE)) return
        val i = Intent(ctx, LockActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NO_ANIMATION)
        try {
            if (Settings.canDrawOverlays(ctx)) {
                ctx.startActivity(i)
                return
            }
        } catch (e: Exception) {
        }
        if (Shell.state() == ShizukuState.READY) {
            val cmp = "${ctx.packageName}/.LockActivity"
            Thread { Shell.run("am start -n $cmp") }.start()
        }
    }

    fun preview(ctx: Context) {
        ctx.startActivity(Intent(ctx, LockActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
