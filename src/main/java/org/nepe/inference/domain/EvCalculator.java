package org.nepe.inference.domain;

import org.nepe.shared.exception.DomainValidationException;

/**
 * Pure mathematical utility for calculating Expected Value (EV) in Betting Exchange markets.
 * <p>
 * Implements net Expected Value calculations for Back (Punta) and Lay (Banca) positions,
 * incorporating exchange commissions and liability risk-adjustment:
 * <pre>
 *   EV_Back  = P * (K_Back - 1) * (1 - comm) - (1 - P)
 *   EV_Lay   = (1 - P) * (1 - comm) - P * (K_Lay - 1)
 *   EV_Risk  = EV_Lay / (K_Lay - 1)
 * </pre>
 */
public final class EvCalculator {

    private EvCalculator() {
        // Pure utility class - prevent instantiation
    }

    /**
     * Immutable value record summarizing the evaluation of Back and Lay opportunities.
     */
    public record EvEvaluation(
            Double evBack,
            Double evLay,
            Double evLayRiskAdjusted,
            boolean hasBackValue,
            boolean hasLayValue
    ) {
        public boolean hasAnyValue() {
            return hasBackValue || hasLayValue;
        }
    }

    /**
     * Calculates Expected Value for a Back (Punta) position deducting commission from net profit.
     *
     * @param probability    true estimated probability P in range [0.0, 1.0]
     * @param backOdds       market Back odds K_Back (must be > 1.0)
     * @param commissionRate exchange commission rate in range [0.0, 1.0) (e.g. 0.05 for 5%)
     * @return net expected profit per 1 unit staked
     */
    public static double calculateEvBack(double probability, double backOdds, double commissionRate) {
        validateInputs(probability, commissionRate);
        validateOdds("Back", backOdds);

        double winProfit = (backOdds - 1.0) * (1.0 - commissionRate);
        return (probability * winProfit) - (1.0 - probability);
    }

    /**
     * Calculates Expected Value for a Lay (Banca) position per 1 unit of backer stake won.
     *
     * @param probability    true estimated probability P in range [0.0, 1.0]
     * @param layOdds        market Lay odds K_Lay (must be > 1.0)
     * @param commissionRate exchange commission rate in range [0.0, 1.0)
     * @return net expected profit per 1 unit of backer stake
     */
    public static double calculateEvLay(double probability, double layOdds, double commissionRate) {
        validateInputs(probability, commissionRate);
        validateOdds("Lay", layOdds);

        double winStake = (1.0 - probability) * (1.0 - commissionRate);
        double lossLiability = probability * (layOdds - 1.0);
        return winStake - lossLiability;
    }

    /**
     * Calculates Risk-Adjusted Expected Value (Return on Capital at Risk) for a Lay position.
     * <p>
     * Normalizes the expected gain against the actual liability lost (K_Lay - 1).
     *
     * @param probability    true estimated probability P in range [0.0, 1.0]
     * @param layOdds        market Lay odds K_Lay (must be > 1.0)
     * @param commissionRate exchange commission rate
     * @return ROI percentage on risked liability
     */
    public static double calculateEvLayRiskAdjusted(double probability, double layOdds, double commissionRate) {
        double evLay = calculateEvLay(probability, layOdds, commissionRate);
        double liability = layOdds - 1.0;
        return evLay / liability;
    }

    /**
     * Comprehensive evaluation evaluating both Back and Lay odds against estimated probability.
     *
     * @param probability    true estimated probability P in range [0.0, 1.0]
     * @param backOdds       market Back odds (nullable if not offered)
     * @param layOdds        market Lay odds (nullable if not offered)
     * @param commissionRate exchange commission rate
     * @return {@link EvEvaluation} containing computed indices and positive value flags
     */
    public static EvEvaluation evaluate(double probability, Double backOdds, Double layOdds, double commissionRate) {
        Double evBack = (backOdds != null) ? calculateEvBack(probability, backOdds, commissionRate) : null;
        Double evLay = (layOdds != null) ? calculateEvLay(probability, layOdds, commissionRate) : null;
        Double evLayRisk = (layOdds != null) ? calculateEvLayRiskAdjusted(probability, layOdds, commissionRate) : null;

        boolean hasBackVal = evBack != null && evBack > 0.0;
        boolean hasLayVal = evLay != null && evLay > 0.0;

        return new EvEvaluation(evBack, evLay, evLayRisk, hasBackVal, hasLayVal);
    }

    // --- Invariant Validations ---

    private static void validateInputs(double probability, double commissionRate) {
        if (Double.isNaN(probability) || Double.isInfinite(probability) || probability < 0.0 || probability > 1.0) {
            throw new DomainValidationException("Probability must be a valid number in range [0.0, 1.0] (received: " + probability + ").");
        }
        if (Double.isNaN(commissionRate) || Double.isInfinite(commissionRate) || commissionRate < 0.0 || commissionRate >= 1.0) {
            throw new DomainValidationException("Commission rate must be in range [0.0, 1.0) (received: " + commissionRate + ").");
        }
    }

    private static void validateOdds(String label, double odds) {
        if (Double.isNaN(odds) || Double.isInfinite(odds) || odds <= 1.0) {
            throw new DomainValidationException(label + " odds must be strictly greater than 1.0 (received: " + odds + ").");
        }
    }
}
