package org.nepe.match.adapter.out;

import org.nepe.match.domain.MatchEvent;
import org.nepe.match.domain.MatchEventType;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Bidirectional mapper between {@link MatchEvent} domain entities
 * and {@link MatchEventJpaEntity} database entities.
 */
@Component
public class MatchEventMapper {

    /**
     * Converts a pure {@link MatchEvent} domain model into a {@link MatchEventJpaEntity}.
     *
     * @param domain the domain model (nullable)
     * @return the corresponding JPA entity, or {@code null} if input is null
     */
    public MatchEventJpaEntity toJpa(MatchEvent domain) {
        if (domain == null) {
            return null;
        }

        return new MatchEventJpaEntity(
                domain.getId(),
                domain.getMatchId(),
                domain.getEventType().name(),
                domain.getMinute(),
                domain.getCreatedAt()
        );
    }

    /**
     * Converts a {@link MatchEventJpaEntity} loaded from MariaDB into a pure {@link MatchEvent} domain entity.
     *
     * @param jpa the JPA entity (nullable)
     * @return the reconstructed domain entity, or {@code null} if input is null
     */
    public MatchEvent toDomain(MatchEventJpaEntity jpa) {
        if (jpa == null) {
            return null;
        }

        return new MatchEvent(
                jpa.getId(),
                jpa.getMatchId(),
                MatchEventType.fromString(jpa.getEventType()),
                jpa.getMinute(),
                jpa.getCreatedAt()
        );
    }

    /**
     * Converts a list of JPA entities to a list of pure domain events.
     *
     * @param jpaList the list of JPA entities
     * @return unmodifiable list of domain models, or empty list if input is null/empty
     */
    public List<MatchEvent> toDomainList(List<MatchEventJpaEntity> jpaList) {
        if (jpaList == null || jpaList.isEmpty()) {
            return Collections.emptyList();
        }

        return jpaList.stream()
                .map(this::toDomain)
                .toList();
    }
}
