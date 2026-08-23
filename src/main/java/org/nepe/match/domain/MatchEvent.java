package org.nepe.match.domain;

import org.nepe.shared.exception.DomainValidationException;

import java.time.Instant;
import java.util.Objects;

/**
 * Domain entity representing an atomic in-game event (Goal or Red Card) tied to a specific match.
 * <p>
 * Implements {@link Comparable} for chronological ordering by minute and timestamp.
 */
public class MatchEvent implements Comparable<MatchEvent> {

    public static final int MIN_MINUTE = 0;
    public static final int MAX_MINUTE = 130; // Accommodates regular time, injury time, and extra time

    private Integer id;
    private Integer matchId;
    private MatchEventType eventType;
    private int minute;
    private Instant createdAt;

    /**
     * Factory method for creating a newly recorded MatchEvent with the current timestamp.
     */
    public static MatchEvent create(Integer matchId, MatchEventType eventType, int minute) {
        return new MatchEvent(null, matchId, eventType, minute, Instant.now());
    }

    public static MatchEvent goalHome(Integer matchId, int minute) {
        return create(matchId, MatchEventType.GOAL_HOME, minute);
    }

    public static MatchEvent goalAway(Integer matchId, int minute) {
        return create(matchId, MatchEventType.GOAL_AWAY, minute);
    }

    public static MatchEvent redCardHome(Integer matchId, int minute) {
        return create(matchId, MatchEventType.RED_CARD_HOME, minute);
    }

    public static MatchEvent redCardAway(Integer matchId, int minute) {
        return create(matchId, MatchEventType.RED_CARD_AWAY, minute);
    }

    /**
     * Full constructor for domain reconstruction (e.g., loaded from persistence).
     */
    public MatchEvent(Integer id, Integer matchId, MatchEventType eventType, int minute, Instant createdAt) {
        validateMatchId(matchId);
        validateEventType(eventType);
        validateMinute(minute);

        this.id = id;
        this.matchId = matchId;
        this.eventType = eventType;
        this.minute = minute;
        this.createdAt = (createdAt != null) ? createdAt : Instant.now();
    }

    // --- Domain Queries ---

    public boolean isGoal() {
        return eventType.isGoal();
    }

    public boolean isRedCard() {
        return eventType.isRedCard();
    }

    public boolean isHomeTeamEvent() {
        return eventType.isHomeTeamEvent();
    }

    public boolean isAwayTeamEvent() {
        return eventType.isAwayTeamEvent();
    }

    public void assignId(Integer id) {
        if (id == null || id <= 0) {
            throw new DomainValidationException("MatchEvent ID must be a positive integer.");
        }
        if (this.id != null && !this.id.equals(id)) {
            throw new DomainValidationException("Cannot reassign an existing MatchEvent ID.");
        }
        this.id = id;
    }

    // --- Invariant Validations ---

    private static void validateMatchId(Integer matchId) {
        if (matchId == null || matchId <= 0) {
            throw new DomainValidationException("MatchEvent must be associated with a valid positive matchId.");
        }
    }

    private static void validateEventType(MatchEventType eventType) {
        if (eventType == null) {
            throw new DomainValidationException("MatchEventType cannot be null.");
        }
    }

    private static void validateMinute(int minute) {
        if (minute < MIN_MINUTE || minute > MAX_MINUTE) {
            throw new DomainValidationException(
                    String.format("MatchEvent minute must be between %d and %d (received: %d).", MIN_MINUTE, MAX_MINUTE, minute)
            );
        }
    }

    // --- Getters ---

    public Integer getId() {
        return id;
    }

    public Integer getMatchId() {
        return matchId;
    }

    public MatchEventType getEventType() {
        return eventType;
    }

    public int getMinute() {
        return minute;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    // --- Chronological Ordering ---

    @Override
    public int compareTo(MatchEvent other) {
        if (other == null) return 1;
        int minuteCmp = Integer.compare(this.minute, other.minute);
        if (minuteCmp != 0) {
            return minuteCmp;
        }
        return this.createdAt.compareTo(other.createdAt);
    }

    // --- Equality & Identity ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MatchEvent that = (MatchEvent) o;
        if (id != null && that.id != null) {
            return Objects.equals(id, that.id);
        }
        return minute == that.minute &&
                Objects.equals(matchId, that.matchId) &&
                eventType == that.eventType &&
                Objects.equals(createdAt, that.createdAt);
    }

    @Override
    public int hashCode() {
        if (id != null) {
            return Objects.hash(id);
        }
        return Objects.hash(matchId, eventType, minute, createdAt);
    }

    @Override
    public String toString() {
        return "MatchEvent{" +
                "id=" + id +
                ", matchId=" + matchId +
                ", eventType=" + eventType +
                ", minute=" + minute +
                ", createdAt=" + createdAt +
                '}';
    }
}
