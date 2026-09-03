package com.echotune.app.data

import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.stream.VideoStream
import java.io.IOException

object NewPipeRepository {

    private val service: StreamingService = ServiceList.YouTube

    // Stream URLs YouTube hands out are throttled/short-lived (the "n" and
    // signature-cipher params expire). A single extraction attempt sometimes
    // returns a URL that 403s immediately, or the extraction itself hiccups
    // on a transient network/parse error. Retrying a couple of times with a
    // short backoff clears most of these without any extra plumbing.
    private const val MAX_RETRIES = 3
    private const val RETRY_DELAY_MS = 800L

    // The id NewPipeExtractor's YouTube service registers its trending
    // kiosk under (mirrors what the official NewPipe app's "Trending" tab
    // uses under the hood).
    private const val TRENDING_KIOSK_ID = "Trending"

    init {
        NewPipe.init(DownloaderImpl.getInstance())
    }

    fun search(query: String): List<SearchResult> = withRetry {
        val handler = service.searchQHFactory.fromQuery(query, emptyList(), "")
        SearchInfo.getInfo(service, handler).relatedItems.toSearchResults()
    }

    /** Home-tab feed: YouTube's trending kiosk, same data source style used by NewPipe-based apps. */
    fun trending(): List<SearchResult> = withRetry {
        val extractor = service.kioskList.getExtractorById(TRENDING_KIOSK_ID, null)
        extractor.fetchPage()
        extractor.initialPage.items.toSearchResults()
    }

    /**
     * Resolves a playable stream for [url]. Always does a *fresh* extraction
     * (NewPipeExtractor doesn't cache StreamInfo itself), so calling this
     * again later is exactly how you "refresh" an expired/throttled URL —
     * see [refreshStream].
     */
    fun fetchStream(url: String): PlayableStream = withRetry {
        val info = StreamInfo.getInfo(service, url)

        val videoStream: VideoStream? = pickVideoStream(info)
        val audioStream: AudioStream? = info.audioStreams
            .maxByOrNull { it.bitrate }

        PlayableStream(
            title = info.name,
            uploader = info.uploaderName,
            thumbnailUrl = info.thumbnails.firstOrNull()?.url ?: "",
            videoUrl = videoStream?.content,
            audioUrl = audioStream?.content,
            durationSeconds = info.duration
        )
    }

    /** Explicit alias for re-resolving a stream after playback fails (expired/403 URL). */
    fun refreshStream(originalUrl: String): PlayableStream = fetchStream(originalUrl)

    private fun List<InfoItem>.toSearchResults(): List<SearchResult> =
        filterIsInstance<StreamInfoItem>().map { item ->
            SearchResult(
                id = item.url,
                title = item.name,
                uploader = item.uploaderName,
                thumbnailUrl = item.thumbnails.firstOrNull()?.url ?: "",
                url = item.url,
                durationSeconds = item.duration,
                viewCount = item.viewCount
            )
        }

    /**
     * Runs [block], retrying on transient extractor/network failures.
     * Doesn't retry on things a retry can't fix (e.g. bad query/URL).
     */
    private fun <T> withRetry(block: () -> T): T {
        var lastError: Throwable? = null
        repeat(MAX_RETRIES) { attempt ->
            try {
                return block()
            } catch (e: IOException) {
                lastError = e
            } catch (e: ExtractionException) {
                lastError = e
            }
            if (attempt < MAX_RETRIES - 1) {
                Thread.sleep(RETRY_DELAY_MS * (attempt + 1))
            }
        }
        throw lastError ?: IllegalStateException("Unknown extraction failure")
    }

    private fun pickVideoStream(info: StreamInfo): VideoStream? {
        val all = info.videoStreams
        if (all.isEmpty()) return null

        fun height(v: VideoStream): Int =
            try { v.height } catch (_: Throwable) { 0 }

        val atOrAbove = all.filter { height(it) >= 360 }
        val chosen = (atOrAbove.ifEmpty { all })
            .minByOrNull { height(it) }
        return chosen
    }
}
