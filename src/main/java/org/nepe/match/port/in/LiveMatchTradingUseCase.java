package org.nepe.match.port.in;

import org.nepe.match.domain.Match;
import org.nepe.match.domain.MatchEvent;
import org.nepe.match.domain.MatchModifiers;

import java.util.List;

/**
 * Inbound Port (Driving Port / Use Case) defining the business operations for real-time live match trading.
 * <p>
 * Controls state transitions (SCHEDULED -> LIVE -> FINISHED), minute advancement, event recording,
 * and event reversion (rollback).
 * Invoked by the JavaFX Live Console Controller.
 */
public interface LiveMatchTradingUseCase {

    /**
     * Starts live trading for a match, transitioning its state from SCHEDULED to LIVE.
     *
     * @param matchId primary key identifier
     * @return the updated live {@link Match}
     */
    Match startLiveTrading(int matchId);

    /**
     * Records an in-game event (e.g. Goal, Red Card), updates the match aggregate score/cards,
     * and persists the event record.
     *
     * @param command the {@link RecordMatchEventCommand} payload (must not be null)
     * @return the updated {@link Match}
     */
    Match recordEvent(RecordMatchEventCommand command);

    /**
     * Reverts (rolls back) the most recently recorded event for a match, adjusting scores/cards
     * backwards and deleting the event record from persistence.
     *
     * @param matchId primary key identifier
     * @return the updated {@link Match}
     */
    Match revertLastEvent(int matchId);

    /**
     * Updates the elapsed minute of the match.
     *
     * @param matchId       primary key identifier
     * @param currentMinute current elapsed minute (0 to 130)
     * @return the updated {@link Match}
     */
    Match updateLiveMinute(int matchId, int currentMinute);

    /**
     * Dynamically updates tactical modifiers during the live match (e.g. Must-Win motivation).
     *
     * @param matchId   primary key identifier
     * @param modifiers updated tactical modifiers
     * @return the updated {@link Match}
     */
    Match updateLiveModifiers(int matchId, MatchModifiers modifiers);

    /**
     * Concludes live trading and transitions the match to FINISHED state.
     *
     * @param matchId primary key identifier
     * @return the final {@link Match}
     */
    Match finishLiveMatch(int matchId);

    /**
     * Retrieves the chronological timeline of events recorded for the live match.
     *
     * @param matchId primary key identifier
     * @return list of {@link MatchEvent} instances ordered by minute
     */
    List<MatchEvent> getMatchEvents(int matchId);
}
