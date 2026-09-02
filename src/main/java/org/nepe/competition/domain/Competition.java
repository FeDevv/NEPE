package org.nepe.competition.domain;

import org.nepe.shared.exception.DomainValidationException;

import java.util.Objects;

/**
 * Domain entity representing a football competition / league (e.g., Serie A, Premier League).
 * <p>
 * Encapsulates the league identification, metadata, and the competition-specific
 * Dixon-Coles dependence parameter ({@code rho}) used for low-score joint probability correction.
 */
public class Competition {

    public static final double DEFAULT_DIXON_COLES_RHO = -0.1200;
    public static final double MIN_RHO = -1.0;
    public static final double MAX_RHO = 1.0;
    public static final double MIN_HOME_ADVANTAGE = 1.0;
    public static final double MAX_HOME_ADVANTAGE = 2.0;

    private Integer id;
    private String code;
    private String name;
    private String country;
    private double dixonColesRho;
    private Double homeAdvantage;

    /**
     * Factory method for creating a new Competition without a persisted ID and with default Dixon-Coles rho.
     *
     * @param code    unique Football-Data competition code (e.g., "I1", "E0")
     * @param name    descriptive league name (e.g., "Serie A")
     * @param country host country (e.g., "Italy")
     * @return a new validated {@link Competition} instance
     */
    public static Competition create(String code, String name, String country) {
        return new Competition(null, code, name, country, DEFAULT_DIXON_COLES_RHO, null);
    }

    /**
     * Factory method for creating a new Competition with a custom Dixon-Coles rho.
     *
     * @param code          unique Football-Data competition code
     * @param name          descriptive league name
     * @param country       host country
     * @param dixonColesRho custom Dixon-Coles correlation coefficient
     * @return a new validated {@link Competition} instance
     */
    public static Competition create(String code, String name, String country, double dixonColesRho) {
        return new Competition(null, code, name, country, dixonColesRho, null);
    }

    /**
     * Factory method for creating a new Competition with a custom Dixon-Coles rho and optional manual Home Advantage.
     *
     * @param code          unique Football-Data competition code
     * @param name          descriptive league name
     * @param country       host country
     * @param dixonColesRho custom Dixon-Coles correlation coefficient
     * @param homeAdvantage optional manual Home Advantage override (null for auto-calculation)
     * @return a new validated {@link Competition} instance
     */
    public static Competition create(String code, String name, String country, double dixonColesRho, Double homeAdvantage) {
        return new Competition(null, code, name, country, dixonColesRho, homeAdvantage);
    }

    /**
     * Full constructor for domain reconstruction without home advantage override (defaults to null / automatic).
     */
    public Competition(Integer id, String code, String name, String country, double dixonColesRho) {
        this(id, code, name, country, dixonColesRho, null);
    }

    /**
     * Full constructor for domain reconstruction (e.g., when loaded from persistence).
     */
    public Competition(Integer id, String code, String name, String country, double dixonColesRho, Double homeAdvantage) {
        validateCode(code);
        validateName(name);
        validateCountry(country);
        validateDixonColesRho(dixonColesRho);
        validateHomeAdvantage(homeAdvantage);

        this.id = id;
        this.code = code.trim().toUpperCase();
        this.name = name.trim();
        this.country = country.trim();
        this.dixonColesRho = dixonColesRho;
        this.homeAdvantage = homeAdvantage;
    }

    // --- Domain Business Logic & State Mutations ---

    public void updateDetails(String name, String country) {
        validateName(name);
        validateCountry(country);
        this.name = name.trim();
        this.country = country.trim();
    }

    public void updateDixonColesRho(double dixonColesRho) {
        validateDixonColesRho(dixonColesRho);
        this.dixonColesRho = dixonColesRho;
    }

    public void updateHomeAdvantage(Double homeAdvantage) {
        validateHomeAdvantage(homeAdvantage);
        this.homeAdvantage = homeAdvantage;
    }

    public boolean hasManualHomeAdvantage() {
        return homeAdvantage != null;
    }

    public void assignId(Integer id) {
        if (id == null || id <= 0) {
            throw new DomainValidationException("Competition ID must be a positive integer.");
        }
        if (this.id != null && !this.id.equals(id)) {
            throw new DomainValidationException("Cannot reassign an existing Competition ID.");
        }
        this.id = id;
    }

    // --- Invariant Validations ---

    private static void validateCode(String code) {
        if (code == null || code.isBlank()) {
            throw new DomainValidationException("Competition code cannot be null or blank.");
        }
        if (code.trim().length() > 10) {
            throw new DomainValidationException("Competition code cannot exceed 10 characters.");
        }
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new DomainValidationException("Competition name cannot be null or blank.");
        }
        if (name.trim().length() > 100) {
            throw new DomainValidationException("Competition name cannot exceed 100 characters.");
        }
    }

    private static void validateCountry(String country) {
        if (country == null || country.isBlank()) {
            throw new DomainValidationException("Competition country cannot be null or blank.");
        }
        if (country.trim().length() > 50) {
            throw new DomainValidationException("Competition country cannot exceed 50 characters.");
        }
    }

    private static void validateDixonColesRho(double rho) {
        if (Double.isNaN(rho) || Double.isInfinite(rho)) {
            throw new DomainValidationException("Dixon-Coles rho must be a valid finite number.");
        }
        if (rho <= MIN_RHO || rho >= MAX_RHO) {
            throw new DomainValidationException(
                    String.format("Dixon-Coles rho must be strictly between %f and %f (received: %f).", MIN_RHO, MAX_RHO, rho)
            );
        }
    }

    private static void validateHomeAdvantage(Double homeAdvantage) {
        if (homeAdvantage == null) {
            return;
        }
        if (Double.isNaN(homeAdvantage) || Double.isInfinite(homeAdvantage)) {
            throw new DomainValidationException("Home advantage must be a valid finite number.");
        }
        if (homeAdvantage < MIN_HOME_ADVANTAGE || homeAdvantage > MAX_HOME_ADVANTAGE) {
            throw new DomainValidationException(
                    String.format("Home advantage must be between %.2f and %.2f (received: %.2f).",
                            MIN_HOME_ADVANTAGE, MAX_HOME_ADVANTAGE, homeAdvantage)
            );
        }
    }

    // --- Getters ---

    public Integer getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getCountry() {
        return country;
    }

    public double getDixonColesRho() {
        return dixonColesRho;
    }

    public Double getHomeAdvantage() {
        return homeAdvantage;
    }

    // --- Identity & Equality based on unique business key (code) ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Competition that = (Competition) o;
        return Objects.equals(code, that.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code);
    }

    @Override
    public String toString() {
        return "Competition{" +
                "id=" + id +
                ", code='" + code + '\'' +
                ", name='" + name + '\'' +
                ", country='" + country + '\'' +
                ", dixonColesRho=" + dixonColesRho +
                ", homeAdvantage=" + homeAdvantage +
                '}';
    }
}
