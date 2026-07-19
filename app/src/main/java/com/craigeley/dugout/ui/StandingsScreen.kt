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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.craigeley.dugout.DugoutViewModel
import com.craigeley.dugout.api.TeamStanding
import com.craigeley.dugout.ui.theme.DugoutColors
import com.craigeley.dugout.ui.theme.DugoutDimens
import com.craigeley.dugout.ui.theme.DugoutType

@Composable
fun StandingsScreen(viewModel: DugoutViewModel, listState: LazyListState) {
    val state by viewModel.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = DugoutDimens.screenPadding)) {
            Spacer(Modifier.height(24.dp))
            HapticText(
                text = "Standings",
                style = DugoutType.title,
                color = DugoutColors.onSurface,
                onClick = { viewModel.refreshStandings() },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
        }

        when {
            state.standingsLoading && state.standings.isEmpty() -> PaddedHint("Loading…")
            state.standings.isEmpty() -> PaddedHint(state.message ?: "No standings")
            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = DugoutDimens.screenPadding),
                ) {
                    state.standings.forEach { division ->
                        item(key = division.division) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp),
                            ) {
                                Text(
                                    text = division.division,
                                    style = DugoutType.metaMedium,
                                    color = DugoutColors.onSurface,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    text = "W-L",
                                    style = DugoutType.meta,
                                    color = DugoutColors.onSurfaceDim,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.width(70.dp),
                                )
                                Text(
                                    text = "GB",
                                    style = DugoutType.meta,
                                    color = DugoutColors.onSurfaceDim,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.width(52.dp),
                                )
                            }
                        }
                        items(division.teams, key = { division.division + it.name }) { team ->
                            StandingRow(team)
                        }
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }
            }
        }
    }
}

@Composable
private fun StandingRow(team: TeamStanding) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(
            text = team.name,
            style = DugoutType.meta,
            color = DugoutColors.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "${team.wins}-${team.losses}",
            style = DugoutType.meta,
            color = DugoutColors.onSurface,
            textAlign = TextAlign.End,
            maxLines = 1,
            modifier = Modifier.width(70.dp),
        )
        Text(
            text = if (team.gamesBack == "-") "—" else team.gamesBack,
            style = DugoutType.meta,
            color = DugoutColors.onSurfaceDim,
            textAlign = TextAlign.End,
            maxLines = 1,
            modifier = Modifier.width(52.dp),
        )
    }
}
