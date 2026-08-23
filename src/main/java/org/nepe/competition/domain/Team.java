package org.nepe.competition.domain;

import org.nepe.shared.exception.DomainValidationException;

import java.util.Objects;

/**
 * Domain entity representing a football team / club (e.g., "Manchester City", "Inter").
 * <p>
 * Encapsulates the official team name displayed in the UI and used as the authoritative identity.
 */
public class Team {

    private Integer id;
    private String name;

    /**
     * Factory method for creating a new unpersisted Team instance.
     *
     * @param name official team name
     * @return a new validated {@link Team} instance
     */
    public static Team create(String name) {
        return new Team(null, name);
    }

    /**
     * Full constructor for domain reconstruction (e.g., when loaded from persistence).
     */
    public Team(Integer id, String name) {
        validateName(name);

        this.id = id;
        this.name = name.trim();
    }

    // --- Domain Business Logic & State Mutations ---

    public void rename(String newName) {
        validateName(newName);
        this.name = newName.trim();
    }

    public void assignId(Integer id) {
        if (id == null || id <= 0) {
            throw new DomainValidationException("Team ID must be a positive integer.");
        }
        if (this.id != null && !this.id.equals(id)) {
            throw new DomainValidationException("Cannot reassign an existing Team ID.");
        }
        this.id = id;
    }

    // --- Invariant Validations ---

    private static void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new DomainValidationException("Team name cannot be null or blank.");
        }
        if (name.trim().length() > 100) {
            throw new DomainValidationException("Team name cannot exceed 100 characters.");
        }
    }

    // --- Getters ---

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    // --- Identity & Equality based on unique business name (case-insensitive) ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Team team = (Team) o;
        return name.equalsIgnoreCase(team.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name.toLowerCase());
    }

    @Override
    public String toString() {
        return "Team{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }
}
