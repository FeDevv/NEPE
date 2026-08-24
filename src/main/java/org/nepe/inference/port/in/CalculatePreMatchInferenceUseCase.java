package org.nepe.inference.port.in;

import org.nepe.match.domain.MarketOdds;

import java.util.List;

/**
 * Inbound Port (Driving Port / Use Case) defining the mathematical inference engine contract
 * for pre-match probability estimation, scoreline distributions, and Betting Exchange Expected Values.
 * <p>
 * Invoked by Inbound Adapters (e.g. JavaFX Pre-Match Analysis Controller, Match View Controller).
 */
public interface CalculatePreMatchInferenceUseCase {

    /**
     * Executes the complete pre-match analytical pipeline starting from historical team performances,
     * league baselines, tactical modifiers, and current exchange market odds.
     *
     * @param query the validated {@link PreMatchInferenceQuery} payload (must not be null)
     * @return the computed {@link PreMatchAnalysisResult}
     */
    PreMatchAnalysisResult calculate(PreMatchInferenceQuery query);

    /**
     * Executes the pre-match analytical evaluation directly from established expected goal rates (lambda and mu).
     * Useful when rates have already been determined or manually adjusted via tactical sliders in the UI.
     *
     * @param lambdaHome     pre-match expected goals for the Home team
     * @param muAway         pre-match expected goals for the Away team
     * @param dixonColesRho  competition Dixon-Coles dependence parameter (rho)
     * @param commissionRate exchange commission rate (e.g. 0.05)
     * @param marketOddsList list of current market odds to evaluate EV against (nullable or empty)
     * @return the computed {@link PreMatchAnalysisResult}
     */
    PreMatchAnalysisResult calculate(double lambdaHome,
                                     double muAway,
                                     double dixonColesRho,
                                     double commissionRate,
                                     List<MarketOdds> marketOddsList);
}
