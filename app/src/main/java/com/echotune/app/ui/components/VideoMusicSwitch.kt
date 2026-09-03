package com.echotune.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.echotune.app.ui.Mode

/** Small pill toggle inside the player screen — switches the SAME track between video and audio-only playback. */
@Composable
fun VideoMusicSwitch(mode: Mode, onModeChange: (Mode) -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF282828))
            .padding(4.dp)
    ) {
        SwitchTab("Video", mode == Mode.VIDEO) { onModeChange(Mode.VIDEO) }
        SwitchTab("Music", mode == Mode.MUSIC) { onModeChange(Mode.MUSIC) }
    }
}

@Composable
private fun SwitchTab(label: String, selected: Boolean, onClick: () -> Unit) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) Color(0xFF1DB954) else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = if (selected) Color.Black else Color(0xFFA7A7A7),
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
