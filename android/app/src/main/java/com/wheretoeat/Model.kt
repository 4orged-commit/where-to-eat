package com.wheretoeat

import org.json.JSONArray
import org.json.JSONObject
import java.time.OffsetDateTime

data class Branch(
    val merchant: String,
    val name: String,
    val address: String,
    val area: String,
    val lat: Double?,
    val lon: Double?,
)

data class Promo(
    val id: String,
    val bank: String,
    val title: String,
    val description: String,
    /** Cards the promo applies to; empty means every card from that bank. */
    val cards: List<String>,
    val start: OffsetDateTime?,
    val end: OffsetDateTime?,
    val url: String,
    /** The restaurant's logo / promo thumbnail, if the bank provides one. */
    val image: String?,
    val branches: List<Branch>,
)

data class BankStatus(
    /** ok = fetched automatically, manual = checked by hand, stale = last automatic read failed. */
    val status: String,
    val updated: String?,
    val count: Int,
)

data class Feed(
    val updated: String?,
    val banks: Map<String, BankStatus>,
    val cards: Map<String, List<String>>,
    val promos: List<Promo>,
)

private fun JSONObject.str(key: String): String? =
    if (isNull(key)) null else optString(key).takeIf { it.isNotBlank() }

/**
 * Links and images from the feed are only used if they're plain https web addresses. The app hands promo links to
 * the phone to open, so this stops a tampered feed from making it open anything else (another app, a file, an intent).
 */
private fun safeWebUrl(s: String?): String? =
    s?.trim()?.takeIf { url ->
        runCatching { java.net.URI(url) }.getOrNull()?.let { it.scheme == "https" && !it.host.isNullOrBlank() } == true
    }

private fun parseDate(s: String?): OffsetDateTime? =
    runCatching { OffsetDateTime.parse(s) }.getOrNull()

private fun JSONArray?.strings(): List<String> =
    if (this == null) emptyList() else List(length()) { getString(it) }

fun parseFeed(text: String): Feed {
    val root = JSONObject(text)
    val banks = mutableMapOf<String, BankStatus>()
    root.optJSONObject("banks")?.let { b ->
        b.keys().forEach { k ->
            val o = b.getJSONObject(k)
            banks[k] = BankStatus(o.optString("status"), o.str("updated"), o.optInt("count"))
        }
    }
    val cards = mutableMapOf<String, List<String>>()
    root.optJSONObject("cards")?.let { c -> c.keys().forEach { k -> cards[k] = c.optJSONArray(k).strings() } }
    val promos = root.optJSONArray("promos")?.let { arr ->
        List(arr.length()) { i ->
            val p = arr.getJSONObject(i)
            val branches = p.optJSONArray("branches")?.let { br ->
                List(br.length()) { j ->
                    val b = br.getJSONObject(j)
                    Branch(
                        merchant = b.str("merchant") ?: p.optString("title"),
                        name = b.str("name") ?: "",
                        address = b.str("address") ?: "",
                        area = b.optString("area"),
                        lat = if (b.isNull("lat")) null else b.optDouble("lat"),
                        lon = if (b.isNull("lon")) null else b.optDouble("lon"),
                    )
                }
            } ?: emptyList()
            Promo(
                id = p.getString("id"),
                bank = p.getString("bank"),
                title = p.getString("title"),
                description = p.str("description") ?: "",
                cards = p.optJSONArray("cards").strings(),
                start = parseDate(p.str("start")),
                end = parseDate(p.str("end")),
                url = safeWebUrl(p.str("url")) ?: "",
                image = safeWebUrl(p.str("image")),
                branches = branches,
            )
        }
    } ?: emptyList()
    return Feed(root.str("updated"), banks, cards, promos)
}
