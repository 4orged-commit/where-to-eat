package com.wheretoeat

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.net.ssl.HttpsURLConnection
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

    /** The feed is ~70 KB; anything far bigger isn't ours, so it's refused rather than read into memory. */
    private const val MAX_BYTES = 3 * 1024 * 1024

    /** Downloads the latest feed. Returns it, or null if the phone is offline or the feed is unreadable. */
    suspend fun refresh(ctx: Context): Feed? = withContext(Dispatchers.IO) {
        runCatching {
            val conn = (URL("$FEED_URL?t=${System.currentTimeMillis()}").openConnection() as HttpsURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 20_000
                instanceFollowRedirects = false // only ever this exact https address
                useCaches = false
            }
            check(conn.responseCode == 200) { "feed returned ${conn.responseCode}" }
            val bytes = conn.inputStream.use { input ->
                val out = java.io.ByteArrayOutputStream()
                val buf = ByteArray(16 * 1024)
                while (true) {
                    val n = input.read(buf)
                    if (n < 0) break
                    out.write(buf, 0, n)
                    check(out.size() <= MAX_BYTES) { "feed too large" }
                }
                out.toByteArray()
            }
            val text = String(bytes, Charsets.UTF_8)
            val feed = parseFeed(text) // only keep it if it parses
            cacheFile(ctx).writeText(text)
            feed
        }.getOrNull()
    }
}
