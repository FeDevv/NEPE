package org.nepe.competition.port.out;

import org.nepe.competition.domain.Season;

import java.util.List;
import java.util.Optional;

/**
 * Outbound Port (Driven Port / SPI) defining persistence operations for {@link Season} domain entities.
 * <p>
 * Supports seasonal partitioning, multi-season buffer queries, and chronological navigation.
 */
public interface SeasonRepositoryPort {

    /**
     * Persists a season entity (inserting if new, or updating if existing).
     *
     * @param season the {@link Season} entity to save (must not be null)
     * @return the saved {@link Season} instance with populated ID
     */
    Season save(Season season);

    /**
     * Finds a season by its unique surrogate database ID.
     *
     * @param id primary key identifier
     * @return an {@link Optional} containing the found season, or empty if not found
     */
    Optional<Season> findById(int id);

    /**
     * Finds a season by its formatted name (e.g., "2025/2026").
     *
     * @param name formatted season name
     * @return an {@link Optional} containing the found season, or empty if not found
     */
    Optional<Season> findByName(String name);

    /**
     * Retrieves the most recent (chronologically latest) season registered in the database.
     *
     * @return an {@link Optional} containing the latest season, or empty if no seasons exist
     */
    Optional<Season> findLatest();

    /**
     * Retrieves all seasons registered in the database, ordered chronologically (newest first).
     *
     * @return list of all {@link Season} entities
     */
    List<Season> findAll();

    /**
     * Checks if a season exists with the given name.
     *
     * @param name formatted season name
     * @return true if season exists, false otherwise
     */
    boolean existsByName(String name);

    /**
     * Deletes a season entity by its database ID.
     *
     * @param id primary key identifier
     */
    void deleteById(int id);

    /**
     * Returns the total count of registered seasons.
     *
     * @return total count
     */
    long count();
}
