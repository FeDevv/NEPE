package org.nepe.competition.adapter.out;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Objects;

/**
 * JPA Entity mapping for the {@code competitions} database table.
 * <p>
 * This entity resides exclusively within the outbound adapter layer, ensuring that
 * ORM / persistence concerns (Jakarta Persistence annotations) remain completely
 * isolated from the pure domain core.
 */
@Entity
@Table(name = "competitions")
public class CompetitionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "code", nullable = false, unique = true, length = 10)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "country", nullable = false, length = 50)
    private String country;

    @Column(name = "dixon_coles_rho", nullable = false, precision = 5, scale = 4)
    private double dixonColesRho;

    /**
     * Default no-args constructor required by JPA / Hibernate.
     */
    protected CompetitionJpaEntity() {
    }

    /**
     * Constructor for creating a new entity before persistence (without an ID).
     */
    public CompetitionJpaEntity(String code, String name, String country, double dixonColesRho) {
        this(null, code, name, country, dixonColesRho);
    }

    /**
     * Full constructor for reconstruction or mapping from the domain.
     */
    public CompetitionJpaEntity(Integer id, String code, String name, String country, double dixonColesRho) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.country = country;
        this.dixonColesRho = dixonColesRho;
    }

    // --- Getters and Setters ---

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public double getDixonColesRho() {
        return dixonColesRho;
    }

    public void setDixonColesRho(double dixonColesRho) {
        this.dixonColesRho = dixonColesRho;
    }

    // --- Identity, Equality and Diagnostics ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CompetitionJpaEntity that = (CompetitionJpaEntity) o;
        if (id != null && that.id != null) {
            return Objects.equals(id, that.id);
        }
        return Objects.equals(code, that.code);
    }

    @Override
    public int hashCode() {
        if (id != null) {
            return Objects.hash(id);
        }
        return Objects.hash(code);
    }

    @Override
    public String toString() {
        return "CompetitionJpaEntity{" +
                "id=" + id +
                ", code='" + code + '\'' +
                ", name='" + name + '\'' +
                ", country='" + country + '\'' +
                ", dixonColesRho=" + dixonColesRho +
                '}';
    }
}
