package org.nepe.competition.port.in;

/**
 * Inbound Command DTO for registering a new official team in the system.
 *
 * @param name official team name (e.g., "Manchester City", "Inter")
 */
public record CreateTeamCommand(
        String name
) {
}
