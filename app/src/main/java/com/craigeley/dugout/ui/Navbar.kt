package com.craigeley.dugout.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.SportsBaseball
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.craigeley.dugout.Tab
import com.craigeley.dugout.ui.theme.DugoutColors

// Sizes follow Amp's bottom bar (sublunarian/light-amp, LP3-43): the LP3 is
// density 3.0, so Amp's panel-pixel constants divide by three.
private val BarHeight = 53.dp          // 160px
private val TileSize = 48.dp           // 144px, the tap target around each glyph
private val GlyphBox = 38.dp           // 114px, before per-glyph equalisation
private val InactiveGray = Color(0xFF6E6E6E)

/**
 * Per-glyph scale so every icon draws ~24dp of ink. Material glyphs fill their
 * 24dp viewport differently (a baseball is a full circle, a leaderboard is three
 * short bars), so one box size reads as two sizes. Measured on the panel; re-measure
 * if an icon changes.
 */
private fun Tab.icon(): ImageVector = when (this) {
    Tab.SCORES -> Icons.Filled.SportsBaseball
    Tab.STANDINGS -> Icons.Filled.Leaderboard
}

private fun Tab.glyphScale(): Float = when (this) {
    Tab.SCORES -> 0.76f
    Tab.STANDINGS -> 0.83f
}

private fun Tab.label(): String = when (this) {
    Tab.SCORES -> "Scores"
    Tab.STANDINGS -> "Standings"
}

/**
 * Icon bottom-nav in Amp's style: fixed-height bar, tiles spread evenly across
 * the full width (Amp's five-tab bar; with two tabs this lands them at thirds),
 * the active tab's glyph at full white, the rest gray. The whole 48dp tile is
 * the tap target, with a haptic; nothing is drawn behind the glyph.
 */
@Composable
fun Navbar(current: Tab, onSelect: (Tab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(BarHeight),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Tab.entries.forEach { tab ->
            NavTile(tab.icon(), tab.label(), tab.glyphScale(), tab == current) { onSelect(tab) }
        }
    }
}

@Composable
private fun NavTile(icon: ImageVector, description: String, glyphScale: Float, active: Boolean, onClick: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(TileSize)
            .clickable(interactionSource = interaction, indication = null) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = if (active) DugoutColors.onSurface else InactiveGray,
            modifier = Modifier.size(GlyphBox).scale(glyphScale),
        )
    }
}
