package uk.hakkaren.wingman.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import uk.hakkaren.wingman.R

@Composable
fun WingmanLogo(
    modifier: Modifier = Modifier,
    contentDescription: String? = "孔明帽軍師標誌",
) {
    Image(
        painter = painterResource(R.drawable.ic_launcher_foreground),
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .shadow(14.dp, CircleShape, ambientColor = WingmanColors.Caramel)
            .clip(CircleShape)
            .background(WingmanColors.Cream, CircleShape)
            .border(1.dp, WingmanColors.CreamStrong, CircleShape)
            .padding(5.dp),
    )
}
