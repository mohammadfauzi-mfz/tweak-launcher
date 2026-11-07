package com.fauzi.tweaklauncher

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

private val DLG_GRAY = Color(0xFFC0C0C0)
private val DLG_NAVY = Color(0xFF000080)

/**
 * Dialog bersama. Mode Windows 95 tampil sebagai jendela klasik (title bar biru, tepi timbul,
 * tombol timbul); mode lain memakai AlertDialog biasa.
 */
@Composable
fun TweakDialog(
    onDismissRequest: () -> Unit,
    containerColor: Color,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
    confirmButton: @Composable () -> Unit,
    dismissButton: (@Composable () -> Unit)? = null
) {
    val mode = LocalCfg.current.uiMode
    if (mode != "win95" && mode != "xp" && mode != "wp") {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            containerColor = containerColor,
            title = title,
            text = text,
            confirmButton = confirmButton,
            dismissButton = dismissButton
        )
        return
    }
    if (mode == "wp") {
        Dialog(onDismissRequest = onDismissRequest, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Column(Modifier.fillMaxWidth().background(Color(0xFF1F1F1F)).padding(horizontal = 20.dp, vertical = 20.dp)) {
                if (title != null) title()
                if (text != null) {
                    Box(Modifier.padding(top = 12.dp)) { text() }
                }
                Row(Modifier.fillMaxWidth().padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f)) { confirmButton() }
                    if (dismissButton != null) Box(Modifier.weight(1f)) { dismissButton() }
                }
            }
        }
        return
    }
    val white = LocalPal.current.copy(text = Color.White, soft = Color.White, sub = Color.White)
    val xp = mode == "xp"
    Dialog(onDismissRequest = onDismissRequest, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxWidth().padding(horizontal = 18.dp), contentAlignment = Alignment.Center) {
            Column(
                Modifier
                    .widthIn(max = 440.dp)
                    .fillMaxWidth()
                    .then(
                        if (xp) Modifier.clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)).background(XP_BLUE_DARK)
                        else Modifier.background(DLG_GRAY).bevel(true)
                    )
                    .padding(3.dp)
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .then(if (xp) Modifier.background(xpTitleBrush()) else Modifier.background(DLG_NAVY))
                        .padding(start = 8.dp, top = 3.dp, bottom = 3.dp, end = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.weight(1f)) {
                        if (title != null) CompositionLocalProvider(LocalPal provides white) { title() }
                    }
                    Box(
                        Modifier
                            .background(DLG_GRAY)
                            .bevel(true)
                            .clickable(onClick = onDismissRequest)
                            .padding(horizontal = 9.dp, vertical = 1.dp),
                        contentAlignment = Alignment.Center
                    ) { Text("✕", color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                }
                Column(Modifier.fillMaxWidth().background(if (xp) XP_BEIGE else DLG_GRAY).padding(14.dp)) {
                    if (text != null) text()
                    Row(
                        Modifier.fillMaxWidth().padding(top = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (dismissButton != null) dismissButton()
                        confirmButton()
                    }
                }
            }
        }
    }
}

/** Tombol dialog: timbul ala Windows 95 di mode itu, TextButton biasa di mode lain. */
@Composable
fun DlgButton(onClick: () -> Unit, enabled: Boolean = true, content: @Composable RowScope.() -> Unit) {
    val mode = LocalCfg.current.uiMode
    if (mode == "wp") {
        Row(
            Modifier
                .fillMaxWidth()
                .background(Color(0xFF3A3A3A))
                .clickable(enabled = enabled, onClick = onClick)
                .padding(vertical = 12.dp)
                .alpha(if (enabled) 1f else 0.5f),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    } else if (mode == "win95" || mode == "xp") {
        Row(
            Modifier
                .then(
                    if (mode == "xp") Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFF1EFE2))
                        .border(1.dp, Color(0xFF003C74), RoundedCornerShape(4.dp))
                    else Modifier.background(DLG_GRAY).bevel(true)
                )
                .clickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 7.dp)
                .alpha(if (enabled) 1f else 0.5f),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    } else {
        TextButton(onClick = onClick, enabled = enabled, content = content)
    }
}
