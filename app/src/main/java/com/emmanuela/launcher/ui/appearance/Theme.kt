package com.emmanuela.launcher.ui.appearance

import com.emmanuela.launcher.data.Preferences
import com.emmanuela.launcher.ui.components.LocalLauncherLanguage


import android.graphics.Typeface
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun EmmanuelaTheme(settings: Preferences, bars: (Boolean, Boolean) -> Unit, content: @Composable () -> Unit) {
    val dark = when (settings.theme) { "Dark" -> true; "Light" -> false; "Custom" -> androidx.compose.ui.graphics.Color(settings.ui.v2.customBackground).let { (it.red+it.green+it.blue)<1.5f }; else -> isSystemInDarkTheme() }
    val context = LocalContext.current
    val family=launcherFont(settings.font,settings.customFont,italic=settings.ui.italic,weight=weightOf(if(settings.bold)"Bold" else settings.ui.experience.textWeight))
    val typography=remember(family,settings.bold,settings.textScale,settings.ui.italic,settings.ui.experience.textWeight){
        styledTypography(Typography(),family,weightOf(if(settings.bold)"Bold" else settings.ui.experience.textWeight),settings.ui.italic,settings.textScale)
    }
    SideEffect { bars(settings.notificationBar, dark) }
    val colors = if (dark) darkColorScheme(primary = Color.White, background = Color.Black, surface = Color.Black,
        onBackground = Color.White, onSurface = Color.White, surfaceContainerHigh = Color(0xFF191919), outline = Color(0xFF5F5F5F))
    else lightColorScheme(primary = Color.Black, background = Color(0xFFFAFAFA), surface = Color(0xFFFAFAFA),
        onBackground = Color.Black, onSurface = Color.Black, surfaceContainerHigh = Color(0xFFEEEEEE), outline = Color(0xFF999999))
    val accent = if(settings.ui.experience.accentColor!=0L)Color(settings.ui.experience.accentColor)else when (settings.ui.palette) { "Sage" -> if (dark) Color(0xFFAACCB4) else Color(0xFF31533C); "Ocean" -> if (dark) Color(0xFFACCCE8) else Color(0xFF244866); "Rose" -> if (dark) Color(0xFFE6B8C4) else Color(0xFF663541); else -> colors.primary }
    val themedBase = if(settings.theme=="Custom") colors.copy(background=Color(settings.ui.v2.customBackground),surface=Color(settings.ui.v2.customBackground),onBackground=Color(settings.ui.v2.customForeground),onSurface=Color(settings.ui.v2.customForeground)) else colors
    val opacity=settings.ui.experience.textOpacity
    val foreground=if(settings.ui.experience.textColor!=0L)Color(settings.ui.experience.textColor)else themedBase.onSurface
    val themed=themedBase.copy(onBackground=foreground.copy(alpha=opacity),onSurface=foreground.copy(alpha=opacity))
    MaterialTheme(colorScheme = themed.copy(primary = accent), typography = typography) {
        // Text without an explicit Material typography style must still honor the global font family/style.
        CompositionLocalProvider(LocalLauncherLanguage provides settings.ui.experience.language) { ProvideTextStyle(typography.bodyLarge) { content() } }
    }
}
