package org.nepe.match.adapter.out;

import org.nepe.match.domain.MatchEvent;
import org.nepe.match.port.out.MatchEventRepositoryPort;
import org.nepe.shared.exception.DomainValidationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Outbound Persistence Adapter implementing {@link MatchEventRepositoryPort}.
 * <p>
 * Connects the domain event engine with MariaDB via Spring Data JPA and {@link MatchEventMapper}.
 */
@Repository
public class MatchEventRepositoryAdapter implements MatchEventRepositoryPort {

    private final SpringDataMatchEventRepository springDataRepository;
    private final MatchEventMapper mapper;

    public MatchEventRepositoryAdapter(SpringDataMatchEventRepository springDataRepository,
                                       MatchEventMapper mapper) {
        this.springDataRepository = Objects.requireNonNull(springDataRepository, "SpringDataMatchEventRepository must not be null");
        this.mapper = Objects.requireNonNull(mapper, "MatchEventMapper must not be null");
    }

    @Override
    @Transactional
    public MatchEvent save(MatchEvent event) {
        if (event == null) {
            throw new DomainValidationException("MatchEvent to save cannot be null.");
        }

        try {
            MatchEventJpaEntity jpaEntity = mapper.toJpa(event);
            MatchEventJpaEntity savedEntity = springDataRepository.save(jpaEntity);
            return mapper.toDomain(savedEntity);
        } catch (DataIntegrityViolationException e) {
            throw new DomainValidationException(
                    String.format("Unable to persist match event for match ID %d: database constraint violated.", event.getMatchId()),
                    e
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MatchEvent> findById(int id) {
        return springDataRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MatchEvent> findByMatchIdOrderByMinuteAsc(int matchId) {
        List<MatchEventJpaEntity> entities = springDataRepository.findByMatchIdOrderByMinuteAscCreatedAtAsc(matchId);
        return mapper.toDomainList(entities);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MatchEvent> findLatestEventByMatchId(int matchId) {
        return springDataRepository.findFirstByMatchIdOrderByCreatedAtDescIdDesc(matchId)
                .map(mapper::toDomain);
    }

    @Override
    @Transactional
    public void deleteById(int id) {
        try {
            springDataRepository.deleteById(id);
        } catch (DataIntegrityViolationException e) {
            throw new DomainValidationException(
                    String.format("Cannot delete match event with ID %d due to database constraint violation.", id),
                    e
            );
        }
    }

    @Override
    @Transactional
    public void deleteByMatchId(int matchId) {
        try {
            springDataRepository.deleteByMatchId(matchId);
        } catch (DataIntegrityViolationException e) {
            throw new DomainValidationException(
                    String.format("Cannot delete events for match ID %d due to database constraint violation.", matchId),
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
