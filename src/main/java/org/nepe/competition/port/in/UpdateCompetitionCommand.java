package org.nepe.competition.port.in;

/**
 * Inbound Command DTO for updating an existing competition's details, Dixon-Coles calibration, and home advantage.
 *
 * @param id            database identifier of the competition to update
 * @param name          updated descriptive league name
 * @param country       updated host country
 * @param dixonColesRho calibrated Dixon-Coles rho correlation coefficient
 * @param homeAdvantage optional manual Home Advantage override (null for auto-calculation)
 */
public record UpdateCompetitionCommand(
        int id,
        String name,
        String country,
        double dixonColesRho,
        Double homeAdvantage
) {
    public UpdateCompetitionCommand(int id, String name, String country, double dixonColesRho) {
        this(id, name, country, dixonColesRho, null);
    }
}
