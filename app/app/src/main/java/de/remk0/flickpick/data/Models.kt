package de.remk0.flickpick.data

import kotlinx.serialization.Serializable

@Serializable
data class MoviesFile(
    val generatedAt: String,
    val region: String = "DE",
    val genres: List<Genre> = emptyList(),
    val movies: List<Movie> = emptyList(),
)

@Serializable
data class Genre(val id: Int, val de: String, val en: String)

@Serializable
data class Movie(
    val tmdbId: Int,
    val imdbId: String? = null,
    val title: String,
    val originalTitle: String = title,
    val year: Int? = null,
    val genreIds: List<Int> = emptyList(),
    val imdbRating: Double? = null,
    val imdbVotes: Int? = null,
    val posterPath: String? = null,
    val overview: String = "",
)

fun posterUrl(path: String?, size: String = "w342"): String? =
    path?.let { "https://image.tmdb.org/t/p/$size$it" }
