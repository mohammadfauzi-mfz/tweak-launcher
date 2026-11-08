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
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
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
    when (cfg.uiMode) {
        "xp" -> XpLock(time, date, onUnlock, onCancel)
        "pda" -> PdaLock(time, date, onUnlock, onCancel)
        "mac" -> MacLock(time, date, onUnlock, onCancel)
        else -> Win95Lock(cfg, time, date, onUnlock, onCancel)
    }
}

@androidx.compose.runtime.Composable
private fun Win95Lock(cfg: Cfg, time: String, date: String, onUnlock: () -> Unit, onCancel: () -> Unit) {
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

private fun Modifier.unlockSwipe(onUnlock: () -> Unit): Modifier = this.swipes(onEnd = { d, _ ->
    if (d.y < -220f && kotlin.math.abs(d.y) > kotlin.math.abs(d.x)) onUnlock()
})

// ---------------------------------------------------------------- Windows XP

@androidx.compose.runtime.Composable
private fun XpLock(time: String, date: String, onUnlock: () -> Unit, onCancel: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0xFF00309C), Color(0xFF5A7EDC), Color(0xFF5A7EDC), Color(0xFF00309C))))
            .unlockSwipe(onUnlock)
    ) {
        Spacer(Modifier.statusBarsPadding().height(64.dp))
        Box(Modifier.fillMaxWidth().height(2.dp).background(Color(0xFFB9C8F2)))
        Column(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                W95Icon(3, 44.dp)
                Column {
                    Text("Microsoft", color = Color.White, fontSize = 12.sp)
                    Text("Windows xp", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(28.dp))
            Row(
                Modifier
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
                    .background(Color(0x33FFFFFF))
                    .clickable(onClick = onUnlock)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    Modifier
                        .size(56.dp)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
                        .background(Color(0xFFECE9D8)),
                    contentAlignment = Alignment.Center
                ) { W95Icon(0, 40.dp) }
                Column {
                    Text(time, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                    Text(date, color = Color.White, fontSize = 12.sp)
                    Text("Ketuk untuk masuk", color = Color.White, fontSize = 13.sp)
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(2.dp).background(Color(0xFFF2A14B)))
        Row(
            Modifier.fillMaxWidth().background(Color(0xFF00309C)).navigationBarsPadding().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Geser ke atas atau ketuk untuk membuka kunci.", color = Color.White, fontSize = 12.sp, modifier = Modifier.weight(1f))
            Box(
                Modifier
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                    .background(Color(0xFFD6452E))
                    .clickable(onClick = onCancel)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) { Text("Batal", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

// ---------------------------------------------------------------- PDA

@androidx.compose.runtime.Composable
private fun PdaLock(time: String, date: String, onUnlock: () -> Unit, onCancel: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Pal.bg).unlockSwipe(onUnlock)) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().height(30.dp).background(Pal.accent).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            PdaFlag(16.dp)
            Text("Terkunci", color = Pal.onAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text(time, color = Pal.onAccent, fontSize = 13.sp)
        }
        Box(Modifier.weight(1f).padding(10.dp), contentAlignment = Alignment.Center) {
            Column(Modifier.fillMaxWidth().border(1.dp, Pal.text).background(Pal.card)) {
                Text(
                    "PERANGKAT TERKUNCI", color = Pal.bg, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth().background(Pal.text).padding(horizontal = 8.dp, vertical = 3.dp)
                )
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(time, color = Pal.text, fontSize = 48.sp, fontWeight = FontWeight.Bold)
                    Text(date, color = Pal.soft, fontSize = 13.sp)
                    Text("Ketuk Buka atau geser ke atas untuk membuka kunci.", color = Pal.text, fontSize = 12.sp)
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().background(Pal.card).navigationBarsPadding().padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PdaKey("Buka", Modifier.weight(1f), onUnlock)
            PdaKey("Batal", Modifier.weight(1f), onCancel)
        }
    }
}

@androidx.compose.runtime.Composable
private fun PdaKey(text: String, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.height(44.dp).border(2.dp, Pal.text).background(Pal.bg).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Text(text, color = Pal.text, fontSize = 15.sp, fontWeight = FontWeight.Bold) }
}

// ---------------------------------------------------------------- Mac OS klasik

@androidx.compose.runtime.Composable
private fun MacLock(time: String, date: String, onUnlock: () -> Unit, onCancel: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(Color(0xFFA6A6A6))
                val step = 4.dp.toPx()
                var y = 0f
                while (y < size.height) {
                    drawRect(Color(0xFF8E8E8E), androidx.compose.ui.geometry.Offset(0f, y), androidx.compose.ui.geometry.Size(size.width, 1.dp.toPx()))
                    y += step
                }
            }
            .unlockSwipe(onUnlock)
    ) {
        Column(Modifier.fillMaxWidth().background(Color.White).statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().height(30.dp).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Tweak", color = Color.Black, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(time, color = Color.Black, fontSize = 14.sp)
            }
            Box(Modifier.fillMaxWidth().height(2.dp).background(Color.Black))
        }
        Box(Modifier.weight(1f).padding(24.dp), contentAlignment = Alignment.Center) {
            Column(
                Modifier
                    .widthIn(max = 340.dp)
                    .fillMaxWidth()
                    .macShadow()
                    .background(Color.White)
                    .border(2.dp, Color.Black)
            ) {
                Box(Modifier.fillMaxWidth().height(24.dp).background(Color.White).stripes(), contentAlignment = Alignment.Center) {
                    Text("Terkunci", color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.background(Color.White).padding(horizontal = 8.dp))
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color.Black))
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        MacIcon(0, 44.dp)
                        Column {
                            Text(time, color = Color.Black, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                            Text(date, color = Color.Black, fontSize = 12.sp)
                        }
                    }
                    Text("Ketuk Buka atau geser ke atas untuk membuka kunci.", color = Color.Black, fontSize = 13.sp)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Box(
                            Modifier.width(80.dp).height(30.dp)
                                .border(1.dp, Color.Black, androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                                .clickable(onClick = onCancel),
                            contentAlignment = Alignment.Center
                        ) { Text("Batal", color = Color.Black, fontSize = 13.sp) }
                        Spacer(Modifier.width(10.dp))
                        Box(
                            Modifier.width(80.dp).height(30.dp)
                                .border(3.dp, Color.Black, androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
                                .clickable(onClick = onUnlock),
                            contentAlignment = Alignment.Center
                        ) { Text("Buka", color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
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
            .setContentTitle("Layar kunci aktif")
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
