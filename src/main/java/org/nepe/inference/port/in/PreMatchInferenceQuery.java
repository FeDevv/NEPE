package org.nepe.inference.port.in;

import org.nepe.inference.domain.TeamStrengthCalculator.MatchPerformance;
import org.nepe.match.domain.MarketOdds;
import org.nepe.match.domain.MatchModifiers;

import java.util.Collections;
import java.util.List;

/**
 * Inbound Query DTO encapsulating all statistical and contextual inputs required
 * to compute pre-match inferences, probability distributions, and Betting Exchange Expected Values.
 *
 * @param homeHistoricalMatches historical match performances for the Home team (newest first)
 * @param awayHistoricalMatches historical match performances for the Away team (newest first)
 * @param leagueAvgXgPerTeam    average xG scored per team per match across the competition (e.g. 1.35)
 * @param homeAdvantageRatio    league-wide home advantage ratio (e.g. 1.20)
 * @param seasonalDecayGamma    discount factor for previous season matches (e.g. 0.70)
 * @param dixonColesRho         competition Dixon-Coles dependence coefficient (e.g. -0.12)
 * @param commissionRate        Betting Exchange commission rate (e.g. 0.05 for 5%)
 * @param modifiers             tactical context modifiers (injuries, neutral venue, motivation)
 * @param marketOddsList        current market Back/Lay odds entered by the user (nullable or empty)
 */
public record PreMatchInferenceQuery(
        List<MatchPerformance> homeHistoricalMatches,
        List<MatchPerformance> awayHistoricalMatches,
        double leagueAvgXgPerTeam,
        double homeAdvantageRatio,
        double seasonalDecayGamma,
        double dixonColesRho,
        double commissionRate,
        MatchModifiers modifiers,
        List<MarketOdds> marketOddsList
) {

    public PreMatchInferenceQuery {
        homeHistoricalMatches = (homeHistoricalMatches != null) ? List.copyOf(homeHistoricalMatches) : Collections.emptyList();
        awayHistoricalMatches = (awayHistoricalMatches != null) ? List.copyOf(awayHistoricalMatches) : Collections.emptyList();
        modifiers = (modifiers != null) ? modifiers : MatchModifiers.defaultModifiers();
        marketOddsList = (marketOddsList != null) ? List.copyOf(marketOddsList) : Collections.emptyList();
    }
}
