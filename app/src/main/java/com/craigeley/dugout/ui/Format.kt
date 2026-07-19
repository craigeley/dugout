package com.craigeley.dugout.ui

import com.craigeley.dugout.api.GameSummary
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Display formatting for games and dates. */
object Format {
    private val timeFmt = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
    private val dayFmt = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.US)
    private val dayYearFmt = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.US)

    /** "Today, Jul 18" / "Fri, Jul 11" / "Wed, Oct 1, 2025" */
    fun dateLabel(date: LocalDate): String {
        val today = LocalDate.now()
        return when {
            date == today -> "Today, " + date.format(DateTimeFormatter.ofPattern("MMM d", Locale.US))
            date.year == today.year -> date.format(dayFmt)
            else -> date.format(dayYearFmt)
        }
    }

    /** Local start time of a game from its UTC instant, e.g. "6:10 PM". */
    fun startTime(utc: String): String =
        runCatching {
            Instant.parse(utc).atZone(ZoneId.systemDefault()).format(timeFmt)
        }.getOrDefault("")

    /** "Top" + "2nd" -> "Top 2nd"; "Middle" -> "Mid 2nd"; "Bottom" -> "Bot 2nd". */
    fun inning(state: String?, ordinal: String?): String {
        if (state == null || ordinal == null) return ""
        val half = when (state) {
            "Middle" -> "Mid"
            "Bottom" -> "Bot"
            else -> state
        }
        return "$half $ordinal"
    }

    /** The matchup line: "Pirates 7, Guardians 1" once there's a score, else "Twins @ Cubs". */
    fun matchup(g: GameSummary): String =
        if (g.awayScore != null && g.homeScore != null && g.abstractState != "Preview") {
            "${g.awayName} ${g.awayScore}, ${g.homeName} ${g.homeScore}"
        } else {
            "${g.awayName} @ ${g.homeName}"
        }

    /** The status line under a game row: "Final/11", "Bot 2nd", "6:10 PM · Game 2", … */
    fun status(g: GameSummary): String {
        val base = when {
            g.abstractState == "Final" -> {
                val extra = g.currentInning?.takeIf { it != 9 && g.detailedState == "Final" }
                if (extra != null) "Final/$extra" else g.detailedState.ifEmpty { "Final" }
            }
            g.abstractState == "Live" ->
                inning(g.inningState, g.inningOrdinal).ifEmpty { g.detailedState }
            g.detailedState == "Scheduled" || g.detailedState == "Pre-Game" ||
                g.detailedState == "Warmup" -> startTime(g.startUtc)
            else -> g.detailedState  // "Postponed", "Suspended", …
        }
        val parts = mutableListOf(base)
        if (g.doubleHeader != "N") parts += "Game ${g.gameNumber}"
        g.description?.let { parts += it }
        return parts.filter { it.isNotEmpty() }.joinToString(" · ")
    }
}
