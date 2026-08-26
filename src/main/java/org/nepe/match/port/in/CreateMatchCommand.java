package org.nepe.match.port.in;

import java.time.Instant;

/**
 * Inbound Command DTO for manually registering a new match in the system.
 *
 * @param competitionId database identifier of the competition
 * @param seasonId      database identifier of the season
 * @param homeTeamId    database identifier of the Home team
 * @param awayTeamId    database identifier of the Away team
 * @param matchDateTime kickoff timestamp in UTC
 * @param oddsHome      reference pre-match Home win odds (nullable)
 * @param oddsDraw      reference pre-match Draw odds (nullable)
 * @param oddsAway      reference pre-match Away win odds (nullable)
 */
public record CreateMatchCommand(
        int competitionId,
        int seasonId,
        int homeTeamId,
        int awayTeamId,
        Instant matchDateTime,
        Double oddsHome,
        Double oddsDraw,
        Double oddsAway
) {
    public CreateMatchCommand(int competitionId, int seasonId, int homeTeamId, int awayTeamId, Instant matchDateTime) {
        this(competitionId, seasonId, homeTeamId, awayTeamId, matchDateTime, null, null, null);
    }
}
