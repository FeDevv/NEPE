package org.nepe.competition.adapter.out;

import org.nepe.competition.domain.Team;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Bidirectional mapper between the pure domain entity {@link Team}
 * and the relational persistence entity {@link TeamJpaEntity}.
 * <p>
 * Ensures strict decoupling across the hexagonal boundary, keeping the domain core
 * free of any JPA metadata or database lifecycle concerns.
 */
@Component
public class TeamMapper {

    /**
     * Converts a pure {@link Team} domain entity into a {@link TeamJpaEntity}.
     *
     * @param domain the domain model (nullable)
     * @return the corresponding JPA entity, or {@code null} if input is null
     */
    public TeamJpaEntity toJpa(Team domain) {
        if (domain == null) {
            return null;
        }

        return new TeamJpaEntity(
                domain.getId(),
                domain.getName()
        );
    }

    /**
     * Converts a {@link TeamJpaEntity} loaded from the database into a pure {@link Team} domain model.
     *
     * @param jpa the JPA entity (nullable)
     * @return the reconstructed domain entity, or {@code null} if input is null
     */
    public Team toDomain(TeamJpaEntity jpa) {
        if (jpa == null) {
            return null;
        }

        return new Team(
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
    public List<Team> toDomainList(List<TeamJpaEntity> jpaList) {
        if (jpaList == null || jpaList.isEmpty()) {
            return Collections.emptyList();
        }

        return jpaList.stream()
                .map(this::toDomain)
                .toList();
    }
}
