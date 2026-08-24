package org.nepe.competition.port.in;

/**
 * Inbound Command DTO for mapping a raw alias name to an authoritative team entity.
 *
 * @param aliasName raw team string encountered in CSV or external odds feeds (e.g. "Man City")
 * @param teamId    database identifier of the official {@link org.nepe.competition.domain.Team}
 */
public record MapTeamAliasCommand(
        String aliasName,
        int teamId
) {
}
