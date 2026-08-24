package org.nepe.competition.port.in;

import org.nepe.competition.domain.Season;

import java.util.List;

/**
 * Inbound Port (Driving Port / Use Case) defining operations for managing football seasons.
 * <p>
 * Invoked by Inbound Adapters (e.g., JavaFX Controllers, Season Dropdowns, and CSV Import Services).
 */
public interface ManageSeasonUseCase {

    /**
     * Retrieves an existing season by name or creates it if it does not exist yet.
     * Useful for idempotent CSV imports.
     *
     * @param seasonName formatted season name (e.g., "2025/2026")
     * @return existing or newly created {@link Season}
     */
    Season getOrCreateSeason(String seasonName);

    /**
     * Creates and persists a new season from a formatted string name (e.g. "2025/2026").
     *
     * @param name formatted season name
     * @return the created {@link Season}
     */
    Season createSeason(String name);

    /**
     * Creates and persists a new season from a starting year (e.g. 2025 -> "2025/2026").
     *
     * @param startYear starting year of the season
     * @return the created {@link Season}
     */
    Season createSeasonFromYear(int startYear);

    /**
     * Retrieves all seasons registered in the system, ordered chronologically (newest first).
     *
     * @return list of all {@link Season} instances
     */
    List<Season> getAllSeasons();

    /**
     * Retrieves the most recent registered season (the active/latest season).
     *
     * @return the latest {@link Season}
     * @throws org.nepe.shared.exception.EntityNotFoundException if no seasons exist
     */
    Season getLatestSeason();

    /**
     * Retrieves a season by its database ID.
     *
     * @param id primary key identifier
     * @return the found {@link Season}
     * @throws org.nepe.shared.exception.EntityNotFoundException if not found
     */
    Season getSeasonById(int id);

    /**
     * Retrieves a season by its formatted name (e.g., "2025/2026").
     *
     * @param name formatted season name
     * @return the found {@link Season}
     * @throws org.nepe.shared.exception.EntityNotFoundException if not found
     */
    Season getSeasonByName(String name);

    /**
     * Deletes a season by its database ID.
     *
     * @param id primary key identifier
     */
    void deleteSeason(int id);
}
