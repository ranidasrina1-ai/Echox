package com.echotune.app.playback

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

/**
 * Holds a single shared [ExoPlayer] instance for the whole app.
 *
 * Both the Video player screen and the Music player screen (and the mini
 * player) read from this same instance, so playback is continuous and
 * background-friendly. The [Player] is created with a single [Player] per
 * process to avoid the cost of re-creating it on each screen.
 */
class PlayerHolder(private val context: Context) {

    val player: ExoPlayer by lazy {
        ExoPlayer.Builder(context)
            .setHandleAudioBecomingNoisy(true)
            .build().apply {
                playWhenReady = true
            }
    }

    /**
     * Called when playback hits an error mid-stream — almost always a
     * throttled/expired googlevideo URL (HTTP 403) rather than a real
     * network outage. The listener receives the position (ms) playback was
     * at, so the caller can re-resolve the stream and resume from there
     * instead of restarting the track from zero.
     */
    var onPlaybackError: ((positionMs: Long, error: PlaybackException) -> Unit)? = null

    init {
        player.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                onPlaybackError?.invoke(player.currentPosition, error)
            }
        })
    }

    fun playUrl(url: String, startPositionMs: Long = 0L) {
        player.setMediaItem(MediaItem.fromUri(url), startPositionMs)
        player.prepare()
        player.play()
    }

    fun togglePlayPause() {
        if (player.isPlaying) player.pause() else player.play()
    }

    fun release() {
        player.release()
    }
}
