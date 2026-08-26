package org.nepe.competition.adapter.out;

import org.nepe.competition.domain.Competition;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Bidirectional mapper between the pure domain entity {@link Competition}
 * and the relational persistence entity {@link CompetitionJpaEntity}.
 * <p>
 * This component ensures strict decoupling across the hexagonal boundary,
 * preventing persistence details or JPA lifecycle semantics from leaking into the domain.
 */
@Component
public class CompetitionMapper {

    /**
     * Converts a pure {@link Competition} domain model into a {@link CompetitionJpaEntity}.
     *
     * @param domain the domain model (nullable)
     * @return the corresponding JPA entity, or {@code null} if the input was null
     */
    public CompetitionJpaEntity toJpa(Competition domain) {
        if (domain == null) {
            return null;
        }

        return new CompetitionJpaEntity(
                domain.getId(),
                domain.getCode(),
                domain.getName(),
                domain.getCountry(),
                domain.getDixonColesRho()
        );
    }

    /**
     * Converts a {@link CompetitionJpaEntity} loaded from MariaDB into a pure {@link Competition} domain model.
     *
     * @param jpa the JPA entity (nullable)
     * @return the reconstructed domain entity, or {@code null} if the input was null
     */
    public Competition toDomain(CompetitionJpaEntity jpa) {
        if (jpa == null) {
            return null;
        }

        return new Competition(
                jpa.getId(),
                jpa.getCode(),
                jpa.getName(),
                jpa.getCountry(),
                jpa.getDixonColesRho()
        );
    }

    /**
     * Converts a list of JPA entities to a list of pure domain models.
     *
     * @param jpaList the list of JPA entities
     * @return unmodifiable list of domain models, or empty list if input is null/empty
     */
    public List<Competition> toDomainList(List<CompetitionJpaEntity> jpaList) {
        if (jpaList == null || jpaList.isEmpty()) {
            return Collections.emptyList();
        }

        return jpaList.stream()
                .map(this::toDomain)
                .toList();
    }
}
