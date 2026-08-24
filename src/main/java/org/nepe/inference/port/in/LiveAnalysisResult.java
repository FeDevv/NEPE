package org.nepe.inference.port.in;

import java.util.List;

/**
 * Immutable DTO encapsulating the real-time dynamic analytical evaluation for an ongoing live match.
 * <p>
 * Unifies time-decayed residual goal rates, the residual score matrix, and adjusted full-time
 * market predictions (accounting for the current in-game scoreline, cards, and elapsed minutes).
 *
 * @param currentMinute        elapsed match minute (0 to 130)
 * @param currentHomeScore     current goals scored by the Home team
 * @param currentAwayScore     current goals scored by the Away team
 * @param lambdaHomeResidual   residual expected goals for the Home team until minute 90
 * @param muAwayResidual       residual expected goals for the Away team until minute 90
 * @param residualScoreMatrix  (N+1) x (N+1) probability matrix of additional goals scored in remaining time
 * @param finalHomeWin         full-time Home Win prediction ("1") taking current scoreline into account
 * @param finalDraw            full-time Draw prediction ("X") taking current scoreline into account
 * @param finalAwayWin         full-time Away Win prediction ("2") taking current scoreline into account
 * @param underOverPredictions full-time Under/Over predictions (0.5 to 4.5)
 * @param bttsYes              full-time Both Teams to Score ("YES") prediction
 * @param bttsNo               full-time Both Teams to Score ("NO") prediction
 * @param greenUpTargetMet     flag indicating if profit thresholds have been satisfied for cash-out / green-up
 */
public record LiveAnalysisResult(
        int currentMinute,
        int currentHomeScore,
        int currentAwayScore,
        double lambdaHomeResidual,
        double muAwayResidual,
        double[][] residualScoreMatrix,
        MarketPrediction finalHomeWin,
        MarketPrediction finalDraw,
        MarketPrediction finalAwayWin,
        List<MarketPrediction> underOverPredictions,
        MarketPrediction bttsYes,
        MarketPrediction bttsNo,
        boolean greenUpTargetMet
) {

    /**
     * Checks if any live market presents a positive Expected Value (EV > 0) opportunity.
     */
    public boolean hasAnyValueOpportunity() {
        if (finalHomeWin.hasValue() || finalDraw.hasValue() || finalAwayWin.hasValue() || bttsYes.hasValue() || bttsNo.hasValue()) {
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
