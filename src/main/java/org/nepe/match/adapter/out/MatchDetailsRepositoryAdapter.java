package org.nepe.match.adapter.out;

import org.nepe.match.domain.MatchState;
import org.nepe.match.port.out.MatchDetailsDTO;
import org.nepe.match.port.out.MatchDetailsRepositoryPort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Outbound Persistence Adapter implementing {@link MatchDetailsRepositoryPort}.
 * <p>
 * Reads denormalized match projection rows from the MariaDB SQL view {@code v_matches_details}
 * via {@link SpringDataMatchDetailsRepository} and maps them to {@link MatchDetailsDTO} records.
 */
@Repository
@Transactional(readOnly = true)
public class MatchDetailsRepositoryAdapter implements MatchDetailsRepositoryPort {

    private final SpringDataMatchDetailsRepository springDataRepository;
    private final MatchDetailsMapper mapper;

    public MatchDetailsRepositoryAdapter(SpringDataMatchDetailsRepository springDataRepository,
                                         MatchDetailsMapper mapper) {
        this.springDataRepository = Objects.requireNonNull(springDataRepository, "SpringDataMatchDetailsRepository must not be null");
        this.mapper = Objects.requireNonNull(mapper, "MatchDetailsMapper must not be null");
    }

    @Override
    public Optional<MatchDetailsDTO> findDetailsById(int matchId) {
        return springDataRepository.findById(matchId)
                .map(mapper::toDto);
    }

    @Override
    public List<MatchDetailsDTO> findDetailsByCompetitionAndSeason(int competitionId, int seasonId) {
        List<MatchDetailsJpaEntity> entities = springDataRepository.findByCompetitionIdAndSeasonIdOrderByMatchDateTimeAsc(
                competitionId, seasonId
        );
        return mapper.toDtoList(entities);
    }

    @Override
    public List<MatchDetailsDTO> findDetailsByCompetitionAndSeasonAndState(int competitionId, int seasonId, MatchState state) {
        if (state == null) {
            return findDetailsByCompetitionAndSeason(competitionId, seasonId);
        }
        List<MatchDetailsJpaEntity> entities = springDataRepository.findByCompetitionIdAndSeasonIdAndMatchStateOrderByMatchDateTimeAsc(
                competitionId, seasonId, state.name()
        );
        return mapper.toDtoList(entities);
    }

    @Override
    public List<MatchDetailsDTO> findAllDetails() {
        List<MatchDetailsJpaEntity> entities = springDataRepository.findAllByOrderByMatchDateTimeAsc();
        return mapper.toDtoList(entities);
    }
}
