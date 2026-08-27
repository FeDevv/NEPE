package org.nepe.match.adapter.out;

import org.nepe.match.domain.Match;
import org.nepe.match.domain.MatchModifiers;
import org.nepe.match.domain.MatchState;
import org.nepe.match.domain.MatchStatistics;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Bidirectional mapper between the pure {@link Match} aggregate root
 * and the relational persistence entity {@link MatchJpaEntity}.
 * <p>
 * Unpacks and reassembles nested Value Objects ({@link MatchStatistics} and {@link MatchModifiers})
 * to maintain clean DDD domain models with a normalized relational DB schema.
 */
@Component
public class MatchMapper {

    /**
     * Converts a pure {@link Match} domain aggregate into a {@link MatchJpaEntity}.
     *
     * @param domain the domain model (nullable)
     * @return the corresponding JPA entity, or {@code null} if input is null
     */
    public MatchJpaEntity toJpa(Match domain) {
        if (domain == null) {
            return null;
        }

        MatchStatistics stats = domain.getStatistics();
        MatchModifiers mods = domain.getModifiers();

        return new MatchJpaEntity(
                domain.getId(),
                domain.getSeasonId(),
                domain.getCompetitionId(),
                domain.getHomeTeamId(),
                domain.getAwayTeamId(),
                domain.getMatchDateTime(),
                domain.getState().name(),
                domain.isManuallyEdited(),
                stats != null ? stats.getHomeScore() : null,
                stats != null ? stats.getAwayScore() : null,
                stats != null ? stats.getHomeShots() : null,
                stats != null ? stats.getAwayShots() : null,
                stats != null ? stats.getHomeShotsOnTarget() : null,
                stats != null ? stats.getAwayShotsOnTarget() : null,
                stats != null ? stats.getHomeRedCards() : 0,
                stats != null ? stats.getAwayRedCards() : 0,
                stats != null ? stats.getManualHomeXg() : null,
                stats != null ? stats.getManualAwayXg() : null,
                domain.getOddsHome(),
                domain.getOddsDraw(),
                domain.getOddsAway(),
                mods != null && mods.isNeutralVenue(),
                mods != null && mods.isMustWinHome(),
                mods != null && mods.isMustWinAway(),
                mods != null && mods.isLowUrgencyHome(),
                mods != null && mods.isLowUrgencyAway(),
                mods != null ? mods.getModAttHome() : MatchModifiers.DEFAULT_MULTIPLIER,
                mods != null ? mods.getModDefHome() : MatchModifiers.DEFAULT_MULTIPLIER,
                mods != null ? mods.getModAttAway() : MatchModifiers.DEFAULT_MULTIPLIER,
                mods != null ? mods.getModDefAway() : MatchModifiers.DEFAULT_MULTIPLIER,
                domain.getCurrentMinute()
        );
    }

    /**
     * Converts a {@link MatchJpaEntity} loaded from the database into a pure {@link Match} aggregate.
     *
     * @param jpa the JPA entity (nullable)
     * @return the reconstructed domain aggregate, or {@code null} if input is null
     */
    public Match toDomain(MatchJpaEntity jpa) {
        if (jpa == null) {
            return null;
        }

        MatchStatistics statistics = new MatchStatistics(
                jpa.getHomeScore(),
                jpa.getAwayScore(),
                jpa.getHomeShots(),
                jpa.getAwayShots(),
                jpa.getHomeShotsOnTarget(),
                jpa.getAwayShotsOnTarget(),
                jpa.getHomeRedCards(),
                jpa.getAwayRedCards(),
                jpa.getManualHomeXg(),
                jpa.getManualAwayXg()
        );

        MatchModifiers modifiers = new MatchModifiers(
                jpa.isNeutralVenue(),
                jpa.isMustWinHome(),
                jpa.isMustWinAway(),
                jpa.isLowUrgencyHome(),
                jpa.isLowUrgencyAway(),
                jpa.getModAttHome(),
                jpa.getModDefHome(),
                jpa.getModAttAway(),
                jpa.getModDefAway()
        );

        return new Match(
                jpa.getId(),
                jpa.getSeasonId(),
                jpa.getCompetitionId(),
                jpa.getHomeTeamId(),
                jpa.getAwayTeamId(),
                jpa.getMatchDateTime(),
                MatchState.fromString(jpa.getState()),
                jpa.isManuallyEdited(),
                statistics,
                modifiers,
                jpa.getOddsHome(),
                jpa.getOddsDraw(),
                jpa.getOddsAway(),
                jpa.getCurrentMinute()
        );
    }

    /**
     * Converts a list of JPA entities to a list of pure domain aggregates.
     *
     * @param jpaList the list of JPA entities
     * @return unmodifiable list of domain models, or empty list if input is null/empty
     */
    public List<Match> toDomainList(List<MatchJpaEntity> jpaList) {
        if (jpaList == null || jpaList.isEmpty()) {
            return Collections.emptyList();
        }

        return jpaList.stream()
                .map(this::toDomain)
                .toList();
    }
}
