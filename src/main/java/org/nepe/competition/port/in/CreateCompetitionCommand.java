package org.nepe.competition.port.in;

/**
 * Inbound Command DTO for registering a new football competition.
 *
 * @param code          unique Football-Data competition code (e.g. "I1", "E0")
 * @param name          descriptive league name (e.g. "Serie A", "Premier League")
 * @param country       host country (e.g. "Italy", "England")
 * @param dixonColesRho custom Dixon-Coles dependence coefficient (nullable, falls back to default -0.12)
 * @param homeAdvantage optional manual Home Advantage override ratio (nullable, null for dynamic auto-calculation)
 */
public record CreateCompetitionCommand(
        String code,
        String name,
        String country,
        Double dixonColesRho,
        Double homeAdvantage
) {
    public CreateCompetitionCommand(String code, String name, String country) {
        this(code, name, country, null, null);
    }

    public CreateCompetitionCommand(String code, String name, String country, Double dixonColesRho) {
        this(code, name, country, dixonColesRho, null);
    }
}
