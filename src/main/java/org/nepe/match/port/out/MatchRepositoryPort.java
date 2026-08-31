package org.nepe.match.port.out;

import org.nepe.match.domain.Match;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Outbound Port (Driven Port / SPI) defining persistence operations for {@link Match} Aggregate Roots.
 * <p>
 * Decouples the core match domain and trading logic from database persistence (MariaDB / Spring Data JPA).
 */
public interface MatchRepositoryPort {

    /**
     * Persists a match entity (inserting if new, or updating if existing).
     *
     * @param match the {@link Match} aggregate to save (must not be null)
     * @return the saved {@link Match} instance with populated ID
     */
    Match save(Match match);

    /**
     * Finds a match by its unique database ID.
     *
     * @param id primary key identifier
     * @return an {@link Optional} containing the found match, or empty if not found
     */
    Optional<Match> findById(int id);

    /**
     * Finds a match by its unique natural business key (Home Team, Away Team, Kickoff Date/Time).
     * Critical for upsert matching during repeated CSV imports.
     *
     * @param homeTeamId    Home team identifier
     * @param awayTeamId    Away team identifier
     * @param matchDateTime exact kickoff date and time (UTC)
     * @return an {@link Optional} containing the match if already present in DB
     */
     Optional<Match> findByTeamsAndDateTime(int homeTeamId, int awayTeamId, Instant matchDateTime);

    /**
     * Finds a match between two teams within a date range (same UTC calendar day).
     * Used for idempotent CSV re-imports where match kickoff time may be unspecified (12:00 UTC) or updated.
     *
     * @param homeTeamId Home team identifier
     * @param awayTeamId Away team identifier
     * @param startOfDay start of the match day (UTC)
     * @param endOfDay   end of the match day (UTC)
     * @return an {@link Optional} containing the match if found
     */
    Optional<Match> findByTeamsAndDateRange(int homeTeamId, int awayTeamId, Instant startOfDay, Instant endOfDay);

    /**
     * Retrieves all matches belonging to a specific competition and season.
     *
     * @param competitionId competition identifier
     * @param seasonId      season identifier
     * @return list of {@link Match} entities, ordered by kickoff date/time
     */
    List<Match> findByCompetitionAndSeason(int competitionId, int seasonId);

    /**
     * Retrieves all finished matches belonging to a specific competition and season.
     *
     * @param competitionId competition identifier
     * @param seasonId      season identifier
     * @return list of finished {@link Match} entities, ordered by kickoff date/time
     */
    List<Match> findFinishedMatchesByCompetitionAndSeason(int competitionId, int seasonId);

    /**
     * Retrieves finished matches for a specific team within a competition and season, ordered newest first.
     *
     * @param teamId        team identifier
     * @param competitionId competition identifier
     * @param seasonId      season identifier
     * @return list of finished {@link Match} entities
     */
    List<Match> findFinishedMatchesForTeamInSeason(int teamId, int competitionId, int seasonId);

    /**
     * Retrieves the most recent finished matches for a specific team within a competition,
     * ordered from newest to oldest. Used to compute offensive/defensive strength ratings.
     *
     * @param teamId        team identifier
     * @param competitionId competition identifier
     * @param limit         maximum number of historical matches to retrieve
     * @return list of recent finished {@link Match} entities
     */
    List<Match> findRecentMatchesForTeam(int teamId, int competitionId, int limit);

    /**
     * Retrieves all matches registered in the database.
     *
     * @return list of all {@link Match} entities
     */
    List<Match> findAll();

    /**
     * Deletes a match by its database ID.
     *
     * @param id primary key identifier
     */
    void deleteById(int id);

    /**
     * Returns the total count of registered matches.
     *
     * @return total count
     */
    long count();
}
