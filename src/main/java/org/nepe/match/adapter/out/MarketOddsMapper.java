package org.nepe.match.adapter.out;

import org.nepe.match.domain.MarketOdds;
import org.nepe.match.domain.MarketType;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Bidirectional mapper between {@link MarketOdds} domain entities
 * and {@link MarketOddsJpaEntity} database entities.
 */
@Component
public class MarketOddsMapper {

    /**
     * Converts a pure {@link MarketOdds} domain model into a {@link MarketOddsJpaEntity}.
     *
     * @param domain the domain model (nullable)
     * @return the corresponding JPA entity, or {@code null} if input is null
     */
    public MarketOddsJpaEntity toJpa(MarketOdds domain) {
        if (domain == null) {
            return null;
        }

        return new MarketOddsJpaEntity(
                domain.getId(),
                domain.getMatchId(),
                domain.getMarketType().name(),
                domain.getOutcome(),
                domain.getBackOdds(),
                domain.getLayOdds()
        );
    }

    /**
     * Converts a {@link MarketOddsJpaEntity} loaded from MariaDB into a pure {@link MarketOdds} domain entity.
     *
     * @param jpa the JPA entity (nullable)
     * @return the reconstructed domain entity, or {@code null} if input is null
     */
    public MarketOdds toDomain(MarketOddsJpaEntity jpa) {
        if (jpa == null) {
            return null;
        }

        return new MarketOdds(
                jpa.getId(),
                jpa.getMatchId(),
                MarketType.fromString(jpa.getMarketType()),
                jpa.getOutcome(),
                jpa.getBackOdds(),
                jpa.getLayOdds()
        );
    }

    /**
     * Converts a list of JPA entities to a list of pure domain models.
     *
     * @param jpaList the list of JPA entities
     * @return unmodifiable list of domain models, or empty list if input is null/empty
     */
    public List<MarketOdds> toDomainList(List<MarketOddsJpaEntity> jpaList) {
        if (jpaList == null || jpaList.isEmpty()) {
            return Collections.emptyList();
        }

        return jpaList.stream()
                .map(this::toDomain)
                .toList();
    }
}
