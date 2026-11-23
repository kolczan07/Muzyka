package pl.mdinstal.muzyka

import android.content.ComponentName
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import kotlinx.coroutines.delay

internal data class QItem(val index: Int, val id: String, val title: String, val artist: String, val thumb: String)

private fun fmt(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}

/** Duży odtwarzacz: okładka, pasek postępu, sterowanie, losowo/powtarzanie i kolejka. Otwiera się z mini-odtwarzacza. */
@Composable
fun PlayerScreen(onBack: () -> Unit, skin: String = "classic") {
    val ctx = LocalContext.current
    val cs = MaterialTheme.colorScheme

    var controller by remember { mutableStateOf<MediaController?>(null) }
    var title by remember { mutableStateOf("") }
    var artist by remember { mutableStateOf("") }
    var artUri by remember { mutableStateOf("") }
    var artData by remember { mutableStateOf<ByteArray?>(null) }
    var playing by remember { mutableStateOf(false) }
    var isYt by remember { mutableStateOf(false) }
    var shuffle by remember { mutableStateOf(false) }
    var repeat by remember { mutableStateOf(Player.REPEAT_MODE_OFF) }
    var queue by remember { mutableStateOf<List<QItem>>(emptyList()) }
    var curIndex by remember { mutableStateOf(-1) }
    var error by remember { mutableStateOf("") }
    var empty by remember { mutableStateOf(false) }

    var posMs by remember { mutableLongStateOf(0L) }
    var durMs by remember { mutableLongStateOf(0L) }
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableStateOf(0f) }

    fun refresh(c: MediaController) {
        val cur = c.currentMediaItem
        empty = cur == null
        title = cur?.mediaMetadata?.title?.toString().orEmpty()
        artist = cur?.mediaMetadata?.artist?.toString().orEmpty()
        artUri = cur?.mediaMetadata?.artworkUri?.toString().orEmpty()
        artData = cur?.mediaMetadata?.artworkData
        isYt = cur?.localConfiguration?.uri?.scheme == "ytm"
        playing = c.isPlaying
        shuffle = c.shuffleModeEnabled
        repeat = c.repeatMode
        curIndex = c.currentMediaItemIndex
        queue = if (isYt) (0 until c.mediaItemCount).map { i ->
            val m: MediaItem = c.getMediaItemAt(i)
            QItem(i, m.mediaId, m.mediaMetadata.title?.toString().orEmpty(), m.mediaMetadata.artist?.toString().orEmpty(), m.mediaMetadata.artworkUri?.toString().orEmpty())
        } else emptyList()
    }

    DisposableEffect(Unit) {
        val listener = object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) { (player as? MediaController)?.let { refresh(it) } }
            override fun onPlayerError(e: PlaybackException) {
                error = e.errorCodeName + (e.cause?.message?.let { ": $it" } ?: "")
            }
            override fun onPlaybackStateChanged(state: Int) { if (state == Player.STATE_READY) error = "" }
        }
        val token = SessionToken(ctx, ComponentName(ctx, RadioService::class.java))
        val future = MediaController.Builder(ctx, token).buildAsync()
        future.addListener({
            try {
                val c = future.get()
                controller = c
                refresh(c)
                c.addListener(listener)
            } catch (e: Exception) { }
        }, ContextCompat.getMainExecutor(ctx))
        onDispose {
            try { controller?.removeListener(listener) } catch (e: Exception) { }
            MediaController.releaseFuture(future)
            controller = null
        }
    }

    // pozycja odtwarzania odświeżana kilka razy na sekundę
    LaunchedEffect(controller) {
        while (true) {
            val c = controller
            if (c != null && !dragging) {
                posMs = c.currentPosition
                durMs = c.duration.takeIf { it != C.TIME_UNSET && it > 0 } ?: 0L
            }
            delay(400)
        }
    }

    // okładka: z danych w metadanych (radio) albo z adresu (YT Music), w wyższej rozdzielczości
    val art by produceState<Bitmap?>(null, artUri, artData) {
        val d = artData
        value = if (d != null) BitmapFactory.decodeByteArray(d, 0, d.size)
        else if (artUri.startsWith("http")) LogoCache.load(ctx, artUri.replace(Regex("=w\\d+-h\\d+"), "=w600-h600"), 600)
        else null
    }

    val live = !isYt || durMs <= 0L

    if (skin == "winamp") {
        WinampPlayer(
            controller, title, artist, playing, isYt, live, empty, error,
            posMs, durMs, shuffle, repeat, queue, curIndex, onBack
        )
        return
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(gradientTop(), gradientBottom())))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Zamknij odtwarzacz", tint = Color.White, modifier = Modifier.size(32.dp))
            }
            Text("Teraz gra", color = Color.White.copy(alpha = 0.85f), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            Spacer(Modifier.width(48.dp))
        }

        if (empty) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Nic nie gra. Wybierz utwór lub stację.", color = Color.White.copy(alpha = 0.7f))
            }
            return@Column
        }

        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(8.dp))
            // okładka
            Box(
                Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(24.dp)).background(Color.White.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                val b = art
                if (b != null) {
                    Image(b.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    Icon(Icons.Filled.MusicNote, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(96.dp))
                }
            }

            Spacer(Modifier.height(22.dp))
            Text(title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
            if (artist.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(artist, color = Color.White.copy(alpha = 0.7f), fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (error.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text("Błąd odtwarzania", color = cs.error, fontSize = 13.sp)
            }

            Spacer(Modifier.height(14.dp))
            // pasek postępu (radio i transmisje na żywo nie mają długości)
            if (live) {
                Box(
                    Modifier.clip(RoundedCornerShape(50)).background(cs.primary.copy(alpha = 0.25f)).padding(horizontal = 14.dp, vertical = 6.dp)
                ) { Text("NA ŻYWO", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp) }
            } else {
                val value = if (dragging) dragValue else (posMs.toFloat() / durMs).coerceIn(0f, 1f)
                Slider(
                    value = value,
                    onValueChange = { dragging = true; dragValue = it },
                    onValueChangeFinished = {
                        controller?.seekTo((dragValue * durMs).toLong())
                        posMs = (dragValue * durMs).toLong()
                        dragging = false
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = cs.primary,
                        inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                    )
                )
                Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                    Text(fmt(if (dragging) (dragValue * durMs).toLong() else posMs), color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                    Spacer(Modifier.weight(1f))
                    Text(fmt(durMs), color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                }
            }

            Spacer(Modifier.height(10.dp))
            // sterowanie
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled } }, enabled = isYt) {
                    Icon(
                        Icons.Filled.Shuffle, contentDescription = "Losowo",
                        tint = if (!isYt) Color.White.copy(alpha = 0.2f) else if (shuffle) cs.primary else Color.White.copy(alpha = 0.6f)
                    )
                }
                IconButton(onClick = { controller?.seekToPreviousMediaItem() }, enabled = isYt) {
                    Icon(Icons.Filled.SkipPrevious, contentDescription = "Poprzedni", tint = if (isYt) Color.White else Color.White.copy(alpha = 0.25f), modifier = Modifier.size(38.dp))
                }
                Box(
                    Modifier.size(68.dp).clip(CircleShape).background(cs.primary).clickable {
                        controller?.let { c ->
                            if (c.isPlaying) c.pause() else {
                                if (c.playbackState == Player.STATE_IDLE) c.prepare()
                                c.play()
                            }
                        }
                    },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (playing) "Wstrzymaj" else "Odtwarzaj",
                        tint = Color.White, modifier = Modifier.size(38.dp)
                    )
                }
                IconButton(onClick = { controller?.seekToNextMediaItem() }, enabled = isYt) {
                    Icon(Icons.Filled.SkipNext, contentDescription = "Następny", tint = if (isYt) Color.White else Color.White.copy(alpha = 0.25f), modifier = Modifier.size(38.dp))
                }
                IconButton(
                    onClick = {
                        controller?.let {
                            it.repeatMode = when (it.repeatMode) {
                                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                                else -> Player.REPEAT_MODE_OFF
                            }
                        }
                    },
                    enabled = isYt
                ) {
                    Icon(
                        if (repeat == Player.REPEAT_MODE_ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                        contentDescription = "Powtarzanie",
                        tint = if (!isYt) Color.White.copy(alpha = 0.2f) else if (repeat != Player.REPEAT_MODE_OFF) cs.primary else Color.White.copy(alpha = 0.6f)
                    )
                }
            }

            // kolejka (tylko YT Music)
            if (queue.size > 1) {
                Spacer(Modifier.height(22.dp))
                Text(
                    "Kolejka · ${queue.size}",
                    color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                )
                queue.forEach { q ->
                    val isCur = q.index == curIndex
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isCur) cs.primary.copy(alpha = 0.18f) else Color.Transparent)
                            .clickable { controller?.let { it.seekTo(q.index, 0L); it.play() } }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        QueueThumb(q.thumb)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(q.title, color = if (isCur) cs.primary else Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(q.artist, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            } else {
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun QueueThumb(url: String) {
    val ctx = LocalContext.current
    val bmp by produceState<Bitmap?>(null, url) {
        value = if (url.startsWith("http")) LogoCache.load(ctx, url) else null
    }
    val b = bmp
    Box(
        Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.08f)),
        contentAlignment = Alignment.Center
    ) {
        if (b != null) Image(b.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        else Icon(Icons.Filled.MusicNote, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
    }
}
