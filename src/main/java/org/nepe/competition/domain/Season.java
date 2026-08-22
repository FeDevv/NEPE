package org.nepe.competition.domain;

import org.nepe.shared.domain.exception.DomainValidationException;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Domain entity representing a football sports season formatted as {@code YYYY/YYYY} (e.g., "2025/2026").
 * <p>
 * Implements {@link Comparable} to provide natural chronological ordering for historical lookups and fallbacks.
 */
public class Season implements Comparable<Season> {

    private static final Pattern SEASON_PATTERN = Pattern.compile("^(\\d{4})/(\\d{4})$");
    public static final int MIN_YEAR = 1900;
    public static final int MAX_YEAR = 2100;

    private Integer id;
    private String name;

    /**
     * Factory method for creating an unpersisted Season from start year (e.g., 2025 -> "2025/2026").
     *
     * @param startYear starting year of the season
     * @return a new validated {@link Season} instance
     */
    public static Season of(int startYear) {
        validateYearBounds(startYear);
        return new Season(null, String.format("%d/%d", startYear, startYear + 1));
    }

    /**
     * Factory method for creating an unpersisted Season from a string representation (e.g., "2025/2026").
     *
     * @param name formatted season name
     * @return a new validated {@link Season} instance
     */
    public static Season create(String name) {
        return new Season(null, name);
    }

    /**
     * Full constructor for domain reconstruction (e.g., when loaded from persistence).
     */
    public Season(Integer id, String name) {
        validateSeasonName(name);

        this.id = id;
        this.name = name.trim();
    }

    // --- Domain Business Logic & Calendar Queries ---

    public int getStartYear() {
        Matcher matcher = SEASON_PATTERN.matcher(name);
        if (matcher.matches()) {
            return Integer.parseInt(matcher.group(1));
        }
        throw new DomainValidationException("Invalid season internal state: " + name);
    }

    public int getEndYear() {
        Matcher matcher = SEASON_PATTERN.matcher(name);
        if (matcher.matches()) {
            return Integer.parseInt(matcher.group(2));
        }
        throw new DomainValidationException("Invalid season internal state: " + name);
    }

    /**
     * Returns the chronologically preceding season (e.g., "2025/2026" -> "2024/2025").
     */
    public Season previous() {
        return Season.of(getStartYear() - 1);
    }

    /**
     * Returns the chronologically following season (e.g., "2025/2026" -> "2026/2027").
     */
    public Season next() {
        return Season.of(getStartYear() + 1);
    }

    /**
     * Checks if this season is the direct predecessor of the provided season.
     */
    public boolean isDirectPredecessorOf(Season other) {
        if (other == null) return false;
        return this.getEndYear() == other.getStartYear();
    }

    public void assignId(Integer id) {
        if (id == null || id <= 0) {
            throw new DomainValidationException("Season ID must be a positive integer.");
        }
        if (this.id != null && !this.id.equals(id)) {
            throw new DomainValidationException("Cannot reassign an existing Season ID.");
        }
        this.id = id;
    }

    // --- Invariant Validations ---

    private static void validateSeasonName(String name) {
        if (name == null || name.isBlank()) {
            throw new DomainValidationException("Season name cannot be null or blank.");
        }

        Matcher matcher = SEASON_PATTERN.matcher(name.trim());
        if (!matcher.matches()) {
            throw new DomainValidationException(
                    String.format("Invalid season format '%s'. Expected format: YYYY/YYYY (e.g. 2025/2026).", name)
            );
        }

        int startYear = Integer.parseInt(matcher.group(1));
        int endYear = Integer.parseInt(matcher.group(2));

        validateYearBounds(startYear);

        if (endYear != startYear + 1) {
            throw new DomainValidationException(
                    String.format("Invalid season range '%s'. End year (%d) must be exactly start year (%d) + 1.",
                            name, endYear, startYear)
            );
        }
    }

    private static void validateYearBounds(int startYear) {
        if (startYear < MIN_YEAR || startYear >= MAX_YEAR) {
            throw new DomainValidationException(
                    String.format("Season start year (%d) must be between %d and %d.", startYear, MIN_YEAR, MAX_YEAR)
            );
        }
    }

    // --- Getters ---

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    // --- Identity & Equality based on unique season name ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Season season = (Season) o;
        return Objects.equals(name, season.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public int compareTo(Season o) {
        if (o == null) return 1;
        return Integer.compare(this.getStartYear(), o.getStartYear());
    }

    @Override
    public String toString() {
        return "Season{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }
}
