package com.wheretoeat

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val AREAS = listOf("Alabang", "BGC")
private val dateFmt = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH)

private fun ago(iso: String?): String {
    val t = runCatching { OffsetDateTime.parse(iso) }.getOrNull() ?: return "never"
    val mins = java.time.Duration.between(t, OffsetDateTime.now()).toMinutes()
    return when {
        mins < 2 -> "just now"
        mins < 60 -> "$mins min ago"
        mins < 60 * 36 -> "${mins / 60} h ago"
        else -> "${mins / 60 / 24} days ago"
    }
}

private fun endsText(p: Promo): String? {
    val left = daysLeft(p) ?: return null
    return when {
        left == 0L -> "Ends today"
        left == 1L -> "Ends tomorrow"
        left <= 14 -> "Ends in $left days"
        else -> "Until ${p.end!!.format(dateFmt)}"
    }
}

/** "Metrobank · Platinum Mastercard", without repeating the bank when the card name already starts with it. */
private fun cardLabel(bank: String, card: String) =
    if (card.startsWith(bank, ignoreCase = true)) card else "$bank · $card"

private fun openMaps(ctx: Context, b: Branch) {
    val label = Uri.encode(b.name.ifBlank { b.merchant })
    val uri = if (b.lat != null && b.lon != null) Uri.parse("geo:${b.lat},${b.lon}?q=${b.lat},${b.lon}($label)")
    else Uri.parse("geo:0,0?q=${Uri.encode(b.address.ifBlank { b.name })}")
    val web = Uri.parse("https://www.google.com/maps/search/?api=1&query=" + Uri.encode("${b.name} ${b.address}"))
    try {
        ctx.startActivity(Intent(Intent.ACTION_VIEW, uri))
    } catch (_: Exception) {
        runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, web)) }
    }
}

private fun openLink(ctx: Context, url: String) {
    runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
}

// ------------------------------------------------------------------------------------------------ Home

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(state: AppState) {
    val deals = remember(state.feed, state.area, state.myCards) { rankDeals(state.feed, state.area, state.myCards) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Where to eat", fontWeight = FontWeight.SemiBold) },
                actions = {
                    if (state.refreshing) {
                        CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.5.dp)
                        Spacer(Modifier.width(12.dp))
                    } else {
                        IconButton(onClick = { state.refresh() }) { Icon(Icons.Default.Refresh, "Refresh promos") }
                    }
                    IconButton(onClick = { state.go(Screen.Settings) }) { Icon(Icons.Default.Settings, "My cards") }
                },
            )
        },
        floatingActionButton = {
            if (deals.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { state.open(deals.take(10).random()) },
                    icon = { Icon(Icons.Default.Casino, null) },
                    text = { Text("Surprise me") },
                )
            }
        },
    ) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    AREAS.forEachIndexed { i, a ->
                        SegmentedButton(
                            selected = state.area == a,
                            onClick = { state.chooseArea(a) },
                            shape = SegmentedButtonDefaults.itemShape(i, AREAS.size),
                        ) { Text(a) }
                    }
                }
            }
            if (state.lastRefreshOk == false) {
                item { Note("Couldn't reach the promo feed. Showing the last saved promos.") }
            }
            if (deals.isEmpty()) {
                item {
                    Note(
                        if (state.feed.promos.isEmpty()) "No promos loaded yet."
                        else "No running promos for your cards in ${state.area} right now. Check My cards, or try the other area.",
                    )
                }
            }
            items(deals, key = { it.promo.id + it.merchant }) { d -> DealCard(d) { state.open(d) } }
            item { SourcesFooter(state) }
        }
    }
}

@Composable
private fun Note(text: String) {
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
        Text(text, Modifier.padding(14.dp), color = MaterialTheme.colorScheme.onSecondaryContainer,
            style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun DealCard(d: Deal, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                Text(
                    discountLabel(d.promo).replace("Up to ", "Up to\n").replace(" OFF", "\nOFF"),
                    Modifier.width(80.dp).padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 3,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(d.merchant, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(d.promo.title, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                Spacer(Modifier.height(6.dp))
                Text(
                    cardLabel(d.promo.bank, d.myCards.first()) + if (d.myCards.size > 1) " +${d.myCards.size - 1}" else "",
                    style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary,
                )
                endsText(d.promo)?.let {
                    Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun SourcesFooter(state: AppState) {
    val lines = state.feed.banks.entries.sortedBy { it.key }.joinToString("\n") { (bank, s) ->
        when (s.status) {
            "ok" -> "$bank: updated ${ago(s.updated)}"
            "manual" -> "$bank: checked by hand ${ago(s.updated)}"
            "stale" -> "$bank: couldn't refresh, showing older promos"
            else -> "$bank: not available yet"
        }
    }
    Text(
        "Promo sources\n$lines",
        Modifier.padding(top = 8.dp, start = 4.dp),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

// ------------------------------------------------------------------------------------------------ Detail

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(state: AppState) {
    val deal = state.selected ?: return
    val ctx = LocalContext.current
    val p = deal.promo
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(deal.merchant, maxLines = 1) },
                navigationIcon = { IconButton(onClick = { state.back() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            )
        },
    ) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Column(Modifier.fillMaxWidth().padding(20.dp)) {
                        Text(discountLabel(p), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(p.title, style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (p.description.isNotBlank()) Text(p.description, style = MaterialTheme.typography.bodyLarge)
                    Text("Use your " + deal.myCards.joinToString(" or ") { cardLabel(p.bank, it).replace(" · ", " ") },
                        fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    val dates = listOfNotNull(
                        p.start?.let { "From ${it.format(dateFmt)}" },
                        p.end?.let { "until ${it.format(dateFmt)}" },
                    ).joinToString(" ")
                    val soon = daysLeft(p)?.takeIf { it <= 14 }?.let { endsText(p) }
                    if (dates.isNotBlank()) Text(dates + (soon?.let { " ($it)" } ?: ""),
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item { Text("Where in ${state.area}", style = MaterialTheme.typography.titleMedium) }
            items(deal.branches) { b ->
                Card(shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    modifier = Modifier.fillMaxWidth().clickable { openMaps(ctx, b) }) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Place, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(b.name.ifBlank { b.merchant }, fontWeight = FontWeight.SemiBold)
                            if (b.address.isNotBlank()) Text(b.address, style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = { openMaps(ctx, deal.branches.first()) }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Place, null); Spacer(Modifier.width(6.dp)); Text("Open in Maps")
                    }
                    if (p.url.isNotBlank()) OutlinedButton(onClick = { openLink(ctx, p.url) }, modifier = Modifier.weight(1f)) {
                        Text("Promo page")
                    }
                }
            }
            item {
                Text("Promos change. Check the bank's promo page for the full terms before you go.",
                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ------------------------------------------------------------------------------------------------ Settings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(state: AppState) {
    val catalog = remember(state.feed) {
        val m = state.feed.cards.mapValues { it.value.toMutableList() }.toMutableMap()
        state.feed.promos.forEach { p -> p.cards.forEach { c ->
            val l = m.getOrPut(p.bank) { mutableListOf() }
            if (c !in l) l += c
        } }
        m.toSortedMap()
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My cards") },
                navigationIcon = {
                    if (state.myCards.isNotEmpty())
                        IconButton(onClick = { state.back() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
            )
        },
    ) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(16.dp)) {
            item {
                Text(
                    if (state.myCards.isEmpty()) "Tick the credit cards you own. The app only shows promos you can use."
                    else "Promos are matched to the cards you tick.",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(Modifier.height(8.dp))
            }
            catalog.forEach { (bank, cards) ->
                item {
                    Text(bank, Modifier.padding(top = 14.dp, bottom = 2.dp), style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                }
                items(cards) { c ->
                    val key = "$bank|$c"
                    Row(Modifier.fillMaxWidth().clickable { state.toggleCard(key) }.padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = key in state.myCards, onCheckedChange = { state.toggleCard(key) })
                        Text(c)
                    }
                }
            }
            item {
                Spacer(Modifier.height(20.dp))
                FilledTonalButton(onClick = { state.back() }, enabled = state.myCards.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()) { Text("Done") }
            }
        }
    }
}
