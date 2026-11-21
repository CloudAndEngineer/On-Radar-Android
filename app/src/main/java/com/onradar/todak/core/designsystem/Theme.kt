package com.onradar.todak.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 큰 글씨 원칙: 본문 26sp 이상, 버튼 높이 72dp 이상. */
object TodakDimens {
    val ButtonMinHeight = 72.dp
    val LargeButtonHeight = 96.dp
    val ScreenPadding = 24.dp
    val HelpBarHeight = 80.dp
}

private val TodakTypography = Typography(
    displayLarge = TextStyle(fontSize = 40.sp, lineHeight = 52.sp, fontWeight = FontWeight.Bold),
    headlineLarge = TextStyle(fontSize = 34.sp, lineHeight = 44.sp, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(fontSize = 30.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold),
    bodyLarge = TextStyle(fontSize = 26.sp, lineHeight = 36.sp),
    bodyMedium = TextStyle(fontSize = 26.sp, lineHeight = 36.sp),
    labelLarge = TextStyle(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold),
)

private val TodakColorScheme = lightColorScheme(
    primary = TodakColors.Primary,
    onPrimary = TodakColors.OnPrimary,
    background = TodakColors.Background,
    onBackground = TodakColors.OnSurface,
    surface = TodakColors.Surface,
    onSurface = TodakColors.OnSurface,
    error = TodakColors.Emergency,
    onError = TodakColors.OnEmergency,
    outline = TodakColors.Outline,
)

@Composable
fun TodakTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TodakColorScheme,
        typography = TodakTypography,
        content = content,
    )
}
