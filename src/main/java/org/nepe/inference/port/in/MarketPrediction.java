package org.nepe.inference.port.in;

import org.nepe.inference.domain.EvCalculator.EvEvaluation;

/**
 * Immutable DTO representing the mathematical and financial prediction for a specific betting market outcome.
 * <p>
 * Unifies the estimated pure probability, theoretical fair odds (1/P), and Betting Exchange Expected Value indices.
 *
 * @param outcome      the outcome label (e.g., "1", "X", "2", "OVER", "UNDER", "YES", "NO")
 * @param probability  true estimated probability P in range [0.0, 1.0]
 * @param fairOdds     theoretical fair odds (1.0 / P)
 * @param evEvaluation calculated Expected Value evaluation (nullable if market odds were not supplied)
 */
public record MarketPrediction(
        String outcome,
        double probability,
        double fairOdds,
        EvEvaluation evEvaluation
) {

    /**
     * Checks if this prediction presents a positive Expected Value (EV > 0) on either Back or Lay.
     */
    public boolean hasValue() {
        return evEvaluation != null && evEvaluation.hasAnyValue();
    }

    /**
     * Checks if Back (Punta) has positive Expected Value.
     */
    public boolean hasBackValue() {
        return evEvaluation != null && evEvaluation.hasBackValue();
    }

    /**
     * Checks if Lay (Banca) has positive Expected Value.
     */
    public boolean hasLayValue() {
        return evEvaluation != null && evEvaluation.hasLayValue();
    }

    public Double getEvBack() {
        return evEvaluation != null ? evEvaluation.evBack() : null;
    }

    public Double getEvLay() {
        return evEvaluation != null ? evEvaluation.evLay() : null;
    }

    public Double getEvLayRiskAdjusted() {
        return evEvaluation != null ? evEvaluation.evLayRiskAdjusted() : null;
    }
}
