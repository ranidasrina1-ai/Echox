package com.echotune.app.ui.screens

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.echotune.app.data.PlayableStream
import com.echotune.app.playback.PlayerHolder
import com.echotune.app.ui.Mode
import com.echotune.app.ui.components.VideoMusicSwitch
import kotlinx.coroutines.delay

/**
 * One unified full-screen player. The Video/Music switch at the top swaps
 * the SAME track between its video and audio-only stream (position kept),
 * rather than being two separate screens.
 */
@Composable
fun PlayerScreen(
    mode: Mode,
    stream: PlayableStream,
    isPlaying: Boolean,
    playerHolder: PlayerHolder,
    onBack: () -> Unit,
    onToggle: () -> Unit,
    onModeChange: (Mode) -> Unit
) {
    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Minimize", tint = Color.White)
                }
                Box(modifier = Modifier.weight(2f), contentAlignment = Alignment.Center) {
                    VideoMusicSwitch(mode = mode, onModeChange = onModeChange)
                }
                Spacer(modifier = Modifier.weight(1f))
            }
        },
        containerColor = Color(0xFF121212)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(8.dp))

            when (mode) {
                Mode.VIDEO -> VideoSurface(playerHolder)
                Mode.MUSIC -> AlbumArt(stream)
            }

            Spacer(Modifier.height(24.dp))

            Text(
                stream.title,
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 2
            )
            Spacer(Modifier.height(4.dp))
            Text(stream.uploader, color = Color(0xFFA7A7A7), style = MaterialTheme.typography.bodyMedium)

            Spacer(Modifier.height(24.dp))

            PlaybackSeekbar(stream, isPlaying, playerHolder)

            Spacer(Modifier.height(16.dp))

            IconButton(
                onClick = onToggle,
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1DB954))
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.Black,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }
}

@Composable
private fun VideoSurface(playerHolder: PlayerHolder) {
    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = playerHolder.player
                // Custom seekbar + play button below handle controls for
                // BOTH modes, so the built-in PlayerView controls stay off.
                useController = false
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black)
    )
}

@Composable
private fun AlbumArt(stream: PlayableStream) {
    AsyncImage(
        model = stream.thumbnailUrl,
        contentDescription = null,
        modifier = Modifier
            .fillMaxWidth(0.75f)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp)),
        contentScale = ContentScale.Crop
    )
}

@Composable
private fun PlaybackSeekbar(stream: PlayableStream, isPlaying: Boolean, playerHolder: PlayerHolder) {
    var position by remember { mutableLongStateOf(0L) }
    val duration = stream.durationSeconds.coerceAtLeast(0L) * 1000L

    LaunchedEffect(isPlaying, stream) {
        while (true) {
            position = playerHolder.player.currentPosition.coerceAtLeast(0L)
            delay(500)
        }
    }

    Column(Modifier.fillMaxWidth()) {
        Slider(
            value = if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f,
            onValueChange = { frac -> playerHolder.player.seekTo((duration * frac).toLong()) },
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color(0xFF1DB954),
                inactiveTrackColor = Color(0xFF555555)
            )
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatMs(position), color = Color(0xFFA7A7A7), style = MaterialTheme.typography.bodySmall)
            Text(formatMs(duration), color = Color(0xFFA7A7A7), style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun formatMs(ms: Long): String {
    val totalSec = ms / 1000
    val m = totalSec / 60
    val s = totalSec % 60
    return "%d:%02d".format(m, s)
}
