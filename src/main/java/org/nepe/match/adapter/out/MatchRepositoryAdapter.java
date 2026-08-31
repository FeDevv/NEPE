package org.nepe.match.adapter.out;

import org.nepe.match.domain.Match;
import org.nepe.match.port.out.MatchRepositoryPort;
import org.nepe.shared.exception.DomainValidationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Outbound Persistence Adapter implementing {@link MatchRepositoryPort}.
 * <p>
 * Bridges the {@link Match} Aggregate Root with MariaDB via Spring Data JPA and {@link MatchMapper},
 * supporting upsert matching, historical sample lookups, and transaction boundaries.
 */
@Repository
public class MatchRepositoryAdapter implements MatchRepositoryPort {

    private final SpringDataMatchRepository springDataRepository;
    private final MatchMapper mapper;

    public MatchRepositoryAdapter(SpringDataMatchRepository springDataRepository,
                                  MatchMapper mapper) {
        this.springDataRepository = Objects.requireNonNull(springDataRepository, "SpringDataMatchRepository must not be null");
        this.mapper = Objects.requireNonNull(mapper, "MatchMapper must not be null");
    }

    @Override
    @Transactional
    public Match save(Match match) {
        if (match == null) {
            throw new DomainValidationException("Match to save cannot be null.");
        }

        try {
            MatchJpaEntity jpaEntity = mapper.toJpa(match);
            MatchJpaEntity savedEntity = springDataRepository.save(jpaEntity);
            return mapper.toDomain(savedEntity);
        } catch (DataIntegrityViolationException e) {
            throw new DomainValidationException(
                    String.format("Unable to persist match (Home: %d, Away: %d, Date: %s): unique constraint or foreign key violation.",
                            match.getHomeTeamId(), match.getAwayTeamId(), match.getMatchDateTime()),
                    e
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Match> findById(int id) {
        return springDataRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Match> findByTeamsAndDateTime(int homeTeamId, int awayTeamId, Instant matchDateTime) {
        if (matchDateTime == null) {
            return Optional.empty();
        }
        return springDataRepository.findByHomeTeamIdAndAwayTeamIdAndMatchDateTime(homeTeamId, awayTeamId, matchDateTime)
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Match> findByTeamsAndDateRange(int homeTeamId, int awayTeamId, Instant startOfDay, Instant endOfDay) {
        if (startOfDay == null || endOfDay == null) {
            return Optional.empty();
        }
        return springDataRepository.findByTeamsAndDateRange(homeTeamId, awayTeamId, startOfDay, endOfDay)
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Match> findByCompetitionAndSeason(int competitionId, int seasonId) {
        List<MatchJpaEntity> entities = springDataRepository.findByCompetitionIdAndSeasonIdOrderByMatchDateTimeAsc(
                competitionId, seasonId
        );
        return mapper.toDomainList(entities);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Match> findFinishedMatchesByCompetitionAndSeason(int competitionId, int seasonId) {
        List<MatchJpaEntity> entities = springDataRepository.findFinishedMatchesByCompetitionAndSeason(
                competitionId, seasonId
        );
        return mapper.toDomainList(entities);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Match> findFinishedMatchesForTeamInSeason(int teamId, int competitionId, int seasonId) {
        List<MatchJpaEntity> entities = springDataRepository.findFinishedMatchesForTeamInSeason(
                teamId, competitionId, seasonId
        );
        return mapper.toDomainList(entities);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Match> findRecentMatchesForTeam(int teamId, int competitionId, int limit) {
        int safeLimit = Math.max(1, limit);
        List<MatchJpaEntity> entities = springDataRepository.findRecentFinishedMatchesForTeam(
                teamId, competitionId, PageRequest.of(0, safeLimit)
        );
        return mapper.toDomainList(entities);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Match> findAll() {
        List<MatchJpaEntity> entities = springDataRepository.findAllByOrderByMatchDateTimeAsc();
        return mapper.toDomainList(entities);
    }

    @Override
    @Transactional
    public void deleteById(int id) {
        try {
            springDataRepository.deleteById(id);
        } catch (DataIntegrityViolationException e) {
            throw new DomainValidationException(
                    String.format("Cannot delete match with ID %d because associated events or odds depend on it.", id),
                    e
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public long count() {
        return springDataRepository.count();
    }
}
