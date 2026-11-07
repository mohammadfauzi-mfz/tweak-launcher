package com.fauzi.tweaklauncher

import androidx.compose.foundation.background
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
    if (LocalCfg.current.uiMode != "win95") {
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
    val white = LocalPal.current.copy(text = Color.White, soft = Color.White, sub = Color.White)
    Dialog(onDismissRequest = onDismissRequest, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxWidth().padding(horizontal = 18.dp), contentAlignment = Alignment.Center) {
            Column(
                Modifier
                    .widthIn(max = 440.dp)
                    .fillMaxWidth()
                    .background(DLG_GRAY)
                    .bevel(true)
                    .padding(3.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth().background(DLG_NAVY).padding(start = 8.dp, top = 3.dp, bottom = 3.dp, end = 3.dp),
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
                Column(Modifier.fillMaxWidth().padding(14.dp)) {
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
    if (LocalCfg.current.uiMode == "win95") {
        Row(
            Modifier
                .background(DLG_GRAY)
                .bevel(true)
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
