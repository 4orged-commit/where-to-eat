package com.wheretoeat

import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

/** One place in the chosen area with one promo that the user's cards can use. */
data class Deal(
    val merchant: String,
    val promo: Promo,
    val branches: List<Branch>,
    /** The user's own cards that qualify, as plain card names. */
    val myCards: List<String>,
    val score: Double,
)

private val percent = Regex("""(\d{1,2})\s?%""")
private val flat = Regex("""(?:PHP|P|₱)\s?([\d,]+)\s*(?:OFF|cashback|rebate)""", RegexOption.IGNORE_CASE)
private val bogo = Regex("""buy\s*1\s*(?:,)?\s*get\s*1|1[- ]for[- ]1|\bb1t1\b""", RegexOption.IGNORE_CASE)

/** A rough "how good is this" number: percent off, or a flat amount / freebie scaled to look comparable. */
fun discountScore(p: Promo): Double {
    val text = "${p.title} ${p.description}"
    var best = 0.0
    if (bogo.containsMatchIn(text)) best = 40.0
    percent.findAll(p.title).forEach { best = maxOf(best, it.groupValues[1].toDouble()) }
    if (best == 0.0) percent.findAll(p.description).forEach { best = maxOf(best, it.groupValues[1].toDouble()) }
    if (best == 0.0) flat.find(text)?.let {
        val amount = it.groupValues[1].replace(",", "").toDoubleOrNull() ?: 0.0
        best = minOf(35.0, amount / 20.0)
    }
    if (best == 0.0 && text.contains("free", ignoreCase = true)) best = 12.0
    if (best == 0.0) best = 5.0
    if (p.title.contains("up to", ignoreCase = true)) best *= 0.85
    return best
}

/** Short label for the headline of a deal, e.g. "50% OFF". Falls back to the promo title. */
fun discountLabel(p: Promo): String {
    val text = "${p.title} ${p.description}"
    if (bogo.containsMatchIn(text)) return "Buy 1 Get 1"
    val pct = percent.findAll(p.title).map { it.groupValues[1].toInt() }.maxOrNull()
        ?: percent.findAll(p.description).map { it.groupValues[1].toInt() }.maxOrNull()
    if (pct != null) return (if (p.title.contains("up to", true)) "Up to " else "") + "$pct% OFF"
    flat.find(text)?.let { return "₱${it.groupValues[1]} OFF" }
    if (text.contains("free", true)) return "FREE perk"
    return "Promo"
}

/** The user's own cards that qualify for this promo, as plain names. */
fun qualifyingCards(p: Promo, myCards: Set<String>): List<String> {
    val mine = myCards.filter { it.substringBefore('|') == p.bank }.map { it.substringAfter('|') }
    return if (p.cards.isEmpty()) mine else mine.filter { it in p.cards }
}

fun isRunning(p: Promo, now: OffsetDateTime = OffsetDateTime.now()): Boolean =
    (p.start == null || !p.start.isAfter(now)) && (p.end == null || !p.end.isBefore(now))

fun daysLeft(p: Promo, now: OffsetDateTime = OffsetDateTime.now()): Long? =
    p.end?.let { ChronoUnit.DAYS.between(now, it).coerceAtLeast(0) }

/** Every usable deal in an area, best first. A promo with several places becomes one deal per place. */
fun rankDeals(feed: Feed, area: String, myCards: Set<String>): List<Deal> {
    val deals = mutableListOf<Deal>()
    for (p in feed.promos) {
        if (!isRunning(p)) continue
        val mine = qualifyingCards(p, myCards)
        if (mine.isEmpty()) continue
        val score = discountScore(p)
        p.branches.filter { it.area == area }.groupBy { it.merchant }.forEach { (merchant, branches) ->
            deals += Deal(merchant, p, branches, mine, score)
        }
    }
    // One place can carry several promos; show it once per promo but keep the strongest first.
    return deals.sortedWith(compareByDescending<Deal> { it.score }.thenBy { it.promo.end })
}
