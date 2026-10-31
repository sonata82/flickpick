package de.remk0.flickpick.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import de.remk0.flickpick.BuildConfig
import de.remk0.flickpick.ui.components.FpCard
import de.remk0.flickpick.ui.components.HeaderGlow
import de.remk0.flickpick.ui.theme.Fp

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    HeaderGlow()
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Fp.TextPrimary)
            }
            Text("About", style = MaterialTheme.typography.headlineSmall)
        }
        Spacer(Modifier.height(16.dp))
        Text("Flickpick ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.titleMedium)
        Text(
            "Movies on Netflix Germany, sorted by IMDb rating. A personal, non-commercial project.",
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(20.dp))

        Attribution(
            title = "TMDB",
            body = "This product uses the TMDB API but is not endorsed or certified by TMDB.",
            link = "https://www.themoviedb.org",
        ) { openUrl(context, it) }
        Attribution(
            title = "JustWatch",
            body = "Streaming availability data by JustWatch, provided through TMDB.",
            link = "https://www.justwatch.com",
        ) { openUrl(context, it) }
        Attribution(
            title = "IMDb",
            body = "Ratings from the IMDb non-commercial datasets. Information courtesy of IMDb (https://www.imdb.com). Used with permission for personal, non-commercial use.",
            link = "https://developer.imdb.com/non-commercial-datasets/",
        ) { openUrl(context, it) }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun Attribution(title: String, body: String, link: String, open: (String) -> Unit) {
    FpCard {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            Text(body, style = MaterialTheme.typography.bodyMedium, color = Fp.TextSecondary)
            TextButton(onClick = { open(link) }, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                Text(link.removePrefix("https://").removeSuffix("/"))
            }
        }
    }
    Spacer(Modifier.height(12.dp))
}
