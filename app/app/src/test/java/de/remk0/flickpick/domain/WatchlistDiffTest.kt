package de.remk0.flickpick.domain

import de.remk0.flickpick.data.watchlist.WatchStatus
import de.remk0.flickpick.data.watchlist.WatchlistEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WatchlistDiffTest {
    private fun e(id: Int, status: WatchStatus = WatchStatus.WAITING, availableAt: Long? = null) =
        WatchlistEntry(tmdbId = id, title = "Movie $id", year = 2020, posterPath = null, addedAt = 1L, status = status, availableAt = availableAt)

    /** Simulates the checker applying a diff to the stored rows. */
    private fun apply(entries: List<WatchlistEntry>, diff: WatchlistDiff, now: Long): List<WatchlistEntry> {
        val changed = diff.applied(now).associateBy { it.tmdbId }
        return entries.map { changed[it.tmdbId] ?: it }
    }

    @Test fun newArrivalIsMarkedAvailableAndNotified() {
        val diff = diffWatchlist(listOf(e(1), e(2)), setOf(1, 99))
        assertEquals(listOf(1), diff.newlyAvailable.map { it.tmdbId })
        assertTrue(diff.backToWaiting.isEmpty())
        val updated = diff.applied(now = 500L).single()
        assertEquals(WatchStatus.AVAILABLE, updated.status)
        assertEquals(500L, updated.availableAt)
    }

    @Test fun alreadyNotifiedIsNotNotifiedAgain() {
        val diff = diffWatchlist(listOf(e(1, WatchStatus.AVAILABLE, 500L)), setOf(1))
        assertTrue(diff.isEmpty)
    }

    @Test fun leftCatalogGoesBackToWaiting() {
        val diff = diffWatchlist(listOf(e(1, WatchStatus.AVAILABLE, 500L)), emptySet())
        assertTrue(diff.newlyAvailable.isEmpty())
        val updated = diff.applied(now = 900L).single()
        assertEquals(WatchStatus.WAITING, updated.status)
        assertNull(updated.availableAt)
    }

    @Test fun leftAndCameBackNotifiesAgain() {
        var rows = listOf(e(1))
        // Arrives -> notify once.
        var diff = diffWatchlist(rows, setOf(1)); assertEquals(1, diff.newlyAvailable.size); rows = apply(rows, diff, 1)
        // Still there -> nothing.
        diff = diffWatchlist(rows, setOf(1)); assertTrue(diff.isEmpty)
        // Leaves -> back to waiting, no notification.
        diff = diffWatchlist(rows, emptySet()); assertTrue(diff.newlyAvailable.isEmpty()); rows = apply(rows, diff, 2)
        assertEquals(WatchStatus.WAITING, rows.single().status)
        // Returns -> notify again.
        diff = diffWatchlist(rows, setOf(1)); assertEquals(listOf(1), diff.newlyAvailable.map { it.tmdbId })
    }

    @Test fun multipleArrivalsAtOnce() {
        val diff = diffWatchlist(listOf(e(1), e(2), e(3)), setOf(1, 2, 3))
        assertEquals(setOf(1, 2, 3), diff.newlyAvailable.map { it.tmdbId }.toSet())
    }
}
