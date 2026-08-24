package org.nepe.settings.port.in;

/**
 * Inbound Command DTO encapsulating the parameters required to update application settings.
 * <p>
 * Transferred across the hexagonal boundary from Inbound Adapters (e.g. JavaFX Settings Controller)
 * to the {@link ManageSettingsUseCase}.
 *
 * @param commissionRate       Betfair / Betting Exchange commission rate (e.g., 0.05 for 5%)
 * @param defaultNMatches      default sample size of historical matches to analyze
 * @param seasonalDecayGamma   discount weight applied to previous season matches (e.g., 0.70)
 * @param greenUpProfitTarget  profit threshold target for live trading cash-out alerts (e.g., 0.10 for 10%)
 */
public record UpdateSettingsCommand(
        double commissionRate,
        int defaultNMatches,
        double seasonalDecayGamma,
        double greenUpProfitTarget
) {
}
