package de.remk0.flickpick.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.remk0.flickpick.BuildConfig
import de.remk0.flickpick.data.Movie
import de.remk0.flickpick.domain.FilterState
import de.remk0.flickpick.domain.formatGeneratedAt
import de.remk0.flickpick.domain.formatVotesShort
import de.remk0.flickpick.ui.components.CenteredMessage
import de.remk0.flickpick.ui.components.FilterChip
import de.remk0.flickpick.ui.components.GradientTabs
import de.remk0.flickpick.ui.components.HeaderGlow
import de.remk0.flickpick.ui.components.MovieCard
import de.remk0.flickpick.ui.components.SearchPill
import de.remk0.flickpick.ui.theme.Fp
import de.remk0.flickpick.work.WatchlistCheckWorker

private val HPad = 20.dp

@Composable
fun FlickpickRoot(vm: MainViewModel) {
    var showAbout by rememberSaveable { mutableStateOf(false) }
    val selectedId by vm.selectedMovieId.collectAsStateWithLifecycle()
    val byId by vm.catalogById.collectAsStateWithLifecycle()
    val genreNames by vm.genreNames.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize().background(Fp.Background)) {
        if (showAbout) {
            BackHandler { showAbout = false }
            AboutScreen(onBack = { showAbout = false })
        } else {
            MainScreen(vm, onAbout = { showAbout = true })
        }
    }

    val movie = selectedId?.let { byId[it] }
    if (movie != null) {
        DetailSheet(movie = movie, genreNames = genreNames, onDismiss = vm::closeMovie)
    }
}

@Composable
private fun MainScreen(vm: MainViewModel, onAbout: () -> Unit) {
    val tab by vm.tab.collectAsStateWithLifecycle()
    val catalog by vm.catalog.collectAsStateWithLifecycle()
    val catalogQuery by vm.catalogQuery.collectAsStateWithLifecycle()
    val watchQuery by vm.watchQuery.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize()) {
        HeaderGlow()
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().padding(start = HPad, end = 8.dp, top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Flickpick", style = MaterialTheme.typography.displaySmall)
                    Text(
                        catalog?.let { "Netflix Germany · updated ${formatGeneratedAt(it.generatedAt)}" } ?: "Netflix Germany",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                OverflowMenu(onAbout)
            }
            Spacer(Modifier.height(16.dp))
            if (tab == Tab.TOP_RATED) {
                SearchPill(catalogQuery, { vm.catalogQuery.value = it }, "Search titles", Modifier.padding(horizontal = HPad))
            } else {
                SearchPill(watchQuery, { vm.watchQuery.value = it }, "Add a movie to your watchlist", Modifier.padding(horizontal = HPad))
            }
            Spacer(Modifier.height(8.dp))
            GradientTabs(
                titles = listOf("Top rated", "Watchlist"),
                selected = tab.ordinal,
                onSelect = { vm.tab.value = Tab.entries[it] },
                modifier = Modifier.padding(horizontal = HPad),
            )
            Spacer(Modifier.height(12.dp))
            when (tab) {
                Tab.TOP_RATED -> TopRated(vm, Modifier.weight(1f))
                Tab.WATCHLIST -> WatchlistTab(vm, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun OverflowMenu(onAbout: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    val context = LocalContext.current
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = "More", tint = Fp.TextSecondary)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Text("About") }, onClick = { open = false; onAbout() })
            if (BuildConfig.DEBUG) {
                DropdownMenuItem(
                    text = { Text("Run watchlist check now") },
                    onClick = {
                        open = false
                        WatchlistCheckWorker.runNow(context)
                        Toast.makeText(context, "Watchlist check started", Toast.LENGTH_SHORT).show()
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopRated(vm: MainViewModel, modifier: Modifier) {
    val catalog by vm.catalog.collectAsStateWithLifecycle()
    val load by vm.load.collectAsStateWithLifecycle()
    val movies by vm.visibleMovies.collectAsStateWithLifecycle()
    val filters by vm.filters.collectAsStateWithLifecycle()
    val genreNames by vm.genreNames.collectAsStateWithLifecycle()
    val query by vm.catalogQuery.collectAsStateWithLifecycle()
    var showFilterSheet by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(filters, query) { listState.scrollToItem(0) }

    Column(modifier) {
        GenreRow(
            filters = filters,
            genres = catalog?.genres.orEmpty().map { it.id to it.en }.sortedBy { it.second },
            onOpenFilters = { showFilterSheet = true },
            onToggle = vm::toggleGenre,
            onClear = { vm.updateFilters { it.copy(genreIds = emptySet()) } },
        )
        Spacer(Modifier.height(8.dp))
        val bannerError = load.error
        if (catalog != null && bannerError != null) {
            ErrorBanner(bannerError, vm::dismissError, Modifier.padding(horizontal = HPad).padding(bottom = 8.dp))
        }
        PullToRefreshBox(
            isRefreshing = load.refreshing && catalog != null,
            onRefresh = vm::refresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = HPad, end = HPad, top = 4.dp,
                    bottom = 16.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                val error = load.error
                when {
                    catalog == null && (load.refreshing || !load.initialLoadDone) -> item("loading") {
                        Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Fp.BrandPurple)
                        }
                    }
                    catalog == null -> item("error") {
                        CenteredMessage(
                            title = "Couldn't load movies",
                            body = error ?: "Check your connection and try again.",
                            modifier = Modifier.fillParentMaxSize(),
                            action = { TextButton(onClick = vm::refresh) { Text("Retry") } },
                        )
                    }
                    else -> {
                        if (movies.isEmpty()) {
                            item("empty") {
                                CenteredMessage(
                                    title = "No movies match",
                                    body = "Try fewer genres, a lower vote minimum or a different search.",
                                    modifier = Modifier.fillParentMaxSize(),
                                    action = {
                                        TextButton(onClick = {
                                            vm.catalogQuery.value = ""
                                            vm.updateFilters { FilterState(sort = it.sort) }
                                        }) { Text("Reset filters") }
                                    },
                                )
                            }
                        } else {
                            itemsIndexed(movies, key = { _, m -> m.tmdbId }) { i, m ->
                                MovieCard(
                                    movie = m,
                                    rank = i + 1,
                                    genreNames = genresText(m, genreNames),
                                    onClick = { vm.openMovie(m.tmdbId) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFilterSheet) {
        FilterSheet(filters = filters, onChange = { f -> vm.updateFilters { f } }, onDismiss = { showFilterSheet = false })
    }
}

fun genresText(m: Movie, names: Map<Int, String>, max: Int = 3): String =
    m.genreIds.mapNotNull { names[it] }.take(max).joinToString(", ")

@Composable
private fun GenreRow(
    filters: FilterState,
    genres: List<Pair<Int, String>>,
    onOpenFilters: () -> Unit,
    onToggle: (Int) -> Unit,
    onClear: () -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = HPad),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item("filters") {
            val votes = if (filters.minVotes > 0) "${formatVotesShort(filters.minVotes)}+ votes" else "All votes"
            val match = if (filters.genreIds.size > 1) (if (filters.matchAll) " · All" else " · Any") else ""
            FilterChip(
                label = "${filters.sort.label} · $votes$match",
                selected = false,
                onClick = onOpenFilters,
                trailing = { Icon(Icons.Filled.ArrowDropDown, null, tint = Fp.ChipText, modifier = Modifier.size(18.dp)) },
            )
        }
        if (filters.genreIds.isNotEmpty()) {
            item("clear") {
                FilterChip(
                    label = "Clear",
                    selected = false,
                    onClick = onClear,
                    trailing = { Icon(Icons.Filled.Close, null, tint = Fp.ChipText, modifier = Modifier.size(14.dp)) },
                )
            }
        }
        // Selected genres first so they stay visible.
        val ordered = genres.sortedBy { it.first !in filters.genreIds }
        items(ordered, key = { it.first }) { (id, name) ->
            FilterChip(label = name, selected = id in filters.genreIds, onClick = { onToggle(id) })
        }
    }
}

@Composable
private fun ErrorBanner(message: String, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .background(Fp.Card, MaterialTheme.shapes.medium)
            .padding(start = 14.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(message, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        IconButton(onClick = onDismiss) {
            Icon(Icons.Filled.Close, contentDescription = "Dismiss", tint = Fp.TextSecondary, modifier = Modifier.size(18.dp))
        }
    }
}
