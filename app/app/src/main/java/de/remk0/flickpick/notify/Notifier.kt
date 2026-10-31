package de.remk0.flickpick.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import de.remk0.flickpick.MainActivity
import de.remk0.flickpick.R
import de.remk0.flickpick.data.watchlist.WatchlistEntry

class Notifier(private val context: Context) {

    fun createChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "Watchlist", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "When a movie on your watchlist arrives on Netflix"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun canPost(): Boolean {
        val granted = Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        return granted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun notifyArrivals(entries: List<WatchlistEntry>) {
        if (entries.isEmpty() || !canPost()) return
        val nm = NotificationManagerCompat.from(context)
        try {
            for (e in entries) {
                val n = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle("${e.title} is now on Netflix")
                    .setContentText(e.year?.let { "$it · tap to see details" } ?: "Tap to see details")
                    .setContentIntent(openIntent(e.tmdbId))
                    .setAutoCancel(true)
                    .setGroup(GROUP)
                    .build()
                nm.notify(e.tmdbId, n)
            }
            if (entries.size > 1) {
                val style = NotificationCompat.InboxStyle()
                entries.forEach { style.addLine(it.title) }
                style.setSummaryText("${entries.size} new on Netflix")
                val summary = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle("${entries.size} watchlist movies are now on Netflix")
                    .setContentText(entries.joinToString { it.title })
                    .setStyle(style)
                    .setGroup(GROUP)
                    .setGroupSummary(true)
                    .setAutoCancel(true)
                    .setContentIntent(openIntent(null))
                    .build()
                nm.notify(SUMMARY_ID, summary)
            }
        } catch (_: SecurityException) {
            // Permission revoked between the check and notify.
        }
    }

    private fun openIntent(tmdbId: Int?): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (tmdbId != null) putExtra(MainActivity.EXTRA_TMDB_ID, tmdbId)
            else putExtra(MainActivity.EXTRA_OPEN_WATCHLIST, true)
        }
        return PendingIntent.getActivity(
            context, tmdbId ?: SUMMARY_ID, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        const val CHANNEL_ID = "watchlist"
        private const val GROUP = "de.remk0.flickpick.WATCHLIST_ARRIVALS"
        private const val SUMMARY_ID = -1
    }
}
