package org.nepe.match.adapter.out;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for the read-only {@link MatchDetailsJpaEntity} view projection.
 * <p>
 * Internal data access interface for the Dashboard and palinsesto query operations.
 */
@Repository
public interface SpringDataMatchDetailsRepository extends JpaRepository<MatchDetailsJpaEntity, Integer> {

    /**
     * Retrieves all denormalized match details for a specific competition and season, ordered chronologically.
     */
    List<MatchDetailsJpaEntity> findByCompetitionIdAndSeasonIdOrderByMatchDateTimeAsc(
            Integer competitionId,
            Integer seasonId
    );

    /**
     * Retrieves all match details for a specific competition, season, and lifecycle state.
     * Used to filter the Dashboard by tab (SCHEDULED, LIVE, FINISHED).
     */
    List<MatchDetailsJpaEntity> findByCompetitionIdAndSeasonIdAndMatchStateOrderByMatchDateTimeAsc(
            Integer competitionId,
            Integer seasonId,
            String matchState
    );

    /**
     * Retrieves all match details across all leagues and seasons, ordered chronologically.
     */
    List<MatchDetailsJpaEntity> findAllByOrderByMatchDateTimeAsc();
}
