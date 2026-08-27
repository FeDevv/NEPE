package org.nepe.competition.adapter.out;

import org.nepe.competition.domain.TeamAlias;
import org.nepe.competition.port.out.TeamAliasRepositoryPort;
import org.nepe.shared.exception.DomainValidationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Outbound Persistence Adapter implementing the {@link TeamAliasRepositoryPort}.
 * <p>
 * Connects the domain layer with MariaDB for team alias lookup and resolution,
 * applying the Exception Translation Pattern for SQL constraint violations.
 */
@Repository
public class TeamAliasRepositoryAdapter implements TeamAliasRepositoryPort {

    private final SpringDataTeamAliasRepository springDataRepository;
    private final TeamAliasMapper mapper;

    public TeamAliasRepositoryAdapter(SpringDataTeamAliasRepository springDataRepository,
                                      TeamAliasMapper mapper) {
        this.springDataRepository = Objects.requireNonNull(springDataRepository, "SpringDataTeamAliasRepository must not be null");
        this.mapper = Objects.requireNonNull(mapper, "TeamAliasMapper must not be null");
    }

    @Override
    @Transactional
    public TeamAlias save(TeamAlias teamAlias) {
        if (teamAlias == null) {
            throw new DomainValidationException("TeamAlias to save cannot be null.");
        }

        try {
            TeamAliasJpaEntity jpaEntity = mapper.toJpa(teamAlias);
            TeamAliasJpaEntity savedEntity = springDataRepository.save(jpaEntity);
            return mapper.toDomain(savedEntity);
        } catch (DataIntegrityViolationException e) {
            throw new DomainValidationException(
                    String.format("Unable to persist team alias '%s': either the alias already exists or the target team ID %d is invalid.",
                            teamAlias.getAliasName(), teamAlias.getTeamId()),
                    e
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TeamAlias> findById(int id) {
        return springDataRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TeamAlias> findByAliasName(String aliasName) {
        if (aliasName == null || aliasName.isBlank()) {
            return Optional.empty();
        }
        return springDataRepository.findByAliasNameIgnoreCase(aliasName.trim())
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeamAlias> findByTeamId(int teamId) {
        List<TeamAliasJpaEntity> entities = springDataRepository.findByTeamIdOrderByAliasNameAsc(teamId);
        return mapper.toDomainList(entities);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeamAlias> findAll() {
        List<TeamAliasJpaEntity> entities = springDataRepository.findAllByOrderByAliasNameAsc();
        return mapper.toDomainList(entities);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByAliasName(String aliasName) {
        if (aliasName == null || aliasName.isBlank()) {
            return false;
        }
        return springDataRepository.existsByAliasNameIgnoreCase(aliasName.trim());
    }

    @Override
    @Transactional
    public void deleteById(int id) {
        try {
            springDataRepository.deleteById(id);
        } catch (DataIntegrityViolationException e) {
            throw new DomainValidationException(
                    String.format("Cannot delete team alias with ID %d due to database constraint violation.", id),
                    e
            );
        }
    }

    @Override
    @Transactional
    public void deleteByTeamId(int teamId) {
        try {
            springDataRepository.deleteByTeamId(teamId);
        } catch (DataIntegrityViolationException e) {
            throw new DomainValidationException(
                    String.format("Cannot delete team aliases for team ID %d due to database constraint violation.", teamId),
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
