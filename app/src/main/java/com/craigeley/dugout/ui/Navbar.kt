package com.craigeley.dugout.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.SportsBaseball
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.craigeley.dugout.Tab
import com.craigeley.dugout.ui.theme.DugoutColors

// vandamd/echo's navbar colors: active white, inactive gray.
private val InactiveGray = Color(0xFF6E6E6E)

private fun Tab.icon(): ImageVector = when (this) {
    Tab.SCORES -> Icons.Filled.SportsBaseball
    Tab.STANDINGS -> Icons.Filled.Leaderboard
}

private fun Tab.label(): String = when (this) {
    Tab.SCORES -> "Scores"
    Tab.STANDINGS -> "Standings"
}

/** Icon bottom-nav, echo-style: active white, inactive gray, haptic on tap. */
@Composable
fun Navbar(current: Tab, onSelect: (Tab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Tab.entries.forEach { tab ->
            NavIcon(tab.icon(), tab.label(), tab == current) { onSelect(tab) }
        }
    }
}

@Composable
private fun NavIcon(icon: ImageVector, description: String, active: Boolean, onClick: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    val interaction = remember { MutableInteractionSource() }
    Icon(
        imageVector = icon,
        contentDescription = description,
        tint = if (active) DugoutColors.onSurface else InactiveGray,
        modifier = Modifier
            .size(44.dp)
            .clickable(interactionSource = interaction, indication = null) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
    )
}
