package de.remk0.flickpick.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import de.remk0.flickpick.R

object Fp {
    val Background = Color(0xFF000000)
    val Card = Color(0xFF141417)
    val CardBorder = Color.White.copy(alpha = 0.06f)
    val TextPrimary = Color(0xFFF5F5F7)
    val TextSecondary = Color(0xFF9C9CA6)

    val BrandRed = Color(0xFFE8475F)
    val BrandPurple = Color(0xFF8A4FD8)
    val BrandBlue = Color(0xFF3E63F2)
    val BrandColors = listOf(BrandRed, BrandPurple, BrandBlue)
    val BrandBrush = Brush.horizontalGradient(BrandColors)

    val GlowWine = Color(0xFF7A1C3E)
    val GlowIndigo = Color(0xFF3B1C78)

    val SearchFill = Color.White.copy(alpha = 0.08f)

    val ChipSelectedFill = Color(0x478A4FD8) // rgba(138,79,216,0.28)
    val ChipSelectedBorder = Color(0x99A06EF0) // rgba(160,110,240,0.6)
    val ChipSelectedText = Color(0xFFE2D6FF)
    val ChipFill = Color.White.copy(alpha = 0.06f)
    val ChipText = Color(0xFFC9C9D1)

    val Sheet = Color(0xFF141417)
}

val PlusJakartaSans = FontFamily(
    Font(R.font.plus_jakarta_sans_regular, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans_medium, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans_semibold, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans_bold, FontWeight.Bold),
    Font(R.font.plus_jakarta_sans_extrabold, FontWeight.ExtraBold),
)

private val base = TextStyle(fontFamily = PlusJakartaSans, color = Fp.TextPrimary)

private val typography = Typography(
    displaySmall = base.copy(fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 36.sp),
    headlineSmall = base.copy(fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 28.sp),
    titleLarge = base.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold),
    titleMedium = base.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold, lineHeight = 22.sp),
    titleSmall = base.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
    bodyLarge = base.copy(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = base.copy(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = base.copy(fontSize = 13.sp, lineHeight = 18.sp, color = Fp.TextSecondary),
    labelLarge = base.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = base.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
    labelSmall = base.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
)

private val colors = darkColorScheme(
    primary = Fp.BrandPurple,
    onPrimary = Color.White,
    secondary = Fp.BrandRed,
    tertiary = Fp.BrandBlue,
    background = Fp.Background,
    onBackground = Fp.TextPrimary,
    surface = Fp.Background,
    onSurface = Fp.TextPrimary,
    surfaceVariant = Fp.Card,
    onSurfaceVariant = Fp.TextSecondary,
    surfaceContainerLow = Fp.Card,
    surfaceContainer = Fp.Card,
    surfaceContainerHigh = Color(0xFF1C1C21),
    outline = Color(0xFF3A3A42),
    error = Color(0xFFFF6B7D),
)

@Composable
fun FlickpickTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, typography = typography, content = content)
}
