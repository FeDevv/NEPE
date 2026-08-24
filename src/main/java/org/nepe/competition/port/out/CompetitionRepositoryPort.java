package org.nepe.competition.port.out;

import org.nepe.competition.domain.Competition;

import java.util.List;
import java.util.Optional;

/**
 * Outbound Port (Driven Port / SPI) defining persistence operations for {@link Competition} domain entities.
 * <p>
 * Decouples domain logic from database access mechanisms (e.g. MariaDB / Spring Data JPA).
 */
public interface CompetitionRepositoryPort {

    /**
     * Persists a competition entity (inserting if new, or updating if existing).
     *
     * @param competition the {@link Competition} entity to save (must not be null)
     * @return the saved {@link Competition} instance with populated ID
     */
    Competition save(Competition competition);

    /**
     * Finds a competition by its unique surrogate database ID.
     *
     * @param id primary key identifier
     * @return an {@link Optional} containing the found competition, or empty if not found
     */
    Optional<Competition> findById(int id);

    /**
     * Finds a competition by its unique Football-Data code (e.g., "I1", "E0").
     *
     * @param code unique competition code
     * @return an {@link Optional} containing the found competition, or empty if not found
     */
    Optional<Competition> findByCode(String code);

    /**
     * Retrieves all competitions registered in the database, ordered by name.
     *
     * @return list of all {@link Competition} entities
     */
    List<Competition> findAll();

    /**
     * Checks if a competition exists with the given code.
     *
     * @param code competition code to check
     * @return true if a competition with the code exists, false otherwise
     */
    boolean existsByCode(String code);

    /**
     * Deletes a competition entity by its database ID.
     *
     * @param id primary key identifier
     */
    void deleteById(int id);

    /**
     * Returns the total count of registered competitions.
     *
     * @return total count
     */
    long count();
}
