package de.remk0.flickpick.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.remk0.flickpick.data.Movie
import de.remk0.flickpick.data.SearchResult
import de.remk0.flickpick.data.watchlist.WatchStatus
import de.remk0.flickpick.data.watchlist.WatchlistEntry
import de.remk0.flickpick.ui.components.CenteredMessage
import de.remk0.flickpick.ui.components.FpCard
import de.remk0.flickpick.ui.components.Poster
import de.remk0.flickpick.ui.components.SwipeAction
import de.remk0.flickpick.ui.components.RatingRing
import de.remk0.flickpick.ui.theme.Fp
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val HPad = 20.dp
private val dayFormat = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)
private fun day(millis: Long) = dayFormat.format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()))

@Composable
fun WatchlistTab(vm: MainViewModel, modifier: Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val query by vm.watchQuery.collectAsStateWithLifecycle()
    val search by vm.search.collectAsStateWithLifecycle()
    val entries by vm.watchlist.collectAsStateWithLifecycle()
    val byId by vm.catalogById.collectAsStateWithLifecycle()
    val asked by vm.notificationPermissionAsked.collectAsStateWithLifecycle()

    fun permissionGranted() = Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    var granted by remember { mutableStateOf(permissionGranted()) }
    LifecycleResumeEffect(Unit) {
        granted = permissionGranted()
        onPauseOrDispose { }
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }

    val bottom = 16.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val inWatchlist = entries.mapTo(HashSet()) { it.tmdbId }

    if (query.isNotBlank()) {
        LazyColumn(
            modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = HPad, end = HPad, top = 4.dp, bottom = bottom),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            when (val s = search) {
                SearchState.Idle, SearchState.Loading -> item {
                    if (query.trim().length >= 2) {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Fp.BrandPurple)
                        }
                    } else {
                        CenteredMessage("Keep typing…")
                    }
                }
                is SearchState.Error -> item { CenteredMessage("Search failed", s.message) }
                is SearchState.Results -> {
                    if (s.items.isEmpty()) item { CenteredMessage("No movies found", "Try the original title.") }
                    items(s.items, key = { it.id }) { r ->
                        SearchResultRow(
                            r = r,
                            onNetflix = byId[r.id],
                            alreadyAdded = r.id in inWatchlist,
                            onOpen = { vm.openMovie(r.id) },
                            onAdd = {
                                scope.launch {
                                    val first = vm.addToWatchlist(r)
                                    if (first && !permissionGranted() && Build.VERSION.SDK_INT >= 33) {
                                        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
        return
    }

    val waiting = entries.filter { it.status == WatchStatus.WAITING }
    val available = entries.filter { it.status == WatchStatus.AVAILABLE }
    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = HPad, end = HPad, top = 4.dp, bottom = bottom),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (asked && !granted && entries.isNotEmpty()) {
            item("hint") {
                FpCard {
                    Row(Modifier.padding(start = 14.dp, end = 4.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Notifications are off, so you won't hear when these arrive.",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = {
                            context.startActivity(
                                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                            )
                        }) { Text("Turn on") }
                    }
                }
            }
        }
        if (entries.isEmpty()) {
            item("empty") {
                CenteredMessage(
                    "Your watchlist is empty",
                    "Search above for a movie that isn't on Netflix Germany yet. You'll get a notification when it arrives.",
                )
            }
        }
        if (available.isNotEmpty()) {
            sectionHeader("Now on Netflix", available.size) {
                TextButton(onClick = { vm.clearAvailable() }) { Text("Clear") }
            }
            items(available, key = { "a${it.tmdbId}" }) { e ->
                SwipeAction(Icons.Filled.Delete, "Remove", Fp.BrandRed.copy(alpha = 0.25f), onSwiped = { vm.removeFromWatchlist(e.tmdbId) }) {
                    WatchlistRow(e, byId[e.tmdbId], onClick = { vm.openMovie(e.tmdbId) })
                }
            }
        }
        if (waiting.isNotEmpty()) {
            sectionHeader("Waiting", waiting.size)
            items(waiting, key = { "w${it.tmdbId}" }) { e ->
                SwipeAction(Icons.Filled.Delete, "Remove", Fp.BrandRed.copy(alpha = 0.25f), onSwiped = { vm.removeFromWatchlist(e.tmdbId) }) {
                    WatchlistRow(e, null, onClick = null)
                }
            }
        }
    }
}

private fun LazyListScope.sectionHeader(title: String, count: Int, action: (@Composable () -> Unit)? = null) {
    item("h$title") {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("$title · $count", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            action?.invoke()
        }
    }
}

@Composable
private fun WatchlistRow(e: WatchlistEntry, movie: Movie?, onClick: (() -> Unit)?) {
    FpCard(onClick = onClick) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Poster(e.posterPath, Modifier.size(width = 48.dp, height = 72.dp), radius = 10.dp, size = "w185")
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(e.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                val status = if (e.status == WatchStatus.AVAILABLE && e.availableAt != null) {
                    "On Netflix since ${day(e.availableAt)}"
                } else "Added ${day(e.addedAt)}"
                Text(listOfNotNull(e.year?.toString(), status).joinToString(" · "), style = MaterialTheme.typography.bodySmall, maxLines = 1)
            }
            if (e.status == WatchStatus.AVAILABLE) {
                Spacer(Modifier.width(8.dp))
                RatingRing(movie?.imdbRating)
            }
        }
    }
}

@Composable
private fun SearchResultRow(
    r: SearchResult,
    onNetflix: Movie?,
    alreadyAdded: Boolean,
    onOpen: () -> Unit,
    onAdd: () -> Unit,
) {
    val click: (() -> Unit)? = when {
        onNetflix != null -> onOpen
        alreadyAdded -> null
        else -> onAdd
    }
    FpCard(onClick = click) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Poster(r.posterPath, Modifier.size(width = 48.dp, height = 72.dp), radius = 10.dp, size = "w185")
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(r.title.ifBlank { r.originalTitle }, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (r.originalTitle.isNotBlank() && r.originalTitle != r.title) {
                    Text(r.originalTitle, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                r.year?.let { Text(it.toString(), style = MaterialTheme.typography.bodySmall) }
            }
            Spacer(Modifier.width(8.dp))
            when {
                onNetflix != null -> Column(horizontalAlignment = Alignment.End) {
                    Text("Already on Netflix", style = MaterialTheme.typography.labelSmall, color = Fp.ChipSelectedText)
                    TextButton(onClick = onOpen) { Text("Open") }
                }
                alreadyAdded -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Check, null, tint = Fp.TextSecondary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Added", style = MaterialTheme.typography.labelMedium, color = Fp.TextSecondary)
                }
                else -> Icon(Icons.Filled.Add, contentDescription = "Add to watchlist", tint = Fp.TextPrimary)
            }
        }
    }
}
