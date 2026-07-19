package uk.hakkaren.wingman.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.SentimentVerySatisfied
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(WingmanSpacing.Small),
    ) {
        val availablePanelHeight = if (maxHeight == Dp.Infinity) {
            480.dp
        } else {
            (maxHeight - WingmanSpacing.Medium).coerceAtLeast(280.dp)
        }
        val panelMaxHeight = availablePanelHeight.coerceAtMost(480.dp)

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(max = panelMaxHeight)
                    .semantics { paneTitle = "聊天軍師分析面板" },
                shape = WingmanShapes.Hero,
                color = WingmanColors.Card,
                border = BorderStroke(1.dp, WingmanColors.Border),
                shadowElevation = 4.dp,
            ) {
                AnimatedContent(
                    targetState = state,
                    modifier = Modifier.fillMaxWidth(),
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

            Spacer(Modifier.width(WingmanSpacing.Small))
            Column(
                verticalArrangement = Arrangement.spacedBy(WingmanSpacing.Medium),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                OverlayRailButton(
                    icon = Icons.Default.Close,
                    contentDescription = "關閉軍師面板",
                    onClick = onDismiss,
                )
                OverlayRailButton(
                    icon = Icons.Default.Refresh,
                    contentDescription = "重新分析",
                    onClick = onRefresh,
                )
            }
        }
    }
}

@Composable
private fun OverlayRailButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Surface(
        shape = CircleShape,
        color = WingmanColors.SoftSurface,
        border = BorderStroke(1.dp, WingmanColors.Border),
        shadowElevation = 2.dp,
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(52.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = WingmanColors.TextPrimary,
            )
        }
    }
}

@Composable
private fun ResultPanel(
    result: WingmanResult,
    onFill: (Reply) -> Unit,
    onCopy: (Reply) -> Unit,
) {
    val styleOrder = remember { listOf("認真", "幽默", "曖昧") }
    val repliesByStyle = remember(result.replies) { result.replies.associateBy(Reply::style) }
    val defaultReply = repliesByStyle["幽默"]
        ?: styleOrder.firstNotNullOfOrNull(repliesByStyle::get)
        ?: result.replies.firstOrNull()
    var selectedStyle by remember(result) { mutableStateOf(defaultReply?.style) }
    val selectedReply = selectedStyle?.let(repliesByStyle::get) ?: defaultReply

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(WingmanSpacing.Medium),
        verticalArrangement = Arrangement.spacedBy(WingmanSpacing.Medium),
    ) {
        ResultHeader(result)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(WingmanSpacing.Small),
        ) {
            styleOrder.forEach { style ->
                val reply = repliesByStyle[style]
                StyleButton(
                    label = style,
                    selected = reply != null && selectedReply?.style == style,
                    enabled = reply != null,
                    onClick = { selectedStyle = style },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        if (selectedReply == null) {
            MissingRepliesPanel()
        } else {
            val selected = selectedReply
            ReplyCard(reply = selected, onFill = { onFill(selected) })
            ExplanationCard(explanation = selected.why)
            ReplyActions(
                onFill = { onFill(selected) },
                onCopy = { onCopy(selected) },
            )
        }
    }
}

@Composable
private fun MissingRepliesPanel() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = WingmanColors.SoftSurface,
        shape = WingmanShapes.InnerCard,
        border = BorderStroke(1.dp, WingmanColors.Border),
    ) {
        Row(
            modifier = Modifier.padding(WingmanSpacing.Medium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(WingmanSpacing.Small),
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = WingmanColors.PrimaryDeep,
            )
            Text(
                text = "目前沒有可用回覆，請重新分析",
                color = WingmanColors.TextPrimary,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun ResultHeader(result: WingmanResult) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val stacked = maxWidth < 300.dp || LocalDensity.current.fontScale > 1.3f
        if (stacked) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(WingmanSpacing.Medium),
            ) {
                DeathIndexRing(result.chatDeathIndex)
                ResultSummary(result = result, modifier = Modifier.fillMaxWidth())
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(WingmanSpacing.Medium),
            ) {
                DeathIndexRing(result.chatDeathIndex)
                ResultSummary(result = result, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ResultSummary(result: WingmanResult, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(WingmanSpacing.Small),
    ) {
        Text(
            text = result.context,
            color = WingmanColors.TextPrimary,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
        )
        Surface(
            color = WingmanColors.SuccessSurface,
            shape = RoundedCornerShape(999.dp),
            modifier = Modifier.semantics { stateDescription = "分析完成" },
        ) {
            Row(
                modifier = Modifier.padding(
                    horizontal = WingmanSpacing.Small,
                    vertical = WingmanSpacing.Hairline,
                ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(WingmanSpacing.Hairline),
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = WingmanColors.Success,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = "分析完成",
                    color = WingmanColors.Success,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(WingmanSpacing.Hairline),
        ) {
            Icon(
                imageVector = Icons.Outlined.Shield,
                contentDescription = null,
                tint = WingmanColors.TextSecondary,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = "僅分析這次畫面",
                color = WingmanColors.TextSecondary,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun ReplyCard(reply: Reply, onFill: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = WingmanColors.Background,
        shape = WingmanShapes.InnerCard,
        border = BorderStroke(2.dp, WingmanColors.Primary),
    ) {
        Row(
            modifier = Modifier.padding(WingmanSpacing.Medium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(WingmanSpacing.Small),
        ) {
            Text(
                text = reply.text,
                color = WingmanColors.TextPrimary,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Surface(
                shape = CircleShape,
                color = WingmanColors.Primary,
            ) {
                IconButton(
                    onClick = onFill,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "填入這句回覆，不會自動送出",
                        tint = Color.White,
                    )
                }
            }
        }
    }
}

@Composable
private fun ExplanationCard(explanation: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = WingmanColors.BrandSurface,
        shape = WingmanShapes.InnerCard,
        border = BorderStroke(1.dp, WingmanColors.BrandSurfaceStrong),
    ) {
        Column(
            modifier = Modifier.padding(WingmanSpacing.Medium),
            verticalArrangement = Arrangement.spacedBy(WingmanSpacing.Small),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(WingmanSpacing.Small),
            ) {
                Icon(
                    imageVector = Icons.Outlined.School,
                    contentDescription = null,
                    tint = WingmanColors.PrimaryDeep,
                    modifier = Modifier.size(24.dp),
                )
                Text(
                    text = "軍師解說",
                    color = WingmanColors.PrimaryDeep,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Icons.Outlined.KeyboardArrowUp,
                    contentDescription = null,
                    tint = WingmanColors.PrimaryDeep,
                )
            }
            Text(
                text = explanation,
                color = WingmanColors.TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun ReplyActions(
    onFill: () -> Unit,
    onCopy: () -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val stacked = maxWidth < 280.dp || LocalDensity.current.fontScale > 1.3f
        if (stacked) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(WingmanSpacing.Small),
            ) {
                FillButton(onClick = onFill, modifier = Modifier.fillMaxWidth())
                CopyButton(onClick = onCopy, modifier = Modifier.fillMaxWidth())
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(WingmanSpacing.Small),
            ) {
                FillButton(onClick = onFill, modifier = Modifier.weight(1.5f))
                CopyButton(onClick = onCopy, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun FillButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 56.dp),
        shape = WingmanShapes.Button,
        colors = ButtonDefaults.buttonColors(containerColor = WingmanColors.Primary),
    ) {
        Icon(Icons.Outlined.Edit, contentDescription = null)
        Spacer(Modifier.width(WingmanSpacing.Small))
        Text("填入輸入框", style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun CopyButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 56.dp),
        shape = WingmanShapes.Button,
        contentPadding = PaddingValues(horizontal = WingmanSpacing.Small),
        border = BorderStroke(1.dp, WingmanColors.Border),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = WingmanColors.TextPrimary),
    ) {
        Icon(Icons.Outlined.ContentCopy, contentDescription = null)
        Spacer(Modifier.width(WingmanSpacing.Small))
        Text("複製", style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun StyleButton(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val icon = when (label) {
        "幽默" -> Icons.Default.SentimentVerySatisfied
        "曖昧" -> Icons.Default.Favorite
        else -> Icons.Default.SentimentSatisfied
    }
    val semanticsModifier = modifier.semantics {
        this.selected = selected
        stateDescription = if (selected) "$label，已選取" else "$label，未選取"
    }

    if (selected) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = semanticsModifier.heightIn(min = 52.dp),
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(horizontal = WingmanSpacing.Small),
            colors = ButtonDefaults.buttonColors(
                containerColor = WingmanColors.BrandSurface,
                contentColor = WingmanColors.PrimaryDeep,
            ),
            border = BorderStroke(2.dp, WingmanColors.Primary),
        ) {
            StyleButtonContent(icon, label)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            modifier = semanticsModifier.heightIn(min = 52.dp),
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(horizontal = WingmanSpacing.Small),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = WingmanColors.TextPrimary),
            border = BorderStroke(1.dp, WingmanColors.Border),
        ) {
            StyleButtonContent(icon, label)
        }
    }
}

@Composable
private fun StyleButtonContent(icon: ImageVector, label: String) {
    Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
    Spacer(Modifier.width(WingmanSpacing.Hairline))
    Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
}

@Composable
private fun DeathIndexRing(index: Int) {
    val safeIndex = index.coerceIn(0, 100)
    val fontScale = LocalDensity.current.fontScale.coerceIn(1f, 1.6f)
    val ringSize = 96.dp * fontScale
    Box(
        modifier = Modifier
            .size(ringSize)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(
                    current = safeIndex.toFloat(),
                    range = 0f..100f,
                )
                stateDescription = "聊死指數 $safeIndex，滿分 100"
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(ringSize - 8.dp)) {
            val stroke = (8.dp * fontScale).toPx()
            drawArc(
                color = WingmanColors.BrandSurfaceStrong,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
            drawArc(
                color = WingmanColors.Primary,
                startAngle = -90f,
                sweepAngle = 360f * safeIndex / 100f,
                useCenter = false,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "聊死指數",
                color = WingmanColors.TextSecondary,
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = safeIndex.toString(),
                color = WingmanColors.TextPrimary,
                style = MaterialTheme.typography.headlineMedium,
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
            .padding(WingmanSpacing.ExtraLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        WingmanLogo(
            modifier = Modifier.size(72.dp),
            contentDescription = "孔明帽軍師正在分析",
            variant = WingmanLogoVariant.Loading,
        )
        Spacer(Modifier.height(WingmanSpacing.Medium))
        CircularProgressIndicator(color = WingmanColors.Primary, strokeWidth = 3.dp)
        Spacer(Modifier.height(WingmanSpacing.Medium))
        Text(
            text = "軍師正在判讀對話",
            color = WingmanColors.TextPrimary,
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(WingmanSpacing.Small))
        Text(
            text = "通常只需要幾秒，請先別離開聊天畫面",
            color = WingmanColors.TextSecondary,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ErrorPanel(message: String, onRefresh: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 264.dp)
            .verticalScroll(rememberScrollState())
            .padding(WingmanSpacing.ExtraLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = WingmanColors.Primary,
            modifier = Modifier.size(40.dp),
        )
        Spacer(Modifier.height(WingmanSpacing.Medium))
        Text("這次沒看清楚", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(WingmanSpacing.Small))
        Text(
            text = message,
            color = WingmanColors.TextSecondary,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(WingmanSpacing.Medium))
        Button(
            onClick = onRefresh,
            modifier = Modifier.heightIn(min = 48.dp),
            shape = WingmanShapes.Button,
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(Modifier.width(WingmanSpacing.Small))
            Text("重新分析")
        }
    }
}

/** 僅供 Preview／視覺比對；正式 Overlay 永遠顯示在使用者真實聊天 App 上。 */
@Composable
private fun DemoChatPreviewBackground() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WingmanColors.Background)
            .padding(horizontal = WingmanSpacing.Medium, vertical = 56.dp),
        verticalArrangement = Arrangement.spacedBy(WingmanSpacing.Large),
    ) {
        DemoMessage(text = "你下班了嗎？", fromUser = true)
        DemoMessage(text = "剛到家", fromUser = false)
        DemoMessage(text = "今天忙嗎？", fromUser = true)
        DemoMessage(text = "還好 哈哈", fromUser = false)
    }
}

@Composable
private fun DemoMessage(text: String, fromUser: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (fromUser) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            color = if (fromUser) WingmanColors.SuccessSurface else WingmanColors.Card,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, WingmanColors.Border),
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(
                    horizontal = WingmanSpacing.Medium,
                    vertical = WingmanSpacing.Small,
                ),
                color = WingmanColors.TextPrimary,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun WingmanOverlayPreview() {
    WingmanTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            DemoChatPreviewBackground()
            WingmanOverlayPanel(
                state = OverlayUiState.Success(LocalDemo.result),
                onFill = {},
                onCopy = {},
                onDismiss = {},
                onRefresh = {},
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}
