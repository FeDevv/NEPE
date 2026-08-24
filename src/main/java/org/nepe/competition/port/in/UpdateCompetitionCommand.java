package org.nepe.competition.port.in;

/**
 * Inbound Command DTO for updating an existing competition's details and Dixon-Coles calibration.
 *
 * @param id            database identifier of the competition to update
 * @param name          updated descriptive league name
 * @param country       updated host country
 * @param dixonColesRho calibrated Dixon-Coles rho correlation coefficient
 */
public record UpdateCompetitionCommand(
        int id,
        String name,
        String country,
        double dixonColesRho
) {
}
