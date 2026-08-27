package org.nepe.competition.adapter.out;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Objects;

/**
 * JPA Entity mapping for the {@code team_aliases} database table.
 * <p>
 * Stores raw / alternative team name aliases (e.g. from Football-Data CSVs) mapped
 * to the foreign key {@code team_id} of the official team in MariaDB.
 */
@Entity
@Table(name = "team_aliases")
public class TeamAliasJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "alias_name", nullable = false, unique = true, length = 100)
    private String aliasName;

    @Column(name = "team_id", nullable = false)
    private Integer teamId;

    /**
     * Default no-args constructor required by JPA / Hibernate.
     */
    protected TeamAliasJpaEntity() {
    }

    /**
     * Constructor for creating a new alias before persistence (without an ID).
     */
    public TeamAliasJpaEntity(String aliasName, Integer teamId) {
        this(null, aliasName, teamId);
    }

    /**
     * Full constructor for reconstruction or mapping from the domain.
     */
    public TeamAliasJpaEntity(Integer id, String aliasName, Integer teamId) {
        this.id = id;
        this.aliasName = aliasName;
        this.teamId = teamId;
    }

    // --- Getters and Setters ---

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getAliasName() {
        return aliasName;
    }

    public void setAliasName(String aliasName) {
        this.aliasName = aliasName;
    }

    public Integer getTeamId() {
        return teamId;
    }

    public void setTeamId(Integer teamId) {
        this.teamId = teamId;
    }

    // --- Identity, Equality and Diagnostics ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TeamAliasJpaEntity that = (TeamAliasJpaEntity) o;
        if (id != null && that.id != null) {
            return Objects.equals(id, that.id);
        }
        return aliasName != null && aliasName.equalsIgnoreCase(that.aliasName);
    }

    @Override
    public int hashCode() {
        if (id != null) {
            return Objects.hash(id);
        }
        return Objects.hash(aliasName != null ? aliasName.toLowerCase() : null);
    }

    @Override
    public String toString() {
        return "TeamAliasJpaEntity{" +
                "id=" + id +
                ", aliasName='" + aliasName + '\'' +
                ", teamId=" + teamId +
                '}';
    }
}
