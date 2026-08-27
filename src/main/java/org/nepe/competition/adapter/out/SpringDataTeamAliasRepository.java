package org.nepe.competition.adapter.out;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link TeamAliasJpaEntity}.
 * <p>
 * Internal data access interface within the outbound persistence adapter for managing
 * team alias mappings and resolving raw names from external data feeds.
 */
@Repository
public interface SpringDataTeamAliasRepository extends JpaRepository<TeamAliasJpaEntity, Integer> {

    /**
     * Finds an alias mapping by its exact alias name, ignoring case.
     *
     * @param aliasName raw name to look up
     * @return an {@link Optional} containing the JPA entity if present
     */
    Optional<TeamAliasJpaEntity> findByAliasNameIgnoreCase(String aliasName);

    /**
     * Checks whether an alias mapping exists with the specified name, ignoring case.
     *
     * @param aliasName raw name to check
     * @return {@code true} if an alias exists, {@code false} otherwise
     */
    boolean existsByAliasNameIgnoreCase(String aliasName);

    /**
     * Retrieves all aliases mapped to a specific team ID, ordered by alias name.
     *
     * @param teamId primary key of the target team
     * @return list of matching alias entities
     */
    List<TeamAliasJpaEntity> findByTeamIdOrderByAliasNameAsc(int teamId);

    /**
     * Retrieves all alias mappings in the system ordered alphabetically.
     *
     * @return list of all aliases sorted by name
     */
    List<TeamAliasJpaEntity> findAllByOrderByAliasNameAsc();

    /**
     * Deletes all alias mappings associated with a specific team ID.
     *
     * @param teamId primary key of the team whose aliases should be removed
     */
    void deleteByTeamId(int teamId);
}
