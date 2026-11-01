package de.remk0.flickpick.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.ensureActive
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import de.remk0.flickpick.data.Movie
import de.remk0.flickpick.data.posterUrl
import de.remk0.flickpick.ui.theme.Fp
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/** CSS `linear-gradient(160deg, #7A1C3E 0%, #3B1C78 38%, transparent 78%)`, faded into black at the bottom. */
@Composable
fun HeaderGlow(modifier: Modifier = Modifier, height: Dp = 340.dp) {
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
            .drawWithCache {
                val w = size.width
                val h = size.height
                val rad = Math.toRadians(160.0)
                val dx = sin(rad).toFloat()
                val dy = -cos(rad).toFloat()
                val len = abs(w * dx) + abs(h * dy)
                val c = Offset(w / 2, h / 2)
                val glow = Brush.linearGradient(
                    0f to Fp.GlowWine,
                    0.38f to Fp.GlowIndigo,
                    0.78f to Fp.GlowIndigo.copy(alpha = 0f),
                    start = c - Offset(dx * len / 2, dy * len / 2),
                    end = c + Offset(dx * len / 2, dy * len / 2),
                )
                val fade = Brush.verticalGradient(0f to Color.Black, 0.55f to Color.Black, 1f to Color.Transparent)
                onDrawBehind {
                    drawRect(glow)
                    drawRect(fade, blendMode = BlendMode.DstIn)
                }
            },
    )
}

@Composable
fun SearchPill(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val focus = LocalFocusManager.current
    Row(
        modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(CircleShape)
            .background(Fp.SearchFill)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Search, contentDescription = null, tint = Fp.TextSecondary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) {
                Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = Fp.TextSecondary, maxLines = 1)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge,
                cursorBrush = SolidColor(Fp.TextPrimary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { focus.clearFocus() }),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (value.isNotEmpty()) {
            Icon(
                Icons.Filled.Close,
                contentDescription = "Clear search",
                tint = Fp.TextSecondary,
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .clickable { onValueChange("") },
            )
        }
    }
}

@Composable
fun GradientTabs(titles: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        titles.forEachIndexed { i, title ->
            val active = i == selected
            Column(
                Modifier
                    .width(IntrinsicSize.Max)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onSelect(i) }
                    .padding(top = 8.dp),
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (active) Fp.TextPrimary else Fp.TextSecondary,
                )
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .then(if (active) Modifier.background(Fp.BrandBrush) else Modifier),
                )
            }
        }
    }
}

@Composable
fun FilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    val shape = CircleShape
    Row(
        modifier
            .height(34.dp)
            .clip(shape)
            .background(if (selected) Fp.ChipSelectedFill else Fp.ChipFill)
            .then(if (selected) Modifier.border(1.dp, Fp.ChipSelectedBorder, shape) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) Fp.ChipSelectedText else Fp.ChipText,
            maxLines = 1,
        )
        if (trailing != null) {
            Spacer(Modifier.width(4.dp))
            trailing()
        }
    }
}

@Composable
fun Poster(path: String?, modifier: Modifier = Modifier, radius: Dp = 12.dp, size: String = "w342") {
    val shape = RoundedCornerShape(radius)
    Box(modifier.clip(shape).background(Color(0xFF1E1E23)), contentAlignment = Alignment.Center) {
        if (path == null) {
            Text("🎬", fontSize = 22.sp)
        } else {
            AsyncImage(
                model = posterUrl(path, size),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
fun FpCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Fp.Card)
            .border(1.dp, Fp.CardBorder, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) { content() }
}

@Composable
fun MovieCard(
    movie: Movie,
    rank: Int,
    genreNames: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    seen: Boolean = false,
) {
    FpCard(modifier, onClick = onClick) {
        Row(Modifier.padding(12.dp)) {
            Box {
                Poster(movie.posterPath, Modifier.size(width = 64.dp, height = 96.dp), size = "w185")
                Box(
                    Modifier
                        .padding(4.dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.72f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        rank.toString(),
                        color = Fp.TextPrimary,
                        fontSize = if (rank >= 1000) 8.sp else if (rank >= 100) 9.sp else 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                }
                if (seen) {
                    Box(
                        Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(Fp.BrandPurple),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = "Seen", tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f).padding(top = 2.dp)) {
                Text(
                    movie.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    listOfNotNull(movie.year?.toString(), genreNames.ifBlank { null }).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(end = 50.dp),
                )
            }
        }
        RatingRing(movie.imdbRating, Modifier.align(Alignment.BottomEnd).padding(12.dp))
    }
}

@Composable
fun CenteredMessage(
    title: String,
    body: String? = null,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Box(modifier.fillMaxSize().padding(PaddingValues(32.dp)), contentAlignment = Alignment.TopCenter) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 48.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (body != null) {
                Spacer(Modifier.height(6.dp))
                Text(body, style = MaterialTheme.typography.bodySmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
            if (action != null) {
                Spacer(Modifier.height(16.dp))
                action()
            }
        }
    }
}

/**
 * Swipe a card sideways (either direction) to trigger [onSwiped]. Set [resetAfter] when the
 * item stays in the list afterwards, so the card slides back.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeAction(
    icon: ImageVector,
    label: String,
    background: Color,
    onSwiped: () -> Unit,
    resetAfter: Boolean = false,
    content: @Composable () -> Unit,
) {
    val state = rememberSwipeToDismissBoxState()
    val currentOnSwiped by rememberUpdatedState(onSwiped)
    val currentResetAfter by rememberUpdatedState(resetAfter)
    // Lazy lists restore saved item state by key, so an item that comes back (e.g. after Undo)
    // still has its dismissed swipe state. Remember whether that dismissal was already handled.
    var handled by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state) {
        // Handle values one at a time; a keyed LaunchedEffect would cancel itself mid-reset.
        snapshotFlow { state.currentValue }.collect { value ->
            when {
                value == SwipeToDismissBoxValue.Settled -> handled = false
                !handled -> {
                    handled = true
                    if (currentResetAfter) {
                        // Slide the card back before applying the change, so the list update can't
                        // interrupt the animation. The swipe's own settle can interrupt reset(): retry.
                        repeat(20) {
                            if (state.currentValue == SwipeToDismissBoxValue.Settled) return@repeat
                            try {
                                state.reset()
                            } catch (e: kotlinx.coroutines.CancellationException) {
                                coroutineContext.ensureActive() // a real cancellation: rethrow
                                kotlinx.coroutines.delay(50)
                            }
                        }
                    }
                    currentOnSwiped()
                }
                else -> state.snapTo(SwipeToDismissBoxValue.Settled) // came back: show the card again
            }
        }
    }
    SwipeToDismissBox(
        state = state,
        backgroundContent = {
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(20.dp))
                    .background(background)
                    .padding(horizontal = 24.dp),
                contentAlignment = if (state.dismissDirection == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, contentDescription = null, tint = Fp.TextPrimary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(label, style = MaterialTheme.typography.labelLarge, color = Fp.TextPrimary)
                }
            }
        },
    ) { content() }
}
