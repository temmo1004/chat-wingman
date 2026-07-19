package uk.hakkaren.wingman.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class PermissionUiState(
    val overlayGranted: Boolean,
    val captureGranted: Boolean,
    val accessibilityGranted: Boolean,
)

@Composable
fun WingmanHomeScreen(
    permissionState: PermissionUiState,
    onTestBubble: () -> Unit,
    onOverlayPermission: () -> Unit,
    onCapturePermission: () -> Unit,
    onAccessibilityPermission: () -> Unit,
    onUnavailableTab: (String) -> Unit,
) {
    var showInstructions by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = WingmanColors.WarmWhite,
        bottomBar = { WingmanBottomBar(onUnavailableTab) },
    ) { scaffoldPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 22.dp,
                top = scaffoldPadding.calculateTopPadding(),
                end = 22.dp,
                bottom = scaffoldPadding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Column(
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(top = 20.dp, bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text(
                        text = "聊天軍師",
                        color = WingmanColors.Ink,
                        fontSize = 40.sp,
                        lineHeight = 44.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-1).sp,
                    )
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = WingmanColors.SuccessSoft,
                        border = BorderStroke(1.dp, WingmanColors.Success.copy(alpha = 0.2f)),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(WingmanColors.Success, CircleShape),
                            )
                            Text(
                                text = "軍師在線",
                                color = WingmanColors.Success,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
            }

            item {
                HeroCard(
                    onTestBubble = onTestBubble,
                    onInstructions = { showInstructions = true },
                )
            }

            item {
                PermissionCard(
                    state = permissionState,
                    onOverlayPermission = onOverlayPermission,
                    onCapturePermission = onCapturePermission,
                    onAccessibilityPermission = onAccessibilityPermission,
                )
            }

            item { DailyLessonCard() }
        }
    }

    if (showInstructions) {
        InstructionsDialog(
            accessibilityGranted = permissionState.accessibilityGranted,
            onEnableAccessibility = onAccessibilityPermission,
            onDismiss = { showInstructions = false },
        )
    }
}

@Composable
private fun HeroCard(
    onTestBubble: () -> Unit,
    onInstructions: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = WingmanColors.Cream,
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, WingmanColors.CreamStrong),
        shadowElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 306.dp)
                .padding(horizontal = 22.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = "軍師已上線",
                    color = WingmanColors.Ink,
                    fontSize = 31.sp,
                    lineHeight = 36.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.weight(1f),
                )
                WingmanLogo(modifier = Modifier.size(92.dp))
            }
            Text(
                text = "在 LINE 或任何聊天 App，\n點一下浮動球就能救場",
                color = WingmanColors.Muted,
                fontSize = 16.sp,
                lineHeight = 25.sp,
            )
            Button(
                onClick = onTestBubble,
                modifier = Modifier.height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WingmanColors.Orange),
                contentPadding = PaddingValues(horizontal = 18.dp),
            ) {
                WingmanLogo(modifier = Modifier.size(28.dp), contentDescription = null)
                Spacer(Modifier.width(9.dp))
                Text("測試浮動球", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            TextButton(
                onClick = onInstructions,
                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 0.dp),
                colors = ButtonDefaults.textButtonColors(contentColor = WingmanColors.OrangeDark),
            ) {
                Text("查看使用方法", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Icon(
                    imageVector = Icons.Outlined.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

@Composable
private fun PermissionCard(
    state: PermissionUiState,
    onOverlayPermission: () -> Unit,
    onCapturePermission: () -> Unit,
    onAccessibilityPermission: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, WingmanColors.Border),
    ) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp)) {
            Text(
                text = "開始前確認",
                color = WingmanColors.Ink,
                fontSize = 21.sp,
                fontWeight = FontWeight.Black,
            )
            Spacer(Modifier.height(12.dp))
            PermissionRow(
                label = "顯示在其他 App 上層",
                granted = state.overlayGranted,
                onClick = onOverlayPermission,
            )
            HorizontalDivider(color = WingmanColors.Border.copy(alpha = 0.7f))
            PermissionRow(
                label = "螢幕擷取權限",
                granted = state.captureGranted,
                onClick = onCapturePermission,
            )
            if (!state.accessibilityGranted) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .heightIn(min = 48.dp)
                        .clickable(onClick = onAccessibilityPermission),
                    color = WingmanColors.Cream,
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(9.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Shield,
                            contentDescription = null,
                            tint = WingmanColors.OrangeDark,
                            modifier = Modifier.size(19.dp),
                        )
                        Text(
                            text = "想一鍵填入？開啟選用的無障礙服務",
                            color = WingmanColors.OrangeDark,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(
                            imageVector = Icons.Outlined.ArrowForwardIos,
                            contentDescription = null,
                            tint = WingmanColors.OrangeDark,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionRow(
    label: String,
    granted: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(34.dp),
            shape = CircleShape,
            color = if (granted) WingmanColors.Success else WingmanColors.SoftSurface,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (granted) Icons.Default.Check else Icons.Outlined.Info,
                    contentDescription = null,
                    tint = if (granted) Color.White else WingmanColors.Muted,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            color = WingmanColors.Ink,
            fontSize = 15.sp,
            modifier = Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = if (granted) "已開啟" else "待設定",
            color = if (granted) WingmanColors.Success else WingmanColors.OrangeDark,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        )
        Icon(
            imageVector = Icons.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = WingmanColors.Muted,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun DailyLessonCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, WingmanColors.Border),
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.School,
                        contentDescription = null,
                        tint = WingmanColors.Orange,
                    )
                    Text(
                        text = "今日軍師課",
                        color = WingmanColors.OrangeDark,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    text = "少問選擇題，多給一個好接的畫面",
                    color = WingmanColors.Ink,
                    fontSize = 18.sp,
                    lineHeight = 26.sp,
                    fontWeight = FontWeight.Black,
                )
            }
            Surface(
                shape = CircleShape,
                color = WingmanColors.Cream,
                modifier = Modifier.size(58.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                        tint = WingmanColors.Orange,
                        modifier = Modifier.size(27.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun WingmanBottomBar(onUnavailableTab: (String) -> Unit) {
    val itemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = WingmanColors.OrangeDark,
        selectedTextColor = WingmanColors.OrangeDark,
        indicatorColor = WingmanColors.CreamStrong,
        unselectedIconColor = WingmanColors.Muted,
        unselectedTextColor = WingmanColors.Muted,
    )
    NavigationBar(
        modifier = Modifier.navigationBarsPadding(),
        containerColor = Color.White,
        tonalElevation = 0.dp,
    ) {
        NavigationBarItem(
            selected = true,
            onClick = {},
            icon = { Icon(Icons.Default.Home, contentDescription = null) },
            label = { Text("首頁") },
            colors = itemColors,
        )
        NavigationBarItem(
            selected = false,
            onClick = { onUnavailableTab("紀錄") },
            icon = { Icon(Icons.Outlined.History, contentDescription = null) },
            label = { Text("紀錄") },
            colors = itemColors,
        )
        NavigationBarItem(
            selected = false,
            onClick = { onUnavailableTab("設定") },
            icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
            label = { Text("設定") },
            colors = itemColors,
        )
    }
}

@Composable
private fun InstructionsDialog(
    accessibilityGranted: Boolean,
    onEnableAccessibility: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { WingmanLogo(modifier = Modifier.size(56.dp)) },
        title = { Text("三步叫出軍師", fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("1. 開啟顯示在其他 App 上層")
                Text("2. 授權螢幕擷取後，到聊天畫面短按孔明帽分析目前畫面")
                Text("3. 長按孔明帽可從相簿選擇聊天截圖")
                Text("4. 選語氣，填入輸入框或複製；訊息仍由你親自送出")
                Text(
                    text = "軍師只分析你當次主動擷取的畫面，不會在背景監看聊天。",
                    color = WingmanColors.Muted,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("知道了") }
        },
        dismissButton = if (!accessibilityGranted) {
            {
                TextButton(onClick = onEnableAccessibility) {
                    Text("開啟一鍵填入")
                }
            }
        } else null,
    )
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun WingmanHomePreview() {
    WingmanTheme {
        WingmanHomeScreen(
            permissionState = PermissionUiState(
                overlayGranted = true,
                captureGranted = true,
                accessibilityGranted = true,
            ),
            onTestBubble = {},
            onOverlayPermission = {},
            onCapturePermission = {},
            onAccessibilityPermission = {},
            onUnavailableTab = {},
        )
    }
}
