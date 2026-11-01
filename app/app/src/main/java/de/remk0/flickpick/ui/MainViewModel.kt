package de.remk0.flickpick.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.remk0.flickpick.FlickpickApp
import de.remk0.flickpick.data.Movie
import de.remk0.flickpick.data.MoviesFile
import de.remk0.flickpick.data.SearchResult
import de.remk0.flickpick.data.watchlist.SeenMovie
import de.remk0.flickpick.data.watchlist.WatchStatus
import de.remk0.flickpick.data.watchlist.WatchlistEntry
import de.remk0.flickpick.domain.FilterState
import de.remk0.flickpick.domain.filterAndSort
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class Tab { TOP_RATED, WATCHLIST }

data class LoadState(
    val initialLoadDone: Boolean = false,
    val refreshing: Boolean = false,
    val error: String? = null,
)

sealed interface SearchState {
    data object Idle : SearchState
    data object Loading : SearchState
    data class Results(val items: List<SearchResult>) : SearchState
    data class Error(val message: String) : SearchState
}

@OptIn(FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val c = (app as FlickpickApp).container
    private val repo = c.repository

    val catalog: StateFlow<MoviesFile?> = repo.data

    private val _load = MutableStateFlow(LoadState())
    val load: StateFlow<LoadState> = _load.asStateFlow()

    val tab = MutableStateFlow(Tab.TOP_RATED)
    val catalogQuery = MutableStateFlow("")
    val watchQuery = MutableStateFlow("")
    val selectedMovieId = MutableStateFlow<Int?>(null)

    val filters: StateFlow<FilterState> =
        c.prefs.filters.stateIn(viewModelScope, SharingStarted.Eagerly, FilterState())

    /** English genre names, as the app UI is English. */
    val genreNames: StateFlow<Map<Int, String>> = catalog
        .map { f -> f?.genres?.associate { it.id to it.en }.orEmpty() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    val catalogById: StateFlow<Map<Int, Movie>> = catalog
        .map { f -> f?.movies?.associateBy { it.tmdbId }.orEmpty() }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    val seenIds: StateFlow<Set<Int>> = c.seenDao.observeIds()
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    /** The filtered list plus the filters/query it was built for, so the UI can scroll to top only when those change. */
    data class VisibleMovies(val filterKey: Pair<FilterState, String>, val movies: List<Movie>)

    private val visible: StateFlow<VisibleMovies> =
        combine(catalog, filters, catalogQuery, seenIds) { f, filter, q, seen ->
            VisibleMovies(filter to q, filterAndSort(f?.movies.orEmpty(), q, filter, seen))
        }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.Eagerly, VisibleMovies(FilterState() to "", emptyList()))

    val visibleMovies: StateFlow<VisibleMovies> = visible

    val watchlist: StateFlow<List<WatchlistEntry>> =
        c.watchlistDao.observeAll().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val notificationPermissionAsked: StateFlow<Boolean> =
        c.prefs.notificationPermissionAsked.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val search: StateFlow<SearchState> = watchQuery
        .map { it.trim() }
        .distinctUntilChanged()
        .debounce(400)
        .flatMapLatest { q ->
            if (q.length < 2) flowOf<SearchState>(SearchState.Idle)
            else flow<SearchState> {
                emit(SearchState.Loading)
                emit(
                    runCatching { SearchState.Results(c.searchApi.search(q)) }
                        .getOrElse { SearchState.Error(it.message ?: "Search failed") },
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, SearchState.Idle)

    init {
        viewModelScope.launch {
            repo.loadCached()
            _load.update { it.copy(initialLoadDone = true) }
            if (repo.data.value == null || repo.isStale()) refresh()
        }
    }

    fun refresh() {
        if (_load.value.refreshing) return
        viewModelScope.launch {
            _load.update { it.copy(refreshing = true, error = null) }
            val result = repo.refresh()
            _load.update {
                it.copy(
                    refreshing = false,
                    error = result.exceptionOrNull()?.let { e -> friendlyError(e) },
                )
            }
        }
    }

    fun dismissError() = _load.update { it.copy(error = null) }

    fun updateFilters(transform: (FilterState) -> FilterState) {
        val next = transform(filters.value)
        viewModelScope.launch { c.prefs.save(next) }
    }

    fun toggleGenre(id: Int) = updateFilters { f ->
        f.copy(genreIds = if (id in f.genreIds) f.genreIds - id else f.genreIds + id)
    }

    fun openMovie(tmdbId: Int) {
        selectedMovieId.value = tmdbId
    }

    fun closeMovie() {
        selectedMovieId.value = null
    }

    /** Returns true if this is the first add, so the UI should ask for notification permission. */
    suspend fun addToWatchlist(r: SearchResult): Boolean {
        c.watchlistDao.insert(
            WatchlistEntry(
                tmdbId = r.id,
                title = r.title.ifBlank { r.originalTitle },
                year = r.year,
                posterPath = r.posterPath,
                addedAt = System.currentTimeMillis(),
                status = WatchStatus.WAITING,
            ),
        )
        watchQuery.value = ""
        val firstAsk = !c.prefs.notificationPermissionAsked.first()
        if (firstAsk) c.prefs.setNotificationPermissionAsked()
        return firstAsk
    }

    fun setSeen(tmdbId: Int, seen: Boolean) = viewModelScope.launch {
        if (seen) c.seenDao.insert(SeenMovie(tmdbId, System.currentTimeMillis()))
        else c.seenDao.delete(tmdbId)
    }

    fun removeFromWatchlist(tmdbId: Int) = viewModelScope.launch { c.watchlistDao.delete(tmdbId) }

    fun clearAvailable() = viewModelScope.launch { c.watchlistDao.deleteAvailable() }

    private fun friendlyError(e: Throwable): String = when (e) {
        is java.net.UnknownHostException, is java.net.ConnectException, is java.net.SocketTimeoutException ->
            "No connection. Showing saved data."
        else -> "Couldn't update the catalog (${e.message ?: e.javaClass.simpleName})."
    }
}
