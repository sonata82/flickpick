package de.remk0.flickpick

import android.app.Application
import android.content.Context
import de.remk0.flickpick.data.FilterPrefs
import de.remk0.flickpick.data.MovieRepository
import de.remk0.flickpick.data.TmdbSearchApi
import de.remk0.flickpick.data.watchlist.WatchlistDb
import de.remk0.flickpick.domain.WatchlistChecker
import de.remk0.flickpick.notify.Notifier
import de.remk0.flickpick.work.WatchlistCheckWorker
import kotlinx.serialization.json.Json

/** Manual dependency container – the app is small enough not to need a DI framework. */
class AppContainer(context: Context) {
    val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
    val httpClient = MovieRepository.httpClient()
    val notifier = Notifier(context)
    val db = WatchlistDb.create(context)
    val watchlistDao = db.dao()
    val seenDao = db.seenDao()
    val watchlistChecker = WatchlistChecker(watchlistDao, notifier)
    val prefs = FilterPrefs(context)
    val searchApi = TmdbSearchApi(httpClient, json, BuildConfig.TMDB_TOKEN)
    val repository = MovieRepository(context, httpClient, json, BuildConfig.DATA_URL) { file ->
        watchlistChecker.check(file)
    }
}

class FlickpickApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.notifier.createChannel()
        WatchlistCheckWorker.schedule(this)
    }
}
