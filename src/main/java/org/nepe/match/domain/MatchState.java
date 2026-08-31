package org.nepe.match.domain;

import org.nepe.shared.exception.DomainValidationException;

/**
 * Lifecycle state of a football match within NEPE.
 */
public enum MatchState {

    /**
     * Match is scheduled to be played in the future. Eligible for pre-match analysis and odds evaluation.
     */
    SCHEDULED,

    /**
     * Match is currently in progress. Live console operations and real-time re-estimations are active.
     */
    LIVE,

    /**
     * Match has ended (full time). Scores and statistics are definitive for historical analysis.
     */
    FINISHED,

    /**
     * Match has been postponed to a later date/time.
     */
    POSTPONED,

    /**
     * Match was cancelled or abandoned.
     */
    CANCELLED;

    /**
     * Checks if the match is currently in progress.
     */
    public boolean isLive() {
        return this == LIVE;
    }

    /**
     * Checks if the match is scheduled or awaiting kick-off.
     */
    public boolean isScheduled() {
        return this == SCHEDULED;
    }

    /**
     * Checks if the match is completed.
     */
    public boolean isFinished() {
        return this == FINISHED;
    }

    /**
     * Checks if the match is in a terminal state (cannot undergo standard live updates).
     */
    public boolean isTerminal() {
        return this == FINISHED || this == CANCELLED;
    }

    /**
     * Checks if the state allows pre-match inference and odds calculations.
     */
    public boolean allowsPreMatchAnalysis() {
        return this == SCHEDULED || this == POSTPONED;
    }

    /**
     * Checks if the state allows live console event tracking.
     */
    public boolean allowsLiveTrading() {
        return this == LIVE;
    }

    /**
     * Validates and returns a {@link MatchState} from a string representation.
     *
     * @param value raw state name
     * @return corresponding {@link MatchState}
     * @throws DomainValidationException if the value is null, blank, or unrecognized
     */
    public static MatchState fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new DomainValidationException("MatchState cannot be null or blank.");
        }
        try {
            return MatchState.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new DomainValidationException(
                    String.format("Unrecognized MatchState: '%s'. Valid states: SCHEDULED, LIVE, FINISHED, POSTPONED, CANCELLED.", value),
                    e
            );
        }
    }
}
