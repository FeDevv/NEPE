package org.nepe.match.port.in;

import org.nepe.match.domain.MarketType;

/**
 * Inbound Command DTO for creating or updating Betting Exchange market odds (Back and Lay) for a specific outcome.
 *
 * @param matchId    database identifier of the match
 * @param marketType market category (e.g. MATCH_ODDS, UNDER_OVER_25, BTTS)
 * @param outcome    outcome label (e.g. "1", "X", "2", "OVER", "UNDER", "YES", "NO")
 * @param backOdds   Back (Punta) decimal odds (nullable)
 * @param layOdds    Lay (Banca) decimal odds (nullable)
 */
public record SaveMarketOddsCommand(
        int matchId,
        MarketType marketType,
        String outcome,
        Double backOdds,
        Double layOdds
) {
}
