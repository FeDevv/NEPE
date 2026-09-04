package org.nepe.competition.service;

import org.nepe.competition.domain.Team;
import org.nepe.competition.domain.TeamAlias;
import org.nepe.competition.port.in.CreateTeamCommand;
import org.nepe.competition.port.in.ManageTeamUseCase;
import org.nepe.competition.port.in.MapTeamAliasCommand;
import org.nepe.competition.port.in.RenameTeamCommand;
import org.nepe.competition.port.out.TeamAliasRepositoryPort;
import org.nepe.competition.port.out.TeamRepositoryPort;
import org.nepe.shared.exception.AliasMappingRequiredException;
import org.nepe.shared.exception.DomainValidationException;
import org.nepe.shared.exception.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Application Service implementing the {@link ManageTeamUseCase} Inbound Port.
 * <p>
 * Orchestrates team catalog management, name updates, alias mapping and resolution
 * required by CSV ingestion and UI search autocompletion.
 */
@Service
public class TeamService implements ManageTeamUseCase {

    private final TeamRepositoryPort teamRepositoryPort;
    private final TeamAliasRepositoryPort teamAliasRepositoryPort;

    public TeamService(TeamRepositoryPort teamRepositoryPort,
                       TeamAliasRepositoryPort teamAliasRepositoryPort) {
        this.teamRepositoryPort = Objects.requireNonNull(
                teamRepositoryPort,
                "TeamRepositoryPort must not be null"
        );
        this.teamAliasRepositoryPort = Objects.requireNonNull(
                teamAliasRepositoryPort,
                "TeamAliasRepositoryPort must not be null"
        );
    }

    // --- Team Operations ---

    @Override
    @Transactional
    public Team createTeam(CreateTeamCommand command) {
        if (command == null) {
            throw new DomainValidationException("CreateTeamCommand cannot be null.");
        }
        if (command.name() == null || command.name().isBlank()) {
            throw new DomainValidationException("Team name cannot be null or blank.");
        }

        String normalizedName = command.name().trim();
        if (teamRepositoryPort.existsByName(normalizedName)) {
            throw new DomainValidationException(
                    String.format("A team with name '%s' already exists.", normalizedName)
            );
        }

        Team team = Team.create(normalizedName);
        return teamRepositoryPort.save(team);
    }

    @Override
    @Transactional
    public Team renameTeam(RenameTeamCommand command) {
        if (command == null) {
            throw new DomainValidationException("RenameTeamCommand cannot be null.");
        }
        if (command.newName() == null || command.newName().isBlank()) {
            throw new DomainValidationException("New team name cannot be null or blank.");
        }

        Team team = teamRepositoryPort.findById(command.id())
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format("Team with ID %d not found.", command.id())
                ));

        String normalizedNewName = command.newName().trim();
        Optional<Team> existingWithSameName = teamRepositoryPort.findByName(normalizedNewName);
        if (existingWithSameName.isPresent() && !existingWithSameName.get().getId().equals(command.id())) {
            throw new DomainValidationException(
                    String.format("Another team with name '%s' already exists.", normalizedNewName)
            );
        }

        team.rename(normalizedNewName);
        return teamRepositoryPort.save(team);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Team> getAllTeams() {
        return teamRepositoryPort.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Team> searchTeams(String query) {
        if (query == null || query.isBlank()) {
            return teamRepositoryPort.findAll();
        }
        return teamRepositoryPort.searchByName(query.trim());
    }

    @Override
    @Transactional(readOnly = true)
    public Team getTeamById(int id) {
        return teamRepositoryPort.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format("Team with ID %d not found.", id)
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public Team getTeamByName(String name) {
        if (name == null || name.isBlank()) {
            throw new DomainValidationException("Team name cannot be null or blank.");
        }

        return teamRepositoryPort.findByName(name.trim())
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format("Team with name '%s' not found.", name.trim())
                ));
    }

    @Override
    @Transactional
    public void deleteTeam(int id) {
        if (teamRepositoryPort.findById(id).isEmpty()) {
            throw new EntityNotFoundException(
                    String.format("Team with ID %d not found.", id)
            );
        }

        teamAliasRepositoryPort.deleteByTeamId(id);
        teamRepositoryPort.deleteById(id);
    }

    // --- Competition Association Operations ---

    @Override
    @Transactional(readOnly = true)
    public List<Team> getTeamsByCompetition(int competitionId) {
        return teamRepositoryPort.findByCompetitionId(competitionId);
    }

    @Override
    @Transactional
    public void associateTeamToCompetition(int competitionId, int teamId) {
        if (teamRepositoryPort.findById(teamId).isEmpty()) {
            throw new EntityNotFoundException(
                    String.format("Team with ID %d not found.", teamId)
            );
        }
        teamRepositoryPort.associateTeamToCompetition(competitionId, teamId);
    }

    @Override
    @Transactional
    public void disassociateTeamFromCompetition(int competitionId, int teamId) {
        teamRepositoryPort.disassociateTeamFromCompetition(competitionId, teamId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isTeamAssociatedWithCompetition(int competitionId, int teamId) {
        return teamRepositoryPort.isTeamAssociatedWithCompetition(competitionId, teamId);
    }

    // --- Alias Operations ---

    @Override
    @Transactional
    public TeamAlias mapAlias(MapTeamAliasCommand command) {
        if (command == null) {
            throw new DomainValidationException("MapTeamAliasCommand cannot be null.");
        }
        if (command.aliasName() == null || command.aliasName().isBlank()) {
            throw new DomainValidationException("Alias name cannot be null or blank.");
        }

        // Verify target team exists
        if (teamRepositoryPort.findById(command.teamId()).isEmpty()) {
            throw new EntityNotFoundException(
                    String.format("Target team with ID %d not found.", command.teamId())
            );
        }

        String normalizedAlias = command.aliasName().trim();
        Optional<TeamAlias> existingAliasOpt = teamAliasRepositoryPort.findByAliasName(normalizedAlias);

        if (existingAliasOpt.isPresent()) {
            TeamAlias existingAlias = existingAliasOpt.get();
            if (!existingAlias.getTeamId().equals(command.teamId())) {
                existingAlias.reassignTeam(command.teamId());
                return teamAliasRepositoryPort.save(existingAlias);
            }
            return existingAlias;
        }

        TeamAlias newAlias = TeamAlias.create(normalizedAlias, command.teamId());
        return teamAliasRepositoryPort.save(newAlias);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeamAlias> getAliasesForTeam(int teamId) {
        if (teamRepositoryPort.findById(teamId).isEmpty()) {
            throw new EntityNotFoundException(
                    String.format("Team with ID %d not found.", teamId)
            );
        }
        return teamAliasRepositoryPort.findByTeamId(teamId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeamAlias> getAllAliases() {
        return teamAliasRepositoryPort.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Team resolveTeamByRawName(String rawName) {
        if (rawName == null || rawName.isBlank()) {
            throw new DomainValidationException("Raw team name cannot be null or blank.");
        }

        String normalized = rawName.trim();

        // 1. Check mapped aliases first
        Optional<TeamAlias> aliasOpt = teamAliasRepositoryPort.findByAliasName(normalized);
        if (aliasOpt.isPresent()) {
            return teamRepositoryPort.findById(aliasOpt.get().getTeamId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            String.format("Mapped team with ID %d not found for alias '%s'.",
                                    aliasOpt.get().getTeamId(), normalized)
                    ));
        }

        // 2. Fallback to direct match on official team names
        Optional<Team> directMatchOpt = teamRepositoryPort.findByName(normalized);
        if (directMatchOpt.isPresent()) {
            return directMatchOpt.get();
        }

        // 3. Unrecognized name triggers human-in-the-loop mapping exception
        throw new AliasMappingRequiredException(normalized);
    }

    @Override
    @Transactional
    public void deleteAlias(int aliasId) {
        if (teamAliasRepositoryPort.findById(aliasId).isEmpty()) {
            throw new EntityNotFoundException(
                    String.format("Team alias with ID %d not found.", aliasId)
            );
        }
        teamAliasRepositoryPort.deleteById(aliasId);
    }
}
