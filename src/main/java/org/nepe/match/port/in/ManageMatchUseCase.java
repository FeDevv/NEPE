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
     * Retrieves all match details across all competitions and seasons, ordered chronologically.
     *
     * @return list of all {@link MatchDetailsDTO} projections
     */
    List<MatchDetailsDTO> getAllMatchDetails();

    /**
     * Retrieves historical match performances for a specific team in a competition,
     * enforcing the scientific N_min = 10 sample size rule with inter-season gamma decay.
     *
     * @param teamId        team identifier
     * @param competitionId competition identifier
     * @param seasonId      current season identifier
     * @param minSampleSize minimum historical matches required (default 10)
     * @return list of chronological {@link org.nepe.inference.domain.TeamStrengthCalculator.MatchPerformance} objects
     */
    List<org.nepe.inference.domain.TeamStrengthCalculator.MatchPerformance> getHistoricalTeamPerformances(
            int teamId,
            int competitionId,
            int seasonId,
            int minSampleSize
    );

    /**
     * Calculates the league average xG per team per match in a competition/season.
     *
     * @param competitionId competition identifier
     * @param seasonId      season identifier
     * @return average xG per team (e.g. 1.35)
     */
    double getLeagueAverageXgPerTeam(int competitionId, int seasonId);

    /**
     * Deletes a match by its database ID.
     *
     * @param matchId primary key identifier
     */
    void deleteMatch(int matchId);
}
