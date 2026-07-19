package uk.hakkaren.wingman.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 暖色品牌 Token。所有用於文字的前景／背景組合皆符合 WCAG AA 4.5:1。 */
object WingmanColors {
    val Primary = Color(0xFFBE3D16)
    val PrimaryDeep = Color(0xFF942D12)
    val Caramel = Color(0xFF9B5E32)
    val WarmWhite = Color(0xFFFFF9F3)
    val Surface = Color(0xFFFFFFFF)
    val BrandCream = Color(0xFFF9E9D3)
    val Success = Color(0xFF0B7540)
    val Ink = Color(0xFF1D1712)
    val Muted = Color(0xFF665A50)

    val Border = Color(0xFFE4D5C5)
    val BrandCreamStrong = Color(0xFFEAC6A3)
    val SoftSurface = Color(0xFFF6F1EB)
    val SuccessSurface = Color(0xFFE6F4EA)

    // Screen-level semantic aliases keep call sites readable while sharing one token source.
    val Background = WarmWhite
    val Card = Surface
    val BrandSurface = BrandCream
    val BrandSurfaceStrong = BrandCreamStrong
    val TextPrimary = Ink
    val TextSecondary = Muted
    val Cream = BrandCream
    val CreamStrong = BrandCreamStrong
    val Orange = Primary
    val OrangeDark = PrimaryDeep
    val SuccessSoft = SuccessSurface
}

object WingmanSpacing {
    val Hairline = 4.dp
    val Small = 8.dp
    val Medium = 16.dp
    val Large = 24.dp
    val ExtraLarge = 32.dp
}

object WingmanShapes {
    val Hero = RoundedCornerShape(28.dp)
    val Card = RoundedCornerShape(24.dp)
    val InnerCard = RoundedCornerShape(20.dp)
    val Button = RoundedCornerShape(16.dp)
    val CompactButton = RoundedCornerShape(14.dp)
}

private val WingmanLightColors = lightColorScheme(
    primary = WingmanColors.Primary,
    onPrimary = Color.White,
    primaryContainer = WingmanColors.BrandSurface,
    onPrimaryContainer = WingmanColors.PrimaryDeep,
    secondary = WingmanColors.Caramel,
    onSecondary = Color.White,
    background = WingmanColors.Background,
    onBackground = WingmanColors.TextPrimary,
    surface = WingmanColors.Card,
    onSurface = WingmanColors.TextPrimary,
    surfaceVariant = WingmanColors.SoftSurface,
    onSurfaceVariant = WingmanColors.TextSecondary,
    outline = WingmanColors.Border,
)

private val WingmanTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Black,
        fontSize = 40.sp,
        lineHeight = 48.sp,
        letterSpacing = (-0.5f).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Black,
        fontSize = 30.sp,
        lineHeight = 38.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Black,
        fontSize = 24.sp,
        lineHeight = 32.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
)

@Composable
fun WingmanTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = WingmanLightColors,
        typography = WingmanTypography,
        content = content,
    )
}
