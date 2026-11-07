package com.fauzi.tweaklauncher

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val DAYS = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")

/**
 * Popup kecil kalender dan jam, muncul di atas taskbar saat kotak jam diketuk.
 * [style] 0 = Windows 95 (abu-abu timbul), 1 = Windows XP (krem dan biru).
 */
@Composable
fun ClockPopup(vm: MainVm, style: Int, barHeight: Dp) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var off by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }
    val xp = style == 1
    val loc = Locale("id", "ID")
    val today = Calendar.getInstance().apply { timeInMillis = now }
    val cal = Calendar.getInstance().apply {
        timeInMillis = now
        set(Calendar.DAY_OF_MONTH, 1)
        add(Calendar.MONTH, off)
    }
    val first = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
    val days = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val weeks = (first + days + 6) / 7
    val accent = if (xp) Color(0xFF245EDC) else Color(0xFF000080)
    val panel = if (xp) Color(0xFFECE9D8) else Color(0xFFC0C0C0)

    Box(
        Modifier
            .fillMaxSize()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { vm.clockOpen = false }
    ) {
        val outer = Modifier
            .align(Alignment.BottomEnd)
            .navigationBarsPadding()
            .padding(end = 4.dp, bottom = barHeight + 2.dp)
            .width(260.dp)
        val shaped = if (xp) {
            outer.clip(RoundedCornerShape(8.dp)).background(panel).border(2.dp, Color(0xFF0A4FC8), RoundedCornerShape(8.dp))
        } else {
            outer.background(panel).bevel(true)
        }
        Column(
            shaped
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Jam digital
            Column(
                Modifier
                    .fillMaxWidth()
                    .then(if (xp) Modifier.clip(RoundedCornerShape(6.dp)).background(accent) else Modifier.background(accent))
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    SimpleDateFormat("HH:mm:ss", loc).format(Date(now)),
                    color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold
                )
                Text(
                    SimpleDateFormat("EEEE, d MMMM yyyy", loc).format(Date(now)),
                    color = Color.White, fontSize = 12.sp
                )
            }
            // Pilih bulan
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "‹", color = Color.Black, fontSize = 22.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { off-- }.padding(horizontal = 14.dp, vertical = 2.dp)
                )
                Text(
                    SimpleDateFormat("MMMM yyyy", loc).format(cal.time),
                    color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center, modifier = Modifier.weight(1f)
                )
                Text(
                    "›", color = Color.Black, fontSize = 22.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { off++ }.padding(horizontal = 14.dp, vertical = 2.dp)
                )
            }
            // Kalender
            Column(Modifier.fillMaxWidth().background(Color.White).padding(4.dp)) {
                Row(Modifier.fillMaxWidth()) {
                    DAYS.forEach {
                        Text(it, color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(2.dp))
                for (w in 0 until weeks) {
                    Row(Modifier.fillMaxWidth()) {
                        for (d in 0 until 7) {
                            val n = w * 7 + d - first + 1
                            val ok = n in 1..days
                            val isToday = ok && off == 0 && n == today.get(Calendar.DAY_OF_MONTH)
                            Box(
                                Modifier
                                    .weight(1f)
                                    .height(28.dp)
                                    .then(
                                        if (isToday) {
                                            if (xp) Modifier.clip(RoundedCornerShape(4.dp)).background(accent) else Modifier.background(accent)
                                        } else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (ok) {
                                    Text(
                                        n.toString(),
                                        color = if (isToday) Color.White else if (d == 6) Color(0xFFC62828) else Color.Black,
                                        fontSize = 12.sp,
                                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Text("Baterai ${vm.stats.battery}%", color = Color.Black, fontSize = 12.sp)
        }
    }
}
