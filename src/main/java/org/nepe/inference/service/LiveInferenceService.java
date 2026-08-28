package org.nepe.inference.service;

import org.nepe.inference.domain.EvCalculator;
import org.nepe.inference.domain.EvCalculator.EvEvaluation;
import org.nepe.inference.domain.LiveEngineModifiers;
import org.nepe.inference.domain.LiveEngineModifiers.LiveRates;
import org.nepe.inference.domain.PoissonModel;
import org.nepe.inference.port.in.CalculateLiveInferenceUseCase;
import org.nepe.inference.port.in.LiveAnalysisResult;
import org.nepe.inference.port.in.LiveInferenceQuery;
import org.nepe.inference.port.in.MarketPrediction;
import org.nepe.match.domain.MarketOdds;
import org.nepe.match.domain.MarketType;
import org.nepe.shared.exception.DomainValidationException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Application Service implementing {@link CalculateLiveInferenceUseCase}.
 * <p>
 * Orchestrates real-time in-game quantitative calculations:
 * 1. Time-decay residual expected goal rates calculation (minute 0 to 90).
 * 2. Cumulative red card tactical adjustments and 2nd-half Must-Win motivation balance.
 * 3. Residual score matrix generation and full-time probability updates based on current score.
 * 4. Real-time Betting Exchange Expected Value and Green-Up profit target monitoring.
 */
@Service
public class LiveInferenceService implements CalculateLiveInferenceUseCase {

    private static final double[] UNDER_OVER_THRESHOLDS = {0.5, 1.5, 2.5, 3.5, 4.5};
    private static final MarketType[] UNDER_OVER_MARKET_TYPES = {
            MarketType.UNDER_OVER_05,
            MarketType.UNDER_OVER_15,
            MarketType.UNDER_OVER_25,
            MarketType.UNDER_OVER_35,
            MarketType.UNDER_OVER_45
    };

    @Override
    public LiveAnalysisResult calculate(LiveInferenceQuery query) {
        if (query == null) {
            throw new DomainValidationException("LiveInferenceQuery cannot be null.");
        }

        // 1. Calculate time-decayed and tactically adjusted residual goal rates
        LiveRates residualRates = LiveEngineModifiers.calculateResidualRates(
                query.lambdaHomePre(),
                query.muAwayPre(),
                query.currentMinute(),
                query.homeRedCards(),
                query.awayRedCards(),
                query.currentHomeScore(),
                query.currentAwayScore(),
                query.modifiers().isMustWinHome(),
                query.modifiers().isMustWinAway(),
                query.modifiers().isLowUrgencyHome(),
                query.modifiers().isLowUrgencyAway()
        );

        double lambdaRes = residualRates.lambdaHomeResidual();
        double muRes = residualRates.muAwayResidual();

        // 2. Generate residual score matrix for remaining regulation time
        double[][] residualMatrix = PoissonModel.calculateScoreMatrix(lambdaRes, muRes);

        // 3. Index live market odds for O(1) matching
        Map<String, MarketOdds> oddsIndex = indexMarketOdds(query.currentLiveOdds());

        // 4. Compute full-time probabilities taking in-game score into account
        int hScore = query.currentHomeScore();
        int aScore = query.currentAwayScore();
        double commission = query.commissionRate();

        // 4a. Match Odds (1X2)
        double pHome = calculateLiveHomeWin(residualMatrix, hScore, aScore);
        double pDraw = calculateLiveDraw(residualMatrix, hScore, aScore);
        double pAway = calculateLiveAwayWin(residualMatrix, hScore, aScore);

        MarketPrediction finalHomeWin = createPrediction(MarketType.MATCH_ODDS, "1", pHome, commission, oddsIndex);
        MarketPrediction finalDraw = createPrediction(MarketType.MATCH_ODDS, "X", pDraw, commission, oddsIndex);
        MarketPrediction finalAwayWin = createPrediction(MarketType.MATCH_ODDS, "2", pAway, commission, oddsIndex);

        // 4b. Both Teams to Score (BTTS)
        double pBttsYes = calculateLiveBttsYes(residualMatrix, hScore, aScore);
        double pBttsNo = clampProbability(1.0 - pBttsYes);

        MarketPrediction bttsYes = createPrediction(MarketType.BTTS, "YES", pBttsYes, commission, oddsIndex);
        MarketPrediction bttsNo = createPrediction(MarketType.BTTS, "NO", pBttsNo, commission, oddsIndex);

        // 4c. Under / Over Goals (0.5 to 4.5)
        List<MarketPrediction> uoPredictions = new ArrayList<>();
        for (int i = 0; i < UNDER_OVER_THRESHOLDS.length; i++) {
            double threshold = UNDER_OVER_THRESHOLDS[i];
            MarketType marketType = UNDER_OVER_MARKET_TYPES[i];

            double pUnder = calculateLiveUnder(residualMatrix, hScore, aScore, threshold);
            double pOver = clampProbability(1.0 - pUnder);

            uoPredictions.add(createPrediction(marketType, "UNDER", pUnder, commission, oddsIndex));
            uoPredictions.add(createPrediction(marketType, "OVER", pOver, commission, oddsIndex));
        }

        // 5. Evaluate Green-Up profit threshold target
        boolean greenUpTargetMet = evaluateGreenUpTarget(
                query.entryOdds(),
                query.greenUpProfitTarget(),
                query.currentLiveOdds()
        );

        return new LiveAnalysisResult(
                query.currentMinute(),
                hScore,
                aScore,
                lambdaRes,
                muRes,
                residualMatrix,
                finalHomeWin,
                finalDraw,
                finalAwayWin,
                List.copyOf(uoPredictions),
                bttsYes,
                bttsNo,
                greenUpTargetMet
        );
    }

    // --- Dynamic Scoreline Aggregation Helpers ---

    private static double calculateLiveHomeWin(double[][] matrix, int currentHomeScore, int currentAwayScore) {
        double p = 0.0;
        for (int addH = 0; addH < matrix.length; addH++) {
            for (int addA = 0; addA < matrix[addH].length; addA++) {
                if ((currentHomeScore + addH) > (currentAwayScore + addA)) {
                    p += matrix[addH][addA];
                }
            }
        }
        return clampProbability(p);
    }

    private static double calculateLiveDraw(double[][] matrix, int currentHomeScore, int currentAwayScore) {
        double p = 0.0;
        for (int addH = 0; addH < matrix.length; addH++) {
            for (int addA = 0; addA < matrix[addH].length; addA++) {
                if ((currentHomeScore + addH) == (currentAwayScore + addA)) {
                    p += matrix[addH][addA];
                }
            }
        }
        return clampProbability(p);
    }

    private static double calculateLiveAwayWin(double[][] matrix, int currentHomeScore, int currentAwayScore) {
        double p = 0.0;
        for (int addH = 0; addH < matrix.length; addH++) {
            for (int addA = 0; addA < matrix[addH].length; addA++) {
                if ((currentHomeScore + addH) < (currentAwayScore + addA)) {
                    p += matrix[addH][addA];
                }
            }
        }
        return clampProbability(p);
    }

    private static double calculateLiveBttsYes(double[][] matrix, int currentHomeScore, int currentAwayScore) {
        double p = 0.0;
        for (int addH = 0; addH < matrix.length; addH++) {
            for (int addA = 0; addA < matrix[addH].length; addA++) {
                if ((currentHomeScore + addH >= 1) && (currentAwayScore + addA >= 1)) {
                    p += matrix[addH][addA];
                }
            }
        }
        return clampProbability(p);
    }

    private static double calculateLiveUnder(double[][] matrix, int currentHomeScore, int currentAwayScore, double threshold) {
        double p = 0.0;
        for (int addH = 0; addH < matrix.length; addH++) {
            for (int addA = 0; addA < matrix[addH].length; addA++) {
                if ((currentHomeScore + currentAwayScore + addH + addA) < threshold) {
                    p += matrix[addH][addA];
                }
            }
        }
        return clampProbability(p);
    }

    // --- Green-Up Evaluation Helper ---

    private static boolean evaluateGreenUpTarget(Double entryOdds, double profitTarget, List<MarketOdds> liveOddsList) {
        if (entryOdds == null || entryOdds <= 1.0 || liveOddsList == null || liveOddsList.isEmpty()) {
            return false;
        }

        for (MarketOdds odds : liveOddsList) {
            if (odds != null && odds.getLayOdds() != null && odds.getLayOdds() > 1.0) {
                // Hedging Profit Ratio = (Entry Back Odds - Current Lay Odds) / Current Lay Odds
                double profitRatio = (entryOdds - odds.getLayOdds()) / odds.getLayOdds();
                if (profitRatio >= profitTarget) {
                    return true;
                }
            }
        }
        return false;
    }

    // --- Prediction Assembly & Market Indexing ---

    private MarketPrediction createPrediction(MarketType marketType,
                                              String outcome,
                                              double probability,
                                              double commissionRate,
                                              Map<String, MarketOdds> oddsIndex) {
        double fairOdds = PoissonModel.calculateFairOdds(probability);
        String key = buildOddsKey(marketType, outcome);
        MarketOdds odds = oddsIndex.get(key);

        EvEvaluation ev = null;
        if (odds != null) {
            ev = EvCalculator.evaluate(probability, odds.getBackOdds(), odds.getLayOdds(), commissionRate);
        }

        return new MarketPrediction(outcome, probability, fairOdds, ev);
    }

    private Map<String, MarketOdds> indexMarketOdds(List<MarketOdds> oddsList) {
        if (oddsList == null || oddsList.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, MarketOdds> map = new HashMap<>();
        for (MarketOdds odds : oddsList) {
            if (odds != null && odds.getMarketType() != null && odds.getOutcome() != null) {
                map.put(buildOddsKey(odds.getMarketType(), odds.getOutcome()), odds);
            }
        }
        return map;
    }

    private static String buildOddsKey(MarketType marketType, String outcome) {
        return marketType.name() + ":" + outcome.trim().toUpperCase();
    }

    private static double clampProbability(double p) {
        if (p < 0.0) return 0.0;
        if (p > 1.0) return 1.0;
        return p;
    }
}
