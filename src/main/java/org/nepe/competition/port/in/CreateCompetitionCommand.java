package org.nepe.competition.port.in;

/**
 * Inbound Command DTO for registering a new football competition.
 *
 * @param code          unique Football-Data competition code (e.g. "I1", "E0")
 * @param name          descriptive league name (e.g. "Serie A", "Premier League")
 * @param country       host country (e.g. "Italy", "England")
 * @param dixonColesRho custom Dixon-Coles dependence coefficient (nullable, falls back to default -0.12)
 */
public record CreateCompetitionCommand(
        String code,
        String name,
        String country,
        Double dixonColesRho
) {
    public CreateCompetitionCommand(String code, String name, String country) {
        this(code, name, country, null);
    }
}
