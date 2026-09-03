package com.echotune.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Recently-played history for the Library tab, persisted locally with
 * SharedPreferences + JSON — no extra DB dependency needed for a simple
 * capped list like this.
 */
object HistoryStore {
    private const val PREFS = "echo_tune_history"
    private const val KEY = "items"
    private const val MAX_ITEMS = 50

    fun getHistory(context: Context): List<SearchResult> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                SearchResult(
                    id = o.optString("id"),
                    title = o.optString("title"),
                    uploader = o.optString("uploader"),
                    thumbnailUrl = o.optString("thumbnailUrl"),
                    url = o.optString("url"),
                    durationSeconds = o.optLong("durationSeconds", -1L),
                    viewCount = o.optLong("viewCount", -1L)
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun addToHistory(context: Context, item: SearchResult) {
        val current = getHistory(context).toMutableList()
        current.removeAll { it.id == item.id }
        current.add(0, item)
        save(context, current.take(MAX_ITEMS))
    }

    fun clear(context: Context) {
        save(context, emptyList())
    }

    private fun save(context: Context, items: List<SearchResult>) {
        val arr = JSONArray()
        items.forEach { item ->
            arr.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("title", item.title)
                    put("uploader", item.uploader)
                    put("thumbnailUrl", item.thumbnailUrl)
                    put("url", item.url)
                    put("durationSeconds", item.durationSeconds)
                    put("viewCount", item.viewCount)
                }
            )
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, arr.toString())
            .apply()
    }
}
