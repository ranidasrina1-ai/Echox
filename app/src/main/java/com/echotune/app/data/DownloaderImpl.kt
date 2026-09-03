package com.echotune.app.data

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request as NPRequest
import org.schabi.newpipe.extractor.downloader.Response as NPResponse
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import java.io.IOException
import java.util.concurrent.TimeUnit

class DownloaderImpl private constructor() : Downloader() {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    @Throws(IOException::class, ReCaptchaException::class)
    override fun execute(request: NPRequest): NPResponse {
        val url = request.url()
        val method = request.httpMethod()
        val data = request.dataToSend()
        val headers = request.headers()

        val builder = Request.Builder()
            .url(url)
            .method(
                method,
                data?.toRequestBody("application/x-www-form-urlencoded".toMediaType())
            )

        headers.forEach { (key, values) ->
            values.forEach { value -> builder.addHeader(key, value) }
        }

        if (headers["User-Agent"] == null) {
            builder.header(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
            )
        }
        if (headers["Accept-Language"] == null) {
            builder.header("Accept-Language", "en-US,en;q=0.9")
        }

        val response = client.newCall(builder.build()).execute()

        if (response.code == 429) {
            response.close()
            throw ReCaptchaException("reCaptcha challenge requested", url)
        }

        val body = response.body?.string() ?: ""
        val respHeaders = mutableMapOf<String, List<String>>()
        response.headers.forEach { (name, value) ->
            respHeaders.merge(name, listOf(value)) { a, b -> a + b }
        }

        val latestUrl = response.request.url.toString()
        response.close()

        return NPResponse(
            response.code,
            response.message,
            respHeaders.toMap(),
            body,
            latestUrl
        )
    }

    companion object {
        @Volatile private var instance: DownloaderImpl? = null

        fun getInstance(): DownloaderImpl {
            return instance ?: synchronized(this) {
                instance ?: DownloaderImpl().also { instance = it }
            }
        }
    }
}
