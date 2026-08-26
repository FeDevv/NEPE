package org.nepe.match.port.in;

/**
 * Inbound Command DTO for manually updating or correcting match statistics and xG overrides.
 *
 * @param matchId           database identifier of the match to update
 * @param homeScore         final or current goals scored by Home team (nullable)
 * @param awayScore         final or current goals scored by Away team (nullable)
 * @param homeShots         total shots by Home team (nullable)
 * @param awayShots         total shots by Away team (nullable)
 * @param homeShotsOnTarget shots on target by Home team (nullable)
 * @param awayShotsOnTarget shots on target by Away team (nullable)
 * @param manualHomeXg      manual expected goals override for Home team (nullable)
 * @param manualAwayXg      manual expected goals override for Away team (nullable)
 */
public record UpdateMatchStatisticsCommand(
        int matchId,
        Integer homeScore,
        Integer awayScore,
        Integer homeShots,
        Integer awayShots,
        Integer homeShotsOnTarget,
        Integer awayShotsOnTarget,
        Double manualHomeXg,
        Double manualAwayXg
) {
}
