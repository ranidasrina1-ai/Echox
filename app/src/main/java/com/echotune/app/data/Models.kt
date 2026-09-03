package com.echotune.app.data

/**
 * A single search result item shown in the results list.
 * Agnostic of video/music mode — the mode only affects how it's played.
 */
data class SearchResult(
    val id: String,
    val title: String,
    val uploader: String,
    val thumbnailUrl: String,
    val url: String,
    val durationSeconds: Long,   // -1 if unknown / live
    val viewCount: Long          // -1 if unknown
)

/**
 * Resolved playable stream for an item.
 * In Video mode we use [videoUrl]; in Music mode we use [audioUrl].
 */
data class PlayableStream(
    val title: String,
    val uploader: String,
    val thumbnailUrl: String,
    val videoUrl: String?,       // 360p (or closest) video-only/combined stream
    val audioUrl: String?,       // highest-bitrate audio-only stream
    val durationSeconds: Long
)
