package org.nepe.inference.port.in;

import java.util.List;

/**
 * Immutable DTO encapsulating the complete pre-match analytical evaluation for a match.
 * <p>
 * Contains calculated expected goal rates (lambda/mu), the full Dixon-Coles joint score probability matrix,
 * and market predictions (1X2, Under/Over 0.5-4.5, BTTS) along with their respective Expected Values.
 *
 * @param lambdaHome           expected goal rate for the Home team
 * @param muAway               expected goal rate for the Away team
 * @param effectiveHomeAdv     effective home advantage ratio applied (1.0 if neutral venue)
 * @param scoreMatrix          (N+1) x (N+1) joint scoreline probability matrix
 * @param homeWin              prediction for Home Win ("1")
 * @param draw                 prediction for Draw ("X")
 * @param awayWin              prediction for Away Win ("2")
 * @param underOverPredictions predictions for Under/Over threshold markets (0.5 to 4.5)
 * @param bttsYes              prediction for Both Teams to Score ("YES")
 * @param bttsNo               prediction for Both Teams to Score ("NO")
 */
public record PreMatchAnalysisResult(
        double lambdaHome,
        double muAway,
        double effectiveHomeAdv,
        double[][] scoreMatrix,
        MarketPrediction homeWin,
        MarketPrediction draw,
        MarketPrediction awayWin,
        List<MarketPrediction> underOverPredictions,
        MarketPrediction bttsYes,
        MarketPrediction bttsNo
) {

    /**
     * Checks if any market in this analysis presents a positive Expected Value (EV > 0) trading opportunity.
     */
    public boolean hasAnyValueOpportunity() {
        if (homeWin.hasValue() || draw.hasValue() || awayWin.hasValue() || bttsYes.hasValue() || bttsNo.hasValue()) {
            return true;
        }
        if (underOverPredictions != null) {
            for (MarketPrediction pred : underOverPredictions) {
                if (pred.hasValue()) {
                    return true;
                }
            }
        }
        return false;
    }
}
