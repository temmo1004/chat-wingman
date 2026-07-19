package uk.hakkaren.wingman.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object WingmanColors {
    val Ink = Color(0xFF17130F)
    val Muted = Color(0xFF73675E)
    val WarmWhite = Color(0xFFFFFCF8)
    val Cream = Color(0xFFFFF2E4)
    val CreamStrong = Color(0xFFFFE5CF)
    val Orange = Color(0xFFEB572D)
    val OrangeDark = Color(0xFFC84222)
    val Caramel = Color(0xFFC9824B)
    val Border = Color(0xFFEADFD5)
    val SoftSurface = Color(0xFFF8F4EF)
    val Success = Color(0xFF159455)
    val SuccessSoft = Color(0xFFE8F7EE)
}

private val WingmanLightColors = lightColorScheme(
    primary = WingmanColors.Orange,
    onPrimary = Color.White,
    primaryContainer = WingmanColors.CreamStrong,
    onPrimaryContainer = WingmanColors.OrangeDark,
    secondary = WingmanColors.Caramel,
    background = WingmanColors.WarmWhite,
    onBackground = WingmanColors.Ink,
    surface = Color.White,
    onSurface = WingmanColors.Ink,
    surfaceVariant = WingmanColors.SoftSurface,
    onSurfaceVariant = WingmanColors.Muted,
    outline = WingmanColors.Border,
)

@Composable
fun WingmanTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = WingmanLightColors,
        content = content,
    )
}
