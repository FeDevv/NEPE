package org.nepe.competition.port.out;

import org.nepe.competition.domain.TeamAlias;

import java.util.List;
import java.util.Optional;

/**
 * Outbound Port (Driven Port / SPI) defining persistence operations for {@link TeamAlias} domain entities.
 * <p>
 * Supports name normalization, CSV ingestion mapping, and alias resolution.
 */
public interface TeamAliasRepositoryPort {

    /**
     * Persists an alias mapping entity (inserting if new, or updating if existing).
     *
     * @param teamAlias the {@link TeamAlias} entity to save (must not be null)
     * @return the saved {@link TeamAlias} instance with populated ID
     */
    TeamAlias save(TeamAlias teamAlias);

    /**
     * Finds an alias by its unique surrogate database ID.
     *
     * @param id primary key identifier
     * @return an {@link Optional} containing the found alias, or empty if not found
     */
    Optional<TeamAlias> findById(int id);

    /**
     * Finds an alias by its raw alias name (case-insensitive search).
     *
     * @param aliasName raw name to resolve
     * @return an {@link Optional} containing the found alias, or empty if not found
     */
    Optional<TeamAlias> findByAliasName(String aliasName);

    /**
     * Retrieves all alias mappings that point to a specific team.
     *
     * @param teamId target team primary key
     * @return list of {@link TeamAlias} entities associated with the team
     */
    List<TeamAlias> findByTeamId(int teamId);

    /**
     * Retrieves all alias mappings registered in the database, ordered alphabetically by alias name.
     *
     * @return list of all {@link TeamAlias} entities
     */
    List<TeamAlias> findAll();

    /**
     * Checks if an alias mapping exists with the given raw name (case-insensitive).
     *
     * @param aliasName raw alias name
     * @return true if alias is already mapped, false otherwise
     */
    boolean existsByAliasName(String aliasName);

    /**
     * Deletes an alias mapping by its database ID.
     *
     * @param id primary key identifier
     */
    void deleteById(int id);

    /**
     * Deletes all alias mappings associated with a specific team ID.
     *
     * @param teamId target team primary key
     */
    void deleteByTeamId(int teamId);

    /**
     * Returns the total count of registered aliases.
     *
     * @return total count
     */
    long count();
}
