package org.nepe.competition.adapter.out;

import org.nepe.competition.domain.TeamAlias;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Bidirectional mapper between the pure domain entity {@link TeamAlias}
 * and the relational persistence entity {@link TeamAliasJpaEntity}.
 * <p>
 * Maintains the hexagonal decoupling between the domain layer and MariaDB storage.
 */
@Component
public class TeamAliasMapper {

    /**
     * Converts a pure {@link TeamAlias} domain entity into a {@link TeamAliasJpaEntity}.
     *
     * @param domain the domain model (nullable)
     * @return the corresponding JPA entity, or {@code null} if input is null
     */
    public TeamAliasJpaEntity toJpa(TeamAlias domain) {
        if (domain == null) {
            return null;
        }

        return new TeamAliasJpaEntity(
                domain.getId(),
                domain.getAliasName(),
                domain.getTeamId()
        );
    }

    /**
     * Converts a {@link TeamAliasJpaEntity} loaded from the database into a pure {@link TeamAlias} domain model.
     *
     * @param jpa the JPA entity (nullable)
     * @return the reconstructed domain entity, or {@code null} if input is null
     */
    public TeamAlias toDomain(TeamAliasJpaEntity jpa) {
        if (jpa == null) {
            return null;
        }

        return new TeamAlias(
                jpa.getId(),
                jpa.getAliasName(),
                jpa.getTeamId()
        );
    }

    /**
     * Converts a list of JPA entities to a list of pure domain models.
     *
     * @param jpaList the list of JPA entities
     * @return unmodifiable list of domain models, or empty list if input is null/empty
     */
    public List<TeamAlias> toDomainList(List<TeamAliasJpaEntity> jpaList) {
        if (jpaList == null || jpaList.isEmpty()) {
            return Collections.emptyList();
        }

        return jpaList.stream()
                .map(this::toDomain)
                .toList();
    }
}
