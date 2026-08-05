package com.craigeley.dugout.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.craigeley.dugout.DugoutViewModel
import com.craigeley.dugout.ui.theme.DugoutColors
import com.craigeley.dugout.ui.theme.DugoutDimens
import com.craigeley.dugout.ui.theme.DugoutType
import java.time.LocalDate
import kotlinx.coroutines.delay

@Composable
fun ScoresScreen(viewModel: DugoutViewModel, listState: LazyListState) {
    val state by viewModel.state.collectAsState()

    // Keep live scores fresh while looking at today's slate. RESUMED-gated so
    // the poll suspends when the app is backgrounded (LP3-21).
    val anyLive = state.games.any { it.abstractState == "Live" }
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(state.date, anyLive) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (anyLive && state.date == LocalDate.now()) {
                delay(60_000)
                viewModel.refreshGames(silent = true)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = DugoutDimens.screenPadding)) {
            Spacer(Modifier.height(24.dp))
            // Tap the title to refresh, like tune's tab headers.
            HapticText(
                text = "Scores",
                style = DugoutType.title,
                color = DugoutColors.onSurface,
                onClick = { viewModel.refreshGames() },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                HapticText(
                    text = "‹",
                    style = DugoutType.body,
                    color = DugoutColors.onSurfaceVariant,
                    onClick = { viewModel.previousDay() },
                )
                // Tap the date to jump back to today.
                HapticText(
                    text = Format.dateLabel(state.date),
                    style = DugoutType.body,
                    color = DugoutColors.onSurface,
                    onClick = { viewModel.goToToday() },
                )
                HapticText(
                    text = "›",
                    style = DugoutType.body,
                    color = DugoutColors.onSurfaceVariant,
                    onClick = { viewModel.nextDay() },
                )
            }
            Spacer(Modifier.height(12.dp))
        }

        when {
            state.gamesLoading && state.games.isEmpty() -> PaddedHint("Loading…")
            state.games.isEmpty() && state.gamesLoaded -> PaddedHint("No games")
            state.games.isEmpty() -> PaddedHint(state.message ?: "")
            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = DugoutDimens.screenPadding),
                ) {
                    items(state.games, key = { it.gamePk }) { game ->
                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                            HapticText(
                                text = Format.matchup(game),
                                style = DugoutType.body,
                                color = DugoutColors.onSurface,
                                maxLines = 1,
                                onClick = { viewModel.select(game) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Text(
                                text = Format.status(game),
                                style = DugoutType.meta,
                                color = DugoutColors.onSurfaceDim,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }
            }
        }

        state.message?.let {
            if (state.games.isNotEmpty()) {
                Text(
                    text = it,
                    style = DugoutType.hint,
                    color = DugoutColors.onSurfaceDim,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = DugoutDimens.screenPadding, vertical = 12.dp),
                )
            }
        }
    }
}

@Composable
fun PaddedHint(text: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = DugoutDimens.screenPadding)) {
        Hint(text)
    }
}
