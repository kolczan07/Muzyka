package pl.mdinstal.muzyka

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

fun skinLabel(skin: String): String = if (skin == "winamp") "Winamp" else "Klasyczny"

/** Motyw aplikacji i wygląd dużego odtwarzacza — z miniaturami, jak w MD-Tools. */
@Composable
fun ThemeScreen(
    themeMode: String, onTheme: (String) -> Unit,
    skin: String, onSkin: (String) -> Unit,
    onBack: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    AppScaffold("Motyw", onBack) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).consumeWindowInsets(pad)
                .verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SectionTitle("Wygląd aplikacji")
            listOf(
                Triple("club", "Klubowy", "Ciemny fiolet z różowymi akcentami"),
                Triple("oled", "Ciemny OLED", "Czarne tło, oszczędza baterię na ekranach OLED")
            ).forEach { (key, title, desc) ->
                OptionCard(title, desc, themeMode == key, { onTheme(key) }) { MiniHome(oled = key == "oled") }
            }

            SectionTitle("Duży odtwarzacz")
            listOf(
                Triple("classic", "Klasyczny", "Duża okładka i okrągły przycisk odtwarzania"),
                Triple("winamp", "Winamp", "Skórka jak w Winampie 2: wyświetlacz LCD, korektor i lista utworów")
            ).forEach { (key, title, desc) ->
                OptionCard(title, desc, skin == key, { onSkin(key) }) {
                    if (key == "winamp") MiniWinamp() else MiniClassic()
                }
            }
            Text(
                "Skórka dotyczy ekranu „Teraz gra”, który otwiera się z mini-odtwarzacza.",
                color = cs.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp), fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun OptionCard(title: String, desc: String, selected: Boolean, onClick: () -> Unit, preview: @Composable () -> Unit) {
    val cs = MaterialTheme.colorScheme
    AppCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.width(78.dp).height(140.dp).clip(RoundedCornerShape(12.dp))
                    .border(BorderStroke(if (selected) 2.dp else 1.dp, if (selected) cs.primary else cs.outlineVariant), RoundedCornerShape(12.dp))
            ) { preview() }
            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                Text(title, fontWeight = FontWeight.Bold, color = cs.onSurface)
                SubText(desc)
            }
            RadioButton(selected = selected, onClick = onClick)
        }
    }
}

@Composable
private fun MiniHome(oled: Boolean) {
    val top = if (oled) Color(0xFF14091F) else Color(0xFF2A1250)
    val bottom = if (oled) Color.Black else Color(0xFF0B0714)
    Column(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(top, bottom))).padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(Modifier.fillMaxWidth(0.5f).height(7.dp).clip(RoundedCornerShape(3.dp)).background(Brush.horizontalGradient(listOf(MusicOrange, MusicPink, MusicViolet))))
        Box(Modifier.align(Alignment.CenterHorizontally).size(44.dp).clip(CircleShape).background(Color(0xFF120C22)).padding(14.dp).clip(CircleShape).background(MusicPink))
        Box(Modifier.fillMaxWidth().height(24.dp).clip(RoundedCornerShape(7.dp)).background(Brush.horizontalGradient(listOf(MusicViolet, Color(0xFF4F46E5)))))
        Box(Modifier.fillMaxWidth().height(24.dp).clip(RoundedCornerShape(7.dp)).background(Brush.horizontalGradient(listOf(MusicPink, MusicOrange))))
    }
}

@Composable
private fun MiniClassic() {
    Column(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF2A1250), Color(0xFF0B0714)))).padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(Modifier.fillMaxWidth().height(62.dp).clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.12f)))
        Box(Modifier.fillMaxWidth(0.7f).height(5.dp).clip(RoundedCornerShape(3.dp)).background(Color.White))
        Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(alpha = 0.2f)))
        Box(Modifier.size(24.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
    }
}

@Composable
private fun MiniWinamp() {
    Column(Modifier.fillMaxSize().background(Color(0xFF232838)).padding(4.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Box(Modifier.fillMaxWidth().height(7.dp).background(Brush.horizontalGradient(listOf(Color(0xFF3E4A78), Color(0xFF1B2140)))))
        Box(Modifier.fillMaxWidth().height(34.dp).background(Color.Black).padding(3.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom, modifier = Modifier.align(Alignment.BottomEnd).height(22.dp)) {
                listOf(8, 14, 20, 12, 17, 9, 15).forEach { h ->
                    Box(Modifier.width(4.dp).height(h.dp).background(Color(0xFF22E000)))
                }
            }
            Box(Modifier.align(Alignment.TopStart).width(20.dp).height(6.dp).background(Color(0xFF22E000)))
        }
        Box(Modifier.fillMaxWidth().height(5.dp).background(Color(0xFF0B0E18)))
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            repeat(5) { Box(Modifier.weight(1f).height(9.dp).background(Color(0xFF59627F))) }
        }
        Box(Modifier.fillMaxSize().background(Color.Black).padding(3.dp), contentAlignment = Alignment.TopStart) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                repeat(4) { i ->
                    Box(Modifier.fillMaxWidth(if (i == 1) 0.9f else 0.7f).height(4.dp).background(if (i == 1) Color(0xFF0000C6) else Color(0xFF22E000)))
                }
            }
        }
    }
}
