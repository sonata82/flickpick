package de.remk0.flickpick.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import de.remk0.flickpick.domain.FilterState
import de.remk0.flickpick.domain.SortOption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "prefs")

class FilterPrefs(private val context: Context) {
    private object Keys {
        val genres = stringSetPreferencesKey("genres")
        val matchAll = booleanPreferencesKey("match_all")
        val minVotes = intPreferencesKey("min_votes")
        val sort = stringPreferencesKey("sort")
        val notifAsked = booleanPreferencesKey("notif_permission_asked")
    }

    val filters: Flow<FilterState> = context.dataStore.data.map { p ->
        FilterState(
            genreIds = p[Keys.genres].orEmpty().mapNotNull { it.toIntOrNull() }.toSet(),
            matchAll = p[Keys.matchAll] ?: false,
            minVotes = p[Keys.minVotes] ?: FilterState.DEFAULT_MIN_VOTES,
            sort = p[Keys.sort]?.let { s -> SortOption.entries.firstOrNull { it.name == s } } ?: SortOption.RATING,
        )
    }

    val notificationPermissionAsked: Flow<Boolean> = context.dataStore.data.map { it[Keys.notifAsked] ?: false }

    suspend fun save(f: FilterState) {
        context.dataStore.edit { p ->
            p[Keys.genres] = f.genreIds.map { it.toString() }.toSet()
            p[Keys.matchAll] = f.matchAll
            p[Keys.minVotes] = f.minVotes
            p[Keys.sort] = f.sort.name
        }
    }

    suspend fun setNotificationPermissionAsked() {
        context.dataStore.edit { it[Keys.notifAsked] = true }
    }
}
