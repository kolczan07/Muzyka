package pl.mdinstal.muzyka

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/** Animowany korektor (equalizer) z kolorowych słupków. */
@Composable
fun EqualizerBars(modifier: Modifier = Modifier, bars: Int = 5, barWidth: androidx.compose.ui.unit.Dp = 8.dp) {
    val t = rememberInfiniteTransition(label = "eq")
    val hs = (0 until bars).map { i ->
        t.animateFloat(
            initialValue = 0.2f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(420 + i * 130, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "b$i"
        )
    }
    Canvas(modifier) {
        val bw = barWidth.toPx()
        val gap = (size.width - bw * bars) / (bars - 1).coerceAtLeast(1)
        hs.forEachIndexed { i, h ->
            val bh = size.height * h.value
            drawRoundRect(
                Brush.verticalGradient(listOf(MusicPink, MusicViolet), startY = size.height - bh, endY = size.height),
                topLeft = Offset(i * (bw + gap), size.height - bh),
                size = Size(bw, bh),
                cornerRadius = CornerRadius(bw / 2)
            )
        }
    }
}

/** Ekran startowy: equalizer i litery nazwy wjeżdżające po kolei. */
@Composable
fun BootLogo() {
    Column(
        Modifier.fillMaxSize().background(Color(0xFF0B0714)).padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        EqualizerBars(Modifier.size(width = 90.dp, height = 70.dp), bars = 5, barWidth = 12.dp)
        Spacer(Modifier.height(24.dp))
        Row {
            "Muzyka".forEachIndexed { i, ch ->
                val a = remember { Animatable(0f) }
                LaunchedEffect(Unit) {
                    delay(100L + i * 90L)
                    a.animateTo(1f, tween(350, easing = FastOutSlowInEasing))
                }
                Text(
                    ch.toString(), color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.graphicsLayer { alpha = a.value; translationY = (1f - a.value) * 24.dp.toPx() }
                )
            }
        }
        val sub = remember { Animatable(0f) }
        LaunchedEffect(Unit) { delay(800); sub.animateTo(1f, tween(500)) }
        Text(
            "RADIO  •  YT MUSIC", color = Color(0xFFA99BC9), fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 8.dp).graphicsLayer { alpha = sub.value }
        )
    }
}
