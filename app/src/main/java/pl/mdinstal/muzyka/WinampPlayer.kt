@file:OptIn(ExperimentalFoundationApi::class)

package pl.mdinstal.muzyka

import android.content.Context
import android.media.AudioManager
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.random.Random

// Paleta jak w Winampie 2.x
private val WaDesk = Color(0xFF05070D)
private val WaFace = Color(0xFF39405A)
private val WaLight = Color(0xFF7A84A6)
private val WaDark = Color(0xFF12152A)
private val WaBar1 = Color(0xFF4B5C98)
private val WaBar2 = Color(0xFF1A2147)
private val WaLcd = Color(0xFF22E000)
private val WaLcdDim = Color(0xFF062406)
private val WaSel = Color(0xFF0000C6)
private val WaText = Color(0xFFD8DCEC)

/** Wypukła (raised) lub wklęsła ramka w stylu Winampa. */
private fun Modifier.bevel(raised: Boolean = true, face: Color = WaFace): Modifier = this
    .background(face)
    .drawBehind {
        val hi = if (raised) WaLight else WaDark
        val lo = if (raised) WaDark else WaLight
        val s = 1.5.dp.toPx()
        drawRect(hi, Offset.Zero, Size(size.width, s))
        drawRect(hi, Offset.Zero, Size(s, size.height))
        drawRect(lo, Offset(0f, size.height - s), Size(size.width, s))
        drawRect(lo, Offset(size.width - s, 0f), Size(s, size.height))
    }

private val Mono = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)

/** Skórka „Winamp” dla dużego odtwarzacza: okno główne z wyświetlaczem LCD i okno listy utworów. */
@Composable
internal fun WinampPlayer(
    controller: MediaController?,
    title: String, artist: String,
    playing: Boolean, isYt: Boolean, live: Boolean, empty: Boolean, error: String,
    posMs: Long, durMs: Long,
    shuffle: Boolean, repeat: Int,
    queue: List<QItem>, curIndex: Int,
    onBack: () -> Unit
) {
    val ctx = LocalContext.current
    var dragging by remember { mutableStateOf(false) }
    var dragV by remember { mutableStateOf(0f) }
    var remaining by remember { mutableStateOf(false) }

    // głośność systemowa (strumień muzyki)
    val am = remember { ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVol = remember { am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }
    var vol by remember { mutableStateOf(am.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxVol) }

    val shownPos = if (dragging && !live) (dragV * durMs).toLong() else posMs
    val timeText = when {
        empty || live -> "--:--"
        remaining -> "-" + fmtWa((durMs - shownPos).coerceAtLeast(0L))
        else -> fmtWa(shownPos)
    }
    val marquee = when {
        empty -> "*** NIC NIE GRA ***"
        error.isNotEmpty() -> "*** BŁĄD ODTWARZANIA ***"
        else -> {
            val base = listOf(artist, title).filter { it.isNotBlank() }.joinToString(" - ")
            val n = if (isYt && curIndex >= 0) "${curIndex + 1}. " else ""
            n + base + (if (!live) " (${fmtWa(durMs)})" else "") + "  ***  "
        }
    }

    Column(
        Modifier.fillMaxSize().background(WaDesk).statusBarsPadding().navigationBarsPadding()
            .verticalScroll(rememberScrollState()).padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // ===== okno główne =====
        WaWindow("WINAMP", onClose = onBack) {
            Row(Modifier.fillMaxWidth().height(78.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                // wyświetlacz: status, czas, widmo
                Column(Modifier.weight(0.46f).fillMaxHeight().bevel(false, Color.Black).padding(5.dp)) {
                    Row(Modifier.fillMaxWidth().height(30.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            when { empty -> "■"; playing -> "▶"; else -> "❚❚" },
                            color = WaLcd, fontSize = 11.sp, style = Mono, modifier = Modifier.width(18.dp)
                        )
                        SevenSeg(timeText, Modifier.weight(1f).fillMaxHeight().clickable(enabled = !live && !empty) { remaining = !remaining })
                    }
                    Spacer(Modifier.height(3.dp))
                    Spectrum(active = playing && !empty, modifier = Modifier.fillMaxWidth().weight(1f))
                }
                // tytuł, info, głośność
                Column(Modifier.weight(0.54f).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                    Box(Modifier.fillMaxWidth().height(22.dp).bevel(false, Color.Black).padding(horizontal = 4.dp), contentAlignment = Alignment.CenterStart) {
                        Text(
                            marquee, color = WaLcd, style = Mono, fontSize = 12.sp, maxLines = 1, softWrap = false,
                            modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE, velocity = 40.dp)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Lcd(if (isYt) "YT MUSIC" else "RADIO", Modifier.weight(1f))
                        Lcd(if (live) "LIVE" else "STEREO", Modifier.weight(1f))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("VOL", color = WaText, fontSize = 9.sp, style = Mono, modifier = Modifier.width(24.dp))
                        WaSlider(
                            vol, onChange = { vol = it; am.setStreamVolume(AudioManager.STREAM_MUSIC, (it * maxVol).roundToInt(), 0) },
                            fill = Brush.horizontalGradient(listOf(WaLcd, Color(0xFFE0E000), Color(0xFFE02000))),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(6.dp))
            // pasek postępu
            val seekValue = if (live) 0f else if (dragging) dragV else (posMs.toFloat() / durMs.coerceAtLeast(1L)).coerceIn(0f, 1f)
            WaSlider(
                seekValue,
                onChange = { dragging = true; dragV = it },
                onFinished = {
                    if (dragging) {
                        controller?.seekTo((dragV * durMs).toLong())
                        dragging = false
                    }
                },
                enabled = !live && !empty,
                fill = null,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(6.dp))
            // przyciski transportu
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                WaButton(Icons.Filled.SkipPrevious, "Poprzedni", enabled = isYt, modifier = Modifier.weight(1f)) { controller?.seekToPreviousMediaItem() }
                WaButton(Icons.Filled.PlayArrow, "Odtwarzaj", active = playing, modifier = Modifier.weight(1f)) {
                    controller?.let { c ->
                        if (c.playbackState == Player.STATE_IDLE) c.prepare()
                        c.play()
                    }
                }
                WaButton(Icons.Filled.Pause, "Pauza", modifier = Modifier.weight(1f)) { controller?.pause() }
                WaButton(Icons.Filled.Stop, "Stop", modifier = Modifier.weight(1f)) {
                    controller?.let { it.pause(); it.seekTo(0L) }
                }
                WaButton(Icons.Filled.SkipNext, "Następny", enabled = isYt, modifier = Modifier.weight(1f)) { controller?.seekToNextMediaItem() }
            }
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                WaToggle("SHUFFLE", shuffle, isYt, Modifier.weight(1f)) { controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled } }
                WaToggle(
                    when (repeat) { Player.REPEAT_MODE_ONE -> "REPEAT 1"; else -> "REPEAT" },
                    repeat != Player.REPEAT_MODE_OFF, isYt, Modifier.weight(1f)
                ) {
                    controller?.let {
                        it.repeatMode = when (it.repeatMode) {
                            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                            else -> Player.REPEAT_MODE_OFF
                        }
                    }
                }
            }
        }

        // ===== okno listy utworów =====
        WaWindow("WINAMP PLAYLIST EDITOR") {
            Column(
                Modifier.fillMaxWidth().height(300.dp).bevel(false, Color.Black).verticalScroll(rememberScrollState()).padding(2.dp)
            ) {
                if (empty) {
                    Text("Lista jest pusta.", color = WaLcd, style = Mono, fontSize = 13.sp, modifier = Modifier.padding(6.dp))
                } else if (queue.isEmpty()) {
                    // radio: jedna pozycja — bieżąca stacja
                    WaRow("1. " + listOf(artist, title).filter { it.isNotBlank() }.joinToString(" - "), "LIVE", true) { }
                } else {
                    queue.forEach { q ->
                        WaRow(
                            "${q.index + 1}. " + listOf(q.artist, q.title).filter { it.isNotBlank() }.joinToString(" - "),
                            "", q.index == curIndex
                        ) { controller?.let { it.seekTo(q.index, 0L); it.play() } }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth().bevel(false, Color.Black).padding(horizontal = 6.dp, vertical = 3.dp)) {
                Text(
                    if (queue.isEmpty()) "0/1" else "${(curIndex + 1).coerceAtLeast(0)}/${queue.size}",
                    color = WaLcd, style = Mono, fontSize = 11.sp
                )
                Spacer(Modifier.weight(1f))
                Text(if (live) "--:--" else fmtWa(durMs), color = WaLcd, style = Mono, fontSize = 11.sp)
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

private fun fmtWa(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%02d:%02d".format((s / 60).coerceAtMost(99), s % 60)
}

@Composable
private fun WaWindow(title: String, onClose: (() -> Unit)? = null, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().bevel(true, WaFace).padding(3.dp)) {
        Row(
            Modifier.fillMaxWidth().height(18.dp).background(Brush.horizontalGradient(listOf(WaBar1, WaBar2))),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Stripes(Modifier.weight(1f).fillMaxHeight())
            Text(" $title ", color = WaText, style = Mono, fontSize = 10.sp, letterSpacing = 1.sp, modifier = Modifier.background(WaBar2))
            Stripes(Modifier.weight(1f).fillMaxHeight())
            if (onClose != null) {
                Box(
                    Modifier.padding(2.dp).size(14.dp).bevel(true, WaFace).clickable(onClick = onClose),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Filled.Close, contentDescription = "Zamknij", tint = WaText, modifier = Modifier.size(10.dp)) }
            } else Spacer(Modifier.width(4.dp))
        }
        Spacer(Modifier.height(4.dp))
        content()
        Spacer(Modifier.height(2.dp))
    }
}

@Composable
private fun Stripes(modifier: Modifier) {
    Canvas(modifier) {
        var y = 3.dp.toPx()
        while (y < size.height - 2.dp.toPx()) {
            drawLine(Color(0xFF98A4D6).copy(alpha = 0.55f), Offset(2.dp.toPx(), y), Offset(size.width - 2.dp.toPx(), y), 1.dp.toPx())
            y += 3.dp.toPx()
        }
    }
}

@Composable
private fun Lcd(text: String, modifier: Modifier) {
    Box(modifier.height(16.dp).bevel(false, Color.Black), contentAlignment = Alignment.Center) {
        Text(text, color = WaLcd, style = Mono, fontSize = 9.sp, maxLines = 1)
    }
}

@Composable
private fun WaRow(text: String, right: String, current: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(if (current) WaSel else Color.Transparent).clickable(onClick = onClick).padding(horizontal = 4.dp, vertical = 3.dp)
    ) {
        Text(
            text, color = if (current) Color.White else WaLcd, style = Mono, fontSize = 13.sp,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
        )
        if (right.isNotEmpty()) Text(right, color = if (current) Color.White else WaLcd, style = Mono, fontSize = 13.sp)
    }
}

@Composable
private fun WaButton(icon: ImageVector, desc: String, enabled: Boolean = true, active: Boolean = false, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    Box(
        modifier.height(30.dp).bevel(!pressed && !active, WaFace).clickable(src, null, enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, desc, tint = if (active) WaLcd else if (enabled) WaText else WaText.copy(alpha = 0.3f), modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun WaToggle(text: String, on: Boolean, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.height(22.dp).bevel(!on, WaFace).clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text, style = Mono, fontSize = 10.sp,
            color = if (!enabled) WaText.copy(alpha = 0.3f) else if (on) WaLcd else WaText
        )
    }
}

/** Suwak w stylu Winampa: wklęsły rowek i prostokątna wypukła rączka. */
@Composable
private fun WaSlider(
    value: Float,
    onChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    onFinished: () -> Unit = {},
    enabled: Boolean = true,
    fill: Brush? = null
) {
    var w by remember { mutableStateOf(1f) }
    val change by rememberUpdatedState(onChange)
    val finished by rememberUpdatedState(onFinished)
    val thumbW = 14.dp
    Box(
        modifier.height(20.dp).onSizeChanged { w = it.width.toFloat().coerceAtLeast(1f) }
            .pointerInput(enabled) {
                if (enabled) detectTapGestures(onTap = { o -> change((o.x / w).coerceIn(0f, 1f)); finished() })
            }
            .pointerInput(enabled) {
                if (enabled) detectHorizontalDragGestures(
                    onDragStart = { o -> change((o.x / w).coerceIn(0f, 1f)) },
                    onDragEnd = { finished() },
                    onDragCancel = { finished() }
                ) { ch, _ -> ch.consume(); change((ch.position.x / w).coerceIn(0f, 1f)) }
            }
    ) {
        Box(Modifier.align(Alignment.Center).fillMaxWidth().height(8.dp).bevel(false, Color(0xFF0A0C16))) {
            if (fill != null) Box(Modifier.fillMaxHeight().fillMaxWidth(value.coerceIn(0f, 1f)).padding(2.dp).background(fill))
        }
        Box(
            Modifier.align(Alignment.CenterStart)
                .offset { IntOffset(((w - thumbW.toPx()) * value.coerceIn(0f, 1f)).toInt(), 0) }
                .size(thumbW, 18.dp).bevel(true, if (enabled) Color(0xFF8C94B4) else Color(0xFF4B5068))
        )
    }
}

private val Segments = arrayOf("abcdef", "bc", "abdeg", "abcdg", "bcfg", "acdfg", "acdefg", "abc", "abcdefg", "abcdfg")

/** Czas w postaci siedmiosegmentowych cyfr LCD (obsługuje cyfry, „:” i „-”). */
@Composable
private fun SevenSeg(text: String, modifier: Modifier) {
    Canvas(modifier) {
        val h = size.height
        val t = h * 0.13f
        val dw = h * 0.52f
        val cw = h * 0.26f
        val gap = h * 0.14f
        var total = 0f
        text.forEach { total += (if (it == ':') cw else dw) + gap }
        val k = if (total > size.width) size.width / total else 1f
        var x = 0f
        text.forEach { ch ->
            if (ch == ':') {
                val cx = x + cw * k / 2
                drawCircle(WaLcd, t * 0.6f * k, Offset(cx, h * 0.33f))
                drawCircle(WaLcd, t * 0.6f * k, Offset(cx, h * 0.67f))
                x += (cw + gap) * k
            } else {
                val lit = when {
                    ch.isDigit() -> Segments[ch - '0']
                    ch == '-' -> "g"
                    else -> ""
                }
                val x0 = x
                val w = dw * k
                val hh = h
                val tt = t * k
                fun seg(name: Char, a: Offset, b: Offset) {
                    drawLine(if (name in lit) WaLcd else WaLcdDim, a, b, tt, StrokeCap.Square)
                }
                val l = x0 + tt; val r = x0 + w - tt
                val top = tt; val mid = hh / 2; val bot = hh - tt
                seg('a', Offset(l, top), Offset(r, top))
                seg('g', Offset(l, mid), Offset(r, mid))
                seg('d', Offset(l, bot), Offset(r, bot))
                seg('f', Offset(l - tt * 0.1f, top + tt * 0.6f), Offset(l - tt * 0.1f, mid - tt * 0.6f))
                seg('b', Offset(r + tt * 0.1f, top + tt * 0.6f), Offset(r + tt * 0.1f, mid - tt * 0.6f))
                seg('e', Offset(l - tt * 0.1f, mid + tt * 0.6f), Offset(l - tt * 0.1f, bot - tt * 0.6f))
                seg('c', Offset(r + tt * 0.1f, mid + tt * 0.6f), Offset(r + tt * 0.1f, bot - tt * 0.6f))
                x += (dw + gap) * k
            }
        }
    }
}

/** Widmo jak w Winampie. To animacja (aplikacja nie ma dostępu do surowego dźwięku) — rusza się tylko podczas odtwarzania. */
@Composable
private fun Spectrum(active: Boolean, modifier: Modifier) {
    val n = 19
    var levels by remember { mutableStateOf(FloatArray(n)) }
    var peaks by remember { mutableStateOf(FloatArray(n)) }
    LaunchedEffect(active) {
        val rnd = Random(System.nanoTime())
        while (true) {
            val cur = levels.copyOf()
            val pk = peaks.copyOf()
            for (i in 0 until n) {
                val target = if (active) rnd.nextFloat() * (1f - i.toFloat() / n * 0.55f) else 0f
                cur[i] = if (target > cur[i]) cur[i] + (target - cur[i]) * 0.7f else (cur[i] - 0.09f).coerceAtLeast(0f)
                pk[i] = if (cur[i] >= pk[i]) cur[i] else (pk[i] - 0.025f).coerceAtLeast(0f)
            }
            levels = cur
            peaks = pk
            delay(70)
        }
    }
    Canvas(modifier) {
        val bw = size.width / n
        val seg = 3.dp.toPx()
        val sg = 1.dp.toPx()
        val rows = (size.height / (seg + sg)).toInt().coerceAtLeast(1)
        for (i in 0 until n) {
            val lit = (levels[i] * rows).roundToInt()
            for (r in 0 until lit) {
                val f = r.toFloat() / rows
                val c = if (f < 0.5f) WaLcd else if (f < 0.8f) Color(0xFFE0E000) else Color(0xFFE02000)
                drawRect(c, Offset(i * bw, size.height - (r + 1) * (seg + sg) + sg), Size(bw - sg, seg))
            }
            val p = (peaks[i] * rows).roundToInt()
            if (p > 0) drawRect(Color(0xFFD0D0D0), Offset(i * bw, size.height - p * (seg + sg) + sg), Size(bw - sg, seg))
        }
    }
}
