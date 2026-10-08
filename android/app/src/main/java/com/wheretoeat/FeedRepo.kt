package com.wheretoeat

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Loads the promo feed: the last downloaded copy if there is one, otherwise the copy bundled in the app. */
object FeedRepo {
    const val FEED_URL = "https://4orged-commit.github.io/where-to-eat/promos.json"

    private fun cacheFile(ctx: Context) = File(ctx.filesDir, "promos.json")

    fun load(ctx: Context): Feed {
        val cached = cacheFile(ctx)
        if (cached.exists()) runCatching { return parseFeed(cached.readText()) }
        return parseFeed(ctx.assets.open("promos.json").bufferedReader().use { it.readText() })
    }

    fun cacheAgeHours(ctx: Context): Long {
        val f = cacheFile(ctx)
        return if (f.exists()) (System.currentTimeMillis() - f.lastModified()) / 3_600_000 else Long.MAX_VALUE
    }

    /** Downloads the latest feed. Returns it, or null if the phone is offline or the feed is unreadable. */
    suspend fun refresh(ctx: Context): Feed? = withContext(Dispatchers.IO) {
        runCatching {
            val conn = (URL("$FEED_URL?t=${System.currentTimeMillis()}").openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 20_000
            }
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            val feed = parseFeed(text) // only keep it if it parses
            cacheFile(ctx).writeText(text)
            feed
        }.getOrNull()
    }
}
