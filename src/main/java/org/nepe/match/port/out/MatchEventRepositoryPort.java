package org.nepe.match.port.out;

import org.nepe.match.domain.MatchEvent;

import java.util.List;
import java.util.Optional;

/**
 * Outbound Port (Driven Port / SPI) defining persistence operations for in-game {@link MatchEvent} entities.
 * <p>
 * Supports event logging, timeline retrieval, and event rollback during live match trading.
 */
public interface MatchEventRepositoryPort {

    /**
     * Persists an in-game match event (e.g. Goal, Red Card).
     *
     * @param event the {@link MatchEvent} entity to save (must not be null)
     * @return the saved {@link MatchEvent} instance with populated ID
     */
    MatchEvent save(MatchEvent event);

    /**
     * Finds a match event by its primary key ID.
     *
     * @param id primary key identifier
     * @return an {@link Optional} containing the event, or empty if not found
     */
    Optional<MatchEvent> findById(int id);

    /**
     * Retrieves all events recorded for a specific match, ordered chronologically by minute and timestamp.
     *
     * @param matchId target match identifier
     * @return chronologically ordered list of {@link MatchEvent} instances
     */
    List<MatchEvent> findByMatchIdOrderByMinuteAsc(int matchId);

    /**
     * Retrieves the most recently recorded event for a specific match.
     * Essential for the "Undo Last Event" (Rollback) feature in the live console.
     *
     * @param matchId target match identifier
     * @return an {@link Optional} containing the latest event, or empty if no events exist
     */
    Optional<MatchEvent> findLatestEventByMatchId(int matchId);

    /**
     * Deletes a match event by its database ID.
     *
     * @param id primary key identifier
     */
    void deleteById(int id);

    /**
     * Deletes all events associated with a match.
     *
     * @param matchId target match identifier
     */
    void deleteByMatchId(int matchId);

    /**
     * Returns total count of recorded match events.
     *
     * @return total count
     */
    long count();
}
