package org.nepe.match.port.in;

import org.nepe.match.domain.MatchEventType;

/**
 * Inbound Command DTO for recording an in-game match event (e.g. Goal, Red Card) during live trading.
 *
 * @param matchId   database identifier of the live match
 * @param eventType the type of event (GOAL_HOME, GOAL_AWAY, RED_CARD_HOME, RED_CARD_AWAY)
 * @param minute    elapsed match minute when the event occurred (0 to 130)
 */
public record RecordMatchEventCommand(
        int matchId,
        MatchEventType eventType,
        int minute
) {
}
