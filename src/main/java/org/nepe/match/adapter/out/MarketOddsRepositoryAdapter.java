package org.nepe.match.adapter.out;

import org.nepe.match.domain.MarketOdds;
import org.nepe.match.domain.MarketType;
import org.nepe.match.port.out.MarketOddsRepositoryPort;
import org.nepe.shared.exception.DomainValidationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Outbound Persistence Adapter implementing {@link MarketOddsRepositoryPort}.
 * <p>
 * Connects the EV evaluation engine with MariaDB via Spring Data JPA and {@link MarketOddsMapper}.
 */
@Repository
public class MarketOddsRepositoryAdapter implements MarketOddsRepositoryPort {

    private final SpringDataMarketOddsRepository springDataRepository;
    private final MarketOddsMapper mapper;

    public MarketOddsRepositoryAdapter(SpringDataMarketOddsRepository springDataRepository,
                                       MarketOddsMapper mapper) {
        this.springDataRepository = Objects.requireNonNull(springDataRepository, "SpringDataMarketOddsRepository must not be null");
        this.mapper = Objects.requireNonNull(mapper, "MarketOddsMapper must not be null");
    }

    @Override
    @Transactional
    public MarketOdds save(MarketOdds marketOdds) {
        if (marketOdds == null) {
            throw new DomainValidationException("MarketOdds to save cannot be null.");
        }

        try {
            MarketOddsJpaEntity jpaEntity = mapper.toJpa(marketOdds);
            MarketOddsJpaEntity savedEntity = springDataRepository.save(jpaEntity);
            return mapper.toDomain(savedEntity);
        } catch (DataIntegrityViolationException e) {
            throw new DomainValidationException(
                    String.format("Unable to persist market odds for match ID %d (Market: %s, Outcome: %s): constraint violation.",
                            marketOdds.getMatchId(), marketOdds.getMarketType(), marketOdds.getOutcome()),
                    e
            );
        }
    }

    @Override
    @Transactional
    public List<MarketOdds> saveAll(List<MarketOdds> oddsList) {
        if (oddsList == null || oddsList.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            List<MarketOddsJpaEntity> jpaEntities = oddsList.stream()
                    .map(mapper::toJpa)
                    .toList();
            List<MarketOddsJpaEntity> savedEntities = springDataRepository.saveAll(jpaEntities);
            return mapper.toDomainList(savedEntities);
        } catch (DataIntegrityViolationException e) {
            throw new DomainValidationException("Unable to persist market odds batch: database constraint violated.", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MarketOdds> findById(int id) {
        return springDataRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MarketOdds> findByMatchId(int matchId) {
        List<MarketOddsJpaEntity> entities = springDataRepository.findByMatchId(matchId);
        return mapper.toDomainList(entities);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MarketOdds> findByMatchIdAndMarketTypeAndOutcome(int matchId, MarketType marketType, String outcome) {
        if (marketType == null || outcome == null || outcome.isBlank()) {
            return Optional.empty();
        }
        return springDataRepository.findByMatchIdAndMarketTypeAndOutcome(matchId, marketType.name(), outcome.trim().toUpperCase())
                .map(mapper::toDomain);
    }

    @Override
    @Transactional
    public void deleteById(int id) {
        try {
            springDataRepository.deleteById(id);
        } catch (DataIntegrityViolationException e) {
            throw new DomainValidationException(
                    String.format("Cannot delete market odds with ID %d due to database constraint violation.", id),
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
                    String.format("Cannot delete market odds for match ID %d due to database constraint violation.", matchId),
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
