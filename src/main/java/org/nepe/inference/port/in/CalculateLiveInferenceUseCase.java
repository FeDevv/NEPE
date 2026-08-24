package org.nepe.inference.port.in;

/**
 * Inbound Port (Driving Port / Use Case) defining the mathematical inference engine contract
 * for real-time live trading, dynamic scoreline probability updates, time-decay adjustments,
 * red card re-calibrations, and Green-Up target monitoring.
 * <p>
 * Invoked by Inbound Adapters (e.g., JavaFX Live Console Controller).
 */
public interface CalculateLiveInferenceUseCase {

    /**
     * Computes the real-time analytical evaluation for an ongoing match based on current in-game parameters.
     *
     * @param query the validated {@link LiveInferenceQuery} payload (must not be null)
     * @return the computed {@link LiveAnalysisResult} containing residual rates, adjusted matrices, and markets
     */
    LiveAnalysisResult calculate(LiveInferenceQuery query);
}
