package com.wheretoeat

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

/** One promo at one restaurant in the chosen area that the user's cards can use. */
data class Deal(
    val merchant: String,
    val promo: Promo,
    val branches: List<Branch>,
    /** The user's own cards that qualify, as plain card names. */
    val myCards: List<String>,
    val score: Double,
)

/** A restaurant with every deal the user can use there, best (and valid today) first. */
data class Place(
    val key: String,
    val name: String,
    val image: String?,
    val deals: List<Deal>,
) {
    val best get() = deals.first()
}

private val percent = Regex("""(\d{1,2})\s?%""")
private val flat = Regex("""(?:PHP|P|₱)\s?([\d,]+)\s*(?:OFF|cashback|rebate)""", RegexOption.IGNORE_CASE)
private val bogo = Regex("""buy\s*1\s*(?:,)?\s*get\s*1|1[- ]for[- ]1|\bb1t1\b|\b1\+1\b""", RegexOption.IGNORE_CASE)
private val minSpendRx = Regex(
    """minimum(?:\s+(?:single[- ]receipt|dine[- ]in|total))?\s+(?:spend|bill|purchase)(?:\s+of)?\s+(?:PHP|P|₱)\s?([\d,]+)""",
    RegexOption.IGNORE_CASE,
)

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

/** Short label for the headline of a deal, e.g. "50% OFF". */
fun discountLabel(p: Promo): String {
    val text = "${p.title} ${p.description}"
    if (bogo.containsMatchIn(text)) return "1 + 1"
    val pct = percent.findAll(p.title).map { it.groupValues[1].toInt() }.maxOrNull()
        ?: percent.findAll(p.description).map { it.groupValues[1].toInt() }.maxOrNull()
    if (pct != null) return (if (p.title.contains("up to", true)) "Up to " else "") + "$pct% OFF"
    flat.find(text)?.let { return "₱${it.groupValues[1]} OFF" }
    if (text.contains("free", true)) return "FREE"
    return "Promo"
}

/** Minimum spend in pesos, if the promo states one. */
fun minSpend(p: Promo): Int? =
    minSpendRx.find("${p.title} ${p.description}")?.groupValues?.get(1)?.replace(",", "")?.toIntOrNull()

// ------------------------------------------------------------------------------------------------ day rules

private val dayWord = """(monday|tuesday|wednesday|thursday|friday|saturday|sunday|mon|tues?|wed|thu(?:rs?)?|fri|sat|sun)s?"""
private val rangeRx = Regex("""\b$dayWord\s*(?:to|-|–|—|through|thru|until)\s*$dayWord\b""", RegexOption.IGNORE_CASE)
private val singleRx = Regex("""\b$dayWord\b""", RegexOption.IGNORE_CASE)

private fun toDay(word: String): DayOfWeek = when (word.lowercase().take(3)) {
    "mon" -> DayOfWeek.MONDAY
    "tue" -> DayOfWeek.TUESDAY
    "wed" -> DayOfWeek.WEDNESDAY
    "thu" -> DayOfWeek.THURSDAY
    "fri" -> DayOfWeek.FRIDAY
    "sat" -> DayOfWeek.SATURDAY
    else -> DayOfWeek.SUNDAY
}

/** The days of the week a promo is valid, read from its wording; null when it doesn't limit the days. */
fun validDays(p: Promo): Set<DayOfWeek>? {
    var text = "${p.title} ${p.description}"
    val days = mutableSetOf<DayOfWeek>()
    if (Regex("""\bweekdays?\b""", RegexOption.IGNORE_CASE).containsMatchIn(text)) days += DayOfWeek.entries.take(5)
    if (Regex("""\bweekends?\b""", RegexOption.IGNORE_CASE).containsMatchIn(text)) days += listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
    rangeRx.findAll(text).forEach { m ->
        var d = toDay(m.groupValues[1])
        val end = toDay(m.groupValues[2])
        days += d
        while (d != end) { d = d.plus(1); days += d }
    }
    text = rangeRx.replace(text, " ")
    singleRx.findAll(text).forEach { days += toDay(it.groupValues[1]) }
    return days.takeIf { it.isNotEmpty() && it.size < 7 }
}

fun worksToday(p: Promo, today: LocalDate = LocalDate.now()): Boolean =
    validDays(p)?.contains(today.dayOfWeek) ?: true

/** "Mon–Fri", "Fri & Sat", "Tue, Wed & Thu". */
fun dayRuleText(p: Promo): String? {
    val days = validDays(p)?.sorted() ?: return null
    fun short(d: DayOfWeek) = d.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
    val contiguous = days.zipWithNext().all { (a, b) -> b.value == a.value + 1 }
    return when {
        days.size == 1 -> short(days[0]) + "s"
        contiguous && days.size > 2 -> "${short(days.first())}–${short(days.last())}"
        else -> days.dropLast(1).joinToString(", ") { short(it) } + " & " + short(days.last())
    }
}

// ------------------------------------------------------------------------------------------------ matching

/** The user's own cards that qualify for this promo, as plain names. */
fun qualifyingCards(p: Promo, myCards: Set<String>): List<String> {
    val mine = myCards.filter { it.substringBefore('|') == p.bank }.map { it.substringAfter('|') }
    return if (p.cards.isEmpty()) mine else mine.filter { it in p.cards }
}

fun isRunning(p: Promo, now: OffsetDateTime = OffsetDateTime.now()): Boolean =
    (p.start == null || !p.start.isAfter(now)) && (p.end == null || !p.end.isBefore(now))

fun daysLeft(p: Promo, now: OffsetDateTime = OffsetDateTime.now()): Long? =
    p.end?.let { ChronoUnit.DAYS.between(now, it).coerceAtLeast(0) }

/** Same restaurant across banks: "Melo’s Steakhouse" and "Melo's Steakhouse" share a key. */
fun placeKey(name: String): String = name.lowercase().replace('’', '\'').replace(Regex("[^a-z0-9]"), "")

/** Every area that has at least one branch in the feed, for the location picker. */
fun areasOf(feed: Feed): List<String> =
    feed.promos.flatMap { p -> p.branches.map { it.area } }.filter { it.isNotBlank() }.distinct().sorted()
        .ifEmpty { listOf("Alabang", "BGC") }

/** Restaurants in an area with deals the user's cards can use: valid today first, then best discount. */
fun rankPlaces(feed: Feed, area: String, myCards: Set<String>): List<Place> {
    val deals = mutableListOf<Deal>()
    for (p in feed.promos) {
        if (!isRunning(p)) continue
        val mine = qualifyingCards(p, myCards)
        if (mine.isEmpty()) continue
        val score = discountScore(p)
        p.branches.filter { it.area == area }.groupBy { placeKey(it.merchant) }.forEach { (_, branches) ->
            deals += Deal(branches.first().merchant, p, branches, mine, score)
        }
    }
    val order = compareByDescending<Deal> { worksToday(it.promo) }.thenByDescending { it.score }.thenBy { it.promo.end }
    return deals.groupBy { placeKey(it.merchant) }.map { (key, ds) ->
        val sorted = ds.sortedWith(order)
        Place(key, sorted.first().merchant, sorted.firstNotNullOfOrNull { it.promo.image }, sorted)
    }.sortedWith(compareByDescending<Place> { worksToday(it.best.promo) }.thenByDescending { it.best.score }
        .thenBy { it.best.promo.end })
}
