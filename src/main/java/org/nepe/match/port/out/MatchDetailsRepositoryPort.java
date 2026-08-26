package org.nepe.match.port.out;

import org.nepe.match.domain.MatchState;

import java.util.List;
import java.util.Optional;

/**
 * Outbound Port (Driven Port / SPI) defining read-only query operations for denormalized match details.
 * <p>
 * Backed by the database view {@code v_matches_details} to power the Dashboard TableView and Match cards
 * without incurring N+1 query overhead.
 */
public interface MatchDetailsRepositoryPort {

    /**
     * Retrieves full denormalized details for a single match by its ID.
     *
     * @param matchId primary key identifier
     * @return an {@link Optional} containing the details DTO, or empty if not found
     */
    Optional<MatchDetailsDTO> findDetailsById(int matchId);

    /**
     * Retrieves all match details for a specific competition and season, ordered by kickoff time.
     *
     * @param competitionId competition identifier
     * @param seasonId      season identifier
     * @return list of denormalized {@link MatchDetailsDTO} projections
     */
    List<MatchDetailsDTO> findDetailsByCompetitionAndSeason(int competitionId, int seasonId);

    /**
     * Retrieves match details filtered by lifecycle state within a competition and season.
     * Used by the Dashboard tabs (Scheduled, Live, Finished).
     *
     * @param competitionId competition identifier
     * @param seasonId      season identifier
     * @param state         match lifecycle state filter
     * @return list of matching {@link MatchDetailsDTO} projections
     */
    List<MatchDetailsDTO> findDetailsByCompetitionAndSeasonAndState(int competitionId, int seasonId, MatchState state);

    /**
     * Retrieves all match details across all leagues and seasons.
     *
     * @return list of all {@link MatchDetailsDTO} projections
     */
    List<MatchDetailsDTO> findAllDetails();
}
