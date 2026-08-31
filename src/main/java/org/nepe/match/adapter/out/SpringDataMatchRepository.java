package org.nepe.match.adapter.out;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link MatchJpaEntity}.
 * <p>
 * Internal data access interface supporting multi-season queries, upsert business-key lookups,
 * and historical finished match retrieval for Poisson/xG calculations.
 */
@Repository
public interface SpringDataMatchRepository extends JpaRepository<MatchJpaEntity, Integer> {

    /**
     * Finds a match by its composite business key (home team, away team, exact kickoff datetime).
     * Used during repeated CSV ingestion to identify existing matches without duplication.
     */
    Optional<MatchJpaEntity> findByHomeTeamIdAndAwayTeamIdAndMatchDateTime(
            Integer homeTeamId,
            Integer awayTeamId,
            Instant matchDateTime
    );

    /**
     * Finds a match by its composite business key within a given date range (same UTC calendar day).
     */
    @Query("""
            SELECT m FROM MatchJpaEntity m
            WHERE m.homeTeamId = :homeTeamId
              AND m.awayTeamId = :awayTeamId
              AND m.matchDateTime >= :startOfDay
              AND m.matchDateTime <= :endOfDay
            """)
    Optional<MatchJpaEntity> findByTeamsAndDateRange(
            @Param("homeTeamId") Integer homeTeamId,
            @Param("awayTeamId") Integer awayTeamId,
            @Param("startOfDay") Instant startOfDay,
            @Param("endOfDay") Instant endOfDay
    );

    /**
     * Retrieves all matches belonging to a specific competition and season, ordered chronologically.
     */
    List<MatchJpaEntity> findByCompetitionIdAndSeasonIdOrderByMatchDateTimeAsc(
            Integer competitionId,
            Integer seasonId
    );

    /**
     * Retrieves finished matches for a specific team in a specific season, ordered newest first.
     */
    @Query("""
            SELECT m FROM MatchJpaEntity m
            WHERE m.competitionId = :competitionId
              AND m.seasonId = :seasonId
              AND m.state = 'FINISHED'
              AND (m.homeTeamId = :teamId OR m.awayTeamId = :teamId)
            ORDER BY m.matchDateTime DESC
            """)
    List<MatchJpaEntity> findFinishedMatchesForTeamInSeason(
            @Param("teamId") Integer teamId,
            @Param("competitionId") Integer competitionId,
            @Param("seasonId") Integer seasonId
    );

    /**
     * Retrieves recent finished matches for a specific team within a competition, ordered newest first.
     * Uses JPA Pageable to limit the sample size to N matches.
     */
    @Query("""
            SELECT m FROM MatchJpaEntity m
            WHERE m.competitionId = :competitionId
              AND m.state = 'FINISHED'
              AND (m.homeTeamId = :teamId OR m.awayTeamId = :teamId)
            ORDER BY m.matchDateTime DESC
            """)
    List<MatchJpaEntity> findRecentFinishedMatchesForTeam(
            @Param("teamId") Integer teamId,
            @Param("competitionId") Integer competitionId,
            Pageable pageable
    );

    /**
     * Retrieves all matches ordered chronologically by kickoff date/time.
     */
    List<MatchJpaEntity> findAllByOrderByMatchDateTimeAsc();
}
