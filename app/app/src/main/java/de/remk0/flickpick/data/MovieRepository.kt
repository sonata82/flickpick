package de.remk0.flickpick.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Downloads movies.json, keeps the last good copy on disk (offline + instant start)
 * and exposes the parsed catalog. After every successful refresh, [onRefreshed] runs
 * (used for the watchlist check).
 */
class MovieRepository(
    private val context: Context,
    private val client: OkHttpClient,
    private val json: Json,
    private val dataUrl: String,
    private val onRefreshed: suspend (MoviesFile) -> Unit,
) {
    private val cacheFile = File(context.filesDir, "movies.json")
    private val mutex = Mutex()
    private val _data = MutableStateFlow<MoviesFile?>(null)
    val data: StateFlow<MoviesFile?> = _data.asStateFlow()

    /** Loads the on-disk cache (or the bundled debug seed) if nothing is loaded yet. */
    suspend fun loadCached(): MoviesFile? = mutex.withLock {
        _data.value ?: withContext(Dispatchers.IO) {
            val text = when {
                cacheFile.exists() -> cacheFile.readText()
                else -> runCatching { context.assets.open(SEED_ASSET).use { it.reader().readText() } }.getOrNull()
            }
            text?.let { runCatching { json.decodeFromString<MoviesFile>(it) }.getOrNull() }
        }?.also { _data.value = it }
    }

    /** Millis of the last successful download, 0 if never. */
    fun lastDownloadedAt(): Long = if (cacheFile.exists()) cacheFile.lastModified() else 0L

    fun isStale(maxAgeMillis: Long = STALE_AFTER_MS): Boolean =
        System.currentTimeMillis() - lastDownloadedAt() > maxAgeMillis

    suspend fun refresh(): Result<MoviesFile> = mutex.withLock {
        runCatching {
            val (text, parsed) = withContext(Dispatchers.IO) {
                val request = Request.Builder().url(dataUrl).header("Cache-Control", "no-cache").build()
                val body = client.newCall(request).execute().use { resp ->
                    if (!resp.isSuccessful) throw IOException("HTTP ${resp.code}")
                    resp.body?.string() ?: throw IOException("Empty response")
                }
                body to json.decodeFromString<MoviesFile>(body)
            }
            withContext(Dispatchers.IO) {
                val tmp = File(cacheFile.parentFile, "movies.json.tmp")
                tmp.writeText(text)
                if (!tmp.renameTo(cacheFile)) {
                    cacheFile.delete(); tmp.renameTo(cacheFile)
                }
            }
            _data.value = parsed
            parsed
        }
    }.onSuccess { file ->
        runCatching { onRefreshed(file) }.onFailure { android.util.Log.w("MovieRepository", "post-refresh hook failed", it) }
    }

    companion object {
        const val STALE_AFTER_MS = 12L * 60 * 60 * 1000
        private const val SEED_ASSET = "movies_seed.json"

        fun httpClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }
}
