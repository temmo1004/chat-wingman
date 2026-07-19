package uk.hakkaren.wingman.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.SentimentVerySatisfied
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uk.hakkaren.wingman.LocalDemo
import uk.hakkaren.wingman.Reply
import uk.hakkaren.wingman.WingmanResult

sealed interface OverlayUiState {
    data object Loading : OverlayUiState
    data class Success(val result: WingmanResult) : OverlayUiState
    data class Error(val message: String) : OverlayUiState
}

@Composable
fun WingmanOverlayPanel(
    state: OverlayUiState,
    onFill: (Reply) -> Unit,
    onCopy: (Reply) -> Unit,
    onDismiss: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(topStart = 38.dp, topEnd = 20.dp, bottomStart = 38.dp, bottomEnd = 20.dp),
            color = Color.White,
            border = BorderStroke(1.dp, WingmanColors.Border),
            shadowElevation = 18.dp,
        ) {
            AnimatedContent(
                targetState = state,
                transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(140)) },
                label = "overlayState",
            ) { current ->
                when (current) {
                    OverlayUiState.Loading -> LoadingPanel()
                    is OverlayUiState.Error -> ErrorPanel(current.message, onRefresh)
                    is OverlayUiState.Success -> ResultPanel(
                        result = current.result,
                        onFill = onFill,
                        onCopy = onCopy,
                    )
                }
            }
        }

        Spacer(Modifier.width(6.dp))
        Column(
            modifier = Modifier.padding(bottom = 18.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                shape = CircleShape,
                color = WingmanColors.SoftSurface,
                border = BorderStroke(1.dp, WingmanColors.Border),
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(50.dp),
                ) {
                    Icon(Icons.Default.Close, contentDescription = "關閉軍師面板")
                }
            }
            Spacer(Modifier.height(224.dp))
            Surface(
                shape = CircleShape,
                color = WingmanColors.SoftSurface,
                border = BorderStroke(1.dp, WingmanColors.Border),
            ) {
                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.size(50.dp),
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "重新分析")
                }
            }
        }
    }
}

@Composable
private fun ResultPanel(
    result: WingmanResult,
    onFill: (Reply) -> Unit,
    onCopy: (Reply) -> Unit,
) {
    val defaultIndex = result.replies.indexOfFirst { it.style == "幽默" }.coerceAtLeast(0)
    var selectedIndex by remember(result) { mutableIntStateOf(defaultIndex) }
    val selected = result.replies.getOrNull(selectedIndex) ?: result.replies.firstOrNull()

    Column(
        modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            DeathIndexRing(result.chatDeathIndex)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = result.context,
                    color = WingmanColors.Ink,
                    fontSize = 16.sp,
                    lineHeight = 23.sp,
                    fontWeight = FontWeight.Medium,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Surface(
                        color = WingmanColors.SuccessSoft,
                        shape = RoundedCornerShape(999.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = WingmanColors.Success,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                "分析完成",
                                color = WingmanColors.Success,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Shield,
                            contentDescription = null,
                            tint = WingmanColors.Muted,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("僅分析這次畫面", color = WingmanColors.Muted, fontSize = 12.sp)
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            result.replies.forEachIndexed { index, reply ->
                StyleButton(
                    label = reply.style,
                    selected = index == selectedIndex,
                    onClick = { selectedIndex = index },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        if (selected != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = WingmanColors.WarmWhite,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.5.dp, WingmanColors.Orange),
            ) {
                Row(
                    modifier = Modifier.padding(start = 16.dp, top = 15.dp, end = 12.dp, bottom = 15.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    AnimatedContent(
                        targetState = selected.text,
                        modifier = Modifier.weight(1f),
                        label = "replyText",
                    ) { replyText ->
                        Text(
                            text = replyText,
                            color = WingmanColors.Ink,
                            fontSize = 17.sp,
                            lineHeight = 25.sp,
                        )
                    }
                    Surface(
                        shape = CircleShape,
                        color = WingmanColors.Orange,
                    ) {
                        IconButton(
                            onClick = { onFill(selected) },
                            modifier = Modifier.size(46.dp),
                        ) {
                            Icon(
                                Icons.Default.Send,
                                contentDescription = "填入這句回覆",
                                tint = Color.White,
                            )
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = WingmanColors.Cream.copy(alpha = 0.62f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, WingmanColors.CreamStrong),
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.School,
                            contentDescription = null,
                            tint = WingmanColors.Orange,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "軍師解說",
                            color = WingmanColors.OrangeDark,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(
                            Icons.Outlined.KeyboardArrowUp,
                            contentDescription = null,
                            tint = WingmanColors.OrangeDark,
                        )
                    }
                    Text(
                        text = selected.why,
                        color = WingmanColors.Muted,
                        fontSize = 14.sp,
                        lineHeight = 21.sp,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = { onFill(selected) },
                    modifier = Modifier
                        .weight(1.55f)
                        .height(52.dp),
                    shape = RoundedCornerShape(15.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WingmanColors.Orange),
                ) {
                    Icon(Icons.Outlined.Edit, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("填入輸入框", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = { onCopy(selected) },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(15.dp),
                    border = BorderStroke(1.dp, WingmanColors.Border),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = WingmanColors.Ink),
                ) {
                    Icon(Icons.Outlined.ContentCopy, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("複製", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun StyleButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val icon = when (label) {
        "幽默" -> Icons.Default.SentimentVerySatisfied
        "曖昧" -> Icons.Default.Favorite
        else -> Icons.Default.SentimentSatisfied
    }
    if (selected) {
        Button(
            onClick = onClick,
            modifier = modifier.height(50.dp),
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(horizontal = 8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = WingmanColors.Cream,
                contentColor = WingmanColors.OrangeDark,
            ),
            border = BorderStroke(1.5.dp, WingmanColors.Orange),
        ) {
            StyleButtonContent(icon, label)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.height(50.dp),
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(horizontal = 8.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = WingmanColors.Ink),
            border = BorderStroke(1.dp, WingmanColors.Border),
        ) {
            StyleButtonContent(icon, label)
        }
    }
}

@Composable
private fun StyleButtonContent(icon: ImageVector, label: String) {
    Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
    Spacer(Modifier.width(6.dp))
    Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun DeathIndexRing(index: Int) {
    val safeIndex = index.coerceIn(0, 100)
    Box(
        modifier = Modifier.size(92.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(84.dp)) {
            val stroke = 7.dp.toPx()
            drawArc(
                color = WingmanColors.CreamStrong,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(stroke, cap = StrokeCap.Butt),
            )
            drawArc(
                color = WingmanColors.Orange,
                startAngle = -90f,
                sweepAngle = 360f * safeIndex / 100f,
                useCenter = false,
                style = Stroke(stroke, cap = StrokeCap.Butt),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("聊死指數", color = WingmanColors.Muted, fontSize = 10.sp)
            Text(
                text = safeIndex.toString(),
                color = WingmanColors.Ink,
                fontSize = 30.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

@Composable
private fun LoadingPanel() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 280.dp)
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        WingmanLogo(modifier = Modifier.size(76.dp))
        Spacer(Modifier.height(20.dp))
        CircularProgressIndicator(color = WingmanColors.Orange, strokeWidth = 3.dp)
        Spacer(Modifier.height(16.dp))
        Text(
            "軍師正在判讀對話",
            color = WingmanColors.Ink,
            fontSize = 19.sp,
            fontWeight = FontWeight.Black,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "通常只需要幾秒，請先別離開聊天畫面",
            color = WingmanColors.Muted,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ErrorPanel(message: String, onRefresh: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 260.dp)
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Default.Warning,
            contentDescription = null,
            tint = WingmanColors.Orange,
            modifier = Modifier.size(36.dp),
        )
        Spacer(Modifier.height(14.dp))
        Text("這次沒看清楚", fontSize = 19.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(6.dp))
        Text(
            message,
            color = WingmanColors.Muted,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(18.dp))
        Button(onClick = onRefresh) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("重新分析")
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 600)
@Composable
private fun WingmanOverlayPreview() {
    WingmanTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(WingmanColors.WarmWhite)
                .padding(top = 48.dp),
            contentAlignment = Alignment.BottomCenter,
        ) {
            WingmanOverlayPanel(
                state = OverlayUiState.Success(LocalDemo.result),
                onFill = {},
                onCopy = {},
                onDismiss = {},
                onRefresh = {},
            )
        }
    }
}
