package org.nepe.match.port.in;

import org.nepe.match.domain.Match;
import org.nepe.match.domain.MatchState;
import org.nepe.match.port.out.MatchDetailsDTO;

import java.util.List;

/**
 * Inbound Port (Driving Port / Use Case) defining operations for scheduling, updating,
 * and managing football matches and their lifecycle states.
 * <p>
 * Invoked by Inbound Adapters (e.g., JavaFX Dashboard, Match Editor, Competition Views).
 */
public interface ManageMatchUseCase {

    /**
     * Creates and schedules a new match.
     *
     * @param command the {@link CreateMatchCommand} payload (must not be null)
     * @return the created and persisted {@link Match}
     */
    Match createMatch(CreateMatchCommand command);

    /**
     * Updates match kickoff schedule, reference odds, and tactical modifiers.
     *
     * @param command the {@link UpdateMatchCommand} payload (must not be null)
     * @return the updated and persisted {@link Match}
     */
    Match updateMatch(UpdateMatchCommand command);

    /**
     * Manually updates or overrides match statistics, scoreline, and xG values.
     * Marks the match as manually edited to prevent CSV overwrite.
     *
     * @param command the {@link UpdateMatchStatisticsCommand} payload (must not be null)
     * @return the updated and persisted {@link Match}
     */
    Match updateStatistics(UpdateMatchStatisticsCommand command);

    /**
     * Transitions a match to POSTPONED state.
     *
     * @param matchId primary key identifier
     * @return the updated {@link Match}
     */
    Match markAsPostponed(int matchId);

    /**
     * Transitions a match to CANCELLED state.
     *
     * @param matchId primary key identifier
     * @return the updated {@link Match}
     */
    Match markAsCancelled(int matchId);

    /**
     * Manually finishes a match, transitioning state to FINISHED.
     *
     * @param matchId primary key identifier
     * @return the updated {@link Match}
     */
    Match markAsFinished(int matchId);

    /**
     * Retrieves an aggregate match entity by its database ID.
     *
     * @param matchId primary key identifier
     * @return the found {@link Match}
     * @throws org.nepe.shared.exception.EntityNotFoundException if not found
     */
    Match getMatchById(int matchId);

    /**
     * Retrieves full denormalized details for a match (including team and league names).
     *
     * @param matchId primary key identifier
     * @return the {@link MatchDetailsDTO} projection
     * @throws org.nepe.shared.exception.EntityNotFoundException if not found
     */
    MatchDetailsDTO getMatchDetailsById(int matchId);

    /**
     * Retrieves all match details for a specific competition and season.
     *
     * @param competitionId competition identifier
     * @param seasonId      season identifier
     * @return list of denormalized {@link MatchDetailsDTO} projections
     */
    List<MatchDetailsDTO> getMatchDetailsByCompetitionAndSeason(int competitionId, int seasonId);

    /**
     * Retrieves match details filtered by lifecycle state within a competition and season.
     *
     * @param competitionId competition identifier
     * @param seasonId      season identifier
     * @param state         state filter (e.g. SCHEDULED, LIVE, FINISHED)
     * @return list of matching {@link MatchDetailsDTO} projections
     */
    List<MatchDetailsDTO> getMatchDetailsByState(int competitionId, int seasonId, MatchState state);

    /**
     * Deletes a match by its database ID.
     *
     * @param matchId primary key identifier
     */
    void deleteMatch(int matchId);
}
