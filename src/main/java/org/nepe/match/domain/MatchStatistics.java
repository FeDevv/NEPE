package org.nepe.match.domain;

import org.nepe.shared.exception.DomainValidationException;

import java.util.Objects;
import java.util.OptionalDouble;

/**
 * Value Object encapsulating all in-game match statistics, scoreline, cards, and Expected Goals (xG).
 * <p>
 * Implements the domain heuristic xG formula with manual override priority:
 * <pre>
 *   xG = (ShotsOnTarget * 0.30) + ((TotalShots - ShotsOnTarget) * 0.05)
 * </pre>
 */
public class MatchStatistics {

    public static final double SHOT_ON_TARGET_XG_WEIGHT = 0.30;
    public static final double OFF_TARGET_SHOT_XG_WEIGHT = 0.05;

    private Integer homeScore;
    private Integer awayScore;
    private Integer homeShots;
    private Integer awayShots;
    private Integer homeShotsOnTarget;
    private Integer awayShotsOnTarget;
    private int homeRedCards;
    private int awayRedCards;
    private Double manualHomeXg;
    private Double manualAwayXg;

    /**
     * Factory method creating an empty statistics object (for upcoming scheduled matches).
     */
    public static MatchStatistics empty() {
        return new MatchStatistics(null, null, null, null, null, null, 0, 0, null, null);
    }

    /**
     * Full constructor with invariant validations.
     */
    public MatchStatistics(Integer homeScore,
                           Integer awayScore,
                           Integer homeShots,
                           Integer awayShots,
                           Integer homeShotsOnTarget,
                           Integer awayShotsOnTarget,
                           int homeRedCards,
                           int awayRedCards,
                           Double manualHomeXg,
                           Double manualAwayXg) {
        validateScores(homeScore, awayScore);
        validateShots(homeShots, homeShotsOnTarget, "Home");
        validateShots(awayShots, awayShotsOnTarget, "Away");
        validateRedCards(homeRedCards, "Home");
        validateRedCards(awayRedCards, "Away");
        validateManualXg(manualHomeXg, "Home");
        validateManualXg(manualAwayXg, "Away");

        this.homeScore = homeScore;
        this.awayScore = awayScore;
        this.homeShots = homeShots;
        this.awayShots = awayShots;
        this.homeShotsOnTarget = homeShotsOnTarget;
        this.awayShotsOnTarget = awayShotsOnTarget;
        this.homeRedCards = homeRedCards;
        this.awayRedCards = awayRedCards;
        this.manualHomeXg = manualHomeXg;
        this.manualAwayXg = manualAwayXg;
    }

    // --- Domain Business Logic: Dynamic & Effective xG Calculation ---

    /**
     * Resolves the effective Home xG following domain priority rules:
     * 1. Manual user override if present.
     * 2. Heuristic xG based on shots and shots on target if present.
     * 3. Fallback to actual goals scored.
     * 4. Empty if the match has not been played.
     */
    public OptionalDouble getEffectiveHomeXg() {
        if (manualHomeXg != null) {
            return OptionalDouble.of(manualHomeXg);
        }
        if (homeShots != null && homeShotsOnTarget != null) {
            return OptionalDouble.of(calculateHeuristicXg(homeShots, homeShotsOnTarget));
        }
        if (homeScore != null) {
            return OptionalDouble.of(homeScore.doubleValue());
        }
        return OptionalDouble.empty();
    }

    /**
     * Resolves the effective Away xG following domain priority rules:
     * 1. Manual user override if present.
     * 2. Heuristic xG based on shots and shots on target if present.
     * 3. Fallback to actual goals scored.
     * 4. Empty if the match has not been played.
     */
    public OptionalDouble getEffectiveAwayXg() {
        if (manualAwayXg != null) {
            return OptionalDouble.of(manualAwayXg);
        }
        if (awayShots != null && awayShotsOnTarget != null) {
            return OptionalDouble.of(calculateHeuristicXg(awayShots, awayShotsOnTarget));
        }
        if (awayScore != null) {
            return OptionalDouble.of(awayScore.doubleValue());
        }
        return OptionalDouble.empty();
    }

    private static double calculateHeuristicXg(int totalShots, int shotsOnTarget) {
        int offTarget = totalShots - shotsOnTarget;
        return (shotsOnTarget * SHOT_ON_TARGET_XG_WEIGHT) + (offTarget * OFF_TARGET_SHOT_XG_WEIGHT);
    }

    // --- State Mutations for Live and Historical Updates ---

    public void incrementHomeScore() {
        this.homeScore = (this.homeScore != null ? this.homeScore : 0) + 1;
    }

    public void incrementAwayScore() {
        this.awayScore = (this.awayScore != null ? this.awayScore : 0) + 1;
    }

    public void incrementHomeRedCards() {
        this.homeRedCards++;
    }

    public void incrementAwayRedCards() {
        this.awayRedCards++;
    }

    public void decrementHomeScore() {
        if (this.homeScore != null && this.homeScore > 0) {
            this.homeScore--;
        }
    }

    public void decrementAwayScore() {
        if (this.awayScore != null && this.awayScore > 0) {
            this.awayScore--;
        }
    }

    public void decrementHomeRedCards() {
        if (this.homeRedCards > 0) {
            this.homeRedCards--;
        }
    }

    public void decrementAwayRedCards() {
        if (this.awayRedCards > 0) {
            this.awayRedCards--;
        }
    }

    public void updateScores(Integer homeScore, Integer awayScore) {
        validateScores(homeScore, awayScore);
        this.homeScore = homeScore;
        this.awayScore = awayScore;
    }

    public void updateShots(Integer homeShots, Integer awayShots, Integer homeShotsOnTarget, Integer awayShotsOnTarget) {
        validateShots(homeShots, homeShotsOnTarget, "Home");
        validateShots(awayShots, awayShotsOnTarget, "Away");
        this.homeShots = homeShots;
        this.awayShots = awayShots;
        this.homeShotsOnTarget = homeShotsOnTarget;
        this.awayShotsOnTarget = awayShotsOnTarget;
    }

    public void updateManualXg(Double manualHomeXg, Double manualAwayXg) {
        validateManualXg(manualHomeXg, "Home");
        validateManualXg(manualAwayXg, "Away");
        this.manualHomeXg = manualHomeXg;
        this.manualAwayXg = manualAwayXg;
    }

    // --- Invariant Validations ---

    private static void validateScores(Integer homeScore, Integer awayScore) {
        if (homeScore != null && homeScore < 0) {
            throw new DomainValidationException("Home score cannot be negative.");
        }
        if (awayScore != null && awayScore < 0) {
            throw new DomainValidationException("Away score cannot be negative.");
        }
    }

    private static void validateShots(Integer totalShots, Integer shotsOnTarget, String side) {
        if (totalShots != null && totalShots < 0) {
            throw new DomainValidationException(side + " total shots cannot be negative.");
        }
        if (shotsOnTarget != null && shotsOnTarget < 0) {
            throw new DomainValidationException(side + " shots on target cannot be negative.");
        }
        if (totalShots != null && shotsOnTarget != null && shotsOnTarget > totalShots) {
            throw new DomainValidationException(
                    String.format("%s shots on target (%d) cannot exceed total shots (%d).", side, shotsOnTarget, totalShots)
            );
        }
    }

    private static void validateRedCards(int redCards, String side) {
        if (redCards < 0) {
            throw new DomainValidationException(side + " red cards cannot be negative.");
        }
    }

    private static void validateManualXg(Double xg, String side) {
        if (xg != null) {
            if (Double.isNaN(xg) || Double.isInfinite(xg)) {
                throw new DomainValidationException(side + " manual xG must be a valid finite number.");
            }
            if (xg < 0.0) {
                throw new DomainValidationException(side + " manual xG cannot be negative.");
            }
        }
    }

    // --- Getters ---

    public Integer getHomeScore() {
        return homeScore;
    }

    public Integer getAwayScore() {
        return awayScore;
    }

    public Integer getHomeShots() {
        return homeShots;
    }

    public Integer getAwayShots() {
        return awayShots;
    }

    public Integer getHomeShotsOnTarget() {
        return homeShotsOnTarget;
    }

    public Integer getAwayShotsOnTarget() {
        return awayShotsOnTarget;
    }

    public int getHomeRedCards() {
        return homeRedCards;
    }

    public int getAwayRedCards() {
        return awayRedCards;
    }

    public Double getManualHomeXg() {
        return manualHomeXg;
    }

    public Double getManualAwayXg() {
        return manualAwayXg;
    }

    // --- Equality & Identity ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MatchStatistics that = (MatchStatistics) o;
        return homeRedCards == that.homeRedCards &&
                awayRedCards == that.awayRedCards &&
                Objects.equals(homeScore, that.homeScore) &&
                Objects.equals(awayScore, that.awayScore) &&
                Objects.equals(homeShots, that.homeShots) &&
                Objects.equals(awayShots, that.awayShots) &&
                Objects.equals(homeShotsOnTarget, that.homeShotsOnTarget) &&
                Objects.equals(awayShotsOnTarget, that.awayShotsOnTarget) &&
                Objects.equals(manualHomeXg, that.manualHomeXg) &&
                Objects.equals(manualAwayXg, that.manualAwayXg);
    }

    @Override
    public int hashCode() {
        return Objects.hash(homeScore, awayScore, homeShots, awayShots, homeShotsOnTarget, awayShotsOnTarget,
                homeRedCards, awayRedCards, manualHomeXg, manualAwayXg);
    }

    @Override
    public String toString() {
        return "MatchStatistics{" +
                "homeScore=" + homeScore +
                ", awayScore=" + awayScore +
                ", homeShots=" + homeShots +
                ", awayShots=" + awayShots +
                ", homeShotsOnTarget=" + homeShotsOnTarget +
                ", awayShotsOnTarget=" + awayShotsOnTarget +
                ", homeRedCards=" + homeRedCards +
                ", awayRedCards=" + awayRedCards +
                ", manualHomeXg=" + manualHomeXg +
                ", manualAwayXg=" + manualAwayXg +
                '}';
    }
}
