package org.nepe.competition.adapter.out;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link SeasonJpaEntity}.
 * <p>
 * Internal data access interface within the outbound persistence adapter for managing
 * sports seasons and retrieving chronological records.
 */
@Repository
public interface SpringDataSeasonRepository extends JpaRepository<SeasonJpaEntity, Integer> {

    /**
     * Finds a season by its formatted name (e.g., "2025/2026").
     *
     * @param name formatted season name
     * @return an {@link Optional} containing the JPA entity if found
     */
    Optional<SeasonJpaEntity> findByName(String name);

    /**
     * Retrieves the most recent season (highest year range) registered in the database.
     *
     * @return an {@link Optional} containing the latest season entity
     */
    Optional<SeasonJpaEntity> findFirstByOrderByNameDesc();

    /**
     * Retrieves all seasons ordered chronologically in descending order (newest first).
     *
     * @return list of seasons sorted from newest to oldest
     */
    List<SeasonJpaEntity> findAllByOrderByNameDesc();

    /**
     * Checks whether a season exists with the specified name.
     *
     * @param name formatted season name to check
     * @return {@code true} if a record exists with this name, {@code false} otherwise
     */
    boolean existsByName(String name);
}
