package org.nepe.competition.port.out;

import org.nepe.competition.domain.Team;

import java.util.List;
import java.util.Optional;

/**
 * Outbound Port (Driven Port / SPI) defining persistence operations for {@link Team} domain entities.
 * <p>
 * Decouples domain logic and use cases from database access mechanisms.
 */
public interface TeamRepositoryPort {

    /**
     * Persists a team entity (inserting if new, or updating if existing).
     *
     * @param team the {@link Team} entity to save (must not be null)
     * @return the saved {@link Team} instance with populated ID
     */
    Team save(Team team);

    /**
     * Finds a team by its unique surrogate database ID.
     *
     * @param id primary key identifier
     * @return an {@link Optional} containing the found team, or empty if not found
     */
    Optional<Team> findById(int id);

    /**
     * Finds a team by its official name (case-insensitive search).
     *
     * @param name official team name
     * @return an {@link Optional} containing the found team, or empty if not found
     */
    Optional<Team> findByName(String name);

    /**
     * Retrieves all teams registered in the database, ordered alphabetically by name.
     *
     * @return list of all {@link Team} entities
     */
    List<Team> findAll();

    /**
     * Searches teams whose name contains the specified query substring (case-insensitive).
     * Useful for UI autocomplete and search filters.
     *
     * @param query search substring
     * @return matching list of {@link Team} entities
     */
    List<Team> searchByName(String query);

    /**
     * Checks if a team exists with the given name (case-insensitive).
     *
     * @param name official team name
     * @return true if team exists, false otherwise
     */
    boolean existsByName(String name);

    /**
     * Deletes a team by its database ID.
     *
     * @param id primary key identifier
     */
    void deleteById(int id);

    /**
     * Returns the total count of registered teams.
     *
     * @return total count
     */
    long count();
}
