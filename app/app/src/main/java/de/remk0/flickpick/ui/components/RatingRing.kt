package de.remk0.flickpick.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.remk0.flickpick.domain.formatRating
import de.remk0.flickpick.ui.theme.Fp
import de.remk0.flickpick.ui.theme.PlusJakartaSans

/** The single source of truth for rating bands: thresholds, ring paint and number color. */
enum class RatingBand(val ringColors: List<Color>, val numberColor: Color) {
    EXCELLENT(Fp.BrandColors, Color.White),               // >= 8.0
    GOOD(listOf(Color(0xFF8A4FD8)), Color.White),        // 7.0 – 7.9
    OKAY(listOf(Color(0xFF4A4A55)), Color(0xFFE4E4EA)),  // 6.0 – 6.9
    LOW(listOf(Color(0xFF26262C)), Color(0xFF8E8E98));   // < 6.0 or no rating

    companion object {
        fun of(rating: Double?): RatingBand = when {
            rating == null -> LOW
            rating >= 8.0 -> EXCELLENT
            rating >= 7.0 -> GOOD
            rating >= 6.0 -> OKAY
            else -> LOW
        }
    }
}

@Composable
fun RatingRing(rating: Double?, modifier: Modifier = Modifier, size: Dp = 46.dp) {
    val band = RatingBand.of(rating)
    val scale = size.value / 46f
    val label = formatRating(rating)
    Box(
        modifier
            .size(size)
            .semantics { contentDescription = if (rating == null) "No IMDb rating" else "IMDb $label" },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 3.dp.toPx() * scale
            val brush: Brush = if (band.ringColors.size > 1) {
                Brush.linearGradient(band.ringColors, start = Offset.Zero, end = Offset(this.size.width, this.size.height))
            } else SolidColor(band.ringColors.first())
            drawCircle(
                brush = brush,
                radius = (this.size.minDimension - stroke) / 2,
                style = Stroke(width = stroke),
            )
        }
        Text(
            label,
            color = band.numberColor,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.ExtraBold,
            fontSize = (14 * scale).sp,
        )
    }
}
