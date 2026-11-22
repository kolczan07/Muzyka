package pl.mdinstal.muzyka

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val MusicViolet = Color(0xFF7C3AED)
val MusicPink = Color(0xFFEC4899)
val MusicOrange = Color(0xFFF97316)

/** Ciemny, „klubowy” motyw: głęboka czerń z fioletem i różowo-pomarańczowe akcenty. */
private val MusicColors = darkColorScheme(
    primary = Color(0xFFD946EF), onPrimary = Color.White,
    background = Color(0xFF0B0714), onBackground = Color(0xFFF5F0FF),
    surface = Color(0xFF161025), onSurface = Color(0xFFF5F0FF),
    surfaceVariant = Color(0xFF221A3A), onSurfaceVariant = Color(0xFFA99BC9),
    outline = Color(0xFF6B5C92), outlineVariant = Color(0xFF2D2350),
    error = Color(0xFFF87171)
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = MusicColors, content = content)
}
