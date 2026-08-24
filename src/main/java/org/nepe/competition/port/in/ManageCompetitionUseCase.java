package org.nepe.competition.port.in;

import org.nepe.competition.domain.Competition;

import java.util.List;

/**
 * Inbound Port (Driving Port / Use Case) defining operations for managing competitions.
 * <p>
 * Invoked by Inbound Adapters (e.g., JavaFX Competition View Controller) to perform CRUD actions
 * and calibrate statistical model parameters (such as Dixon-Coles rho).
 */
public interface ManageCompetitionUseCase {

    /**
     * Registers a new competition in the system.
     *
     * @param command the {@link CreateCompetitionCommand} payload (must not be null)
     * @return the newly created and persisted {@link Competition}
     */
    Competition createCompetition(CreateCompetitionCommand command);

    /**
     * Updates descriptive details or Dixon-Coles rho parameter of an existing competition.
     *
     * @param command the {@link UpdateCompetitionCommand} payload (must not be null)
     * @return the updated and persisted {@link Competition}
     */
    Competition updateCompetition(UpdateCompetitionCommand command);

    /**
     * Retrieves all registered competitions, sorted alphabetically by name.
     *
     * @return list of all {@link Competition} instances
     */
    List<Competition> getAllCompetitions();

    /**
     * Retrieves a single competition by its database ID.
     *
     * @param id primary key identifier
     * @return the found {@link Competition}
     * @throws org.nepe.shared.exception.EntityNotFoundException if not found
     */
    Competition getCompetitionById(int id);

    /**
     * Retrieves a single competition by its unique code (e.g., "I1").
     *
     * @param code unique competition code
     * @return the found {@link Competition}
     * @throws org.nepe.shared.exception.EntityNotFoundException if not found
     */
    Competition getCompetitionByCode(String code);

    /**
     * Deletes a competition by its database ID.
     *
     * @param id primary key identifier
     */
    void deleteCompetition(int id);
}
