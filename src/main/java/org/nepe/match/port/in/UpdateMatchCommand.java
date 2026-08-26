package org.nepe.match.port.in;

import org.nepe.match.domain.MatchModifiers;

import java.time.Instant;

/**
 * Inbound Command DTO for updating match metadata, kickoff schedule, reference odds, and tactical modifiers.
 *
 * @param matchId       database identifier of the match to update
 * @param matchDateTime updated kickoff timestamp in UTC
 * @param oddsHome      updated reference Home win odds (nullable)
 * @param oddsDraw      updated reference Draw odds (nullable)
 * @param oddsAway      updated reference Away win odds (nullable)
 * @param modifiers     updated tactical modifiers (must-win, low-urgency, custom multiplier weights)
 */
public record UpdateMatchCommand(
        int matchId,
        Instant matchDateTime,
        Double oddsHome,
        Double oddsDraw,
        Double oddsAway,
        MatchModifiers modifiers
) {
}
