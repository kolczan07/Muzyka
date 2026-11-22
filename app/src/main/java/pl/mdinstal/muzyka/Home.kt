package pl.mdinstal.muzyka

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Obracająca się płyta winylowa. */
@Composable
fun Vinyl(modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "vinyl")
    val angle = t.animateFloat(0f, 360f, infiniteRepeatable(tween(7000, easing = LinearEasing)), label = "a")
    Canvas(modifier.rotate(angle.value)) {
        val r = size.minDimension / 2
        val c = center
        drawCircle(Color(0xFF120C22), r, c)
        var rr = r * 0.95f
        while (rr > r * 0.4f) { drawCircle(Color(0xFF2A2045), rr, c, style = Stroke(1.2f)); rr -= r * 0.07f }
        drawCircle(Brush.linearGradient(listOf(MusicOrange, MusicPink, MusicViolet), Offset.Zero, Offset(size.width, size.height)), r * 0.34f, c)
        drawCircle(Color(0xFF0B0714), r * 0.06f, c)
        // połysk
        drawArc(Color.White.copy(alpha = 0.07f), -50f, 40f, true, topLeft = Offset.Zero, size = size)
    }
}

@Composable
fun HomeScreen(onOpen: (Screen) -> Unit) {
    Column(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF2A1250), Color(0xFF0B0714)), endY = 900f))
            .statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Muzyka",
                    style = TextStyle(
                        brush = Brush.linearGradient(listOf(MusicOrange, MusicPink, MusicViolet)),
                        fontSize = 38.sp, fontWeight = FontWeight.ExtraBold
                    )
                )
                Text("Co dziś gramy?", color = Color(0xFFA99BC9), fontSize = 15.sp)
            }
            IconButton(onClick = { onOpen(Screen.About) }) {
                Icon(Icons.Filled.Info, contentDescription = "O aplikacji", tint = Color(0xFFA99BC9))
            }
        }
        Box(Modifier.fillMaxWidth().padding(vertical = 22.dp), contentAlignment = Alignment.Center) {
            Vinyl(Modifier.size(190.dp))
        }
        BigTile(
            "Radio", "Stacje internetowe i ulubione", Icons.Filled.Radio,
            listOf(MusicViolet, Color(0xFF4F46E5))
        ) { onOpen(Screen.Radio) }
        Spacer(Modifier.height(14.dp))
        BigTile(
            "YT Music", "Szukaj, playlisty, bez reklam", Icons.Filled.PlayArrow,
            listOf(MusicPink, MusicOrange)
        ) { onOpen(Screen.YtMusic) }
        Spacer(Modifier.weight(1f))
        EqualizerBars(Modifier.align(Alignment.CenterHorizontally).size(width = 70.dp, height = 28.dp), bars = 7, barWidth = 6.dp)
    }
}

@Composable
private fun BigTile(title: String, sub: String, icon: androidx.compose.ui.graphics.vector.ImageVector, colors: List<Color>, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
            .background(Brush.horizontalGradient(colors)).clickable(onClick = onClick).padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(32.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            Text(sub, color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp)
        }
    }
}

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    AppScaffold("O aplikacji", onBack) { pad ->
        Column(Modifier.padding(pad).padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AppCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Muzyka", fontWeight = FontWeight.Bold, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface)
                    SubText("Wersja ${appVersionName(ctx)} • Radio i YouTube Music bez reklam")
                }
            }
            UpdateCard()
            DebugDownloadCard()
        }
    }
}
