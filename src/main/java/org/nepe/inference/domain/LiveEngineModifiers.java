package org.nepe.inference.domain;

import org.nepe.match.domain.MatchModifiers;
import org.nepe.match.domain.MatchStatistics;
import org.nepe.shared.exception.DomainValidationException;

/**
 * Pure mathematical utility for computing real-time residual expected goal rates (lambda and mu)
 * during live match trading.
 * <p>
 * Incorporates:
 * <ul>
 *     <li>Linear Time-Decay toward minute 90 (clamped to 0 at and beyond minute 90)</li>
 *     <li>Cumulative Red Card penalties (0.70^C) and opponent advantages (1.30^C)</li>
 *     <li>Second-half Must-Win tactical imbalance (+25% attack, +20% conceded)</li>
 *     <li>Mutual Low-Urgency draw conservativeness (-35% for both teams)</li>
 * </ul>
 */
public final class LiveEngineModifiers {

    public static final double RED_CARD_PENALTY_FACTOR = 0.70;
    public static final double RED_CARD_BONUS_FACTOR = 1.30;
    public static final double MUST_WIN_ATTACK_BOOST = 1.25;
    public static final double MUST_WIN_DEFENSE_PENALTY = 1.20;
    public static final double LOW_URGENCY_FACTOR = 0.65;

    public static final int REGULAR_TIME_MINUTES = 90;
    public static final int SECOND_HALF_START_MINUTE = 45;

    private LiveEngineModifiers() {
        // Pure utility class - prevent instantiation
    }

    /**
     * Immutable value object holding calculated residual rates.
     */
    public record LiveRates(double lambdaHomeResidual, double muAwayResidual) {
        public LiveRates {
            if (lambdaHomeResidual < 0.0 || muAwayResidual < 0.0) {
                throw new DomainValidationException("Residual rates cannot be negative.");
            }
        }
    }

    /**
     * Computes the linear time-decay multiplier for the remaining regulation minutes.
     *
     * @param minute current match minute (0 to 130)
     * @return time-decay factor in range [0.0, 1.0]
     */
    public static double calculateTimeDecayFactor(int minute) {
        if (minute <= 0) {
            return 1.0;
        }
        if (minute >= REGULAR_TIME_MINUTES) {
            return 0.0;
        }
        return (double) (REGULAR_TIME_MINUTES - minute) / REGULAR_TIME_MINUTES;
    }

    /**
     * Calculates residual expected goal rates from primitive arguments.
     */
    public static LiveRates calculateResidualRates(double lambdaHomePre,
                                                  double muAwayPre,
                                                  int currentMinute,
                                                  int homeRedCards,
                                                  int awayRedCards,
                                                  int currentHomeScore,
                                                  int currentAwayScore,
                                                  boolean mustWinHome,
                                                  boolean mustWinAway,
                                                  boolean lowUrgencyHome,
                                                  boolean lowUrgencyAway) {
        validateInputs(lambdaHomePre, muAwayPre, currentMinute, homeRedCards, awayRedCards);

        // 1. Time-Decay (if beyond 90 minutes, residual rates immediately drop to 0)
        double decayFactor = calculateTimeDecayFactor(currentMinute);
        if (decayFactor == 0.0) {
            return new LiveRates(0.0, 0.0);
        }

        double lambda = lambdaHomePre * decayFactor;
        double mu = muAwayPre * decayFactor;

        // 2. Red Card Correctors (cumulative multiplicative effect)
        if (homeRedCards > 0 || awayRedCards > 0) {
            lambda = lambda * Math.pow(RED_CARD_PENALTY_FACTOR, homeRedCards) * Math.pow(RED_CARD_BONUS_FACTOR, awayRedCards);
            mu = mu * Math.pow(RED_CARD_PENALTY_FACTOR, awayRedCards) * Math.pow(RED_CARD_BONUS_FACTOR, homeRedCards);
        }

        // 3. Second-Half Must-Win Motivation (active from minute 45 onwards when tied or trailing)
        if (currentMinute >= SECOND_HALF_START_MINUTE) {
            if (mustWinHome && currentHomeScore <= currentAwayScore) {
                lambda *= MUST_WIN_ATTACK_BOOST;
                mu *= MUST_WIN_DEFENSE_PENALTY;
            }
            if (mustWinAway && currentAwayScore <= currentHomeScore) {
                mu *= MUST_WIN_ATTACK_BOOST;
                lambda *= MUST_WIN_DEFENSE_PENALTY;
            }
        }

        // 4. Mutual Low-Urgency Rule (applies when both teams are content with a draw)
        if (lowUrgencyHome && lowUrgencyAway) {
            lambda *= LOW_URGENCY_FACTOR;
            mu *= LOW_URGENCY_FACTOR;
        }

        return new LiveRates(Math.max(0.0, lambda), Math.max(0.0, mu));
    }

    /**
     * Convenience method integrating domain Value Objects.
     */
    public static LiveRates calculateResidualRates(double lambdaHomePre,
                                                  double muAwayPre,
                                                  int currentMinute,
                                                  MatchStatistics statistics,
                                                  MatchModifiers modifiers) {
        int homeScore = (statistics != null && statistics.getHomeScore() != null) ? statistics.getHomeScore() : 0;
        int awayScore = (statistics != null && statistics.getAwayScore() != null) ? statistics.getAwayScore() : 0;
        int homeRedCards = (statistics != null) ? statistics.getHomeRedCards() : 0;
        int awayRedCards = (statistics != null) ? statistics.getAwayRedCards() : 0;

        boolean mustWinH = modifiers != null && modifiers.isMustWinHome();
        boolean mustWinA = modifiers != null && modifiers.isMustWinAway();
        boolean lowUrgH = modifiers != null && modifiers.isLowUrgencyHome();
        boolean lowUrgA = modifiers != null && modifiers.isLowUrgencyAway();

        return calculateResidualRates(
                lambdaHomePre,
                muAwayPre,
                currentMinute,
                homeRedCards,
                awayRedCards,
                homeScore,
                awayScore,
                mustWinH,
                mustWinA,
                lowUrgH,
                lowUrgA
        );
    }

    private static void validateInputs(double lambda, double mu, int minute, int homeCards, int awayCards) {
        if (Double.isNaN(lambda) || Double.isInfinite(lambda) || lambda < 0.0) {
            throw new DomainValidationException("Pre-match lambda must be a non-negative finite number.");
        }
        if (Double.isNaN(mu) || Double.isInfinite(mu) || mu < 0.0) {
            throw new DomainValidationException("Pre-match mu must be a non-negative finite number.");
        }
        if (minute < 0 || minute > 130) {
            throw new DomainValidationException("Game minute must be between 0 and 130.");
        }
        if (homeCards < 0 || awayCards < 0) {
            throw new DomainValidationException("Red cards count cannot be negative.");
        }
    }
}
