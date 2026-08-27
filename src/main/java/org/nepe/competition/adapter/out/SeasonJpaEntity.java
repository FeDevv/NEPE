package org.nepe.competition.adapter.out;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Objects;

/**
 * JPA Entity mapping for the {@code seasons} database table.
 * <p>
 * Stores the season name formatted as {@code YYYY/YYYY} (e.g., "2025/2026").
 * Resides exclusively in the outbound persistence adapter layer.
 */
@Entity
@Table(name = "seasons")
public class SeasonJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "name", nullable = false, unique = true, length = 9)
    private String name;

    /**
     * Default no-args constructor required by JPA / Hibernate.
     */
    protected SeasonJpaEntity() {
    }

    /**
     * Constructor for creating a new season before persistence (without an ID).
     */
    public SeasonJpaEntity(String name) {
        this(null, name);
    }

    /**
     * Full constructor for reconstruction or mapping from the domain.
     */
    public SeasonJpaEntity(Integer id, String name) {
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
        SeasonJpaEntity that = (SeasonJpaEntity) o;
        if (id != null && that.id != null) {
            return Objects.equals(id, that.id);
        }
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        if (id != null) {
            return Objects.hash(id);
        }
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "SeasonJpaEntity{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }
}
