package org.nepe.competition.adapter.out;

import org.nepe.competition.domain.Team;
import org.nepe.competition.port.out.TeamRepositoryPort;
import org.nepe.shared.exception.DomainValidationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Outbound Persistence Adapter implementing the {@link TeamRepositoryPort}.
 * <p>
 * Connects the pure domain layer with MariaDB via Spring Data JPA and {@link TeamMapper}.
 * Adheres to the Exception Translation Pattern to encapsulate SQL/ORM-level exceptions.
 */
@Repository
public class TeamRepositoryAdapter implements TeamRepositoryPort {

    private final SpringDataTeamRepository springDataRepository;
    private final TeamMapper mapper;

    public TeamRepositoryAdapter(SpringDataTeamRepository springDataRepository,
                                 TeamMapper mapper) {
        this.springDataRepository = Objects.requireNonNull(springDataRepository, "SpringDataTeamRepository must not be null");
        this.mapper = Objects.requireNonNull(mapper, "TeamMapper must not be null");
    }

    @Override
    @Transactional
    public Team save(Team team) {
        if (team == null) {
            throw new DomainValidationException("Team to save cannot be null.");
        }

        try {
            TeamJpaEntity jpaEntity = mapper.toJpa(team);
            TeamJpaEntity savedEntity = springDataRepository.save(jpaEntity);
            return mapper.toDomain(savedEntity);
        } catch (DataIntegrityViolationException e) {
            throw new DomainValidationException(
                    String.format("Unable to persist team '%s': a team with this name already exists.", team.getName()),
                    e
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Team> findById(int id) {
        return springDataRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Team> findByName(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        return springDataRepository.findByNameIgnoreCase(name.trim())
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Team> findAll() {
        List<TeamJpaEntity> entities = springDataRepository.findAllByOrderByNameAsc();
        return mapper.toDomainList(entities);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Team> searchByName(String query) {
        if (query == null || query.isBlank()) {
            return Collections.emptyList();
        }
        List<TeamJpaEntity> entities = springDataRepository.findByNameContainingIgnoreCaseOrderByNameAsc(query.trim());
        return mapper.toDomainList(entities);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByName(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        return springDataRepository.existsByNameIgnoreCase(name.trim());
    }

    @Override
    @Transactional
    public void deleteById(int id) {
        try {
            springDataRepository.deleteById(id);
        } catch (DataIntegrityViolationException e) {
            throw new DomainValidationException(
                    String.format("Cannot delete team with ID %d because associated records (matches/aliases) depend on it.", id),
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
