package com.echotune.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.echotune.app.data.SearchResult

/** One row: thumbnail + title/uploader/meta. Shared by Home, Search and Library lists. */
@Composable
fun MediaRow(item: SearchResult, onClick: (SearchResult) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick(item) }
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = item.thumbnailUrl,
            contentDescription = null,
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(6.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = item.title,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = item.uploader,
                color = Color(0xFFA7A7A7),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = formatDurationViews(item.durationSeconds, item.viewCount),
                color = Color(0xFFA7A7A7),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

fun formatDurationViews(duration: Long, views: Long): String {
    val d = if (duration > 0) formatTime(duration) else "LIVE"
    val v = if (views > 0) formatViews(views) else ""
    return listOfNotNull(d, v.ifEmpty { null }).joinToString(" • ")
}

fun formatTime(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

fun formatViews(views: Long): String = when {
    views >= 1_000_000 -> "%.1fM views".format(views / 1_000_000.0)
    views >= 1_000 -> "%.1fK views".format(views / 1_000.0)
    else -> "$views views"
}
