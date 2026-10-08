package com.wheretoeat

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.filled.Share
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

/** Sends a deal to Messenger, Viber, etc. as a short plain-text message. */
private fun shareDeal(ctx: Context, place: Place, d: Deal) {
    val p = d.promo
    val b = d.branches.first()
    val terms = listOfNotNull(
        dayRuleText(p)?.let { "$it only" } ?: "Any day",
        minSpend(p)?.let { "min ₱${pesos.format(it)}" },
        p.end?.let { "until ${it.format(dateFmt)}" },
    ).joinToString(" · ")
    val maps = "https://www.google.com/maps/search/?api=1&query=" + Uri.encode("${b.merchant} ${b.address}")
    val text = buildString {
        appendLine("${place.name}: ${discountLabel(p)}")
        appendLine("${p.bank} ${d.myCards.joinToString(" or ") { cardName(p.bank, it) }} · $terms")
        appendLine(listOf(b.name.ifBlank { b.merchant }, b.address).filter { it.isNotBlank() }.distinct().joinToString(", "))
        appendLine("Map: $maps")
        if (p.url.isNotBlank()) append("Promo: ${p.url}")
    }.trim()
    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
    ctx.startActivity(Intent.createChooser(send, "Share deal"))
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
private fun NewPill() = Pill("NEW", MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.onTertiary)

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
    // The star pops with a little bounce when it's switched on or off.
    val bounce = remember { Animatable(1f) }
    var last by remember { mutableStateOf(on) }
    LaunchedEffect(on) {
        if (on != last) {
            last = on
            bounce.snapTo(0.4f)
            bounce.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = 500f))
        }
    }
    val haptic = rememberHaptics()
    IconButton(
        onClick = { haptic(Haptic.Confirm); onClick() },
        modifier = Modifier.graphicsLayer { scaleX = bounce.value; scaleY = bounce.value },
    ) {
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
    val banks = remember(all) { all.flatMap { p -> p.deals.map { it.promo.bank } }.distinct().sorted() }
    val bankFilter = state.bankFilter.intersect(banks.toSet())
    val q = state.query.trim()
    val places = all.mapNotNull { p ->
        // With bank chips picked, a place keeps only those banks' deals (so its headline deal is one you can use).
        if (bankFilter.isEmpty()) p else p.deals.filter { it.promo.bank in bankFilter }.takeIf { it.isNotEmpty() }
            ?.let { p.copy(deals = it) }
    }.filter { p ->
        (!state.favoritesOnly || p.key in state.favorites) &&
            (q.isEmpty() || p.name.contains(q, true) || p.deals.any { it.promo.title.contains(q, true) })
    }
    var searching by remember { mutableStateOf(state.query.isNotEmpty()) }
    BackHandler(enabled = searching) { state.query = ""; searching = false }
    LaunchedEffect(Unit) {
        delay(1200)
        state.listIntroDone = true
    }
    val haptic = rememberHaptics()
    // The big "Where to eat" header shrinks into a slim bar as the list scrolls up.
    val collapse = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val actions: @Composable RowScope.() -> Unit = {
        if (!searching) IconButton(onClick = { searching = true }) { Icon(Icons.Default.Search, "Search") }
        if (state.refreshing) {
            CircularProgressIndicator(Modifier.padding(horizontal = 13.dp).size(22.dp), strokeWidth = 2.5.dp)
        } else {
            IconButton(onClick = { haptic(Haptic.Tick); state.refresh() }) { Icon(Icons.Default.Refresh, "Refresh promos") }
        }
        IconButton(onClick = { state.go(Screen.Settings) }) { Icon(Icons.Default.Settings, "Settings") }
    }
    Scaffold(
        modifier = Modifier.nestedScroll(collapse.nestedScrollConnection),
        topBar = {
            if (searching) {
                TopAppBar(title = { SearchField(state) { state.query = ""; searching = false } }, actions = actions)
            } else {
                MediumTopAppBar(title = { Text("Where to eat") }, actions = actions, scrollBehavior = collapse)
            }
        },
    ) { pad ->
        PullToRefreshBox(
            isRefreshing = state.refreshing,
            onRefresh = { haptic(Haptic.Tick); state.refresh() },
            modifier = Modifier.fillMaxSize().padding(pad),
        ) {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
                item {
                    Text(
                        java.time.LocalDate.now().dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() } +
                            " · ${places.size} place" + (if (places.size == 1) "" else "s") + " for your cards",
                        Modifier.padding(start = 16.dp, end = 16.dp, bottom = 10.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                item {
                    // One scrolling row of filters: location, favorites, then a chip per bank.
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AreaChip(areas, area) { haptic(Haptic.Tick); state.chooseArea(it) }
                        FilterChip(
                            selected = state.favoritesOnly,
                            onClick = { haptic(Haptic.Tick); state.favoritesOnly = !state.favoritesOnly },
                            label = { Text("Favorites") },
                            leadingIcon = {
                                Icon(if (state.favoritesOnly) Icons.Default.Star else Icons.Outlined.StarOutline, null,
                                    Modifier.size(18.dp))
                            },
                        )
                        if (banks.size > 1) banks.forEach { b ->
                            FilterChip(
                                selected = b in bankFilter,
                                onClick = { haptic(Haptic.Tick); state.toggleBank(b) },
                                label = { Text(b) },
                                leadingIcon = { BankLogo(b, 18.dp) },
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
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
                itemsIndexed(places, key = { _, p -> p.key }) { i, p ->
                    // animateItem: rows slide into place when filters or the location change.
                    Column(Modifier.animateItem().intro(i, state)) {
                        PlaceRow(p, p.key in state.favorites, isNew = p.deals.any { state.isNew(it.promo.id) }) { state.open(p) }
                        if (i < places.lastIndex) {
                            HorizontalDivider(Modifier.padding(start = 68.dp, end = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                    }
                }
                item { SourcesFooter(state) }
            }
        }
    }
}

/** On the first showing of the list, rows ease up and fade in one after another. */
@Composable
private fun Modifier.intro(index: Int, state: AppState): Modifier {
    val play = !state.listIntroDone && index < 14
    val progress = remember { Animatable(if (play) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (play) {
            delay(45L * index)
            progress.animateTo(1f, tween(450, easing = FastOutSlowInEasing))
        }
    }
    return graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * 36.dp.toPx()
    }
}

/** The location, as a compact chip that opens a menu of every area in the feed. */
@Composable
private fun AreaChip(areas: List<String>, area: String, onPick: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    androidx.compose.foundation.layout.Box {
        FilterChip(
            selected = true,
            onClick = { open = true },
            label = { Text(area, fontWeight = FontWeight.SemiBold) },
            leadingIcon = { Icon(Icons.Default.Place, null, Modifier.size(18.dp)) },
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, "Change location", Modifier.size(18.dp)) },
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            areas.forEach { a ->
                DropdownMenuItem(
                    text = { Text(a, fontWeight = if (a == area) FontWeight.SemiBold else FontWeight.Normal) },
                    leadingIcon = { if (a == area) Icon(Icons.Default.Check, null) },
                    onClick = { onPick(a); open = false },
                )
            }
        }
    }
}

/** Search typed straight into the top bar; the X clears it and closes search. */
@Composable
private fun SearchField(state: AppState, onClose: () -> Unit) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    TextField(
        value = state.query,
        onValueChange = { state.query = it },
        singleLine = true,
        placeholder = { Text("Search restaurants") },
        trailingIcon = { IconButton(onClick = onClose) { Icon(Icons.Default.Close, "Close search") } },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
        ),
        modifier = Modifier.fillMaxWidth().focusRequester(focus),
    )
}

@Composable
private fun Note(text: String) {
    Surface(
        shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Text(text, Modifier.padding(14.dp), color = MaterialTheme.colorScheme.onSecondaryContainer,
            style = MaterialTheme.typography.bodyMedium)
    }
}

/**
 * One restaurant as a two-line row: logo, name, then bank logo + card · day rule (only when limited) · min spend ·
 * end date; the discount sits on the right, dimmed when the deal doesn't work today.
 */
@Composable
private fun PlaceRow(place: Place, favorite: Boolean, isNew: Boolean, onClick: () -> Unit) {
    val d = place.best
    val p = d.promo
    val today = worksToday(p)
    val warn = amber()
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val details = buildAnnotatedString {
        append(cardName(p.bank, d.myCards.first()).replace("Mastercard", "MC"))
        dayRuleText(p)?.let { append(" · "); append(if (today) it else "$it only") }
        minSpend(p)?.let { append(" · Min ₱${pesos.format(it)}") }
        if (endingSoon(p)) {
            append(" · ")
            withStyle(SpanStyle(color = warn, fontWeight = FontWeight.SemiBold)) { append(endsText(p)!!) }
        }
    }
    // Rows dip slightly while pressed.
    val press = remember { MutableInteractionSource() }
    val pressed by press.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, spring(stiffness = Spring.StiffnessMediumLow), label = "press")
    Row(
        Modifier.fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable(interactionSource = press, indication = LocalIndication.current, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlaceLogo(place.name, place.image, 40.dp, Modifier.shared("logo-${place.key}"))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    place.name, Modifier.weight(1f, fill = false).shared("name-${place.key}"),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                if (favorite) Icon(Icons.Default.Star, "Favorite", Modifier.padding(start = 4.dp).size(16.dp), tint = warn)
                if (isNew) Text("NEW", Modifier.padding(start = 6.dp), style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                BankLogo(p.bank, 14.dp)
                Spacer(Modifier.width(6.dp))
                Text(details, style = MaterialTheme.typography.bodySmall, color = muted,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                discountLabel(p).removePrefix("Up to "),
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                color = if (today) MaterialTheme.colorScheme.primary else muted,
            )
            if (place.deals.size > 1) Text("+${place.deals.size - 1} more", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary)
        }
    }
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
        Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp),
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
                    PlaceLogo(place.name, place.image, 84.dp, Modifier.shared("logo-${place.key}"))
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(place.name, Modifier.shared("name-${place.key}"), style = MaterialTheme.typography.headlineSmall)
                        Text(
                            "${place.deals.size} deal" + (if (place.deals.size > 1) "s" else "") + " for your cards in ${state.area}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            items(place.deals, key = { it.promo.id }) { d ->
                DealPanel(d, state.isNew(d.promo.id), onShare = { shareDeal(ctx, place, d) }) { openLink(ctx, d.promo.url) }
            }
            item { Text("Where in ${state.area}", style = MaterialTheme.typography.titleMedium) }
            items(branches) { b ->
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    modifier = Modifier.fillMaxWidth().clickable { openMaps(ctx, b) },
                ) {
                    Row(Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 10.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Place, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(b.name.ifBlank { b.merchant }, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                            if (b.address.isNotBlank() && !b.name.contains(b.address, true)) Text(b.address,
                                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        // Directions straight from the branch, instead of a separate full-width button.
                        FilledTonalIconButton(onClick = { openMaps(ctx, b) }) { Icon(Icons.Default.Directions, "Directions") }
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

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun DealPanel(d: Deal, isNew: Boolean, onShare: () -> Unit, onPromoPage: () -> Unit) {
    val p = d.promo
    val today = worksToday(p)
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CountUpLabel(discountLabel(p), Modifier.weight(1f))
                if (isNew) { NewPill(); Spacer(Modifier.width(10.dp)) }
                BankLogo(p.bank, 32.dp)
            }
            Text(p.description.ifBlank { p.title }, style = MaterialTheme.typography.bodyLarge, color = muted)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            // The key facts, each once: card, days, minimum spend, end date.
            Fact(Icons.Default.CreditCard, "${p.bank} " + d.myCards.joinToString(" or ") { cardName(p.bank, it) })
            val rule = dayRuleText(p)
            Fact(
                Icons.Default.CalendarMonth,
                when {
                    rule == null -> "Any day of the week"
                    today -> "$rule · works today"
                    else -> "$rule only · not today"
                },
                if (today) MaterialTheme.colorScheme.onSurface else muted,
            )
            minSpend(p)?.let { Fact(Icons.Default.Payments, "Minimum spend ₱${pesos.format(it)}") }
            p.end?.let { end ->
                val soon = endingSoon(p)
                Fact(
                    Icons.Default.Event,
                    "Ends ${end.format(dateFmt)}" + if (soon) " · ${endsText(p)!!.lowercase()}" else "",
                    if (soon) amber() else MaterialTheme.colorScheme.onSurface,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                FilledTonalButton(onClick = onShare) {
                    Icon(Icons.Default.Share, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Share")
                }
                if (p.url.isNotBlank()) TextButton(onClick = onPromoPage) {
                    Text("Full terms"); Spacer(Modifier.width(6.dp))
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, null, Modifier.size(16.dp))
                }
            }
        }
    }
}

/** One fact on a deal: an icon and a line of text. */
@Composable
private fun Fact(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, color: Color = MaterialTheme.colorScheme.onSurface) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = color)
    }
}

/** "50% OFF" whose number counts up from 0 when the page opens. */
@Composable
private fun CountUpLabel(label: String, modifier: Modifier) {
    val pct = Regex("""(\d+)%""").find(label)?.groupValues?.get(1)?.toIntOrNull()
    val n = remember(label) { Animatable(0f) }
    LaunchedEffect(label) {
        if (pct != null) n.animateTo(pct.toFloat(), tween(900, delayMillis = 200, easing = FastOutSlowInEasing))
    }
    Text(
        if (pct == null) label else label.replace("$pct%", "${n.value.roundToInt()}%"),
        modifier, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary,
    )
}

// ------------------------------------------------------------------------------------------------ Settings

/** Theme (System / Light / Dark) and colour palette. Changing either spreads out in a circle from the tap. */
@Composable
private fun AppearanceSection(state: AppState) {
    val reveal = LocalReveal.current
    val dark = isDark(state.themeMode)
    val spin by animateFloatAsState(if (dark) 360f else 0f, tween(700, easing = FastOutSlowInEasing), label = "sun-moon")
    Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AnimatedContent(
                    targetState = dark,
                    transitionSpec = {
                        (fadeIn(tween(350)) + scaleIn(tween(450), initialScale = 0.3f)) togetherWith
                            (fadeOut(tween(250)) + scaleOut(tween(300), targetScale = 0.3f))
                    },
                    label = "theme-icon",
                ) { d ->
                    Icon(
                        if (d) Icons.Default.DarkMode else Icons.Default.LightMode, null,
                        Modifier.size(40.dp).graphicsLayer { rotationZ = spin },
                        tint = MaterialTheme.colorScheme.tertiary,
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("Appearance", style = MaterialTheme.typography.titleLarge)
                    Text(
                        when (state.themeMode) {
                            ThemeMode.System -> "Follows your phone (${if (dark) "dark" else "light"} now)"
                            ThemeMode.Light -> "Always light"
                            ThemeMode.Dark -> "Always dark"
                        },
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ThemeMode.entries.forEach { m ->
                    ChoiceTile(
                        label = m.name,
                        icon = when (m) {
                            ThemeMode.System -> Icons.Default.BrightnessAuto
                            ThemeMode.Light -> Icons.Default.LightMode
                            ThemeMode.Dark -> Icons.Default.DarkMode
                        },
                        selected = state.themeMode == m,
                        modifier = Modifier.weight(1f),
                    ) { at -> reveal(at) { state.chooseTheme(m) } }
                }
            }
            Spacer(Modifier.height(18.dp))
            Text("Colours", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ChoiceTile("Classic", Icons.Default.Restaurant, state.palette == Palette.Classic, Modifier.weight(1f)) { at ->
                    reveal(at) { state.choosePalette(Palette.Classic) }
                }
                if (Build.VERSION.SDK_INT >= 31) {
                    ChoiceTile("Material You", Icons.Default.Palette, state.palette == Palette.MaterialYou, Modifier.weight(1f)) { at ->
                        reveal(at) { state.choosePalette(Palette.MaterialYou) }
                    }
                }
            }
        }
    }
}

/** A selectable tile; reports its own centre so the theme change can grow out of it. */
@Composable
private fun ChoiceTile(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    modifier: Modifier,
    onPick: (Offset) -> Unit,
) {
    var center by remember { mutableStateOf(Offset.Zero) }
    val haptic = rememberHaptics()
    val bg by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface, label = "tile-bg")
    val edge by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, label = "tile-edge")
    val lift by animateFloatAsState(if (selected) 1f else 0.96f, spring(dampingRatio = 0.5f), label = "tile-lift")
    Surface(
        onClick = { if (!selected) { haptic(Haptic.Confirm); onPick(center) } },
        shape = RoundedCornerShape(20.dp),
        color = bg,
        border = BorderStroke(if (selected) 2.dp else 1.dp, edge),
        modifier = modifier
            .graphicsLayer { scaleX = lift; scaleY = lift }
            .onGloballyPositioned { center = it.boundsInRoot().center },
    ) {
        Column(Modifier.padding(vertical = 14.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            Text(label, style = MaterialTheme.typography.labelLarge,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
        }
    }
}

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
                title = { Text("Settings", style = MaterialTheme.typography.headlineSmall) },
                navigationIcon = {
                    if (state.myCards.isNotEmpty())
                        IconButton(onClick = { state.back() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
            )
        },
    ) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(16.dp)) {
            item {
                AppearanceSection(state)
                Spacer(Modifier.height(28.dp))
                Text("My cards", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(4.dp))
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
