package org.nepe.competition.adapter.out;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for {@link CompetitionTeamJpaEntity}.
 * <p>
 * Internal data access interface managing persistence operations for the
 * {@code competition_teams} association table within the outbound adapter layer.
 */
@Repository
public interface SpringDataCompetitionTeamRepository extends JpaRepository<CompetitionTeamJpaEntity, CompetitionTeamJpaEntity.CompetitionTeamId> {

    /**
     * Retrieves all association records for a given competition ID.
     *
     * @param competitionId competition database identifier
     * @return list of {@link CompetitionTeamJpaEntity} records
     */
    List<CompetitionTeamJpaEntity> findByCompetitionId(int competitionId);

    /**
     * Retrieves all association records for a given team ID.
     *
     * @param teamId team database identifier
     * @return list of {@link CompetitionTeamJpaEntity} records
     */
    List<CompetitionTeamJpaEntity> findByTeamId(int teamId);

    /**
     * Checks if a team is already associated with a competition.
     *
     * @param competitionId competition database identifier
     * @param teamId        team database identifier
     * @return {@code true} if the association exists, {@code false} otherwise
     */
    boolean existsByCompetitionIdAndTeamId(int competitionId, int teamId);

    /**
     * Deletes the association between a specific competition and team.
     *
     * @param competitionId competition database identifier
     * @param teamId        team database identifier
     */
    void deleteByCompetitionIdAndTeamId(int competitionId, int teamId);

    /**
     * Deletes all team associations for a specific competition.
     *
     * @param competitionId competition database identifier
     */
    void deleteByCompetitionId(int competitionId);

    /**
     * Deletes all competition associations for a specific team.
     *
     * @param teamId team database identifier
     */
    void deleteByTeamId(int teamId);
}
