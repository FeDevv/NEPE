package org.nepe.inference.service;

import org.nepe.inference.domain.DixonColesModel;
import org.nepe.inference.domain.EvCalculator;
import org.nepe.inference.domain.EvCalculator.EvEvaluation;
import org.nepe.inference.domain.PoissonModel;
import org.nepe.inference.domain.TeamStrengthCalculator;
import org.nepe.inference.domain.TeamStrengthCalculator.PreMatchRates;
import org.nepe.inference.domain.TeamStrengthCalculator.TeamStrength;
import org.nepe.inference.port.in.CalculatePreMatchInferenceUseCase;
import org.nepe.inference.port.in.MarketPrediction;
import org.nepe.inference.port.in.PreMatchAnalysisResult;
import org.nepe.inference.port.in.PreMatchInferenceQuery;
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
 * Application Service implementing {@link CalculatePreMatchInferenceUseCase}.
 * <p>
 * Orchestrates the full pre-match quantitative pipeline:
 * 1. Team strength rating calculation with recency weighting and inter-season gamma decay.
 * 2. Pre-match expected goal rates generation (lambda / mu) with tactical modifiers.
 * 3. Dixon-Coles bivariate low-score adjusted joint probability matrix calculation.
 * 4. Market aggregation (1X2, Under/Over 0.5-4.5, BTTS) and Betting Exchange Expected Value (EV) evaluation.
 */
@Service
public class PreMatchInferenceService implements CalculatePreMatchInferenceUseCase {

    private static final double[] UNDER_OVER_THRESHOLDS = {0.5, 1.5, 2.5, 3.5, 4.5};
    private static final MarketType[] UNDER_OVER_MARKET_TYPES = {
            MarketType.UNDER_OVER_05,
            MarketType.UNDER_OVER_15,
            MarketType.UNDER_OVER_25,
            MarketType.UNDER_OVER_35,
            MarketType.UNDER_OVER_45
    };

    @Override
    public PreMatchAnalysisResult calculate(PreMatchInferenceQuery query) {
        if (query == null) {
            throw new DomainValidationException("PreMatchInferenceQuery cannot be null.");
        }

        // 1. Calculate relative offensive (alpha) and defensive (beta) strengths
        TeamStrength homeStrength = TeamStrengthCalculator.calculateStrength(
                query.homeHistoricalMatches(),
                query.leagueAvgXgPerTeam(),
                query.seasonalDecayGamma()
        );

        TeamStrength awayStrength = TeamStrengthCalculator.calculateStrength(
                query.awayHistoricalMatches(),
                query.leagueAvgXgPerTeam(),
                query.seasonalDecayGamma()
        );

        // 2. Generate expected goal rates applying home advantage and tactical modifiers
        PreMatchRates rates = TeamStrengthCalculator.calculatePreMatchRates(
                homeStrength,
                awayStrength,
                query.leagueAvgXgPerTeam(),
                query.homeAdvantageRatio(),
                query.modifiers()
        );

        // 3. Compute scoreline matrix, market predictions, and Expected Values
        return buildResult(
                rates.lambdaHome(),
                rates.muAway(),
                rates.effectiveHomeAdvantage(),
                query.dixonColesRho(),
                query.commissionRate(),
                query.marketOddsList()
        );
    }

    @Override
    public PreMatchAnalysisResult calculate(double lambdaHome,
                                           double muAway,
                                           double dixonColesRho,
                                           double commissionRate,
                                           List<MarketOdds> marketOddsList) {
        // Direct rate evaluation: home advantage is assumed already factored into lambdaHome/muAway
        double effectiveHomeAdv = 1.0;
        return buildResult(lambdaHome, muAway, effectiveHomeAdv, dixonColesRho, commissionRate, marketOddsList);
    }

    // --- Core Result Assembly ---

    private PreMatchAnalysisResult buildResult(double lambdaHome,
                                               double muAway,
                                               double effectiveHomeAdv,
                                               double dixonColesRho,
                                               double commissionRate,
                                               List<MarketOdds> marketOddsList) {
        // 1. Calculate Dixon-Coles corrected score matrix
        double[][] scoreMatrix = DixonColesModel.calculateScoreMatrix(lambdaHome, muAway, dixonColesRho);

        // 2. Index market odds for O(1) lookup
        Map<String, MarketOdds> oddsIndex = indexMarketOdds(marketOddsList);

        // 3. Match Odds (1X2)
        double pHome = DixonColesModel.calculateHomeWinProbability(scoreMatrix);
        double pDraw = DixonColesModel.calculateDrawProbability(scoreMatrix);
        double pAway = DixonColesModel.calculateAwayWinProbability(scoreMatrix);

        MarketPrediction homeWin = createPrediction(MarketType.MATCH_ODDS, "1", pHome, commissionRate, oddsIndex);
        MarketPrediction draw = createPrediction(MarketType.MATCH_ODDS, "X", pDraw, commissionRate, oddsIndex);
        MarketPrediction awayWin = createPrediction(MarketType.MATCH_ODDS, "2", pAway, commissionRate, oddsIndex);

        // 4. Both Teams to Score (BTTS)
        double pBttsYes = DixonColesModel.calculateBttsYesProbability(scoreMatrix);
        double pBttsNo = DixonColesModel.calculateBttsNoProbability(scoreMatrix);

        MarketPrediction bttsYes = createPrediction(MarketType.BTTS, "YES", pBttsYes, commissionRate, oddsIndex);
        MarketPrediction bttsNo = createPrediction(MarketType.BTTS, "NO", pBttsNo, commissionRate, oddsIndex);

        // 5. Under / Over Thresholds (0.5 to 4.5)
        List<MarketPrediction> uoPredictions = new ArrayList<>();
        for (int i = 0; i < UNDER_OVER_THRESHOLDS.length; i++) {
            double threshold = UNDER_OVER_THRESHOLDS[i];
            MarketType marketType = UNDER_OVER_MARKET_TYPES[i];

            double pUnder = DixonColesModel.calculateUnderProbability(scoreMatrix, threshold);
            double pOver = DixonColesModel.calculateOverProbability(scoreMatrix, threshold);

            uoPredictions.add(createPrediction(marketType, "UNDER", pUnder, commissionRate, oddsIndex));
            uoPredictions.add(createPrediction(marketType, "OVER", pOver, commissionRate, oddsIndex));
        }

        return new PreMatchAnalysisResult(
                lambdaHome,
                muAway,
                effectiveHomeAdv,
                scoreMatrix,
                homeWin,
                draw,
                awayWin,
                List.copyOf(uoPredictions),
                bttsYes,
                bttsNo
        );
    }

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
}
