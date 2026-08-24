package org.nepe.competition.port.in;

/**
 * Inbound Command DTO for renaming an existing official team.
 *
 * @param id      database identifier of the team to rename
 * @param newName the updated official team name
 */
public record RenameTeamCommand(
        int id,
        String newName
) {
}
