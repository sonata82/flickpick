package de.remk0.flickpick.domain

import de.remk0.flickpick.data.Movie
import java.text.Normalizer

enum class SortOption(val label: String) {
    RATING("Rating"),
    VOTES("Votes"),
    YEAR("Year"),
    TITLE("Title"),
}

data class FilterState(
    val genreIds: Set<Int> = emptySet(),
    val matchAll: Boolean = false,
    val minVotes: Int = DEFAULT_MIN_VOTES,
    val sort: SortOption = SortOption.RATING,
    val showSeen: Boolean = false,
) {
    companion object {
        const val DEFAULT_MIN_VOTES = 10_000
        val MIN_VOTE_PRESETS = listOf(0, 1_000, 10_000, 50_000, 100_000)
    }
}

private val diacritics = "\\p{Mn}+".toRegex()

/** Lowercase and strip accents, so "amelie" finds "Amélie". */
fun normalizeForSearch(s: String): String =
    Normalizer.normalize(s, Normalizer.Form.NFD).replace(diacritics, "").lowercase()

private val byRating = compareBy<Movie> { it.imdbRating == null }
    .thenByDescending { it.imdbRating ?: 0.0 }
    .thenByDescending { it.imdbVotes ?: 0 }
    .thenBy { it.title.lowercase() }

private val byVotes = compareByDescending<Movie> { it.imdbVotes ?: -1 }
    .thenByDescending { it.imdbRating ?: -1.0 }

private val byYear = compareBy<Movie> { it.year == null }
    .thenByDescending { it.year ?: 0 }
    .then(byRating)

private val byTitle = compareBy<Movie> { normalizeForSearch(it.title) }

fun comparatorFor(sort: SortOption): Comparator<Movie> = when (sort) {
    SortOption.RATING -> byRating
    SortOption.VOTES -> byVotes
    SortOption.YEAR -> byYear
    SortOption.TITLE -> byTitle
}

fun filterAndSort(
    movies: List<Movie>,
    query: String,
    filter: FilterState,
    seenIds: Set<Int> = emptySet(),
): List<Movie> {
    val q = normalizeForSearch(query.trim())
    return movies.asSequence()
        .filter { m -> filter.showSeen || m.tmdbId !in seenIds }
        .filter { m -> filter.minVotes <= 0 || (m.imdbVotes ?: 0) >= filter.minVotes }
        .filter { m ->
            when {
                filter.genreIds.isEmpty() -> true
                filter.matchAll -> m.genreIds.containsAll(filter.genreIds)
                else -> m.genreIds.any { it in filter.genreIds }
            }
        }
        .filter { m ->
            q.isEmpty() || normalizeForSearch(m.title).contains(q) || normalizeForSearch(m.originalTitle).contains(q)
        }
        .sortedWith(comparatorFor(filter.sort))
        .toList()
}
