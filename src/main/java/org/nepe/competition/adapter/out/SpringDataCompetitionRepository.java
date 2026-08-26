package org.nepe.competition.adapter.out;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link CompetitionJpaEntity}.
 * <p>
 * This interface is an internal implementation detail of the outbound persistence adapter.
 * It is never exposed outside the {@code org.nepe.competition.adapter.out} package.
 */
@Repository
public interface SpringDataCompetitionRepository extends JpaRepository<CompetitionJpaEntity, Integer> {

    /**
     * Finds a competition by its unique code (case-insensitive or exact match).
     *
     * @param code unique competition code (e.g., "I1", "E0")
     * @return an {@link Optional} containing the JPA entity if found
     */
    Optional<CompetitionJpaEntity> findByCode(String code);

    /**
     * Checks if a competition exists with the specified code.
     *
     * @param code unique competition code to verify
     * @return {@code true} if a record exists with the given code, {@code false} otherwise
     */
    boolean existsByCode(String code);

    /**
     * Retrieves all competitions ordered alphabetically by their display name.
     *
     * @return list of competition entities sorted by name in ascending order
     */
    List<CompetitionJpaEntity> findAllByOrderByNameAsc();
}
