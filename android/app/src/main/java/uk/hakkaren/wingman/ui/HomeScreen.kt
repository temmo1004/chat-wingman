package uk.hakkaren.wingman.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

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
        containerColor = WingmanColors.Background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = { WingmanBottomBar(onUnavailableTab = onUnavailableTab) },
    ) { scaffoldPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(WingmanColors.Background),
            contentPadding = PaddingValues(
                start = WingmanSpacing.Large,
                end = WingmanSpacing.Large,
                bottom = scaffoldPadding.calculateBottomPadding() + WingmanSpacing.Large,
            ),
            verticalArrangement = Arrangement.spacedBy(WingmanSpacing.Medium),
        ) {
            item { HomeHeader() }
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
private fun WingmanBottomBar(onUnavailableTab: (String) -> Unit) {
    val colors = NavigationBarItemDefaults.colors(
        selectedIconColor = WingmanColors.PrimaryDeep,
        selectedTextColor = WingmanColors.PrimaryDeep,
        indicatorColor = WingmanColors.BrandSurface,
        unselectedIconColor = WingmanColors.TextSecondary,
        unselectedTextColor = WingmanColors.TextSecondary,
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = WingmanSpacing.Large, vertical = WingmanSpacing.Small),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = WingmanColors.Card,
            shape = WingmanShapes.Card,
            border = BorderStroke(1.dp, WingmanColors.Border),
            shadowElevation = 2.dp,
        ) {
            NavigationBar(
                containerColor = Color.Transparent,
                tonalElevation = 0.dp,
            ) {
                NavigationBarItem(
                    selected = true,
                    onClick = {},
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("首頁") },
                    colors = colors,
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { onUnavailableTab("紀錄") },
                    icon = { Icon(Icons.Outlined.History, contentDescription = null) },
                    label = { Text("紀錄") },
                    colors = colors,
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { onUnavailableTab("設定") },
                    icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                    label = { Text("設定") },
                    colors = colors,
                )
            }
        }
    }
}

@Composable
private fun HomeHeader() {
    Column(
        modifier = Modifier
            .statusBarsPadding()
            .padding(top = WingmanSpacing.Large, bottom = WingmanSpacing.Small),
        verticalArrangement = Arrangement.spacedBy(WingmanSpacing.Medium),
    ) {
        Text(
            text = "聊天軍師",
            color = WingmanColors.TextPrimary,
            style = MaterialTheme.typography.displayLarge,
            maxLines = 2,
        )
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = WingmanColors.SuccessSurface,
            border = BorderStroke(1.dp, WingmanColors.Success.copy(alpha = 0.24f)),
            modifier = Modifier.semantics { stateDescription = "軍師在線" },
        ) {
            Row(
                modifier = Modifier.padding(
                    horizontal = WingmanSpacing.Medium,
                    vertical = WingmanSpacing.Small,
                ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(WingmanSpacing.Small),
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(WingmanColors.Success, CircleShape),
                )
                Text(
                    text = "軍師在線",
                    color = WingmanColors.Success,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun HeroCard(
    onTestBubble: () -> Unit,
    onInstructions: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = WingmanColors.BrandSurface,
        shape = WingmanShapes.Hero,
        border = BorderStroke(1.dp, WingmanColors.BrandSurfaceStrong),
        shadowElevation = 1.dp,
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val largeText = LocalDensity.current.fontScale > 1.2f
            val stacked = maxWidth < 320.dp || largeText

            Column(
                modifier = Modifier.padding(WingmanSpacing.Large),
                verticalArrangement = Arrangement.spacedBy(WingmanSpacing.Medium),
            ) {
                if (stacked) {
                    WingmanLogo(
                        modifier = Modifier
                            .align(Alignment.End)
                            .size(104.dp),
                        contentDescription = "孔明帽軍師主視覺",
                        variant = WingmanLogoVariant.Hero,
                    )
                    HeroMessage()
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(WingmanSpacing.Small),
                    ) {
                        HeroMessage(modifier = Modifier.weight(1f))
                        WingmanLogo(
                            modifier = Modifier.size(120.dp),
                            contentDescription = "孔明帽軍師主視覺",
                            variant = WingmanLogoVariant.Hero,
                        )
                    }
                }
                HeroActions(
                    onTestBubble = onTestBubble,
                    onInstructions = onInstructions,
                )
            }
        }
    }
}

@Composable
private fun HeroMessage(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(WingmanSpacing.Small),
    ) {
        Text(
            text = "軍師已上線",
            color = WingmanColors.TextPrimary,
            style = MaterialTheme.typography.headlineMedium,
            maxLines = 2,
        )
        Text(
            text = "在 LINE 或任何聊天 App\n點一下浮動球就能救場",
            color = WingmanColors.TextSecondary,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun HeroActions(
    onTestBubble: () -> Unit,
    onInstructions: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(WingmanSpacing.Small),
        horizontalAlignment = Alignment.Start,
    ) {
        Button(
            onClick = onTestBubble,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
            shape = WingmanShapes.Button,
            colors = ButtonDefaults.buttonColors(
                containerColor = WingmanColors.Primary,
                contentColor = Color.White,
            ),
            contentPadding = PaddingValues(horizontal = WingmanSpacing.Large),
        ) {
            WingmanLogo(
                modifier = Modifier.size(28.dp),
                contentDescription = null,
                variant = WingmanLogoVariant.Compact,
            )
            Spacer(Modifier.width(WingmanSpacing.Small))
            Text("測試浮動球", style = MaterialTheme.typography.labelLarge)
        }
        TextButton(
            onClick = onInstructions,
            modifier = Modifier.heightIn(min = 48.dp),
            colors = ButtonDefaults.textButtonColors(contentColor = WingmanColors.PrimaryDeep),
            contentPadding = PaddingValues(horizontal = WingmanSpacing.Small),
        ) {
            Text("查看使用方法", style = MaterialTheme.typography.labelLarge)
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
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
        color = WingmanColors.Card,
        shape = WingmanShapes.Card,
        border = BorderStroke(1.dp, WingmanColors.Border),
        shadowElevation = 1.dp,
    ) {
        Column(modifier = Modifier.padding(WingmanSpacing.Large)) {
            Text(
                text = "開始前確認",
                color = WingmanColors.TextPrimary,
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.height(WingmanSpacing.Small))
            PermissionRow(
                label = "顯示在其他 App 上層",
                granted = state.overlayGranted,
                grantedLabel = "已開啟",
                onClick = onOverlayPermission,
            )
            HorizontalDivider(color = WingmanColors.Border)
            PermissionRow(
                label = "螢幕擷取權限",
                granted = state.captureGranted,
                grantedLabel = "已授權",
                onClick = onCapturePermission,
            )
            Spacer(Modifier.height(WingmanSpacing.Medium))
            AccessibilityNotice(
                granted = state.accessibilityGranted,
                onClick = onAccessibilityPermission,
            )
        }
    }
}

@Composable
private fun PermissionRow(
    label: String,
    granted: Boolean,
    grantedLabel: String,
    onClick: () -> Unit,
) {
    val status = if (granted) grantedLabel else "待設定"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { stateDescription = status }
            .padding(vertical = WingmanSpacing.Small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(WingmanSpacing.Small),
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = if (granted) WingmanColors.Success else WingmanColors.SoftSurface,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (granted) Icons.Default.Check else Icons.Outlined.Info,
                    contentDescription = null,
                    tint = if (granted) Color.White else WingmanColors.TextSecondary,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(WingmanSpacing.Small),
        ) {
            Text(
                text = label,
                color = WingmanColors.TextPrimary,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = status,
                color = if (granted) WingmanColors.Success else WingmanColors.PrimaryDeep,
                style = MaterialTheme.typography.labelMedium,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = WingmanColors.TextSecondary,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun AccessibilityNotice(
    granted: Boolean,
    onClick: () -> Unit,
) {
    val status = if (granted) "一鍵填入已開啟" else "選用，前往設定"
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 88.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { stateDescription = status },
        color = WingmanColors.BrandSurface,
        shape = WingmanShapes.Button,
        border = BorderStroke(1.dp, WingmanColors.BrandSurfaceStrong),
    ) {
        Row(
            modifier = Modifier.padding(WingmanSpacing.Medium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(WingmanSpacing.Small),
        ) {
            Icon(
                imageVector = if (granted) Icons.Default.CheckCircle else Icons.Outlined.Shield,
                contentDescription = null,
                tint = if (granted) WingmanColors.Success else WingmanColors.PrimaryDeep,
                modifier = Modifier.size(24.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(WingmanSpacing.Small),
            ) {
                Text(
                    text = if (granted) "一鍵填入已開啟" else "選用：一鍵填入",
                    color = WingmanColors.TextPrimary,
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    text = "只用來填入文字\n不會自動送出訊息\n未開啟時仍可複製",
                    color = WingmanColors.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = WingmanColors.TextSecondary,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun DailyLessonCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = WingmanColors.Card,
        shape = WingmanShapes.Card,
        border = BorderStroke(1.dp, WingmanColors.Border),
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(WingmanSpacing.Large),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(WingmanSpacing.Medium),
        ) {
            Surface(
                shape = CircleShape,
                color = WingmanColors.BrandSurface,
                modifier = Modifier.size(48.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.School,
                        contentDescription = null,
                        tint = WingmanColors.PrimaryDeep,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(WingmanSpacing.Small),
            ) {
                Text(
                    text = "今日軍師課",
                    color = WingmanColors.PrimaryDeep,
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    text = "少問選擇題，多給一個好接的畫面",
                    color = WingmanColors.TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
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
        icon = {
            WingmanLogo(
                modifier = Modifier.size(64.dp),
                contentDescription = "孔明帽軍師標誌",
                variant = WingmanLogoVariant.Hero,
            )
        },
        title = { Text("三步叫出軍師", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(WingmanSpacing.Small),
            ) {
                Text("1. 開啟顯示在其他 App 上層", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "2. 授權螢幕擷取後，到聊天畫面點孔明帽浮動球",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    "3. 選語氣，填入輸入框或複製；訊息仍由你親自送出",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(Modifier.height(WingmanSpacing.Small))
                Text(
                    text = "軍師只分析你當次主動擷取的畫面，不會在背景監看聊天。",
                    color = WingmanColors.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 48.dp),
                shape = WingmanShapes.Button,
            ) {
                Text("知道了")
            }
        },
        dismissButton = if (!accessibilityGranted) {
            {
                TextButton(
                    onClick = onEnableAccessibility,
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text("開啟一鍵填入")
                }
            }
        } else null,
        containerColor = WingmanColors.Card,
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
                accessibilityGranted = false,
            ),
            onTestBubble = {},
            onOverlayPermission = {},
            onCapturePermission = {},
            onAccessibilityPermission = {},
            onUnavailableTab = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 320, heightDp = 640, fontScale = 1.5f)
@Composable
private fun WingmanHomeLargeTextPreview() {
    WingmanTheme {
        WingmanHomeScreen(
            permissionState = PermissionUiState(
                overlayGranted = false,
                captureGranted = false,
                accessibilityGranted = false,
            ),
            onTestBubble = {},
            onOverlayPermission = {},
            onCapturePermission = {},
            onAccessibilityPermission = {},
            onUnavailableTab = {},
        )
    }
}
