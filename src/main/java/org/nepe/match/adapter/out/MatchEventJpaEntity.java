package org.nepe.match.adapter.out;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;

/**
 * JPA Entity mapping for the {@code match_events} database table.
 * <p>
 * Stores in-game atomic events (Goals, Red Cards) linked to a match.
 */
@Entity
@Table(name = "match_events")
public class MatchEventJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "match_id", nullable = false)
    private Integer matchId;

    @Column(name = "event_type", nullable = false, length = 20)
    private String eventType;

    @Column(name = "minute", nullable = false)
    private int minute;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /**
     * Default no-args constructor required by JPA / Hibernate.
     */
    protected MatchEventJpaEntity() {
    }

    /**
     * Constructor for creating a new unpersisted event.
     */
    public MatchEventJpaEntity(Integer matchId, String eventType, int minute, Instant createdAt) {
        this(null, matchId, eventType, minute, createdAt);
    }

    /**
     * Full constructor for reconstruction and domain mapping.
     */
    public MatchEventJpaEntity(Integer id, Integer matchId, String eventType, int minute, Instant createdAt) {
        this.id = id;
        this.matchId = matchId;
        this.eventType = eventType;
        this.minute = minute;
        this.createdAt = (createdAt != null) ? createdAt : Instant.now();
    }

    // --- Getters and Setters ---

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getMatchId() {
        return matchId;
    }

    public void setMatchId(Integer matchId) {
        this.matchId = matchId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public int getMinute() {
        return minute;
    }

    public void setMinute(int minute) {
        this.minute = minute;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    // --- Identity, Equality and Diagnostics ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MatchEventJpaEntity that = (MatchEventJpaEntity) o;
        if (id != null && that.id != null) {
            return Objects.equals(id, that.id);
        }
        return minute == that.minute &&
                Objects.equals(matchId, that.matchId) &&
                Objects.equals(eventType, that.eventType) &&
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
        return "MatchEventJpaEntity{" +
                "id=" + id +
                ", matchId=" + matchId +
                ", eventType='" + eventType + '\'' +
                ", minute=" + minute +
                ", createdAt=" + createdAt +
                '}';
    }
}
