package org.nepe.match.port.in;

import org.nepe.match.domain.MarketOdds;

import java.util.List;

/**
 * Inbound Port (Driving Port / Use Case) defining operations for managing Betting Exchange market odds.
 * <p>
 * Invoked by Inbound Adapters (e.g. Pre-Match Analysis Controller, Live Console).
 */
public interface ManageMarketOddsUseCase {

    /**
     * Creates or updates market odds for a single outcome.
     *
     * @param command the {@link SaveMarketOddsCommand} payload (must not be null)
     * @return the saved {@link MarketOdds} instance
     */
    MarketOdds saveOdds(SaveMarketOddsCommand command);

    /**
     * Creates or updates a collection of market odds in batch.
     *
     * @param commands list of {@link SaveMarketOddsCommand} payloads
     * @return list of saved {@link MarketOdds} instances
     */
    List<MarketOdds> saveBatchOdds(List<SaveMarketOddsCommand> commands);

    /**
     * Retrieves all market odds registered for a specific match.
     *
     * @param matchId primary key identifier of the match
     * @return list of {@link MarketOdds} instances
     */
    List<MarketOdds> getOddsForMatch(int matchId);

    /**
     * Deletes all market odds associated with a match.
     *
     * @param matchId primary key identifier of the match
     */
    void deleteOddsForMatch(int matchId);
}
