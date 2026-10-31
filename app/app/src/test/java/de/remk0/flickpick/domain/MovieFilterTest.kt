package de.remk0.flickpick.domain

import de.remk0.flickpick.data.Movie
import org.junit.Assert.assertEquals
import org.junit.Test

class MovieFilterTest {
    private fun m(id: Int, title: String, rating: Double?, votes: Int?, year: Int?, genres: List<Int>, original: String = title) =
        Movie(tmdbId = id, title = title, originalTitle = original, imdbRating = rating, imdbVotes = votes, year = year, genreIds = genres)

    private val movies = listOf(
        m(1, "Inception", 8.8, 2_600_000, 2010, listOf(28, 878)),
        m(2, "Die fabelhafte Welt der Amélie", 8.3, 800_000, 2001, listOf(35, 10749), original = "Le Fabuleux Destin d'Amélie Poulain"),
        m(3, "Obscure Gem", 9.6, 40, 2020, listOf(99)),
        m(4, "No Rating", null, null, null, listOf(28)),
        m(5, "Action Comedy", 6.4, 50_000, 2015, listOf(28, 35)),
        m(6, "Tie Low Votes", 8.8, 20_000, 2012, listOf(18)),
    )

    private fun ids(list: List<Movie>) = list.map { it.tmdbId }

    @Test fun defaultFilterHidesFewVotesAndSortsByRating() {
        assertEquals(listOf(1, 6, 2, 5), ids(filterAndSort(movies, "", FilterState())))
    }

    @Test fun minVotesZeroKeepsAllWithNullsLast() {
        assertEquals(listOf(3, 1, 6, 2, 5, 4), ids(filterAndSort(movies, "", FilterState(minVotes = 0))))
    }

    @Test fun genreMatchAny() {
        val f = FilterState(minVotes = 0, genreIds = setOf(28, 35))
        assertEquals(listOf(1, 2, 5, 4), ids(filterAndSort(movies, "", f)))
    }

    @Test fun genreMatchAll() {
        val f = FilterState(minVotes = 0, genreIds = setOf(28, 35), matchAll = true)
        assertEquals(listOf(5), ids(filterAndSort(movies, "", f)))
    }

    @Test fun sortByVotes() {
        assertEquals(listOf(1, 2, 5, 6, 3, 4), ids(filterAndSort(movies, "", FilterState(minVotes = 0, sort = SortOption.VOTES))))
    }

    @Test fun sortByYearNewestFirstNullsLast() {
        assertEquals(listOf(3, 5, 6, 1, 2, 4), ids(filterAndSort(movies, "", FilterState(minVotes = 0, sort = SortOption.YEAR))))
    }

    @Test fun sortByTitle() {
        assertEquals(listOf(5, 2, 1, 4, 3, 6), ids(filterAndSort(movies, "", FilterState(minVotes = 0, sort = SortOption.TITLE))))
    }

    @Test fun searchMatchesOriginalTitleIgnoringCaseAndAccents() {
        assertEquals(listOf(2), ids(filterAndSort(movies, "  fabuleux destin  ", FilterState())))
        assertEquals(listOf(2), ids(filterAndSort(movies, "AMELIE", FilterState())))
        assertEquals(listOf(1), ids(filterAndSort(movies, "incep", FilterState())))
    }
}
