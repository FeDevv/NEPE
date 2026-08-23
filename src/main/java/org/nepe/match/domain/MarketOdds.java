package org.nepe.match.domain;

import org.nepe.shared.exception.DomainValidationException;

import java.util.Objects;
import java.util.OptionalDouble;

/**
 * Domain entity representing Betting Exchange market odds (Back and/or Lay)
 * for a specific match outcome (e.g., Match Odds "1", Under/Over 2.5 "OVER", BTTS "YES").
 */
public class MarketOdds {

    public static final double MIN_ODDS = 1.01;
    public static final double MAX_ODDS = 1000.0;

    private Integer id;
    private Integer matchId;
    private MarketType marketType;
    private String outcome;
    private Double backOdds;
    private Double layOdds;

    /**
     * Factory method for creating an unpersisted MarketOdds instance.
     */
    public static MarketOdds create(Integer matchId, MarketType marketType, String outcome, Double backOdds, Double layOdds) {
        return new MarketOdds(null, matchId, marketType, outcome, backOdds, layOdds);
    }

    public static MarketOdds backOnly(Integer matchId, MarketType marketType, String outcome, double backOdds) {
        return create(matchId, marketType, outcome, backOdds, null);
    }

    public static MarketOdds layOnly(Integer matchId, MarketType marketType, String outcome, double layOdds) {
        return create(matchId, marketType, outcome, null, layOdds);
    }

    /**
     * Full constructor for domain reconstruction and validated creation.
     */
    public MarketOdds(Integer id, Integer matchId, MarketType marketType, String outcome, Double backOdds, Double layOdds) {
        validateMatchId(matchId);
        validateMarketType(marketType);
        String normalizedOutcome = validateAndNormalizeOutcome(marketType, outcome);
        validateOdds(backOdds, layOdds);

        this.id = id;
        this.matchId = matchId;
        this.marketType = marketType;
        this.outcome = normalizedOutcome;
        this.backOdds = backOdds;
        this.layOdds = layOdds;
    }

    // --- Domain Business Logic & Mutations ---

    public void updateOdds(Double backOdds, Double layOdds) {
        validateOdds(backOdds, layOdds);
        this.backOdds = backOdds;
        this.layOdds = layOdds;
    }

    public void updateBackOdds(Double backOdds) {
        validateOdds(backOdds, this.layOdds);
        this.backOdds = backOdds;
    }

    public void updateLayOdds(Double layOdds) {
        validateOdds(this.backOdds, layOdds);
        this.layOdds = layOdds;
    }

    public boolean hasBackOdds() {
        return backOdds != null;
    }

    public boolean hasLayOdds() {
        return layOdds != null;
    }

    public boolean hasBothOdds() {
        return backOdds != null && layOdds != null;
    }

    /**
     * Calculates the price spread between Lay and Back odds (Lay - Back) if both are present.
     */
    public OptionalDouble getSpread() {
        if (hasBothOdds()) {
            return OptionalDouble.of(layOdds - backOdds);
        }
        return OptionalDouble.empty();
    }

    public void assignId(Integer id) {
        if (id == null || id <= 0) {
            throw new DomainValidationException("MarketOdds ID must be a positive integer.");
        }
        if (this.id != null && !this.id.equals(id)) {
            throw new DomainValidationException("Cannot reassign an existing MarketOdds ID.");
        }
        this.id = id;
    }

    // --- Invariant Validations ---

    private static void validateMatchId(Integer matchId) {
        if (matchId == null || matchId <= 0) {
            throw new DomainValidationException("MarketOdds must be associated with a valid positive matchId.");
        }
    }

    private static void validateMarketType(MarketType marketType) {
        if (marketType == null) {
            throw new DomainValidationException("MarketType cannot be null.");
        }
    }

    private static String validateAndNormalizeOutcome(MarketType marketType, String outcome) {
        if (outcome == null || outcome.isBlank()) {
            throw new DomainValidationException("Market outcome cannot be null or blank.");
        }
        String normalized = outcome.trim().toUpperCase();
        if (!marketType.isValidOutcome(normalized)) {
            throw new DomainValidationException(
                    String.format("Invalid outcome '%s' for market %s. Allowed outcomes: %s",
                            outcome, marketType, marketType.getValidOutcomes())
            );
        }
        return normalized;
    }

    private static void validateOdds(Double back, Double lay) {
        if (back == null && lay == null) {
            throw new DomainValidationException("At least one of Back odds or Lay odds must be provided.");
        }

        if (back != null) {
            validateSingleOdd("Back", back);
        }

        if (lay != null) {
            validateSingleOdd("Lay", lay);
        }

        if (back != null && lay != null && lay < back) {
            throw new DomainValidationException(
                    String.format("Lay odds (%f) cannot be strictly lower than Back odds (%f).", lay, back)
            );
        }
    }

    private static void validateSingleOdd(String label, double odd) {
        if (Double.isNaN(odd) || Double.isInfinite(odd)) {
            throw new DomainValidationException(label + " odds must be a valid finite number.");
        }
        if (odd < MIN_ODDS || odd > MAX_ODDS) {
            throw new DomainValidationException(
                    String.format("%s odds must be between %f and %f (received: %f).", label, MIN_ODDS, MAX_ODDS, odd)
            );
        }
    }

    // --- Getters ---

    public Integer getId() {
        return id;
    }

    public Integer getMatchId() {
        return matchId;
    }

    public MarketType getMarketType() {
        return marketType;
    }

    public String getOutcome() {
        return outcome;
    }

    public Double getBackOdds() {
        return backOdds;
    }

    public Double getLayOdds() {
        return layOdds;
    }

    // --- Identity & Equality based on composite business key (matchId, marketType, outcome) ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MarketOdds that = (MarketOdds) o;
        return Objects.equals(matchId, that.matchId) &&
                marketType == that.marketType &&
                Objects.equals(outcome, that.outcome);
    }

    @Override
    public int hashCode() {
        return Objects.hash(matchId, marketType, outcome);
    }

    @Override
    public String toString() {
        return "MarketOdds{" +
                "id=" + id +
                ", matchId=" + matchId +
                ", marketType=" + marketType +
                ", outcome='" + outcome + '\'' +
                ", backOdds=" + backOdds +
                ", layOdds=" + layOdds +
                '}';
    }
}
