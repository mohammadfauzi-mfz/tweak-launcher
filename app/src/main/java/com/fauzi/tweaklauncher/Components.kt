package com.fauzi.tweaklauncher

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun Pill(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .clip(CircleShape)
            .background(if (selected) Pal.accent else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = if (selected) Pal.onAccent else Pal.soft,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
        )
    }
}

/** Kartu dasar dengan sudut dan warna dari tema. */
@Composable
fun SCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(Pal.shape)
            .background(Pal.card)
            .border(1.dp, Pal.line, Pal.shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        content = content
    )
}

@Composable
fun SectionLabel(text: String) {
    Text(text, color = Pal.sub, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
}

@Composable
fun SwitchRow(title: String, sub: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Pal.text, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            if (sub != null) Text(sub, color = Pal.sub, fontSize = 12.sp)
        }
        Spacer(Modifier.size(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = Pal.accent,
                checkedThumbColor = Pal.onAccent,
                uncheckedTrackColor = Pal.inset,
                uncheckedThumbColor = Pal.sub,
                uncheckedBorderColor = Pal.line
            )
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChoiceRow(title: String, options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, color = Pal.text, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { (k, label) ->
                val on = k == selected
                Box(
                    Modifier
                        .height(36.dp)
                        .clip(CircleShape)
                        .background(if (on) Pal.accent else Pal.inset)
                        .border(1.dp, if (on) Pal.accent else Pal.line, CircleShape)
                        .clickable { onSelect(k) }
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        color = if (on) Pal.onAccent else Pal.soft,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun SliderRow(
    title: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    onChange: (Float) -> Unit
) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, color = Pal.text, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(valueText, color = Pal.sub, fontSize = 13.sp)
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = Pal.accent,
                activeTrackColor = Pal.accent,
                inactiveTrackColor = Pal.line
            )
        )
    }
}

@Composable
fun ActionButton(label: String, modifier: Modifier = Modifier, primary: Boolean = false, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        modifier
            .height(46.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .clip(CircleShape)
            .background(if (primary) Pal.accent else Pal.inset)
            .border(1.dp, if (primary) Pal.accent else Pal.line, CircleShape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (primary) Pal.onAccent else Pal.text,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
fun StepRow(title: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = Pal.text, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        ActionButton("-", Modifier.size(width = 52.dp, height = 40.dp), onClick = onMinus)
        Text(value, color = Pal.text, fontSize = 15.sp, modifier = Modifier.padding(horizontal = 14.dp))
        ActionButton("+", Modifier.size(width = 52.dp, height = 40.dp), onClick = onPlus)
    }
}

@Composable
fun InputBox(value: String, onChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier) {
    BasicTextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        textStyle = TextStyle(color = Pal.text, fontSize = 15.sp),
        cursorBrush = SolidColor(Pal.accent),
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(CircleShape)
            .background(Pal.inset)
            .border(1.dp, Pal.line, CircleShape)
            .padding(horizontal = 18.dp),
        decorationBox = { inner ->
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) Text(placeholder, color = Pal.sub, fontSize = 15.sp)
                inner()
            }
        }
    )
}

@Composable
fun StatBar(label: String, value: String, pct: Float, color: Color, modifier: Modifier = Modifier) {
    Column(modifier) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Pal.sub, fontSize = 12.sp)
            Text(value, color = Pal.text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(6.dp))
        Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(Pal.line)) {
            Box(
                Modifier
                    .fillMaxWidth(pct.coerceIn(0.02f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp))
                    .background(color)
            )
        }
    }
}

@Composable
fun Chip(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Pal.inset)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(label, color = Pal.sub, fontSize = 11.sp)
        Text(value, color = Pal.text, fontSize = 17.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

@Composable
fun Tag(text: String, color: Color = Blue) {
    Box(
        Modifier
            .height(26.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
fun MenuItem(label: String, onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 14.dp)) {
        Text(label, color = Pal.soft, fontSize = 15.sp)
    }
}

@Composable
fun AppIcon(app: AppItem, size: Dp, modifier: Modifier = Modifier, dim: Boolean = false, badge: Boolean = true) {
    val cfg = LocalCfg.current
    val shape = iconShape(cfg.iconShape)
    Box(modifier.size(size)) {
        Image(
            bitmap = app.icon,
            contentDescription = app.label,
            modifier = Modifier
                .fillMaxSize()
                .alpha(if (dim) 0.45f else 1f)
                .clip(shape)
        )
        if (badge && cfg.badgeOn) {
            val n = Badges.counts[app.pkg] ?: 0
            if (n > 0 && app.pkg !in Sys.list(cfg.badgeOffCsv)) {
                val col = if (cfg.badgeColor == "accent") Pal.accent else runCatching {
                    Color(android.graphics.Color.parseColor("#" + cfg.badgeColor))
                }.getOrDefault(Red)
                val showNum = cfg.badgeStyle == "count"
                val d = if (showNum) (size * 0.36f).coerceAtLeast(16.dp) else (size * 0.24f).coerceAtLeast(10.dp)
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .size(d)
                        .clip(CircleShape)
                        .background(col)
                        .border(1.5.dp, Pal.bg, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (showNum) {
                        Text(
                            if (n > 99) "99+" else n.toString(),
                            color = Color.White,
                            fontSize = (d.value * 0.5f).coerceAtMost(11f).sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppTile(app: AppItem, modifier: Modifier, vm: MainVm, sleeping: Boolean = false) {
    val cfg = vm.cfg
    Column(
        modifier
            .combinedClickable(
                onClick = { vm.launchApp(app.pkg) },
                onLongClick = { vm.menuApp = app }
            )
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AppIcon(app, cfg.iconSize.dp, dim = sleeping)
        if (cfg.showLabels) {
            Spacer(Modifier.height(6.dp))
            Text(
                app.label,
                color = Pal.soft,
                fontSize = cfg.labelSize.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderTile(name: String, apps: List<AppItem>, modifier: Modifier, vm: MainVm) {
    val cfg = vm.cfg
    val size = cfg.iconSize.dp
    val mini = size * 0.38f
    Column(
        modifier
            .combinedClickable(onClick = { vm.openFolder = name }, onLongClick = { vm.openFolder = name })
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            Modifier
                .size(size)
                .clip(iconShape(cfg.iconShape))
                .background(Pal.inset)
                .border(1.dp, Pal.line, iconShape(cfg.iconShape))
                .padding(size * 0.1f),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            val first = apps.take(4)
            first.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    row.forEach { a -> AppIcon(a, mini, badge = false) }
                }
            }
        }
        if (cfg.showLabels) {
            Spacer(Modifier.height(6.dp))
            Text(
                name,
                color = Pal.soft,
                fontSize = cfg.labelSize.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}
