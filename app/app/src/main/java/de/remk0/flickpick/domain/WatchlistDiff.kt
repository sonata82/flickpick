package de.remk0.flickpick.domain

import de.remk0.flickpick.data.watchlist.WatchStatus
import de.remk0.flickpick.data.watchlist.WatchlistEntry

data class WatchlistDiff(
    /** Were waiting, now in the catalog: mark available and notify. */
    val newlyAvailable: List<WatchlistEntry>,
    /** Were available, gone from the catalog: back to waiting so a return notifies again. */
    val backToWaiting: List<WatchlistEntry>,
) {
    val isEmpty: Boolean get() = newlyAvailable.isEmpty() && backToWaiting.isEmpty()
}

fun diffWatchlist(entries: List<WatchlistEntry>, catalogIds: Set<Int>): WatchlistDiff = WatchlistDiff(
    newlyAvailable = entries.filter { it.status == WatchStatus.WAITING && it.tmdbId in catalogIds },
    backToWaiting = entries.filter { it.status == WatchStatus.AVAILABLE && it.tmdbId !in catalogIds },
)

/** Applies a diff to entries, returning the rows to write back. */
fun WatchlistDiff.applied(now: Long): List<WatchlistEntry> =
    newlyAvailable.map { it.copy(status = WatchStatus.AVAILABLE, availableAt = now) } +
        backToWaiting.map { it.copy(status = WatchStatus.WAITING, availableAt = null) }
