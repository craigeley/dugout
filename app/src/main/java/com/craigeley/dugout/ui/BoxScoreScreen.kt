package com.craigeley.dugout.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.craigeley.dugout.DugoutViewModel
import com.craigeley.dugout.api.BatterLine
import com.craigeley.dugout.api.LineScore
import com.craigeley.dugout.api.PitcherLine
import com.craigeley.dugout.api.TeamBox
import com.craigeley.dugout.ui.theme.DugoutColors
import com.craigeley.dugout.ui.theme.DugoutDimens
import com.craigeley.dugout.ui.theme.DugoutType
import kotlinx.coroutines.delay

@Composable
fun BoxScoreScreen(viewModel: DugoutViewModel) {
    val state by viewModel.state.collectAsState()
    val game = state.selected ?: return
    val line = state.lineScore
    val box = state.boxScore

    // Poll while the game is live so the box score follows along. RESUMED-gated
    // so the poll suspends when the app is backgrounded (LP3-21).
    val live = game.abstractState == "Live"
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(game.gamePk, live) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (live) {
                delay(45_000)
                viewModel.refreshDetail()
            }
        }
    }

    val awayRuns = line?.awayRuns ?: game.awayScore
    val homeRuns = line?.homeRuns ?: game.homeScore
    val status =
        if (live && line != null) {
            listOf(Format.inning(line.inningState, line.inningOrdinal), "${line.outs} out")
                .filter { it.isNotEmpty() }.joinToString(" · ")
        } else {
            Format.status(game)
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = DugoutDimens.screenPadding),
    ) {
        Spacer(Modifier.height(24.dp))
        HapticText(
            text = "‹ Back",
            style = DugoutType.meta,
            color = DugoutColors.onSurfaceVariant,
            onClick = { viewModel.closeDetail() },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "${game.awayAbbrev} $awayRuns, ${game.homeAbbrev} $homeRuns",
            style = DugoutType.title,
            color = DugoutColors.onSurface,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = listOf(status, Format.dateLabel(state.date))
                .filter { it.isNotEmpty() }.joinToString(" · "),
            style = DugoutType.meta,
            color = DugoutColors.onSurfaceDim,
            modifier = Modifier.fillMaxWidth(),
        )

        line?.let {
            Spacer(Modifier.height(16.dp))
            LineScoreGrid(game.awayAbbrev, game.homeAbbrev, it, final = game.abstractState == "Final")
            if (live) {
                Spacer(Modifier.height(12.dp))
                Situation(it)
            }
        }

        box?.let {
            TeamSection(game.awayName, it.away)
            TeamSection(game.homeName, it.home)
        }

        if (state.detailLoading && box == null) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Loading box score…",
                style = DugoutType.meta,
                color = DugoutColors.onSurfaceDisabled,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        state.message?.let {
            Spacer(Modifier.height(16.dp))
            Text(
                text = it,
                style = DugoutType.hint,
                color = DugoutColors.onSurfaceDim,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

/** Classic per-inning grid with R/H/E totals; scrolls sideways in extra innings. */
@Composable
private fun LineScoreGrid(awayAbbrev: String, homeAbbrev: String, line: LineScore, final: Boolean) {
    val innings = (1..maxOf(9, line.innings.size)).map { num ->
        line.innings.find { it.num == num }
    }
    val lastNum = line.innings.lastOrNull()?.num

    Column(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
        Row {
            GridCell("", label = true)
            innings.forEachIndexed { i, _ -> GridCell("${i + 1}", dim = true) }
            GridGap()
            listOf("R", "H", "E").forEach { GridCell(it, dim = true, wide = true) }
        }
        Row {
            GridCell(awayAbbrev, label = true)
            innings.forEach { GridCell(it?.away?.toString() ?: "") }
            GridGap()
            GridCell("${line.awayRuns}", wide = true)
            GridCell("${line.awayHits}", wide = true)
            GridCell("${line.awayErrors}", wide = true)
        }
        Row {
            GridCell(homeAbbrev, label = true)
            innings.forEach { inn ->
                // The home half of the last inning goes unplayed when the home team wins:
                // score it "X", newspaper style.
                val text = inn?.home?.toString()
                    ?: if (final && inn != null && inn.num == lastNum) "X" else ""
                GridCell(text)
            }
            GridGap()
            GridCell("${line.homeRuns}", wide = true)
            GridCell("${line.homeHits}", wide = true)
            GridCell("${line.homeErrors}", wide = true)
        }
    }
}

@Composable
private fun GridCell(text: String, label: Boolean = false, dim: Boolean = false, wide: Boolean = false) {
    Text(
        text = text,
        style = DugoutType.meta,
        color = if (dim) DugoutColors.onSurfaceDim else DugoutColors.onSurface,
        textAlign = if (label) TextAlign.Start else TextAlign.Center,
        maxLines = 1,
        modifier = Modifier.width(if (label) 46.dp else if (wide) 28.dp else 24.dp),
    )
}

@Composable
private fun GridGap() = Spacer(Modifier.width(8.dp))

/** Live-game situation: pitcher, batter, count, and runners. */
@Composable
private fun Situation(line: LineScore) {
    val runners = when (line.runners.size) {
        0 -> "bases empty"
        3 -> "bases loaded"
        1 -> "runner on ${line.runners[0]}"
        else -> "runners on ${line.runners.joinToString(", ")}"
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        line.pitcher?.let {
            Text(
                text = "P: $it",
                style = DugoutType.meta,
                color = DugoutColors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        line.batter?.let {
            Text(
                text = "AB: $it",
                style = DugoutType.meta,
                color = DugoutColors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = "${line.balls}-${line.strikes}, $runners",
            style = DugoutType.meta,
            color = DugoutColors.onSurfaceDim,
        )
    }
}

@Composable
private fun TeamSection(teamName: String, team: TeamBox) {
    Spacer(Modifier.height(20.dp))
    Text(
        text = teamName,
        style = DugoutType.body,
        color = DugoutColors.onSurface,
        modifier = Modifier.fillMaxWidth(),
    )
    if (team.batters.isNotEmpty()) {
        Spacer(Modifier.height(8.dp))
        BattingHeader()
        team.batters.forEach { BatterRow(it) }
        team.notes.forEach { note ->
            Spacer(Modifier.height(2.dp))
            Text(
                text = note,
                style = DugoutType.hint,
                color = DugoutColors.onSurfaceDim,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
    if (team.pitchers.isNotEmpty()) {
        Spacer(Modifier.height(12.dp))
        PitchingHeader()
        team.pitchers.forEach { PitcherRow(it) }
    }
}

@Composable
private fun BattingHeader() {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Batting",
            style = DugoutType.meta,
            color = DugoutColors.onSurfaceDim,
            modifier = Modifier.weight(1f),
        )
        listOf("AB", "R", "H", "BI").forEach { StatCell(it, dim = true) }
        StatCell("AVG", dim = true, wide = true)
    }
}

@Composable
private fun BatterRow(b: BatterLine) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(
            text = buildAnnotatedString {
                if (b.substitute) append("  ")
                append(b.name)
                if (b.position.isNotEmpty()) {
                    withStyle(SpanStyle(color = DugoutColors.onSurfaceDim)) {
                        append(" ${b.position.uppercase()}")
                    }
                }
            },
            style = DugoutType.meta,
            color = DugoutColors.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        StatCell("${b.ab}")
        StatCell("${b.r}")
        StatCell("${b.h}")
        StatCell("${b.rbi}")
        StatCell(b.avg, dim = true, wide = true)
    }
}

@Composable
private fun PitchingHeader() {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Pitching",
            style = DugoutType.meta,
            color = DugoutColors.onSurfaceDim,
            modifier = Modifier.weight(1f),
        )
        StatCell("IP", dim = true, wide = true)
        listOf("H", "R", "ER", "BB", "SO").forEach { StatCell(it, dim = true) }
    }
}

@Composable
private fun PitcherRow(p: PitcherLine) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(
            text = listOfNotNull(p.name, p.note).joinToString(" "),
            style = DugoutType.meta,
            color = DugoutColors.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        StatCell(p.ip, wide = true)
        StatCell("${p.h}")
        StatCell("${p.r}")
        StatCell("${p.er}")
        StatCell("${p.bb}")
        StatCell("${p.so}")
    }
}

@Composable
private fun StatCell(text: String, dim: Boolean = false, wide: Boolean = false) {
    Text(
        text = text,
        style = DugoutType.meta,
        color = if (dim) DugoutColors.onSurfaceDim else DugoutColors.onSurface,
        textAlign = TextAlign.End,
        maxLines = 1,
        modifier = Modifier.width(if (wide) 48.dp else 28.dp),
    )
}
