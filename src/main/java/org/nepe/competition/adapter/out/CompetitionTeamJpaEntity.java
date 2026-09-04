package org.nepe.competition.adapter.out;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.Objects;

/**
 * JPA Entity mapping for the {@code competition_teams} association table.
 * <p>
 * Represents the many-to-many link between competitions and teams,
 * isolated entirely inside the outbound persistence adapter layer.
 */
@Entity
@Table(name = "competition_teams")
@IdClass(CompetitionTeamJpaEntity.CompetitionTeamId.class)
public class CompetitionTeamJpaEntity {

    @Id
    @Column(name = "competition_id", nullable = false)
    private Integer competitionId;

    @Id
    @Column(name = "team_id", nullable = false)
    private Integer teamId;

    /**
     * Default no-args constructor required by JPA / Hibernate.
     */
    protected CompetitionTeamJpaEntity() {
    }

    /**
     * Full constructor for creating a new association.
     *
     * @param competitionId competition database identifier
     * @param teamId        team database identifier
     */
    public CompetitionTeamJpaEntity(Integer competitionId, Integer teamId) {
        this.competitionId = competitionId;
        this.teamId = teamId;
    }

    public Integer getCompetitionId() {
        return competitionId;
    }

    public void setCompetitionId(Integer competitionId) {
        this.competitionId = competitionId;
    }

    public Integer getTeamId() {
        return teamId;
    }

    public void setTeamId(Integer teamId) {
        this.teamId = teamId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CompetitionTeamJpaEntity that = (CompetitionTeamJpaEntity) o;
        return Objects.equals(competitionId, that.competitionId) && Objects.equals(teamId, that.teamId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(competitionId, teamId);
    }

    @Override
    public String toString() {
        return "CompetitionTeamJpaEntity{" +
                "competitionId=" + competitionId +
                ", teamId=" + teamId +
                '}';
    }

    /**
     * Composite primary key identifier class for {@link CompetitionTeamJpaEntity}.
     */
    public static class CompetitionTeamId implements Serializable {

        private Integer competitionId;
        private Integer teamId;

        public CompetitionTeamId() {
        }

        public CompetitionTeamId(Integer competitionId, Integer teamId) {
            this.competitionId = competitionId;
            this.teamId = teamId;
        }

        public Integer getCompetitionId() {
            return competitionId;
        }

        public void setCompetitionId(Integer competitionId) {
            this.competitionId = competitionId;
        }

        public Integer getTeamId() {
            return teamId;
        }

        public void setTeamId(Integer teamId) {
            this.teamId = teamId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CompetitionTeamId that = (CompetitionTeamId) o;
            return Objects.equals(competitionId, that.competitionId) && Objects.equals(teamId, that.teamId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(competitionId, teamId);
        }
    }
}
