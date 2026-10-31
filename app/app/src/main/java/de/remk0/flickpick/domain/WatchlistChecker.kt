package de.remk0.flickpick.domain

import de.remk0.flickpick.data.MoviesFile
import de.remk0.flickpick.data.watchlist.WatchlistDao
import de.remk0.flickpick.notify.Notifier
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Compares the watchlist with a fresh catalog, persists status changes and notifies. */
class WatchlistChecker(
    private val dao: WatchlistDao,
    private val notifier: Notifier,
) {
    private val mutex = Mutex()

    suspend fun check(file: MoviesFile) = mutex.withLock {
        val diff = diffWatchlist(dao.getAll(), file.movies.mapTo(HashSet()) { it.tmdbId })
        if (diff.isEmpty) return@withLock
        dao.update(diff.applied(System.currentTimeMillis()))
        if (diff.newlyAvailable.isNotEmpty()) notifier.notifyArrivals(diff.newlyAvailable)
    }
}
