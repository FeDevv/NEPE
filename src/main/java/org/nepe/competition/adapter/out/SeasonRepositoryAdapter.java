package org.nepe.competition.adapter.out;

import org.nepe.competition.domain.Season;
import org.nepe.competition.port.out.SeasonRepositoryPort;
import org.nepe.shared.exception.DomainValidationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Outbound Persistence Adapter implementing the {@link SeasonRepositoryPort}.
 * <p>
 * Bridges the pure domain model with MariaDB via Spring Data JPA and {@link SeasonMapper}.
 * Translates SQL integrity exceptions and manages read-only vs mutating transactional boundaries.
 */
@Repository
public class SeasonRepositoryAdapter implements SeasonRepositoryPort {

    private final SpringDataSeasonRepository springDataRepository;
    private final SeasonMapper mapper;

    public SeasonRepositoryAdapter(SpringDataSeasonRepository springDataRepository,
                                   SeasonMapper mapper) {
        this.springDataRepository = Objects.requireNonNull(springDataRepository, "SpringDataSeasonRepository must not be null");
        this.mapper = Objects.requireNonNull(mapper, "SeasonMapper must not be null");
    }

    @Override
    @Transactional
    public Season save(Season season) {
        if (season == null) {
            throw new DomainValidationException("Season to save cannot be null.");
        }

        try {
            SeasonJpaEntity jpaEntity = mapper.toJpa(season);
            SeasonJpaEntity savedEntity = springDataRepository.save(jpaEntity);
            return mapper.toDomain(savedEntity);
        } catch (DataIntegrityViolationException e) {
            throw new DomainValidationException(
                    String.format("Unable to persist season '%s': a season with this name already exists.", season.getName()),
                    e
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Season> findById(int id) {
        return springDataRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Season> findByName(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        return springDataRepository.findByName(name.trim())
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Season> findLatest() {
        return springDataRepository.findFirstByOrderByNameDesc()
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Season> findAll() {
        List<SeasonJpaEntity> entities = springDataRepository.findAllByOrderByNameDesc();
        return mapper.toDomainList(entities);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByName(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        return springDataRepository.existsByName(name.trim());
    }

    @Override
    @Transactional
    public void deleteById(int id) {
        try {
            springDataRepository.deleteById(id);
        } catch (DataIntegrityViolationException e) {
            throw new DomainValidationException(
                    String.format("Cannot delete season with ID %d because associated matches depend on it.", id),
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
