package de.remk0.flickpick.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import de.remk0.flickpick.FlickpickApp
import java.util.concurrent.TimeUnit

/** Refreshes movies.json via the repository; the repository's hook runs the watchlist check. */
class WatchlistCheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val repo = (applicationContext as FlickpickApp).container.repository
        return if (repo.refresh().isSuccess) Result.success()
        else if (runAttemptCount < 3) Result.retry() else Result.failure()
    }

    companion object {
        private const val PERIODIC_NAME = "watchlist-check"
        private val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<WatchlistCheckWorker>(12, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(PERIODIC_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        /** Debug menu: "Run watchlist check now". */
        fun runNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<WatchlistCheckWorker>().setConstraints(constraints).build()
            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
