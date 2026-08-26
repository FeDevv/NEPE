package org.nepe.competition.adapter.out;

import org.nepe.competition.domain.Competition;
import org.nepe.competition.port.out.CompetitionRepositoryPort;
import org.nepe.shared.exception.DomainValidationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Outbound Persistence Adapter implementing the {@link CompetitionRepositoryPort}.
 * <p>
 * Bridges the pure domain model with the MariaDB database via Spring Data JPA and
 * {@link CompetitionMapper}, applying the Exception Translation Pattern to shield
 * caller layers from low-level SQL/JPA exceptions.
 */
@Repository
public class CompetitionRepositoryAdapter implements CompetitionRepositoryPort {

    private final SpringDataCompetitionRepository springDataRepository;
    private final CompetitionMapper mapper;

    public CompetitionRepositoryAdapter(SpringDataCompetitionRepository springDataRepository,
                                        CompetitionMapper mapper) {
        this.springDataRepository = Objects.requireNonNull(springDataRepository, "SpringDataCompetitionRepository must not be null");
        this.mapper = Objects.requireNonNull(mapper, "CompetitionMapper must not be null");
    }

    @Override
    @Transactional
    public Competition save(Competition competition) {
        if (competition == null) {
            throw new DomainValidationException("Competition to save cannot be null.");
        }

        try {
            CompetitionJpaEntity jpaEntity = mapper.toJpa(competition);
            CompetitionJpaEntity savedEntity = springDataRepository.save(jpaEntity);
            return mapper.toDomain(savedEntity);
        } catch (DataIntegrityViolationException e) {
            throw new DomainValidationException(
                    String.format("Unable to persist competition with code '%s': a uniqueness or foreign key constraint was violated.",
                            competition.getCode()),
                    e
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Competition> findById(int id) {
        return springDataRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Competition> findByCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        return springDataRepository.findByCode(code.trim().toUpperCase())
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Competition> findAll() {
        List<CompetitionJpaEntity> entities = springDataRepository.findAllByOrderByNameAsc();
        return mapper.toDomainList(entities);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByCode(String code) {
        if (code == null || code.isBlank()) {
            return false;
        }
        return springDataRepository.existsByCode(code.trim().toUpperCase());
    }

    @Override
    @Transactional
    public void deleteById(int id) {
        try {
            springDataRepository.deleteById(id);
        } catch (DataIntegrityViolationException e) {
            throw new DomainValidationException(
                    String.format("Cannot delete competition with ID %d because related records (matches/seasons) depend on it.", id),
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
