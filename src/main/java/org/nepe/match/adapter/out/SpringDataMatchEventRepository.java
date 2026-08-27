package org.nepe.match.adapter.out;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link MatchEventJpaEntity}.
 * <p>
 * Internal data access interface for logging in-game events and retrieving event timelines.
 */
@Repository
public interface SpringDataMatchEventRepository extends JpaRepository<MatchEventJpaEntity, Integer> {

    /**
     * Retrieves all events recorded for a match, ordered chronologically by minute and timestamp.
     *
     * @param matchId identifier of the match
     * @return chronologically ordered list of match event entities
     */
    List<MatchEventJpaEntity> findByMatchIdOrderByMinuteAscCreatedAtAsc(Integer matchId);

    /**
     * Retrieves the most recent event recorded for a match.
     * Used to implement the "Undo Last Event" feature in the live console.
     *
     * @param matchId identifier of the match
     * @return an {@link Optional} containing the latest recorded event
     */
    Optional<MatchEventJpaEntity> findFirstByMatchIdOrderByCreatedAtDescIdDesc(Integer matchId);

    /**
     * Deletes all events associated with a specific match ID.
     *
     * @param matchId identifier of the match
     */
    void deleteByMatchId(Integer matchId);
}
