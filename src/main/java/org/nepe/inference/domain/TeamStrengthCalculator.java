package org.nepe.inference.domain;

import org.nepe.match.domain.MatchModifiers;
import org.nepe.shared.exception.DomainValidationException;

import java.util.List;

/**
 * Pure mathematical engine for computing team offensive (alpha) and defensive (beta) strengths,
 * applying recency weighting, inter-season gamma discounts, and generating pre-match expected goal rates (lambda and mu).
 */
public final class TeamStrengthCalculator {

    public static final double DEFAULT_LEAGUE_AVG_XG = 1.35;
    public static final double DEFAULT_HOME_ADVANTAGE = 1.20;
    public static final double DEFAULT_SEASONAL_GAMMA = 0.70;
    public static final double LOW_URGENCY_PRE_MATCH_FACTOR = 0.65;

    private TeamStrengthCalculator() {
        // Pure utility class - prevent instantiation
    }

    /**
     * Immutable record representing a single historical match's offensive and defensive metrics.
     */
    public record MatchPerformance(double xgScored, double xgConceded, boolean fromPreviousSeason) {
        public MatchPerformance {
            if (xgScored < 0.0 || xgConceded < 0.0) {
                throw new DomainValidationException("xG values in MatchPerformance cannot be negative.");
            }
        }
    }

    /**
     * Immutable record holding computed team relative strength coefficients.
     */
    public record TeamStrength(
            double alphaOffense,
            double betaDefense,
            double weightedAvgXgScored,
            double weightedAvgXgConceded
    ) {
        public TeamStrength {
            if (alphaOffense < 0.0 || betaDefense < 0.0) {
                throw new DomainValidationException("Team strength coefficients cannot be negative.");
            }
        }
    }

    /**
     * Immutable record holding pre-match calculated expected goal rates.
     */
    public record PreMatchRates(double lambdaHome, double muAway, double effectiveHomeAdvantage) {
        public PreMatchRates {
            if (lambdaHome < 0.0 || muAway < 0.0) {
                throw new DomainValidationException("Pre-match rates cannot be negative.");
            }
        }
    }

    /**
     * Computes the offensive (alpha) and defensive (beta) strength ratings for a team
     * given a list of historical match performances ordered from newest (index 0) to oldest.
     *
     * @param matches             list of historical match performances (newest first)
     * @param leagueAvgXgPerTeam  average xG scored by a team per match across the competition
     * @param seasonalDecayGamma  discount factor for previous season matches (e.g., 0.70)
     * @return {@link TeamStrength} containing alpha, beta, and weighted averages
     */
    public static TeamStrength calculateStrength(List<MatchPerformance> matches,
                                                 double leagueAvgXgPerTeam,
                                                 double seasonalDecayGamma) {
        validateParameters(leagueAvgXgPerTeam, seasonalDecayGamma);

        if (matches == null || matches.isEmpty()) {
            // Neutral baseline (average team)
            return new TeamStrength(1.00, 1.00, leagueAvgXgPerTeam, leagueAvgXgPerTeam);
        }

        int count = matches.size();
        double sumWeights = 0.0;
        double weightedScored = 0.0;
        double weightedConceded = 0.0;

        for (int i = 0; i < count; i++) {
            MatchPerformance match = matches.get(i);
            // Linear rank-based recency weight: newest match (i=0) has rank-weight = count, oldest (i=count-1) has 1
            double recencyWeight = (double) (count - i) / count;

            // Apply inter-season discount if match belongs to the previous season buffer
            double weight = match.fromPreviousSeason() ? recencyWeight * seasonalDecayGamma : recencyWeight;

            weightedScored += match.xgScored() * weight;
            weightedConceded += match.xgConceded() * weight;
            sumWeights += weight;
        }

        double avgScored = (sumWeights > 0.0) ? weightedScored / sumWeights : leagueAvgXgPerTeam;
        double avgConceded = (sumWeights > 0.0) ? weightedConceded / sumWeights : leagueAvgXgPerTeam;

        double alpha = avgScored / leagueAvgXgPerTeam;
        double beta = avgConceded / leagueAvgXgPerTeam;

        return new TeamStrength(alpha, beta, avgScored, avgConceded);
    }

    /**
     * Calculates pre-match expected goal rates (lambda and mu) for an upcoming match.
     *
     * @param homeStrength       computed strength ratings for the Home team
     * @param awayStrength       computed strength ratings for the Away team
     * @param leagueAvgXgPerTeam competition baseline average xG
     * @param homeAdvantageRatio ratio of home goals vs away goals across league (e.g. 1.20)
     * @param modifiers          tactical modifiers (injuries, neutral venue, motivation)
     * @return {@link PreMatchRates} containing lambdaHome and muAway
     */
    public static PreMatchRates calculatePreMatchRates(TeamStrength homeStrength,
                                                      TeamStrength awayStrength,
                                                      double leagueAvgXgPerTeam,
                                                      double homeAdvantageRatio,
                                                      MatchModifiers modifiers) {
        validateParameters(leagueAvgXgPerTeam, 1.0);
        if (homeAdvantageRatio < 1.0 || homeAdvantageRatio > 3.0) {
            throw new DomainValidationException("Home advantage ratio must be between 1.0 and 3.0 (received: " + homeAdvantageRatio + ").");
        }

        TeamStrength hStr = (homeStrength != null) ? homeStrength : new TeamStrength(1.0, 1.0, leagueAvgXgPerTeam, leagueAvgXgPerTeam);
        TeamStrength aStr = (awayStrength != null) ? awayStrength : new TeamStrength(1.0, 1.0, leagueAvgXgPerTeam, leagueAvgXgPerTeam);
        MatchModifiers mods = (modifiers != null) ? modifiers : MatchModifiers.defaultModifiers();

        // Effective home advantage (1.0 if neutral venue)
        double effHomeAdv = mods.isNeutralVenue() ? 1.00 : homeAdvantageRatio;
        double effAwayDisadv = 1.0 / effHomeAdv;

        // Base expected goals
        double lambda = hStr.alphaOffense() * aStr.betaDefense() * leagueAvgXgPerTeam * effHomeAdv;
        double mu = aStr.alphaOffense() * hStr.betaDefense() * leagueAvgXgPerTeam * effAwayDisadv;

        // Apply tactical multipliers (injuries / lineup quality)
        lambda = lambda * mods.getModAttHome() * mods.getModDefAway();
        mu = mu * mods.getModAttAway() * mods.getModDefHome();

        // Apply Mutual Low-Urgency discount if applicable
        if (mods.isMutualLowUrgency()) {
            lambda *= LOW_URGENCY_PRE_MATCH_FACTOR;
            mu *= LOW_URGENCY_PRE_MATCH_FACTOR;
        }

        return new PreMatchRates(Math.max(0.0, lambda), Math.max(0.0, mu), effHomeAdv);
    }

    private static void validateParameters(double leagueAvg, double gamma) {
        if (Double.isNaN(leagueAvg) || Double.isInfinite(leagueAvg) || leagueAvg <= 0.0) {
            throw new DomainValidationException("League average xG must be a positive finite number (received: " + leagueAvg + ").");
        }
        if (Double.isNaN(gamma) || Double.isInfinite(gamma) || gamma <= 0.0 || gamma > 1.0) {
            throw new DomainValidationException("Seasonal decay gamma must be in range (0.0, 1.0] (received: " + gamma + ").");
        }
    }
}
