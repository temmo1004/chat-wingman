package uk.hakkaren.wingman.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import uk.hakkaren.wingman.R

enum class WingmanLogoVariant {
    Hero,
    Compact,
    Loading,
}

@Composable
fun WingmanLogo(
    modifier: Modifier = Modifier,
    contentDescription: String? = "孔明帽軍師標誌",
    variant: WingmanLogoVariant = WingmanLogoVariant.Hero,
) {
    val logoModifier = when (variant) {
        // Hero 與 Loading 都保留原始透明外型，避免當成重複的圓形 App Icon。
        WingmanLogoVariant.Hero,
        WingmanLogoVariant.Loading -> modifier

        // CTA 中用一層無陰影的淺色底保留小尺寸辨識度。
        WingmanLogoVariant.Compact -> modifier
            .clip(RoundedCornerShape(6.dp))
            .background(WingmanColors.BrandCream)
            .padding(2.dp)
    }

    Image(
        painter = painterResource(R.drawable.brand_kongming_hat),
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        modifier = logoModifier,
    )
}
