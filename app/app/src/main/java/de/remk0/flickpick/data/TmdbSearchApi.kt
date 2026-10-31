package de.remk0.flickpick.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

@Serializable
data class SearchResult(
    val id: Int,
    val title: String = "",
    @SerialName("original_title") val originalTitle: String = "",
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
) {
    val year: Int? get() = releaseDate?.take(4)?.toIntOrNull()
}

@Serializable
private data class SearchResponse(val results: List<SearchResult> = emptyList())

/** The app's only direct TMDB call: movie search for the watchlist. */
class TmdbSearchApi(
    private val client: OkHttpClient,
    private val json: Json,
    private val token: String,
) {
    val hasToken: Boolean get() = token.isNotBlank()

    suspend fun search(query: String): List<SearchResult> = withContext(Dispatchers.IO) {
        if (!hasToken) throw IOException("TMDB token missing – add TMDB_TOKEN to local.properties")
        val url = "https://api.themoviedb.org/3/search/movie".toHttpUrl().newBuilder()
            .addQueryParameter("query", query)
            .addQueryParameter("language", "en-US")
            .addQueryParameter("include_adult", "false")
            .build()
        val request = Request.Builder().url(url)
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/json")
            .build()
        client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("TMDB search failed (HTTP ${resp.code})")
            json.decodeFromString<SearchResponse>(resp.body!!.string()).results
        }
    }
}
