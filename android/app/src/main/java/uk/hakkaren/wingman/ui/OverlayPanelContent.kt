package uk.hakkaren.wingman.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.SentimentVerySatisfied
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Edit
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uk.hakkaren.wingman.LocalDemo
import uk.hakkaren.wingman.Reply
import uk.hakkaren.wingman.WingmanResult

private const val STATE_ENTER_DURATION_MS = 200
private const val STATE_EXIT_DURATION_MS = 160
private const val REPLY_SWITCH_DURATION_MS = 200
private val ReplyStyleOrder = listOf("認真", "幽默", "曖昧")

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
    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(start = 8.dp, end = 8.dp, bottom = 8.dp)
            .semantics { paneTitle = "聊天軍師分析面板" },
        contentAlignment = Alignment.BottomCenter,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 56.dp),
            shape = RoundedCornerShape(28.dp),
            color = Color.White,
            border = BorderStroke(1.dp, WingmanColors.Border),
            shadowElevation = 18.dp,
        ) {
            AnimatedContent(
                targetState = state,
                transitionSpec = {
                    fadeIn(tween(STATE_ENTER_DURATION_MS)) togetherWith
                        fadeOut(tween(STATE_EXIT_DURATION_MS))
                },
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

        OverlayIconAction(
            icon = Icons.Default.Close,
            contentDescription = "關閉軍師面板",
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 8.dp),
        )

        if (state is OverlayUiState.Success) {
            OverlayIconAction(
                icon = Icons.Default.Refresh,
                contentDescription = "重新分析",
                onClick = onRefresh,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 8.dp),
            )
        }
    }
}

@Composable
private fun OverlayIconAction(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = WingmanColors.SoftSurface,
        border = BorderStroke(1.dp, WingmanColors.Border),
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = WingmanColors.Ink,
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
    val repliesByStyle = remember(result.replies) { result.replies.associateBy(Reply::style) }
    val defaultReply = repliesByStyle["幽默"]
        ?: ReplyStyleOrder.firstNotNullOfOrNull(repliesByStyle::get)
        ?: result.replies.firstOrNull()
    var selectedStyle by remember(result) { mutableStateOf(defaultReply?.style) }
    val selectedReply = selectedStyle?.let(repliesByStyle::get) ?: defaultReply

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DeathIndexRing(result.chatDeathIndex)
            Text(
                text = result.context,
                color = WingmanColors.Ink,
                fontSize = 16.sp,
                lineHeight = 23.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
            )
        }

        AnalysisMetaRow()

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ReplyStyleOrder.forEach { style ->
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
            Crossfade(
                targetState = selectedReply,
                animationSpec = tween(REPLY_SWITCH_DURATION_MS),
                label = "selectedReply",
            ) { reply ->
                SelectedReplyContent(
                    reply = reply,
                    onFill = onFill,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = { onCopy(selectedReply) },
                    modifier = Modifier
                        .weight(1.55f)
                        .heightIn(min = 52.dp),
                    shape = RoundedCornerShape(15.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WingmanColors.Orange),
                ) {
                    Icon(Icons.Outlined.ContentCopy, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("複製回覆", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = { onFill(selectedReply) },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 52.dp),
                    shape = RoundedCornerShape(15.dp),
                    border = BorderStroke(1.dp, WingmanColors.Border),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = WingmanColors.Ink),
                ) {
                    Icon(Icons.Outlined.Edit, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("填入", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun AnalysisMetaRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
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
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.Shield,
                contentDescription = null,
                tint = WingmanColors.Muted,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                "僅分析這次畫面",
                color = WingmanColors.Muted,
                fontSize = 12.sp,
                lineHeight = 17.sp,
            )
        }
    }
}

@Composable
private fun SelectedReplyContent(
    reply: Reply,
    onFill: (Reply) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                Text(
                    text = reply.text,
                    color = WingmanColors.Ink,
                    fontSize = 17.sp,
                    lineHeight = 25.sp,
                    modifier = Modifier.weight(1f),
                )
                Surface(
                    shape = CircleShape,
                    color = WingmanColors.Orange,
                ) {
                    IconButton(
                        onClick = { onFill(reply) },
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "填入這句回覆",
                            tint = Color.White,
                        )
                    }
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = WingmanColors.Cream,
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
                    )
                }
                Text(
                    text = reply.why,
                    color = WingmanColors.Muted,
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                )
            }
        }
    }
}

@Composable
private fun MissingRepliesPanel() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = WingmanColors.Cream,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, WingmanColors.CreamStrong),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = WingmanColors.OrangeDark,
            )
            Text(
                text = "暫時沒有可用回覆，請重新分析。",
                color = WingmanColors.Ink,
                fontSize = 14.sp,
                lineHeight = 21.sp,
                modifier = Modifier.weight(1f),
            )
        }
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
    if (selected) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier.heightIn(min = 50.dp),
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(horizontal = 6.dp),
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
            enabled = enabled,
            modifier = modifier.heightIn(min = 50.dp),
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(horizontal = 6.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = WingmanColors.Ink,
                disabledContentColor = WingmanColors.Muted.copy(alpha = 0.48f),
            ),
            border = BorderStroke(1.dp, WingmanColors.Border),
        ) {
            StyleButtonContent(icon, label)
        }
    }
}

@Composable
private fun StyleButtonContent(icon: ImageVector, label: String) {
    Icon(icon, contentDescription = null, modifier = Modifier.size(19.dp))
    Spacer(Modifier.width(4.dp))
    Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun DeathIndexRing(index: Int) {
    val safeIndex = index.coerceIn(0, 100)
    Box(
        modifier = Modifier
            .size(88.dp)
            .clearAndSetSemantics {
                contentDescription = "聊死指數 $safeIndex 分，滿分 100 分"
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(80.dp)) {
            val stroke = 7.dp.toPx()
            drawArc(
                color = WingmanColors.CreamStrong,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
            drawArc(
                color = WingmanColors.Orange,
                startAngle = -90f,
                sweepAngle = 360f * safeIndex / 100f,
                useCenter = false,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("聊死指數", color = WingmanColors.Muted, fontSize = 10.sp)
            Text(
                text = safeIndex.toString(),
                color = WingmanColors.Ink,
                fontSize = 29.sp,
                lineHeight = 31.sp,
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
        Spacer(Modifier.heightIn(min = 20.dp))
        CircularProgressIndicator(
            color = WingmanColors.Orange,
            strokeWidth = 3.dp,
            modifier = Modifier.size(40.dp),
        )
        Spacer(Modifier.heightIn(min = 16.dp))
        Text(
            "軍師正在判讀對話",
            color = WingmanColors.Ink,
            fontSize = 19.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.heightIn(min = 8.dp))
        Text(
            "通常只需要幾秒，請先別離開聊天畫面",
            color = WingmanColors.Muted,
            fontSize = 13.sp,
            lineHeight = 19.sp,
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
        Spacer(Modifier.heightIn(min = 14.dp))
        Text(
            "這次沒看清楚",
            color = WingmanColors.Ink,
            fontSize = 19.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.heightIn(min = 6.dp))
        Text(
            text = message.ifBlank { "請確認聊天畫面後再試一次。" },
            color = WingmanColors.Muted,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.heightIn(min = 18.dp))
        Button(
            onClick = onRefresh,
            modifier = Modifier.heightIn(min = 50.dp),
            shape = RoundedCornerShape(15.dp),
            colors = ButtonDefaults.buttonColors(containerColor = WingmanColors.Orange),
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("重新分析", fontWeight = FontWeight.Bold)
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 720)
@Composable
private fun WingmanOverlayPreview() {
    WingmanTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(WingmanColors.WarmWhite),
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
