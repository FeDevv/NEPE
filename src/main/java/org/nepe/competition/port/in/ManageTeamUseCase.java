package org.nepe.competition.port.in;

import org.nepe.competition.domain.Team;
import org.nepe.competition.domain.TeamAlias;

import java.util.List;

/**
 * Inbound Port (Driving Port / Use Case) defining operations for managing teams and their aliases.
 * <p>
 * Invoked by Inbound Adapters (e.g., JavaFX Controllers for team management, autocomplete search,
 * and the alias mapping dialog during CSV ingestion).
 */
public interface ManageTeamUseCase {

    // --- Team Operations ---

    /**
     * Registers a new official team in the system.
     *
     * @param command the {@link CreateTeamCommand} payload (must not be null)
     * @return the created and persisted {@link Team}
     */
    Team createTeam(CreateTeamCommand command);

    /**
     * Renames an existing team.
     *
     * @param command the {@link RenameTeamCommand} payload (must not be null)
     * @return the updated and persisted {@link Team}
     */
    Team renameTeam(RenameTeamCommand command);

    /**
     * Retrieves all teams registered in the system, sorted alphabetically by name.
     *
     * @return list of all {@link Team} instances
     */
    List<Team> getAllTeams();

    /**
     * Retrieves all teams associated with a specific competition, sorted alphabetically by name.
     *
     * @param competitionId target competition identifier
     * @return list of {@link Team} instances associated with the competition
     */
    List<Team> getTeamsByCompetition(int competitionId);

    /**
     * Associates an existing team with a competition.
     *
     * @param competitionId target competition identifier
     * @param teamId target team identifier
     */
    void associateTeamToCompetition(int competitionId, int teamId);

    /**
     * Disassociates a team from a competition.
     *
     * @param competitionId target competition identifier
     * @param teamId target team identifier
     */
    void disassociateTeamFromCompetition(int competitionId, int teamId);

    /**
     * Checks whether a team is currently associated with a competition.
     *
     * @param competitionId target competition identifier
     * @param teamId target team identifier
     * @return true if associated, false otherwise
     */
    boolean isTeamAssociatedWithCompetition(int competitionId, int teamId);

    /**
     * Searches teams whose name contains the specified query substring (case-insensitive).
     *
     * @param query search query
     * @return list of matching {@link Team} instances
     */
    List<Team> searchTeams(String query);

    /**
     * Retrieves a team by its database ID.
     *
     * @param id primary key identifier
     * @return the found {@link Team}
     * @throws org.nepe.shared.exception.EntityNotFoundException if not found
     */
    Team getTeamById(int id);

    /**
     * Retrieves a team by its official name (case-insensitive).
     *
     * @param name official team name
     * @return the found {@link Team}
     * @throws org.nepe.shared.exception.EntityNotFoundException if not found
     */
    Team getTeamByName(String name);

    /**
     * Deletes a team by its database ID.
     *
     * @param id primary key identifier
     */
    void deleteTeam(int id);

    // --- Alias Operations ---

    /**
     * Maps a raw name / alias string to an existing official team.
     *
     * @param command the {@link MapTeamAliasCommand} payload (must not be null)
     * @return the created and persisted {@link TeamAlias}
     */
    TeamAlias mapAlias(MapTeamAliasCommand command);

    /**
     * Retrieves all aliases associated with a specific team.
     *
     * @param teamId target team identifier
     * @return list of {@link TeamAlias} instances
     */
    List<TeamAlias> getAliasesForTeam(int teamId);

    /**
     * Retrieves all registered team aliases.
     *
     * @return list of all {@link TeamAlias} instances
     */
    List<TeamAlias> getAllAliases();

    /**
     * Resolves a raw team name (from CSV or bookmaker) to its official {@link Team} entity.
     *
     * @param rawName raw name string
     * @return the resolved official {@link Team}
     * @throws org.nepe.shared.exception.AliasMappingRequiredException if the raw name is unmapped
     */
    Team resolveTeamByRawName(String rawName);

    /**
     * Deletes an alias mapping by its ID.
     *
     * @param aliasId primary key identifier of the alias
     */
    void deleteAlias(int aliasId);
}
