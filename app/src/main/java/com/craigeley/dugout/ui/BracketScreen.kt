package com.craigeley.dugout.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.craigeley.dugout.DugoutViewModel
import com.craigeley.dugout.api.PostseasonSeries
import com.craigeley.dugout.api.SeriesTeam
import com.craigeley.dugout.ui.theme.DugoutColors
import com.craigeley.dugout.ui.theme.DugoutDimens
import com.craigeley.dugout.ui.theme.DugoutType

/**
 * The postseason's stand-in for standings (LP3-76): every series by round,
 * latest round first, each team with its series wins. Once a series is
 * over, the team knocked out is dimmed.
 */
@Composable
fun BracketScreen(viewModel: DugoutViewModel, listState: LazyListState) {
    val state by viewModel.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = DugoutDimens.screenPadding)) {
            Spacer(Modifier.height(24.dp))
            HapticText(
                text = "Postseason",
                style = DugoutType.title,
                color = DugoutColors.onSurface,
                onClick = { viewModel.refreshSecondary() },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
        }

        when {
            state.bracketLoading && state.bracket.isEmpty() -> PaddedHint("Loading…")
            state.bracket.isEmpty() -> PaddedHint(state.message ?: "No series yet")
            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = DugoutDimens.screenPadding),
                ) {
                    state.bracket.groupBy { it.round }.forEach { (round, series) ->
                        item(key = round) {
                            Text(
                                text = round,
                                style = DugoutType.metaMedium,
                                color = DugoutColors.onSurface,
                                modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp),
                            )
                        }
                        series.forEachIndexed { i, s ->
                            item(key = "$round-$i") { SeriesRows(s, last = i == series.lastIndex) }
                        }
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }
            }
        }
    }
}

@Composable
private fun SeriesRows(series: PostseasonSeries, last: Boolean) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = if (last) 0.dp else 10.dp)) {
        SeriesTeamRow(series.top, eliminated = series.over && series.top.wins < series.bottom.wins)
        SeriesTeamRow(series.bottom, eliminated = series.over && series.bottom.wins < series.top.wins)
    }
}

@Composable
private fun SeriesTeamRow(team: SeriesTeam, eliminated: Boolean) {
    val nameColor: Color = if (eliminated) DugoutColors.onSurfaceDisabled else DugoutColors.onSurfaceVariant
    val winsColor: Color = if (eliminated) DugoutColors.onSurfaceDisabled else DugoutColors.onSurface
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(
            text = team.name,
            style = DugoutType.meta,
            color = nameColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = team.wins.toString(),
            style = DugoutType.meta,
            color = winsColor,
            textAlign = TextAlign.End,
            maxLines = 1,
            modifier = Modifier.width(48.dp),
        )
    }
}
