package org.nepe.competition.domain;

import org.nepe.shared.domain.exception.DomainValidationException;

import java.util.Objects;

/**
 * Domain entity representing an alternative raw team name / alias (e.g., "Man City", "Spurs")
 * mapped to an authoritative {@link Team}.
 * <p>
 * Used during CSV ingestion and external odds processing to resolve varied naming conventions
 * to a single official entity in the database.
 */
public class TeamAlias {

    private Integer id;
    private String aliasName;
    private Integer teamId;

    /**
     * Factory method for creating a new unpersisted TeamAlias instance.
     *
     * @param aliasName raw name as encountered in CSV / external sources
     * @param teamId    id of the official {@link Team} this alias resolves to
     * @return a new validated {@link TeamAlias} instance
     */
    public static TeamAlias create(String aliasName, Integer teamId) {
        return new TeamAlias(null, aliasName, teamId);
    }

    /**
     * Full constructor for domain reconstruction (e.g., when loaded from persistence).
     */
    public TeamAlias(Integer id, String aliasName, Integer teamId) {
        validateAliasName(aliasName);
        validateTeamId(teamId);

        this.id = id;
        this.aliasName = aliasName.trim();
        this.teamId = teamId;
    }

    // --- Domain Business Logic & State Mutations ---

    public void reassignTeam(Integer newTeamId) {
        validateTeamId(newTeamId);
        this.teamId = newTeamId;
    }

    public void assignId(Integer id) {
        if (id == null || id <= 0) {
            throw new DomainValidationException("TeamAlias ID must be a positive integer.");
        }
        if (this.id != null && !this.id.equals(id)) {
            throw new DomainValidationException("Cannot reassign an existing TeamAlias ID.");
        }
        this.id = id;
    }

    // --- Invariant Validations ---

    private static void validateAliasName(String aliasName) {
        if (aliasName == null || aliasName.isBlank()) {
            throw new DomainValidationException("Alias name cannot be null or blank.");
        }
        if (aliasName.trim().length() > 100) {
            throw new DomainValidationException("Alias name cannot exceed 100 characters.");
        }
    }

    private static void validateTeamId(Integer teamId) {
        if (teamId == null || teamId <= 0) {
            throw new DomainValidationException("Target team ID must be a valid positive integer.");
        }
    }

    // --- Getters ---

    public Integer getId() {
        return id;
    }

    public String getAliasName() {
        return aliasName;
    }

    public Integer getTeamId() {
        return teamId;
    }

    // --- Identity & Equality based on unique alias name (case-insensitive) ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TeamAlias teamAlias = (TeamAlias) o;
        return aliasName.equalsIgnoreCase(teamAlias.aliasName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(aliasName.toLowerCase());
    }

    @Override
    public String toString() {
        return "TeamAlias{" +
                "id=" + id +
                ", aliasName='" + aliasName + '\'' +
                ", teamId=" + teamId +
                '}';
    }
}
