package org.nepe.competition.adapter.out;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Objects;

/**
 * JPA Entity mapping for the {@code teams} database table.
 * <p>
 * Resides exclusively in the outbound persistence adapter layer, decoupling
 * relational table definitions and ORM annotations from the pure domain core.
 */
@Entity
@Table(name = "teams")
public class TeamJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    /**
     * Default no-args constructor required by JPA / Hibernate.
     */
    protected TeamJpaEntity() {
    }

    /**
     * Constructor for creating a new entity before persistence (without an ID).
     */
    public TeamJpaEntity(String name) {
        this(null, name);
    }

    /**
     * Full constructor for reconstruction or mapping from the domain.
     */
    public TeamJpaEntity(Integer id, String name) {
        this.id = id;
        this.name = name;
    }

    // --- Getters and Setters ---

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    // --- Identity, Equality and Diagnostics ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TeamJpaEntity that = (TeamJpaEntity) o;
        if (id != null && that.id != null) {
            return Objects.equals(id, that.id);
        }
        return name != null && name.equalsIgnoreCase(that.name);
    }

    @Override
    public int hashCode() {
        if (id != null) {
            return Objects.hash(id);
        }
        return Objects.hash(name != null ? name.toLowerCase() : null);
    }

    @Override
    public String toString() {
        return "TeamJpaEntity{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }
}
