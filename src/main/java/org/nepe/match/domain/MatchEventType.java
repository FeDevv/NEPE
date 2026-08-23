package org.nepe.match.domain;

import org.nepe.shared.exception.DomainValidationException;

/**
 * Types of discrete events that can occur during a football match.
 */
public enum MatchEventType {

    /**
     * Goal scored by the home team.
     */
    GOAL_HOME,

    /**
     * Goal scored by the away team.
     */
    GOAL_AWAY,

    /**
     * Red card issued to a player of the home team.
     */
    RED_CARD_HOME,

    /**
     * Red card issued to a player of the away team.
     */
    RED_CARD_AWAY;

    /**
     * Checks if this event represents a scored goal.
     */
    public boolean isGoal() {
        return this == GOAL_HOME || this == GOAL_AWAY;
    }

    /**
     * Checks if this event represents a red card (expulsion).
     */
    public boolean isRedCard() {
        return this == RED_CARD_HOME || this == RED_CARD_AWAY;
    }

    /**
     * Checks if the event is associated with the home team.
     */
    public boolean isHomeTeamEvent() {
        return this == GOAL_HOME || this == RED_CARD_HOME;
    }

    /**
     * Checks if the event is associated with the away team.
     */
    public boolean isAwayTeamEvent() {
        return this == GOAL_AWAY || this == RED_CARD_AWAY;
    }

    /**
     * Validates and parses a string representation into a {@link MatchEventType}.
     *
     * @param value raw event name
     * @return corresponding {@link MatchEventType}
     * @throws DomainValidationException if the value is null, blank, or unrecognized
     */
    public static MatchEventType fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new DomainValidationException("MatchEventType cannot be null or blank.");
        }
        try {
            return MatchEventType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new DomainValidationException(
                    String.format("Unrecognized MatchEventType: '%s'. Valid types: GOAL_HOME, GOAL_AWAY, RED_CARD_HOME, RED_CARD_AWAY.", value),
                    e
            );
        }
    }
}
