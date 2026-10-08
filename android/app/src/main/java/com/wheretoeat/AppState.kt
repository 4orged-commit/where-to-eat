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
    var favoritesOnly by mutableStateOf(false)
    var query by mutableStateOf("")
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
        if (FeedRepo.cacheAgeHours(app) >= 6) refresh()
    }

    fun refresh() {
        if (refreshing) return
        refreshing = true
        viewModelScope.launch {
            val fresh = FeedRepo.refresh(getApplication())
            if (fresh != null) feed = fresh
            lastRefreshOk = fresh != null
            refreshing = false
        }
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
