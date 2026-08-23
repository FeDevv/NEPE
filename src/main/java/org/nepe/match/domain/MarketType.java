package org.nepe.match.domain;

import org.nepe.shared.exception.DomainValidationException;

import java.util.Collections;
import java.util.OptionalDouble;
import java.util.Set;

/**
 * Supported betting exchange market types in NEPE 2.0.
 */
public enum MarketType {

    /**
     * Match Odds / 1X2 (Home Win, Draw, Away Win).
     */
    MATCH_ODDS(Set.of("1", "X", "2")),

    /**
     * Total Goals Under / Over 0.5.
     */
    UNDER_OVER_05(Set.of("UNDER", "OVER"), 0.5),

    /**
     * Total Goals Under / Over 1.5.
     */
    UNDER_OVER_15(Set.of("UNDER", "OVER"), 1.5),

    /**
     * Total Goals Under / Over 2.5.
     */
    UNDER_OVER_25(Set.of("UNDER", "OVER"), 2.5),

    /**
     * Total Goals Under / Over 3.5.
     */
    UNDER_OVER_35(Set.of("UNDER", "OVER"), 3.5),

    /**
     * Total Goals Under / Over 4.5.
     */
    UNDER_OVER_45(Set.of("UNDER", "OVER"), 4.5),

    /**
     * Both Teams to Score (BTTS - Yes / No).
     */
    BTTS(Set.of("YES", "NO"));

    private final Set<String> validOutcomes;
    private final Double threshold;

    MarketType(Set<String> validOutcomes) {
        this(validOutcomes, null);
    }

    MarketType(Set<String> validOutcomes, Double threshold) {
        this.validOutcomes = Collections.unmodifiableSet(validOutcomes);
        this.threshold = threshold;
    }

    /**
     * Checks if this is an Under/Over goal line market.
     */
    public boolean isUnderOver() {
        return threshold != null;
    }

    /**
     * Returns the goal line threshold if this is an Under/Over market (e.g., 2.5).
     */
    public OptionalDouble getThreshold() {
        return threshold != null ? OptionalDouble.of(threshold) : OptionalDouble.empty();
    }

    /**
     * Returns the set of valid outcomes for this market type.
     */
    public Set<String> getValidOutcomes() {
        return validOutcomes;
    }

    /**
     * Validates whether a given outcome string is valid for this market type.
     */
    public boolean isValidOutcome(String outcome) {
        if (outcome == null || outcome.isBlank()) {
            return false;
        }
        return validOutcomes.contains(outcome.trim().toUpperCase());
    }

    /**
     * Validates and parses a string into a {@link MarketType}.
     */
    public static MarketType fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new DomainValidationException("MarketType cannot be null or blank.");
        }
        try {
            return MarketType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new DomainValidationException(
                    String.format("Unrecognized MarketType: '%s'. Valid types: MATCH_ODDS, UNDER_OVER_05..45, BTTS.", value),
                    e
            );
        }
    }
}
