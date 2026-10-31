package de.remk0.flickpick

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import de.remk0.flickpick.ui.FlickpickRoot
import de.remk0.flickpick.ui.MainViewModel
import de.remk0.flickpick.ui.Tab
import de.remk0.flickpick.ui.theme.FlickpickTheme

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) handleIntent(intent)
        setContent { FlickpickTheme { FlickpickRoot(vm) } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    /** Notification taps: open a movie's detail sheet, or the watchlist for the group summary. */
    private fun handleIntent(intent: Intent?) {
        intent ?: return
        val id = intent.getIntExtra(EXTRA_TMDB_ID, -1)
        if (id > 0) {
            vm.tab.value = Tab.WATCHLIST
            vm.openMovie(id)
        } else if (intent.getBooleanExtra(EXTRA_OPEN_WATCHLIST, false)) {
            vm.tab.value = Tab.WATCHLIST
        }
        intent.removeExtra(EXTRA_TMDB_ID)
        intent.removeExtra(EXTRA_OPEN_WATCHLIST)
    }

    companion object {
        const val EXTRA_TMDB_ID = "tmdbId"
        const val EXTRA_OPEN_WATCHLIST = "openWatchlist"
    }
}
