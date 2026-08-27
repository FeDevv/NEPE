package org.nepe.competition.adapter.out;

import org.nepe.competition.domain.Season;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Bidirectional mapper between the pure domain entity {@link Season}
 * and the relational persistence entity {@link SeasonJpaEntity}.
 * <p>
 * Ensures strict decoupling between domain calendar logic and database storage.
 */
@Component
public class SeasonMapper {

    /**
     * Converts a pure {@link Season} domain entity into a {@link SeasonJpaEntity}.
     *
     * @param domain the domain model (nullable)
     * @return the corresponding JPA entity, or {@code null} if input is null
     */
    public SeasonJpaEntity toJpa(Season domain) {
        if (domain == null) {
            return null;
        }

        return new SeasonJpaEntity(
                domain.getId(),
                domain.getName()
        );
    }

    /**
     * Converts a {@link SeasonJpaEntity} loaded from MariaDB into a pure {@link Season} domain model.
     *
     * @param jpa the JPA entity (nullable)
     * @return the reconstructed domain entity, or {@code null} if input is null
     */
    public Season toDomain(SeasonJpaEntity jpa) {
        if (jpa == null) {
            return null;
        }

        return new Season(
                jpa.getId(),
                jpa.getName()
        );
    }

    /**
     * Converts a list of JPA entities to a list of pure domain models.
     *
     * @param jpaList the list of JPA entities
     * @return unmodifiable list of domain models, or empty list if input is null/empty
     */
    public List<Season> toDomainList(List<SeasonJpaEntity> jpaList) {
        if (jpaList == null || jpaList.isEmpty()) {
            return Collections.emptyList();
        }

        return jpaList.stream()
                .map(this::toDomain)
                .toList();
    }
}
