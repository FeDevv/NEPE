package org.nepe.competition.adapter.out;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link TeamJpaEntity}.
 * <p>
 * Internal data access interface within the outbound persistence adapter.
 * Supports case-insensitive name lookups, substring autocomplete queries, and sorted listings.
 */
@Repository
public interface SpringDataTeamRepository extends JpaRepository<TeamJpaEntity, Integer> {

    /**
     * Finds a team by its exact official name, ignoring case.
     *
     * @param name official team name to search for
     * @return an {@link Optional} containing the found JPA entity if present
     */
    Optional<TeamJpaEntity> findByNameIgnoreCase(String name);

    /**
     * Checks whether a team already exists with the specified name, ignoring case.
     *
     * @param name official team name to check
     * @return {@code true} if a team exists with this name, {@code false} otherwise
     */
    boolean existsByNameIgnoreCase(String name);

    /**
     * Retrieves all teams ordered alphabetically by name.
     *
     * @return list of all teams sorted by name in ascending order
     */
    List<TeamJpaEntity> findAllByOrderByNameAsc();

    /**
     * Searches teams whose name contains the specified substring (case-insensitive), ordered alphabetically.
     * Used to power UI autocomplete inputs and search filters.
     *
     * @param query search substring
     * @return list of matching teams ordered by name
     */
    List<TeamJpaEntity> findByNameContainingIgnoreCaseOrderByNameAsc(String query);

    /**
     * Retrieves all teams associated with a specific competition via the competition_teams junction table,
     * ordered alphabetically by name in ascending order.
     *
     * @param competitionId competition database identifier
     * @return list of matching teams ordered by name
     */
    @Query("SELECT t FROM TeamJpaEntity t JOIN CompetitionTeamJpaEntity ct ON t.id = ct.teamId WHERE ct.competitionId = :competitionId ORDER BY t.name ASC")
    List<TeamJpaEntity> findByCompetitionId(@Param("competitionId") int competitionId);
}
