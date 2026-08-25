package org.nepe.match.port.out;

import org.nepe.match.domain.MarketOdds;
import org.nepe.match.domain.MarketType;

import java.util.List;
import java.util.Optional;

/**
 * Outbound Port (Driven Port / SPI) defining persistence operations for {@link MarketOdds} entities.
 * <p>
 * Decouples Betting Exchange market quote storage (Back and Lay odds) from database mechanisms.
 */
public interface MarketOddsRepositoryPort {

    /**
     * Persists a market odds entity (inserting or updating).
     *
     * @param marketOdds the {@link MarketOdds} instance to save (must not be null)
     * @return the saved {@link MarketOdds} instance with populated ID
     */
    MarketOdds save(MarketOdds marketOdds);

    /**
     * Persists a collection of market odds entities in batch.
     *
     * @param oddsList list of {@link MarketOdds} entities to save
     * @return list of saved {@link MarketOdds} entities
     */
    List<MarketOdds> saveAll(List<MarketOdds> oddsList);

    /**
     * Finds a market odds record by its primary key ID.
     *
     * @param id primary key identifier
     * @return an {@link Optional} containing the record, or empty if not found
     */
    Optional<MarketOdds> findById(int id);

    /**
     * Retrieves all market odds associated with a specific match.
     *
     * @param matchId target match identifier
     * @return list of {@link MarketOdds} entities
     */
    List<MarketOdds> findByMatchId(int matchId);

    /**
     * Finds a specific market outcome odds record for a match.
     *
     * @param matchId    target match identifier
     * @param marketType market type (e.g., MATCH_ODDS, UNDER_OVER_25)
     * @param outcome    outcome label (e.g., "1", "OVER")
     * @return an {@link Optional} containing the record, or empty if not found
     */
    Optional<MarketOdds> findByMatchIdAndMarketTypeAndOutcome(int matchId, MarketType marketType, String outcome);

    /**
     * Deletes a market odds record by its database ID.
     *
     * @param id primary key identifier
     */
    void deleteById(int id);

    /**
     * Deletes all market odds records associated with a match.
     *
     * @param matchId target match identifier
     */
    void deleteByMatchId(int matchId);

    /**
     * Returns total count of market odds records.
     *
     * @return total count
     */
    long count();
}
