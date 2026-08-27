package org.nepe.match.adapter.out;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link MarketOddsJpaEntity}.
 * <p>
 * Internal data access interface for retrieving and managing exchange market quotes.
 */
@Repository
public interface SpringDataMarketOddsRepository extends JpaRepository<MarketOddsJpaEntity, Integer> {

    /**
     * Retrieves all market odds recorded for a match.
     *
     * @param matchId identifier of the match
     * @return list of matching market odds entities
     */
    List<MarketOddsJpaEntity> findByMatchId(Integer matchId);

    /**
     * Finds the specific market odds record for a match outcome.
     *
     * @param matchId    identifier of the match
     * @param marketType market type name
     * @param outcome    outcome code
     * @return an {@link Optional} containing the JPA entity if found
     */
    Optional<MarketOddsJpaEntity> findByMatchIdAndMarketTypeAndOutcome(Integer matchId, String marketType, String outcome);

    /**
     * Deletes all market odds records associated with a specific match ID.
     *
     * @param matchId identifier of the match
     */
    void deleteByMatchId(Integer matchId);
}
