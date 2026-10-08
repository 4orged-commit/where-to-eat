package com.wheretoeat

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

enum class Screen { Home, Detail, Settings }

class AppState(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences("wheretoeat", Context.MODE_PRIVATE)

    var feed by mutableStateOf(FeedRepo.load(app))
        private set
    var area by mutableStateOf(prefs.getString("area", "BGC") ?: "BGC")
        private set
    var myCards by mutableStateOf(prefs.getStringSet("cards", emptySet())!!.toSet())
        private set
    var favorites by mutableStateOf(prefs.getStringSet("favorites", emptySet())!!.toSet())
        private set
    var themeMode by mutableStateOf(
        runCatching { ThemeMode.valueOf(prefs.getString("theme", "")!!) }.getOrDefault(ThemeMode.System))
        private set
    var palette by mutableStateOf(
        runCatching { Palette.valueOf(prefs.getString("palette", "")!!) }.getOrDefault(Palette.Classic))
        private set
    /** The list's rows ease in once per launch, not every time they scroll back into view. */
    var listIntroDone = false
    var favoritesOnly by mutableStateOf(false)
    var query by mutableStateOf("")
    /** Banks picked in the filter chips; empty means all banks. */
    var bankFilter by mutableStateOf(emptySet<String>())
        private set

    /**
     * Promo ids the user had already been shown before this launch. Anything else is "New". On the very first
     * launch everything counts as seen, so the first list isn't all badges.
     */
    private val seenBefore: Set<String> =
        prefs.getStringSet("seen", null)?.toSet() ?: feed.promos.map { it.id }.toSet()
    private val newIds = mutableStateOf(emptySet<String>())
    fun isNew(promoId: String) = promoId in newIds.value
    var screen by mutableStateOf(if (myCards.isEmpty()) Screen.Settings else Screen.Home)
        private set
    var selected by mutableStateOf<Place?>(null)
        private set
    var refreshing by mutableStateOf(false)
        private set
    /** null = not tried yet, true = got fresh data, false = offline or failed (showing saved copy). */
    var lastRefreshOk by mutableStateOf<Boolean?>(null)
        private set

    init {
        markSeen()
        if (FeedRepo.cacheAgeHours(app) >= 6) refresh()
    }

    /** Works out which promos are new since the last launch, and remembers the current ones for next time. */
    private fun markSeen() {
        val ids = feed.promos.map { it.id }.toSet()
        newIds.value = ids - seenBefore
        prefs.edit().putStringSet("seen", ids).apply()
    }

    fun refresh() {
        if (refreshing) return
        refreshing = true
        viewModelScope.launch {
            val fresh = FeedRepo.refresh(getApplication())
            if (fresh != null) {
                feed = fresh
                markSeen()
            }
            lastRefreshOk = fresh != null
            refreshing = false
        }
    }

    fun chooseTheme(mode: ThemeMode) {
        themeMode = mode
        prefs.edit().putString("theme", mode.name).apply()
    }

    fun choosePalette(p: Palette) {
        palette = p
        prefs.edit().putString("palette", p.name).apply()
    }

    fun toggleBank(bank: String) {
        bankFilter = if (bank in bankFilter) bankFilter - bank else bankFilter + bank
    }

    fun chooseArea(a: String) {
        area = a
        prefs.edit().putString("area", a).apply()
    }

    fun toggleCard(key: String) {
        myCards = if (key in myCards) myCards - key else myCards + key
        prefs.edit().putStringSet("cards", myCards).apply()
    }

    fun toggleFavorite(placeKey: String) {
        favorites = if (placeKey in favorites) favorites - placeKey else favorites + placeKey
        prefs.edit().putStringSet("favorites", favorites).apply()
    }

    fun open(place: Place) {
        selected = place
        screen = Screen.Detail
    }

    fun go(s: Screen) {
        screen = s
    }

    fun back() {
        screen = Screen.Home
    }
}
