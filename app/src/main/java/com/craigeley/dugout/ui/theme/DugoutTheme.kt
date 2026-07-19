@file:OptIn(ExperimentalTextApi::class)

package com.craigeley.dugout.ui.theme

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.craigeley.dugout.R

object DugoutColors {
    val background = Color.Black
    val onSurface = Color.White
    val onSurfaceVariant = Color.White.copy(alpha = 0.7f)
    val onSurfaceDim = Color.White.copy(alpha = 0.5f)
    val onSurfaceDisabled = Color.White.copy(alpha = 0.3f)
}

val PublicSans = FontFamily(
    Font(
        R.font.publicsans_variablefont_wght,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(400)),
    ),
    Font(
        R.font.publicsans_variablefont_wght,
        weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(FontVariation.weight(500)),
    ),
)

object DugoutType {
    val title = TextStyle(fontFamily = PublicSans, fontSize = 34.sp, fontWeight = FontWeight.Medium)
    val button = TextStyle(fontFamily = PublicSans, fontSize = 24.sp, fontWeight = FontWeight.Normal)
    val body = TextStyle(fontFamily = PublicSans, fontSize = 24.sp, fontWeight = FontWeight.Normal)
    val meta = TextStyle(fontFamily = PublicSans, fontSize = 18.sp, fontWeight = FontWeight.Normal)
    val metaMedium = TextStyle(fontFamily = PublicSans, fontSize = 18.sp, fontWeight = FontWeight.Medium)
    val hint = TextStyle(fontFamily = PublicSans, fontSize = 16.sp, fontWeight = FontWeight.Normal)
}

object DugoutDimens {
    val screenPadding = 24.dp
}

@Composable
fun DugoutTheme(content: @Composable () -> Unit) {
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(density.density, fontScale = 0.85f),
    ) {
        MaterialTheme(
            colorScheme = darkColorScheme(
                background = DugoutColors.background,
                surface = DugoutColors.background,
                onBackground = DugoutColors.onSurface,
                onSurface = DugoutColors.onSurface,
                primary = DugoutColors.onSurface,
            ),
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = DugoutColors.background,
                content = content,
            )
        }
    }
}
