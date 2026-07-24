package com.craigeley.dugout.api

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class ApiException(val code: Int, message: String) : IOException(message)

data class GameSummary(
    val gamePk: Long,
    val awayName: String,       // "Pirates"
    val homeName: String,
    val awayAbbrev: String,     // "PIT"
    val homeAbbrev: String,
    val awayScore: Int?,        // null before first pitch
    val homeScore: Int?,
    val abstractState: String,  // "Preview" | "Live" | "Final"
    val detailedState: String,  // "Scheduled", "In Progress", "Postponed", …
    val startUtc: String,       // ISO instant
    val inningState: String?,   // "Top" | "Bottom" | "Middle" | "End"
    val inningOrdinal: String?, // "2nd"
    val currentInning: Int?,
    val doubleHeader: String,   // "N", "Y", "S"
    val gameNumber: Int,
    val description: String?,   // e.g. "Makeup of 5/9 PPD"
)

data class InningLine(val num: Int, val away: Int?, val home: Int?)

data class LineScore(
    val innings: List<InningLine>,
    val awayRuns: Int,
    val awayHits: Int,
    val awayErrors: Int,
    val homeRuns: Int,
    val homeHits: Int,
    val homeErrors: Int,
    val inningState: String?,
    val inningOrdinal: String?,
    val currentInning: Int?,
    val balls: Int,
    val strikes: Int,
    val outs: Int,
    val batter: String?,
    val pitcher: String?,
    val runners: List<String>,  // occupied bases, e.g. ["1st", "3rd"]
)

data class BatterLine(
    val name: String,
    val position: String,
    val substitute: Boolean,
    val ab: Int,
    val r: Int,
    val h: Int,
    val rbi: Int,
    val bb: Int,
    val so: Int,
    val avg: String,
)

data class PitcherLine(
    val name: String,
    val note: String?,  // decision, e.g. "(W, 8-3)"
    val ip: String,
    val h: Int,
    val r: Int,
    val er: Int,
    val bb: Int,
    val so: Int,
    val era: String,
)

data class TeamBox(
    val batters: List<BatterLine>,
    val pitchers: List<PitcherLine>,
    val notes: List<String>,  // batting footnotes, e.g. "a-Singled for Rocchio in the 8th."
)

data class BoxScore(val away: TeamBox, val home: TeamBox)

data class TeamStanding(
    val name: String,   // "Rays"
    val wins: Int,
    val losses: Int,
    val pct: String,
    val gamesBack: String,  // "-" for the leader
    val wildCardGamesBack: String,  // "+4.5" for teams holding a spot, "-" for the WC leader
    val lastTen: String?,   // "7-3"
    val streak: String?,    // "W3"
)

data class DivisionStandings(val division: String, val teams: List<TeamStanding>)

/**
 * Minimal client for MLB's free Stats API (statsapi.mlb.com) over
 * HttpURLConnection + org.json, in the style of pace's StravaApi.
 * No auth of any kind.
 */
object MlbApi {
    private const val API = "https://statsapi.mlb.com/api/v1"

    // Division ids are stable; a static map avoids an extra hydrate.
    // Insertion order is display order (East, Central, West).
    private val DIVISIONS = mapOf(
        201 to "AL East", 202 to "AL Central", 200 to "AL West",
        204 to "NL East", 205 to "NL Central", 203 to "NL West",
    )

    /** All games on a date (YYYY-MM-DD), with team names and live inning state. */
    suspend fun scoreboard(date: String): List<GameSummary> =
        withContext(Dispatchers.IO) {
            val o = JSONObject(
                http("$API/schedule?sportId=1&date=$date&hydrate=team,linescore"),
            )
            val dates = o.optJSONArray("dates") ?: JSONArray()
            if (dates.length() == 0) return@withContext emptyList()
            val games = dates.getJSONObject(0).getJSONArray("games")
            buildList {
                for (i in 0 until games.length()) {
                    add(parseGame(games.getJSONObject(i)))
                }
            }
        }

    suspend fun lineScore(gamePk: Long): LineScore =
        withContext(Dispatchers.IO) {
            val o = JSONObject(http("$API/game/$gamePk/linescore"))
            val innings = o.optJSONArray("innings") ?: JSONArray()
            val teams = o.optJSONObject("teams") ?: JSONObject()
            val away = teams.optJSONObject("away") ?: JSONObject()
            val home = teams.optJSONObject("home") ?: JSONObject()
            val offense = o.optJSONObject("offense") ?: JSONObject()
            LineScore(
                innings = buildList {
                    for (i in 0 until innings.length()) {
                        val inn = innings.getJSONObject(i)
                        add(
                            InningLine(
                                num = inn.getInt("num"),
                                away = inn.optJSONObject("away")?.optIntOrNull("runs"),
                                home = inn.optJSONObject("home")?.optIntOrNull("runs"),
                            ),
                        )
                    }
                },
                awayRuns = away.optInt("runs"),
                awayHits = away.optInt("hits"),
                awayErrors = away.optInt("errors"),
                homeRuns = home.optInt("runs"),
                homeHits = home.optInt("hits"),
                homeErrors = home.optInt("errors"),
                inningState = o.optString("inningState").takeIf { it.isNotEmpty() },
                inningOrdinal = o.optString("currentInningOrdinal").takeIf { it.isNotEmpty() },
                currentInning = o.optIntOrNull("currentInning"),
                balls = o.optInt("balls"),
                strikes = o.optInt("strikes"),
                outs = o.optInt("outs"),
                batter = offense.optJSONObject("batter")?.optString("fullName"),
                pitcher = o.optJSONObject("defense")?.optJSONObject("pitcher")?.optString("fullName"),
                runners = buildList {
                    if (offense.has("first")) add("1st")
                    if (offense.has("second")) add("2nd")
                    if (offense.has("third")) add("3rd")
                },
            )
        }

    suspend fun boxScore(gamePk: Long): BoxScore =
        withContext(Dispatchers.IO) {
            val teams = JSONObject(http("$API/game/$gamePk/boxscore")).getJSONObject("teams")
            BoxScore(
                away = parseTeamBox(teams.getJSONObject("away")),
                home = parseTeamBox(teams.getJSONObject("home")),
            )
        }

    /** Both leagues' division standings for a season. */
    suspend fun standings(season: Int): List<DivisionStandings> =
        withContext(Dispatchers.IO) {
            val records = JSONObject(
                http("$API/standings?leagueId=103,104&season=$season&standingsTypes=regularSeason"),
            ).getJSONArray("records")
            buildList {
                for (i in 0 until records.length()) {
                    val rec = records.getJSONObject(i)
                    val divisionId = rec.optJSONObject("division")?.optInt("id") ?: 0
                    val teamRecords = rec.getJSONArray("teamRecords")
                    add(
                        DivisionStandings(
                            division = DIVISIONS[divisionId] ?: "Division",
                            teams = buildList {
                                for (j in 0 until teamRecords.length()) {
                                    val t = teamRecords.getJSONObject(j)
                                    add(
                                        TeamStanding(
                                            name = t.getJSONObject("team").optString("name"),
                                            wins = t.optInt("wins"),
                                            losses = t.optInt("losses"),
                                            pct = t.optString("winningPercentage"),
                                            gamesBack = t.optString("gamesBack", "-"),
                                            wildCardGamesBack = t.optString("wildCardGamesBack", "-"),
                                            lastTen = t.optJSONObject("records")
                                                ?.optJSONArray("splitRecords")?.let { splits ->
                                                    (0 until splits.length())
                                                        .map { splits.getJSONObject(it) }
                                                        .firstOrNull { it.optString("type") == "lastTen" }
                                                        ?.let { "${it.optInt("wins")}-${it.optInt("losses")}" }
                                                },
                                            streak = t.optJSONObject("streak")
                                                ?.optString("streakCode")?.takeIf { it.isNotEmpty() },
                                        ),
                                    )
                                }
                            },
                        ),
                    )
                }
            }.sortedBy { DIVISIONS.values.indexOf(it.division) }
        }

    private fun parseGame(g: JSONObject): GameSummary {
        val status = g.getJSONObject("status")
        val teams = g.getJSONObject("teams")
        val away = teams.getJSONObject("away")
        val home = teams.getJSONObject("home")
        val linescore = g.optJSONObject("linescore")
        return GameSummary(
            gamePk = g.getLong("gamePk"),
            awayName = away.getJSONObject("team").optString("teamName"),
            homeName = home.getJSONObject("team").optString("teamName"),
            awayAbbrev = away.getJSONObject("team").optString("abbreviation"),
            homeAbbrev = home.getJSONObject("team").optString("abbreviation"),
            awayScore = away.optIntOrNull("score"),
            homeScore = home.optIntOrNull("score"),
            abstractState = status.optString("abstractGameState"),
            detailedState = status.optString("detailedState"),
            startUtc = g.optString("gameDate"),
            inningState = linescore?.optString("inningState")?.takeIf { it.isNotEmpty() },
            inningOrdinal = linescore?.optString("currentInningOrdinal")?.takeIf { it.isNotEmpty() },
            currentInning = linescore?.optIntOrNull("currentInning"),
            doubleHeader = g.optString("doubleHeader", "N"),
            gameNumber = g.optInt("gameNumber", 1),
            description = g.optString("description").takeIf { it.isNotEmpty() },
        )
    }

    private fun parseTeamBox(team: JSONObject): TeamBox {
        val players = team.getJSONObject("players")
        val batterIds = team.optJSONArray("batters") ?: JSONArray()
        val pitcherIds = team.optJSONArray("pitchers") ?: JSONArray()

        val batters = buildList {
            for (i in 0 until batterIds.length()) {
                val p = players.optJSONObject("ID${batterIds.getLong(i)}") ?: continue
                val bat = p.optJSONObject("stats")?.optJSONObject("batting") ?: continue
                // Pinch runners / defensive subs get a row too; only skip truly empty lines.
                if (bat.length() == 0) continue
                val order = p.optString("battingOrder")
                add(
                    BatterLine(
                        name = p.getJSONObject("person").optString("fullName"),
                        position = p.optJSONObject("position")?.optString("abbreviation").orEmpty(),
                        substitute = order.isNotEmpty() && !order.endsWith("00"),
                        ab = bat.optInt("atBats"),
                        r = bat.optInt("runs"),
                        h = bat.optInt("hits"),
                        rbi = bat.optInt("rbi"),
                        bb = bat.optInt("baseOnBalls"),
                        so = bat.optInt("strikeOuts"),
                        avg = p.optJSONObject("seasonStats")?.optJSONObject("batting")
                            ?.optString("avg").orEmpty(),
                    ),
                )
            }
        }

        val pitchers = buildList {
            for (i in 0 until pitcherIds.length()) {
                val p = players.optJSONObject("ID${pitcherIds.getLong(i)}") ?: continue
                val pit = p.optJSONObject("stats")?.optJSONObject("pitching") ?: continue
                if (pit.length() == 0) continue
                add(
                    PitcherLine(
                        name = p.getJSONObject("person").optString("fullName"),
                        note = pit.optString("note").takeIf { it.isNotEmpty() },
                        ip = pit.optString("inningsPitched"),
                        h = pit.optInt("hits"),
                        r = pit.optInt("runs"),
                        er = pit.optInt("earnedRuns"),
                        bb = pit.optInt("baseOnBalls"),
                        so = pit.optInt("strikeOuts"),
                        era = p.optJSONObject("seasonStats")?.optJSONObject("pitching")
                            ?.optString("era").orEmpty(),
                    ),
                )
            }
        }

        val notes = buildList {
            val arr = team.optJSONArray("note") ?: JSONArray()
            for (i in 0 until arr.length()) {
                val n = arr.getJSONObject(i)
                val label = n.optString("label")
                val value = n.optString("value")
                if (value.isNotEmpty()) add(if (label.isNotEmpty()) "$label-$value" else value)
            }
        }

        return TeamBox(batters = batters, pitchers = pitchers, notes = notes)
    }

    private fun JSONObject.optIntOrNull(key: String): Int? =
        if (has(key)) optInt(key) else null

    private fun http(url: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "GET"
            conn.connectTimeout = 15000
            conn.readTimeout = 15000
            conn.setRequestProperty("Accept", "application/json")
            val code = conn.responseCode
            val text = (if (code in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) {
                val detail = runCatching { JSONObject(text).optString("message") }.getOrNull()
                    ?.takeIf { it.isNotEmpty() }
                throw ApiException(code, detail ?: "request failed ($code)")
            }
            if (text.isEmpty()) throw IOException("empty response")
            return text
        } finally {
            conn.disconnect()
        }
    }
}
