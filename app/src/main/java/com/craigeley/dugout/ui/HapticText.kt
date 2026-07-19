package com.craigeley.dugout.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import com.craigeley.dugout.ui.theme.DugoutColors
import com.craigeley.dugout.ui.theme.DugoutType

/** Tappable text — the vandamd "button". Haptic on press, optional underline-as-selection. */
@Composable
fun HapticText(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    underline: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    textAlign: TextAlign = TextAlign.Start,
    onClick: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val interaction = remember { MutableInteractionSource() }
    Text(
        text = text,
        style = style,
        color = color,
        textAlign = textAlign,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        textDecoration = if (underline) TextDecoration.Underline else TextDecoration.None,
        modifier = modifier.clickable(
            interactionSource = interaction,
            indication = null,
        ) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
    )
}

@Composable
fun Hint(text: String) {
    Text(
        text = text,
        style = DugoutType.body,
        color = DugoutColors.onSurfaceDisabled,
        modifier = Modifier.fillMaxWidth(),
    )
}
