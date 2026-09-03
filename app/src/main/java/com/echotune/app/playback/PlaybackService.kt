package com.echotune.app.playback

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * Foreground media-playback service so audio keeps playing in the background
 * (required by the spec: "Audio should keep playing in background").
 *
 * Exposes a [MediaSession] backed by the shared ExoPlayer so the system
 * media controls, lock screen, and notification can drive playback.
 */
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    companion object {
        const val CHANNEL_ID = "echo_tune_playback"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    fun setPlayer(playerHolder: PlayerHolder) {
        // Recreate the session bound to the current player.
        mediaSession?.release()
        mediaSession = MediaSession.Builder(this, playerHolder.player).build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Echo Tune Playback",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Media playback controls"
        }
        val manager = ContextCompat.getSystemService(this, NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }
}
