package de.remk0.flickpick.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import de.remk0.flickpick.data.Movie
import de.remk0.flickpick.domain.FilterState
import de.remk0.flickpick.domain.SortOption
import de.remk0.flickpick.domain.formatVotes
import de.remk0.flickpick.domain.formatVotesShort
import de.remk0.flickpick.ui.components.FilterChip
import de.remk0.flickpick.ui.components.Poster
import de.remk0.flickpick.ui.components.RatingRing
import de.remk0.flickpick.ui.theme.Fp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailSheet(movie: Movie, genreNames: Map<Int, String>, onDismiss: () -> Unit) {
    val context = LocalContext.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Fp.Sheet,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
        ) {
            Row {
                Poster(movie.posterPath, Modifier.size(width = 120.dp, height = 180.dp), radius = 16.dp, size = "w342")
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(movie.title, style = MaterialTheme.typography.headlineSmall)
                    if (movie.originalTitle.isNotBlank() && movie.originalTitle != movie.title) {
                        Spacer(Modifier.height(2.dp))
                        Text(movie.originalTitle, style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        listOfNotNull(movie.year?.toString(), genresText(movie, genreNames, max = 5).ifBlank { null })
                            .joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RatingRing(movie.imdbRating, size = 56.dp)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("IMDb", style = MaterialTheme.typography.labelMedium, color = Fp.TextSecondary)
                            Text(formatVotes(movie.imdbVotes), style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
            if (movie.overview.isNotBlank()) {
                Spacer(Modifier.height(18.dp))
                Text(movie.overview, style = MaterialTheme.typography.bodyMedium, color = Color(0xFFD6D6DD))
            }
            Spacer(Modifier.height(22.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { openUrl(context, "https://www.netflix.com/search?q=${Uri.encode(movie.title)}") },
                    colors = ButtonDefaults.buttonColors(containerColor = Fp.TextPrimary, contentColor = Color.Black),
                    modifier = Modifier.weight(1f).height(48.dp),
                ) { Text("Open in Netflix", style = MaterialTheme.typography.labelLarge, color = Color.Black) }
                OutlinedButton(
                    onClick = { movie.imdbId?.let { openUrl(context, "https://www.imdb.com/title/$it/") } },
                    enabled = movie.imdbId != null,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                    modifier = Modifier.weight(1f).height(48.dp),
                ) { Text("IMDb", style = MaterialTheme.typography.labelLarge) }
            }
        }
    }
}

fun openUrl(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "No app can open this link", Toast.LENGTH_SHORT).show()
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterSheet(filters: FilterState, onChange: (FilterState) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Fp.Sheet,
    ) {
        Column(Modifier.padding(horizontal = 20.dp).navigationBarsPadding().padding(bottom = 16.dp)) {
            SheetSection("Sort by") {
                SortOption.entries.forEach { s ->
                    FilterChip(s.label, selected = filters.sort == s, onClick = { onChange(filters.copy(sort = s)) })
                }
            }
            SheetSection("Minimum IMDb votes") {
                FilterState.MIN_VOTE_PRESETS.forEach { v ->
                    FilterChip(
                        if (v == 0) "Any" else formatVotesShort(v),
                        selected = filters.minVotes == v,
                        onClick = { onChange(filters.copy(minVotes = v)) },
                    )
                }
            }
            SheetSection("Selected genres must") {
                FilterChip("Match any", selected = !filters.matchAll, onClick = { onChange(filters.copy(matchAll = false)) })
                FilterChip("Match all", selected = filters.matchAll, onClick = { onChange(filters.copy(matchAll = true)) })
            }
            SheetSection("Seen movies") {
                FilterChip("Hide seen", selected = !filters.showSeen, onClick = { onChange(filters.copy(showSeen = false)) })
                FilterChip("Show seen", selected = filters.showSeen, onClick = { onChange(filters.copy(showSeen = true)) })
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { onChange(FilterState()) }) { Text("Reset to defaults") }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SheetSection(title: String, content: @Composable () -> Unit) {
    Text(title, style = MaterialTheme.typography.titleSmall)
    Spacer(Modifier.height(10.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        content()
    }
    Spacer(Modifier.height(20.dp))
}
