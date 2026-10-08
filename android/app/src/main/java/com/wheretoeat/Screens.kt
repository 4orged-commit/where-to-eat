package com.wheretoeat

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateFmt = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH)
private val pesos = java.text.NumberFormat.getIntegerInstance(Locale.US)

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

private fun endingSoon(p: Promo) = (daysLeft(p) ?: Long.MAX_VALUE) <= 7

@Composable
private fun amber() = if (isSystemInDarkTheme()) Color(0xFFFFB74D) else Color(0xFFB45F06)

/** "Platinum Mastercard", without repeating the bank when the card name already starts with it. */
private fun cardName(bank: String, card: String) =
    if (card.startsWith(bank, ignoreCase = true)) card.substring(bank.length).trim().ifBlank { card } else card

private fun openMaps(ctx: Context, b: Branch) {
    val label = Uri.encode(b.name.ifBlank { b.merchant })
    val uri = if (b.lat != null && b.lon != null) Uri.parse("geo:${b.lat},${b.lon}?q=${b.lat},${b.lon}($label)")
    else Uri.parse("geo:0,0?q=${Uri.encode("${b.merchant}, ${b.address.ifBlank { b.name }}")}")
    val web = Uri.parse("https://www.google.com/maps/search/?api=1&query=" + Uri.encode("${b.merchant} ${b.address}"))
    try {
        ctx.startActivity(Intent(Intent.ACTION_VIEW, uri))
    } catch (_: Exception) {
        runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, web)) }
    }
}

private fun openLink(ctx: Context, url: String) {
    runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
}

// ------------------------------------------------------------------------------------------------ small pieces

/** A rounded label: the discount, "Works today", "Fri & Sat only". */
@Composable
private fun Pill(text: String, container: Color, content: Color, icon: (@Composable () -> Unit)? = null) {
    Surface(shape = RoundedCornerShape(50), color = container, contentColor = content) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) { icon(); Spacer(Modifier.width(4.dp)) }
            Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}

@Composable
private fun TodayPill(p: Promo) {
    val rule = dayRuleText(p)
    if (worksToday(p)) {
        Pill(
            if (rule == null) "Any day" else "Works today",
            MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer,
        ) { Icon(Icons.Default.CheckCircle, null, Modifier.size(14.dp)) }
    } else {
        Pill(
            "$rule only", MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant,
        ) { Icon(Icons.Default.Schedule, null, Modifier.size(14.dp)) }
    }
}

/** "Min ₱2,500 · Ends in 3 days" (the ending part in amber when it's within a week). */
@Composable
private fun TermsLine(p: Promo) {
    val min = minSpend(p)?.let { "Min ₱${pesos.format(it)}" }
    val ends = endsText(p)
    if (min == null && ends == null) return
    Row {
        if (min != null) Text(min, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (min != null && ends != null) Text("  ·  ", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (ends != null) Text(
            ends, style = MaterialTheme.typography.labelMedium,
            color = if (endingSoon(p)) amber() else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (endingSoon(p)) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

/** Bank logo + the user's qualifying card, e.g. [M] Platinum Mastercard. */
@Composable
private fun CardLine(d: Deal, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        BankLogo(d.promo.bank, 20.dp)
        Spacer(Modifier.width(8.dp))
        Text(
            cardName(d.promo.bank, d.myCards.first()) + if (d.myCards.size > 1) " +${d.myCards.size - 1}" else "",
            style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun FavoriteButton(on: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        if (on) Icon(Icons.Default.Star, "Remove from favorites", tint = amber())
        else Icon(Icons.Outlined.StarOutline, "Add to favorites", tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ------------------------------------------------------------------------------------------------ Home

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(state: AppState) {
    val areas = remember(state.feed) { areasOf(state.feed) }
    val area = if (state.area in areas) state.area else areas.first()
    val all = remember(state.feed, area, state.myCards) { rankPlaces(state.feed, area, state.myCards) }
    val q = state.query.trim()
    val places = all.filter { p ->
        (!state.favoritesOnly || p.key in state.favorites) &&
            (q.isEmpty() || p.name.contains(q, true) || p.deals.any { it.promo.title.contains(q, true) })
    }
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
            if (places.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = {
                        val today = places.filter { worksToday(it.best.promo) }.ifEmpty { places }
                        state.open(today.take(10).random())
                    },
                    icon = { Icon(Icons.Default.Casino, null) },
                    text = { Text("Surprise me") },
                )
            }
        },
    ) { pad ->
        PullToRefreshBox(
            isRefreshing = state.refreshing,
            onRefresh = { state.refresh() },
            modifier = Modifier.fillMaxSize().padding(pad),
        ) {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AreaPicker(areas, area, Modifier.weight(1f)) { state.chooseArea(it) }
                        Spacer(Modifier.width(10.dp))
                        FilterChip(
                            selected = state.favoritesOnly,
                            onClick = { state.favoritesOnly = !state.favoritesOnly },
                            label = { Text("Favorites") },
                            leadingIcon = {
                                Icon(if (state.favoritesOnly) Icons.Default.Star else Icons.Outlined.StarOutline, null,
                                    Modifier.size(18.dp))
                            },
                        )
                    }
                }
                item { SearchField(state) }
                if (state.lastRefreshOk == false) {
                    item { Note("Couldn't reach the promo feed. Showing the last saved promos.") }
                }
                if (places.isEmpty()) {
                    item {
                        Note(
                            when {
                                state.feed.promos.isEmpty() -> "No promos loaded yet."
                                q.isNotEmpty() -> "Nothing matches \"$q\" in $area."
                                state.favoritesOnly -> "None of your favorites have a deal for your cards in $area right now."
                                else -> "No running promos for your cards in $area right now. Check My cards, or try another location."
                            },
                        )
                    }
                }
                items(places, key = { it.key }) { p ->
                    PlaceCard(p, p.key in state.favorites, onFavorite = { state.toggleFavorite(p.key) }) { state.open(p) }
                }
                item { SourcesFooter(state) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AreaPicker(areas: List<String>, area: String, modifier: Modifier, onPick: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = open, onExpandedChange = { open = it }, modifier = modifier) {
        OutlinedTextField(
            value = area,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text("Location") },
            leadingIcon = { Icon(Icons.Default.Place, null) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = open) },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            areas.forEach { a ->
                DropdownMenuItem(
                    text = { Text(a, fontWeight = if (a == area) FontWeight.SemiBold else FontWeight.Normal) },
                    onClick = { onPick(a); open = false },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}

@Composable
private fun SearchField(state: AppState) {
    val focus = LocalFocusManager.current
    OutlinedTextField(
        value = state.query,
        onValueChange = { state.query = it },
        singleLine = true,
        placeholder = { Text("Search restaurants") },
        leadingIcon = { Icon(Icons.Default.Search, null) },
        trailingIcon = {
            if (state.query.isNotEmpty()) IconButton(onClick = { state.query = ""; focus.clearFocus() }) {
                Icon(Icons.Default.Close, "Clear search")
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { focus.clearFocus() }),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun Note(text: String) {
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
        Text(text, Modifier.padding(14.dp), color = MaterialTheme.colorScheme.onSecondaryContainer,
            style = MaterialTheme.typography.bodyMedium)
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun PlaceCard(place: Place, favorite: Boolean, onFavorite: () -> Unit, onClick: () -> Unit) {
    val d = place.best
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = Modifier.animateContentSize(),
    ) {
        Row(Modifier.padding(start = 14.dp, top = 14.dp, bottom = 14.dp)) {
            PlaceLogo(place.name, place.image, 60.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        place.name, Modifier.weight(1f).padding(top = 2.dp),
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                        maxLines = 2, overflow = TextOverflow.Ellipsis,
                    )
                    Box48 { FavoriteButton(favorite, onFavorite) }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Pill(discountLabel(d.promo), MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary)
                    TodayPill(d.promo)
                }
                Spacer(Modifier.height(6.dp))
                TermsLine(d.promo)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.padding(end = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    CardLine(d, Modifier.weight(1f))
                    if (place.deals.size > 1) {
                        val more = place.deals.size - 1
                        Text("+$more more deal" + if (more > 1) "s" else "", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

/** Keeps the star button from pushing the card's text around. */
@Composable
private fun Box48(content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) { content() }
}

@Composable
private fun SourcesFooter(state: AppState) {
    val lines = state.feed.banks.entries.sortedBy { it.key }.joinToString("\n") { (bank, s) ->
        when (s.status) {
            "ok" -> "$bank: updated ${ago(s.updated)}"
            "manual" -> "$bank: checked by hand ${ago(s.updated)}, may be incomplete"
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

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun DetailScreen(state: AppState) {
    val place = state.selected ?: return
    val ctx = LocalContext.current
    val branches = remember(place) { place.deals.flatMap { it.branches }.distinctBy { it.address.lowercase().ifBlank { it.name } } }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(place.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { IconButton(onClick = { state.back() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = { FavoriteButton(place.key in state.favorites) { state.toggleFavorite(place.key) } },
            )
        },
    ) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PlaceLogo(place.name, place.image, 84.dp)
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(place.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                        Text(
                            "${place.deals.size} deal" + (if (place.deals.size > 1) "s" else "") + " for your cards in ${state.area}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            items(place.deals, key = { it.promo.id }) { d -> DealPanel(d) { openLink(ctx, d.promo.url) } }
            item { Text("Where in ${state.area}", style = MaterialTheme.typography.titleMedium) }
            items(branches) { b ->
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    modifier = Modifier.fillMaxWidth().clickable { openMaps(ctx, b) },
                ) {
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
                Button(onClick = { openMaps(ctx, branches.first()) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Place, null); Spacer(Modifier.width(6.dp)); Text("Open in Maps")
                }
            }
            item {
                Text("Promos change. Check the bank's promo page for the full terms before you go.",
                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun DealPanel(d: Deal, onPromoPage: () -> Unit) {
    val p = d.promo
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(discountLabel(p), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                BankLogo(p.bank, 32.dp)
            }
            Text(p.title, style = MaterialTheme.typography.titleMedium)
            if (p.description.isNotBlank()) Text(p.description, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                TodayPill(p)
            }
            TermsLine(p)
            Text(
                "Pay with your ${p.bank} " + d.myCards.joinToString(" or ") { cardName(p.bank, it) },
                style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
            )
            val dates = listOfNotNull(p.start?.let { "From ${it.format(dateFmt)}" }, p.end?.let { "until ${it.format(dateFmt)}" })
            if (dates.isNotEmpty()) Text(dates.joinToString(" "), style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (p.url.isNotBlank()) OutlinedButton(onClick = onPromoPage) { Text("${p.bank} promo page") }
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
                    if (state.myCards.isEmpty()) "Tick the credit cards you own, or the cards of whoever you eat with. The app only shows promos those cards can use."
                    else "Promos are matched to the cards you tick.",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(Modifier.height(8.dp))
            }
            catalog.forEach { (bank, cards) ->
                item {
                    Row(Modifier.padding(top = 16.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        BankLogo(bank, 24.dp)
                        Spacer(Modifier.width(10.dp))
                        Text(bank, style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    }
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
