package org.nepe.match.adapter.out;

import org.nepe.match.domain.MatchState;
import org.nepe.match.port.out.MatchDetailsDTO;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Mapper component converting {@link MatchDetailsJpaEntity} view projections
 * into immutable {@link MatchDetailsDTO} records.
 */
@Component
public class MatchDetailsMapper {

    /**
     * Converts a {@link MatchDetailsJpaEntity} into a {@link MatchDetailsDTO}.
     *
     * @param jpa the view entity (nullable)
     * @return populated record, or {@code null} if input is null
     */
    public MatchDetailsDTO toDto(MatchDetailsJpaEntity jpa) {
        if (jpa == null) {
            return null;
        }

        return new MatchDetailsDTO(
                jpa.getMatchId(),
                jpa.getMatchDateTime(),
                MatchState.fromString(jpa.getMatchState()),
                jpa.isManuallyEdited(),
                jpa.getHomeScore(),
                jpa.getAwayScore(),
                jpa.getHomeShots(),
                jpa.getAwayShots(),
                jpa.getHomeShotsOnTarget(),
                jpa.getAwayShotsOnTarget(),
                jpa.getHomeRedCards(),
                jpa.getAwayRedCards(),
                jpa.getManualHomeXg(),
                jpa.getManualAwayXg(),
                jpa.getOddsHome(),
                jpa.getOddsDraw(),
                jpa.getOddsAway(),
                jpa.isNeutralVenue(),
                jpa.isMustWinHome(),
                jpa.isMustWinAway(),
                jpa.isLowUrgencyHome(),
                jpa.isLowUrgencyAway(),
                jpa.getModAttHome(),
                jpa.getModDefHome(),
                jpa.getModAttAway(),
                jpa.getModDefAway(),
                jpa.getCurrentMinute(),
                jpa.getCompetitionId(),
                jpa.getCompetitionCode(),
                jpa.getCompetitionName(),
                jpa.getCompetitionCountry(),
                jpa.getDixonColesRho(),
                jpa.getSeasonId(),
                jpa.getSeasonName(),
                jpa.getHomeTeamId(),
                jpa.getHomeTeamName(),
                jpa.getAwayTeamId(),
                jpa.getAwayTeamName()
        );
    }

    /**
     * Converts a list of view JPA entities into a list of immutable projection DTOs.
     *
     * @param jpaList the list of view entities
     * @return unmodifiable list of DTOs, or empty list if input is null/empty
     */
    public List<MatchDetailsDTO> toDtoList(List<MatchDetailsJpaEntity> jpaList) {
        if (jpaList == null || jpaList.isEmpty()) {
            return Collections.emptyList();
        }

        return jpaList.stream()
                .map(this::toDto)
                .toList();
    }
}
