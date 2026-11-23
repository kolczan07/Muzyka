package pl.mdinstal.muzyka

import android.graphics.Color as AColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private var themeMode by mutableStateOf("club")
    private var skin by mutableStateOf("classic")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AColor.TRANSPARENT)
        )
        themeMode = Prefs.theme(this)
        skin = Prefs.playerSkin(this)
        setContent {
            AppTheme(oled = themeMode == "oled") {
                App(
                    themeMode = themeMode,
                    onTheme = { themeMode = it; Prefs.setTheme(this@MainActivity, it) },
                    skin = skin,
                    onSkin = { skin = it; Prefs.setPlayerSkin(this@MainActivity, it) }
                )
            }
        }
    }
}

@Composable
fun App(themeMode: String, onTheme: (String) -> Unit, skin: String, onSkin: (String) -> Unit) {
    val stack = remember { mutableStateListOf(Screen.Home) }
    var splash by remember { mutableStateOf(true) }
    var autoUpdate by remember { mutableStateOf<UpdateInfo?>(null) }
    val ctx = LocalContext.current

    LaunchedEffect(Unit) {
        delay(1600)
        splash = false
        val now = System.currentTimeMillis()
        if (now - Prefs.lastUpdateCheck(ctx) > 6 * 3600 * 1000L) {
            try {
                val info = withContext(Dispatchers.IO) { Updater.latest() }
                Prefs.setLastUpdateCheck(ctx, now)
                if (info != null && Updater.isNewer(info.version, appVersionName(ctx))) autoUpdate = info
            } catch (e: Exception) { }
        }
    }

    fun go(s: Screen) { stack.add(s) }
    fun back() { if (stack.size > 1) stack.removeAt(stack.lastIndex) }
    BackHandler(enabled = stack.size > 1) { back() }

    Box(Modifier.fillMaxSize()) {
        when (stack.last()) {
            Screen.Home -> HomeScreen(onOpen = { go(it) })
            Screen.Radio -> RadioScreen(onBack = { back() }, onOpenPlayer = { go(Screen.Player) })
            Screen.Player -> PlayerScreen(onBack = { back() }, skin = skin)
            Screen.YtMusic -> YtMusicScreen(onBack = { back() }, onOpenPlayer = { go(Screen.Player) })
            Screen.About -> AboutScreen(onBack = { back() })
            Screen.Theme -> ThemeScreen(themeMode, onTheme, skin, onSkin, onBack = { back() })
        }
        autoUpdate?.let { UpdateDialog(it) { autoUpdate = null } }
        AnimatedVisibility(visible = splash, exit = fadeOut()) { BootLogo() }
    }
}
