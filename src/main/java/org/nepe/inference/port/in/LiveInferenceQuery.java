package org.nepe.inference.port.in;

import org.nepe.match.domain.MarketOdds;
import org.nepe.match.domain.MatchModifiers;

import java.util.Collections;
import java.util.List;

/**
 * Inbound Query DTO encapsulating all dynamic state and in-game parameters required
 * to compute real-time live match inferences and trading signals.
 *
 * @param lambdaHomePre       established pre-match expected goal rate for the Home team
 * @param muAwayPre           established pre-match expected goal rate for the Away team
 * @param currentMinute       current match minute (0 to 130)
 * @param currentHomeScore    current goals scored by Home team
 * @param currentAwayScore    current goals scored by Away team
 * @param homeRedCards        number of red cards issued to Home team
 * @param awayRedCards        number of red cards issued to Away team
 * @param modifiers           tactical modifiers (must-win motivation, mutual low urgency)
 * @param dixonColesRho       competition dependence coefficient
 * @param commissionRate      Betting Exchange commission rate (e.g., 0.05)
 * @param greenUpProfitTarget profit percentage threshold for Green-Up notifications (e.g., 0.10)
 * @param currentLiveOdds     current live market Back/Lay odds (nullable or empty)
 * @param entryOdds           initial opening position odds (nullable, used for Green-Up profit evaluation)
 * @param entryMarketType     market type of the initial position (default: MATCH_ODDS)
 * @param entryOutcome        outcome of the initial position (default: "1")
 */
public record LiveInferenceQuery(
        double lambdaHomePre,
        double muAwayPre,
        int currentMinute,
        int currentHomeScore,
        int currentAwayScore,
        int homeRedCards,
        int awayRedCards,
        MatchModifiers modifiers,
        double dixonColesRho,
        double commissionRate,
        double greenUpProfitTarget,
        List<MarketOdds> currentLiveOdds,
        Double entryOdds,
        org.nepe.match.domain.MarketType entryMarketType,
        String entryOutcome
) {

    public LiveInferenceQuery {
        modifiers = (modifiers != null) ? modifiers : MatchModifiers.defaultModifiers();
        currentLiveOdds = (currentLiveOdds != null) ? List.copyOf(currentLiveOdds) : Collections.emptyList();
        entryMarketType = (entryMarketType != null) ? entryMarketType : org.nepe.match.domain.MarketType.MATCH_ODDS;
        entryOutcome = (entryOutcome != null && !entryOutcome.isBlank()) ? entryOutcome.trim().toUpperCase() : "1";
    }

    /**
     * Backward-compatible constructor defaulting entry market to 1X2 Match Odds on Home ("1").
     */
    public LiveInferenceQuery(
            double lambdaHomePre,
            double muAwayPre,
            int currentMinute,
            int currentHomeScore,
            int currentAwayScore,
            int homeRedCards,
            int awayRedCards,
            MatchModifiers modifiers,
            double dixonColesRho,
            double commissionRate,
            double greenUpProfitTarget,
            List<MarketOdds> currentLiveOdds,
            Double entryOdds
    ) {
        this(
                lambdaHomePre,
                muAwayPre,
                currentMinute,
                currentHomeScore,
                currentAwayScore,
                homeRedCards,
                awayRedCards,
                modifiers,
                dixonColesRho,
                commissionRate,
                greenUpProfitTarget,
                currentLiveOdds,
                entryOdds,
                org.nepe.match.domain.MarketType.MATCH_ODDS,
                "1"
        );
    }
}
