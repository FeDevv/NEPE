package org.nepe.inference.domain;

import org.nepe.shared.exception.DomainValidationException;

import java.util.OptionalDouble;

/**
 * Pure mathematical utility for estimating Expected Goals (xG) from basic shot statistics.
 * <p>
 * Implements the standard heuristic formula:
 * <pre>
 *   xG = (ShotsOnTarget * 0.30) + ((TotalShots - ShotsOnTarget) * 0.05)
 * </pre>
 */
public final class XgEstimator {

    public static final double SHOT_ON_TARGET_WEIGHT = 0.30;
    public static final double OFF_TARGET_WEIGHT = 0.05;

    private XgEstimator() {
        // Pure utility class - prevent instantiation
    }

    /**
     * Estimates xG from total shots and shots on target.
     *
     * @param totalShots    total shots taken (must be >= 0)
     * @param shotsOnTarget shots on target (must be >= 0 and <= totalShots)
     * @return estimated xG value
     * @throws DomainValidationException if shots are negative or shotsOnTarget > totalShots
     */
    public static double estimate(int totalShots, int shotsOnTarget) {
        validateShots(totalShots, shotsOnTarget);
        int offTarget = totalShots - shotsOnTarget;
        return (shotsOnTarget * SHOT_ON_TARGET_WEIGHT) + (offTarget * OFF_TARGET_WEIGHT);
    }

    /**
     * Resolves the effective xG adhering to the domain priority hierarchy:
     * <ol>
     *     <li>Manual user override (if provided)</li>
     *     <li>Heuristic calculation (if shot statistics are present)</li>
     *     <li>Actual goals scored (as a fallback when shots are absent)</li>
     *     <li>Empty if no data is available</li>
     * </ol>
     */
    public static OptionalDouble resolveEffectiveXg(Double manualXg,
                                                    Integer totalShots,
                                                    Integer shotsOnTarget,
                                                    Integer fallbackGoals) {
        if (manualXg != null) {
            if (Double.isNaN(manualXg) || Double.isInfinite(manualXg) || manualXg < 0.0) {
                throw new DomainValidationException("Manual xG must be a non-negative finite number.");
            }
            return OptionalDouble.of(manualXg);
        }

        if (totalShots != null && shotsOnTarget != null) {
            return OptionalDouble.of(estimate(totalShots, shotsOnTarget));
        }

        if (fallbackGoals != null) {
            if (fallbackGoals < 0) {
                throw new DomainValidationException("Fallback goals cannot be negative.");
            }
            return OptionalDouble.of(fallbackGoals.doubleValue());
        }

        return OptionalDouble.empty();
    }

    private static void validateShots(int totalShots, int shotsOnTarget) {
        if (totalShots < 0) {
            throw new DomainValidationException("Total shots cannot be negative (received: " + totalShots + ").");
        }
        if (shotsOnTarget < 0) {
            throw new DomainValidationException("Shots on target cannot be negative (received: " + shotsOnTarget + ").");
        }
        if (shotsOnTarget > totalShots) {
            throw new DomainValidationException(
                    String.format("Shots on target (%d) cannot exceed total shots (%d).", shotsOnTarget, totalShots)
            );
        }
    }
}
