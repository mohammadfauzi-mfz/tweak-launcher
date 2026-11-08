package com.fauzi.tweaklauncher

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private val G_GRAY = Color(0xFFC0C0C0)
private val G_DARK = Color(0xFF808080)
private val G_NAVY = Color(0xFF000080)

/** Satu jendela game yang mengambang dan bisa digeser. */
class GameWin(val id: Int, val kind: String, x0: Float, y0: Float, z0: Int) {
    var x by mutableStateOf(x0)
    var y by mutableStateOf(y0)
    var z by mutableIntStateOf(z0)
}

fun MainVm.openGame(kind: String) {
    w95Start = false
    gameZ++
    val ex = gameWins.firstOrNull { it.kind == kind }
    if (ex != null) {
        ex.z = gameZ
        return
    }
    val n = gameWins.size
    gameWins.add(GameWin(gameCounter++, kind, 30f + n * 40f, 140f + n * 60f, gameZ))
}

fun MainVm.gameFront(w: GameWin) {
    if (w.z != gameZ) {
        gameZ++
        w.z = gameZ
    }
}

fun gameTitle(kind: String) = if (kind == "mines") "Minesweeper" else "Solitaire"

// ---------------------------------------------------------------- Ikon

@Composable
fun MinesIcon(dp: Dp) {
    Canvas(Modifier.size(dp)) {
        val w = size.width
        val c = Offset(w / 2f, w / 2f)
        val sw = w * 0.07f
        drawRect(G_GRAY)
        for (a in 0 until 4) {
            val ang = Math.toRadians(a * 45.0)
            val dx = (Math.cos(ang) * w * 0.34).toFloat()
            val dy = (Math.sin(ang) * w * 0.34).toFloat()
            drawLine(Color.Black, Offset(c.x - dx, c.y - dy), Offset(c.x + dx, c.y + dy), sw)
        }
        drawCircle(Color.Black, w * 0.22f, c)
        drawCircle(Color.White, w * 0.06f, Offset(c.x - w * 0.08f, c.y - w * 0.08f))
    }
}

@Composable
fun CardsIcon(dp: Dp) {
    Canvas(Modifier.size(dp)) {
        val w = size.width
        val cw = w * 0.5f
        val ch = w * 0.7f
        drawRect(Color.White, Offset(w * 0.08f, w * 0.18f), Size(cw, ch))
        drawRect(Color.Black, Offset(w * 0.08f, w * 0.18f), Size(cw, ch), style = androidx.compose.ui.graphics.drawscope.Stroke(w * 0.04f))
        drawRect(Color.White, Offset(w * 0.4f, w * 0.1f), Size(cw, ch))
        drawRect(Color.Black, Offset(w * 0.4f, w * 0.1f), Size(cw, ch), style = androidx.compose.ui.graphics.drawscope.Stroke(w * 0.04f))
        drawCircle(Color(0xFFD02020), w * 0.09f, Offset(w * 0.65f, w * 0.38f))
    }
}

// ---------------------------------------------------------------- Jendela mengambang

@Composable
fun GameLayer(vm: MainVm, xp: Boolean) {
    if (vm.gameWins.isEmpty()) return
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val maxW = constraints.maxWidth.toFloat()
        val maxH = constraints.maxHeight.toFloat()
        vm.gameWins.toList().forEach { w -> key(w.id) { GameWindow(vm, w, xp, maxW, maxH) } }
    }
}

@Composable
private fun GameWindow(vm: MainVm, w: GameWin, xp: Boolean, maxW: Float, maxH: Float) {
    val d = LocalDensity.current
    val minX = -maxW * 0.6f
    val maxX = maxW - with(d) { 90.dp.toPx() }
    val maxY = maxH - with(d) { 50.dp.toPx() }
    val frame = if (xp) {
        Modifier.clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)).background(XP_BLUE_DARK)
    } else {
        Modifier.background(G_GRAY).bevel(true)
    }
    Column(
        Modifier
            .offset { IntOffset(w.x.roundToInt(), w.y.roundToInt()) }
            .zIndex(w.z.toFloat())
            .width(IntrinsicSize.Min)
            .then(frame)
            .padding(3.dp)
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    vm.gameFront(w)
                }
            }
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(if (xp) xpTitleBrush() else androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(G_NAVY, G_NAVY)))
                .pointerInput(Unit) {
                    detectDragGestures { change, drag ->
                        change.consume()
                        w.x = (w.x + drag.x).coerceIn(minX, maxX)
                        w.y = (w.y + drag.y).coerceIn(0f, maxY)
                    }
                }
                .padding(start = 6.dp, top = 3.dp, bottom = 3.dp, end = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (w.kind == "mines") MinesIcon(16.dp) else CardsIcon(16.dp)
            Text(gameTitle(w.kind), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Box(
                Modifier
                    .size(22.dp)
                    .then(
                        if (xp) Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFFD6452E))
                        else Modifier.background(G_GRAY).bevel(true)
                    )
                    .clickable { vm.gameWins.remove(w) },
                contentAlignment = Alignment.Center
            ) {
                Text("X", color = if (xp) Color.White else Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        Box(Modifier.background(if (xp) XP_BEIGE else G_GRAY)) {
            if (w.kind == "mines") MinesBody() else SolBody()
        }
    }
}

@Composable
private fun GBtn(text: String, selected: Boolean = false, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .height(26.dp)
            .background(G_GRAY)
            .bevel(!selected)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.Black, fontSize = 12.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}

// ---------------------------------------------------------------- Minesweeper

private class MinesGame(val cols: Int, val rows: Int, val total: Int) {
    // 0 tertutup, 1 terbuka, 2 bendera
    val cells = mutableStateListOf<Int>().apply { repeat(cols * rows) { add(0) } }
    var mine = BooleanArray(cols * rows)
    var status by mutableIntStateOf(0) // 0 siap, 1 jalan, 2 menang, 3 kalah
    var hit by mutableIntStateOf(-1)

    fun neighbors(i: Int): List<Int> {
        val r = i / cols
        val c = i % cols
        val out = ArrayList<Int>(8)
        for (dr in -1..1) for (dc in -1..1) {
            if (dr == 0 && dc == 0) continue
            val rr = r + dr
            val cc = c + dc
            if (rr in 0 until rows && cc in 0 until cols) out.add(rr * cols + cc)
        }
        return out
    }

    fun adj(i: Int) = neighbors(i).count { mine[it] }

    private fun place(safe: Int) {
        val banned = HashSet<Int>()
        banned.add(safe)
        if (cols * rows - 9 >= total) banned.addAll(neighbors(safe))
        val cand = (0 until cols * rows).filter { it !in banned }.shuffled()
        mine = BooleanArray(cols * rows)
        cand.take(total).forEach { mine[it] = true }
    }

    fun reveal(i: Int) {
        if (status >= 2 || cells[i] != 0) return
        if (status == 0) {
            place(i)
            status = 1
        }
        if (mine[i]) {
            hit = i
            status = 3
            for (k in cells.indices) if (mine[k] && cells[k] != 2) cells[k] = 1
            return
        }
        val q = ArrayDeque<Int>()
        q.add(i)
        cells[i] = 1
        while (q.isNotEmpty()) {
            val cur = q.removeFirst()
            if (adj(cur) != 0) continue
            for (n in neighbors(cur)) {
                if (cells[n] == 0 && !mine[n]) {
                    cells[n] = 1
                    q.add(n)
                }
            }
        }
        checkWin()
    }

    fun flag(i: Int) {
        if (status >= 2) return
        if (cells[i] == 0) cells[i] = 2 else if (cells[i] == 2) cells[i] = 0
    }

    fun chord(i: Int) {
        if (status >= 2 || cells[i] != 1) return
        val a = adj(i)
        if (a == 0) return
        val ns = neighbors(i)
        if (ns.count { cells[it] == 2 } != a) return
        ns.filter { cells[it] == 0 }.forEach { reveal(it) }
    }

    private fun checkWin() {
        for (k in cells.indices) if (!mine[k] && cells[k] != 1) return
        status = 2
        for (k in cells.indices) if (mine[k]) cells[k] = 2
    }

    val flags: Int get() = cells.count { it == 2 }
}

private val MINE_LEVELS = listOf(
    Triple("Pemula", 9 to 9, 10),
    Triple("Menengah", 12 to 12, 22),
    Triple("Ahli", 12 to 18, 40)
)

private val NUM_COLORS = listOf(
    Color.Transparent, Color(0xFF0000FF), Color(0xFF008000), Color(0xFFFF0000), Color(0xFF000080),
    Color(0xFF800000), Color(0xFF008080), Color.Black, Color(0xFF808080)
)

@Composable
private fun MinesBody() {
    var level by remember { mutableIntStateOf(0) }
    var gen by remember { mutableIntStateOf(0) }
    var flagMode by remember { mutableStateOf(false) }
    val lv = MINE_LEVELS[level]
    val g = remember(level, gen) { MinesGame(lv.second.first, lv.second.second, lv.third) }
    var secs by remember(level, gen) { mutableIntStateOf(0) }
    LaunchedEffect(g.status, level, gen) {
        if (g.status == 1) {
            while (true) {
                delay(1000)
                secs++
            }
        }
    }
    var cellDp by remember { mutableFloatStateOf(26f) }
    val cell = cellDp.dp
    val density = LocalDensity.current.density
    Column(Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            MINE_LEVELS.forEachIndexed { i, l -> GBtn(l.first, i == level) { level = i; gen++ } }
        }
        Row(
            Modifier.fillMaxWidth().background(G_GRAY).bevel(false).padding(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            LedText((g.total - g.flags).coerceAtLeast(-99))
            Box(
                Modifier.size(34.dp).background(G_GRAY).bevel(true).clickable { gen++ },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    when (g.status) { 2 -> "B)"; 3 -> "X("; else -> ":)" },
                    color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold
                )
            }
            LedText(secs.coerceAtMost(999))
        }
        Column(Modifier.background(G_GRAY).bevel(false).padding(3.dp)) {
            for (r in 0 until g.rows) {
                Row {
                    for (c in 0 until g.cols) {
                        val i = r * g.cols + c
                        MineCell(g, i, cell, flagMode)
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            GBtn(if (flagMode) "Mode: Bendera" else "Mode: Buka", flagMode) { flagMode = !flagMode }
            Text(
                when (g.status) { 2 -> "Menang!"; 3 -> "Kena ranjau"; else -> "Tahan = bendera" },
                color = Color.Black, fontSize = 12.sp
            )
            Spacer(Modifier.weight(1f))
            // pegangan di pojok kanan bawah: seret untuk mengubah ukuran kotak
            Box(
                Modifier
                    .size(24.dp)
                    .pointerInput(g) {
                        detectDragGestures { change, drag ->
                            change.consume()
                            cellDp = (cellDp + drag.x / density / g.cols).coerceIn(18f, 46f)
                        }
                    },
                contentAlignment = Alignment.BottomEnd
            ) {
                Canvas(Modifier.size(16.dp)) {
                    val w = this.size.width
                    for (k in 1..3) {
                        val o = w * k / 4f
                        drawLine(Color(0xFF404040), Offset(w, o), Offset(o, w), 2f)
                    }
                }
            }
        }
    }
}

@Composable
private fun LedText(v: Int) {
    val t = if (v < 0) "-" + (-v).toString().padStart(2, '0') else v.toString().padStart(3, '0')
    Box(Modifier.background(Color.Black).padding(horizontal = 6.dp, vertical = 2.dp)) {
        Text(t, color = Color(0xFFFF2020), fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun MineCell(g: MinesGame, i: Int, size: Dp, flagMode: Boolean) {
    val st = g.cells[i]
    val open = st == 1
    Box(
        Modifier
            .size(size)
            .then(
                if (open) Modifier.background(if (i == g.hit) Color(0xFFFF2020) else G_GRAY).border(0.5.dp, G_DARK)
                else Modifier.background(G_GRAY).bevel(true)
            )
            .pointerInput(g, i, flagMode) {
                detectTapGestures(
                    onTap = {
                        if (g.cells[i] == 1) g.chord(i)
                        else if (flagMode) g.flag(i)
                        else if (g.cells[i] == 0) g.reveal(i)
                    },
                    onLongPress = { g.flag(i) }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        if (open) {
            if (g.mine[i]) {
                Canvas(Modifier.size(size * 0.6f)) {
                    val c = Offset(this.size.width / 2f, this.size.height / 2f)
                    drawCircle(Color.Black, this.size.width * 0.32f, c)
                    drawLine(Color.Black, Offset(0f, c.y), Offset(this.size.width, c.y), 2f)
                    drawLine(Color.Black, Offset(c.x, 0f), Offset(c.x, this.size.height), 2f)
                }
            } else {
                val n = g.adj(i)
                if (n > 0) Text("$n", color = NUM_COLORS[n], fontSize = (size.value * 0.58f).sp, fontWeight = FontWeight.Bold)
            }
        } else if (st == 2) {
            Canvas(Modifier.size(size * 0.6f)) {
                val w = this.size.width
                val h = this.size.height
                drawRect(Color.Black, Offset(w * 0.45f, h * 0.15f), Size(w * 0.1f, h * 0.7f))
                drawRect(Color.Black, Offset(w * 0.2f, h * 0.8f), Size(w * 0.6f, h * 0.12f))
                val p = Path().apply {
                    moveTo(w * 0.5f, h * 0.1f)
                    lineTo(w * 0.5f, h * 0.55f)
                    lineTo(w * 0.1f, h * 0.32f)
                    close()
                }
                drawPath(p, Color(0xFFE02020))
            }
        }
    }
}

// ---------------------------------------------------------------- Solitaire (Klondike)

private data class Card(val rank: Int, val suit: Int, val up: Boolean = false)

private data class Sol(
    val stock: List<Card>,
    val waste: List<Card>,
    val found: List<List<Card>>,
    val tab: List<List<Card>>,
    val moves: Int = 0
)

private data class Sel(val kind: Int, val col: Int = 0, val idx: Int = 0) // 0 waste, 1 tableau, 2 fondasi

private fun newSol(): Sol {
    val deck = (0..3).flatMap { s -> (1..13).map { Card(it, s) } }.shuffled()
    var p = 0
    val tab = (0 until 7).map { c ->
        val col = (0..c).map { k ->
            val card = deck[p++]
            if (k == c) card.copy(up = true) else card
        }
        col
    }
    return Sol(deck.drop(p), emptyList(), List(4) { emptyList() }, tab)
}

private fun isRed(s: Int) = s == 1 || s == 2

private fun canFound(card: Card, pile: List<Card>) =
    if (pile.isEmpty()) card.rank == 1 else pile.last().suit == card.suit && card.rank == pile.last().rank + 1

private fun canTab(card: Card, col: List<Card>) =
    if (col.isEmpty()) card.rank == 13
    else col.last().up && isRed(col.last().suit) != isRed(card.suit) && col.last().rank == card.rank + 1

private fun Sol.cardsOf(s: Sel): List<Card> = when (s.kind) {
    0 -> waste.takeLast(1)
    1 -> tab[s.col].drop(s.idx)
    else -> found[s.col].takeLast(1)
}

private fun Sol.removeSel(s: Sel): Sol = when (s.kind) {
    0 -> copy(waste = waste.dropLast(1))
    1 -> {
        val rest = tab[s.col].take(s.idx).toMutableList()
        if (rest.isNotEmpty() && !rest.last().up) rest[rest.lastIndex] = rest.last().copy(up = true)
        copy(tab = tab.mapIndexed { i, c -> if (i == s.col) rest else c })
    }
    else -> copy(found = found.mapIndexed { i, c -> if (i == s.col) c.dropLast(1) else c })
}

/** Pindahkan pilihan ke kolom tableau (kind 1) atau fondasi (kind 2). Null jika tidak sah. */
private fun Sol.moveTo(s: Sel, kind: Int, dest: Int): Sol? {
    val cards = cardsOf(s)
    if (cards.isEmpty() || !cards.first().up) return null
    if (kind == 1) {
        if (s.kind == 1 && s.col == dest) return null
        if (!canTab(cards.first(), tab[dest])) return null
        val r = removeSel(s)
        return r.copy(tab = r.tab.mapIndexed { i, c -> if (i == dest) c + cards else c }, moves = moves + 1)
    }
    if (cards.size != 1) return null
    if (s.kind == 2 && s.col == dest) return null
    if (!canFound(cards.first(), found[dest])) return null
    val r = removeSel(s)
    return r.copy(found = r.found.mapIndexed { i, c -> if (i == dest) c + cards else c }, moves = moves + 1)
}

private fun Sol.autoFound(s: Sel): Sol? {
    val cards = cardsOf(s)
    if (cards.size != 1) return null
    for (f in 0 until 4) {
        if (found[f].isNotEmpty() || cards.first().rank == 1) {
            val r = moveTo(s, 2, f)
            if (r != null) return r
        }
    }
    return null
}

private fun Sol.drawStock(n: Int): Sol {
    if (stock.isNotEmpty()) {
        val take = stock.takeLast(n).reversed().map { it.copy(up = true) }
        return copy(stock = stock.dropLast(take.size), waste = waste + take, moves = moves + 1)
    }
    if (waste.isNotEmpty()) {
        return copy(stock = waste.reversed().map { it.copy(up = false) }, waste = emptyList(), moves = moves + 1)
    }
    return this
}

private val SUITS = listOf("♠", "♥", "♦", "♣")
private val RANKS = listOf("", "A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K")
private val CARD_W = 40.dp
private val CARD_H = 56.dp

@Composable
private fun CardView(card: Card?, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(3.dp)
    val base = modifier.size(CARD_W, CARD_H).clip(shape)
    when {
        card == null -> Box(
            base.border(1.dp, Color(0x66FFFFFF), shape).clickable(onClick = onClick)
        )
        !card.up -> Box(
            base.background(Color(0xFF1F3F9E)).border(1.dp, Color.White, shape).clickable(onClick = onClick)
        ) {
            Box(Modifier.padding(3.dp).fillMaxSize().border(1.dp, Color(0xFF8FA6E8)))
        }
        else -> {
            val col = if (isRed(card.suit)) Color(0xFFD02020) else Color.Black
            Box(
                base
                    .background(if (selected) Color(0xFFFFF2A0) else Color.White)
                    .border(if (selected) 2.dp else 1.dp, if (selected) Color(0xFFE0A000) else Color.Black, shape)
                    .clickable(onClick = onClick)
            ) {
                Text(
                    RANKS[card.rank] + SUITS[card.suit], color = col, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 3.dp, top = 2.dp)
                )
                Text(
                    SUITS[card.suit], color = col, fontSize = 22.sp,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(end = 4.dp, bottom = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun SolBody() {
    var sol by remember { mutableStateOf(newSol()) }
    val history = remember { mutableStateListOf<Sol>() }
    var sel by remember { mutableStateOf<Sel?>(null) }
    var draw3 by remember { mutableStateOf(false) }
    val won = sol.found.sumOf { it.size } == 52

    fun apply(n: Sol) {
        history.add(sol)
        sol = n
        sel = null
    }

    fun tryMove(s: Sel, kind: Int, dest: Int): Boolean {
        val r = sol.moveTo(s, kind, dest) ?: return false
        apply(r)
        return true
    }

    Column(
        Modifier.background(Color(0xFF008000)).padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            // tumpukan sisa
            CardView(sol.stock.lastOrNull(), false) { apply(sol.drawStock(if (draw3) 3 else 1)) }
            // buangan
            val wc = sol.waste.lastOrNull()
            CardView(wc, sel?.kind == 0) {
                if (wc != null) {
                    val s = sel
                    if (s != null && s.kind == 0) {
                        val r = sol.autoFound(s)
                        if (r != null) apply(r) else sel = null
                    } else sel = Sel(0)
                }
            }
            Spacer(Modifier.size(CARD_W, CARD_H))
            for (f in 0 until 4) {
                val top = sol.found[f].lastOrNull()
                CardView(top, sel?.kind == 2 && sel?.col == f) {
                    val s = sel
                    if (s != null) {
                        if (!tryMove(s, 2, f)) sel = if (top != null && !(s.kind == 2 && s.col == f)) Sel(2, f) else null
                    } else if (top != null) sel = Sel(2, f)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            for (c in 0 until 7) {
                val col = sol.tab[c]
                var y = 0.dp
                val ys = col.map { card ->
                    val cur = y
                    y += if (card.up) 22.dp else 8.dp
                    cur
                }
                val h = if (col.isEmpty()) CARD_H else ys.last() + CARD_H
                Box(Modifier.width(CARD_W).height(h.coerceAtLeast(CARD_H))) {
                    if (col.isEmpty()) {
                        CardView(null, false) {
                            val s = sel
                            if (s != null) {
                                if (!tryMove(s, 1, c)) sel = null
                            }
                        }
                    }
                    col.forEachIndexed { i, card ->
                        val isSel = sel?.kind == 1 && sel?.col == c && i >= (sel?.idx ?: Int.MAX_VALUE)
                        CardView(card, isSel, Modifier.offset(y = ys[i])) {
                            val s = sel
                            if (s != null) {
                                if (s.kind == 1 && s.col == c && s.idx == i) {
                                    val r = sol.autoFound(s)
                                    if (r != null) apply(r) else sel = null
                                } else if (!tryMove(s, 1, c)) {
                                    sel = if (card.up) Sel(1, c, i) else null
                                }
                            } else if (card.up) sel = Sel(1, c, i)
                        }
                    }
                }
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GBtn("Baru") { sol = newSol(); history.clear(); sel = null }
            GBtn("Urungkan") {
                if (history.isNotEmpty()) {
                    sol = history.removeAt(history.lastIndex)
                    sel = null
                }
            }
            GBtn(if (draw3) "Ambil 3" else "Ambil 1") { draw3 = !draw3 }
            Text(
                if (won) "MENANG!" else "${sol.moves} langkah",
                color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                textAlign = TextAlign.End, modifier = Modifier.weight(1f)
            )
        }
    }
}
