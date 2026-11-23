package pl.mdinstal.muzyka

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

val MusicViolet = Color(0xFF7C3AED)
val MusicPink = Color(0xFFEC4899)
val MusicOrange = Color(0xFFF97316)

/** Czy aktywny jest wariant OLED (czarne tło) — używają go gradienty ekranów. */
val LocalOled = compositionLocalOf { false }

/** Ciemny, „klubowy” motyw: głęboka czerń z fioletem i różowo-pomarańczowe akcenty. */
val ClubColors = darkColorScheme(
    primary = Color(0xFFD946EF), onPrimary = Color.White,
    background = Color(0xFF0B0714), onBackground = Color(0xFFF5F0FF),
    surface = Color(0xFF161025), onSurface = Color(0xFFF5F0FF),
    surfaceVariant = Color(0xFF221A3A), onSurfaceVariant = Color(0xFFA99BC9),
    outline = Color(0xFF6B5C92), outlineVariant = Color(0xFF2D2350),
    error = Color(0xFFF87171)
)

/** Wariant z czystą czernią — oszczędza baterię na ekranach OLED. */
val OledColors = ClubColors.copy(
    background = Color.Black, surface = Color(0xFF0A0A0A),
    surfaceVariant = Color(0xFF16121F), outlineVariant = Color(0xFF241C3F)
)

/** Górny kolor tła gradientu ekranów (Home, odtwarzacz). */
@Composable
fun gradientTop(): Color = if (LocalOled.current) Color(0xFF14091F) else Color(0xFF2A1250)

/** Dolny kolor tła gradientu ekranów. */
@Composable
fun gradientBottom(): Color = if (LocalOled.current) Color.Black else Color(0xFF0B0714)

fun themeLabel(mode: String): String = if (mode == "oled") "Ciemny OLED" else "Klubowy"

@Composable
fun AppTheme(oled: Boolean = false, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalOled provides oled) {
        MaterialTheme(colorScheme = if (oled) OledColors else ClubColors, content = content)
    }
}
