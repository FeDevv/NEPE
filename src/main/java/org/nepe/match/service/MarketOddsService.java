package org.nepe.match.service;

import org.nepe.match.domain.MarketOdds;
import org.nepe.match.port.in.ManageMarketOddsUseCase;
import org.nepe.match.port.in.SaveMarketOddsCommand;
import org.nepe.match.port.out.MarketOddsRepositoryPort;
import org.nepe.match.port.out.MatchRepositoryPort;
import org.nepe.shared.exception.DomainValidationException;
import org.nepe.shared.exception.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Application Service implementing {@link ManageMarketOddsUseCase}.
 * <p>
 * Coordinates the storage, retrieval, and updating of Betting Exchange market quotes (Back & Lay)
 * associated with match fixtures.
 */
@Service
public class MarketOddsService implements ManageMarketOddsUseCase {

    private final MarketOddsRepositoryPort marketOddsRepositoryPort;
    private final MatchRepositoryPort matchRepositoryPort;

    public MarketOddsService(MarketOddsRepositoryPort marketOddsRepositoryPort,
                             MatchRepositoryPort matchRepositoryPort) {
        this.marketOddsRepositoryPort = Objects.requireNonNull(
                marketOddsRepositoryPort,
                "MarketOddsRepositoryPort must not be null"
        );
        this.matchRepositoryPort = Objects.requireNonNull(
                matchRepositoryPort,
                "MatchRepositoryPort must not be null"
        );
    }

    @Override
    @Transactional
    public MarketOdds saveOdds(SaveMarketOddsCommand command) {
        if (command == null) {
            throw new DomainValidationException("SaveMarketOddsCommand cannot be null.");
        }

        verifyMatchExists(command.matchId());

        Optional<MarketOdds> existingOpt = marketOddsRepositoryPort.findByMatchIdAndMarketTypeAndOutcome(
                command.matchId(),
                command.marketType(),
                command.outcome()
        );

        if (existingOpt.isPresent()) {
            MarketOdds existing = existingOpt.get();
            existing.updateOdds(command.backOdds(), command.layOdds());
            return marketOddsRepositoryPort.save(existing);
        }

        MarketOdds newOdds = MarketOdds.create(
                command.matchId(),
                command.marketType(),
                command.outcome(),
                command.backOdds(),
                command.layOdds()
        );

        return marketOddsRepositoryPort.save(newOdds);
    }

    @Override
    @Transactional
    public List<MarketOdds> saveBatchOdds(List<SaveMarketOddsCommand> commands) {
        if (commands == null || commands.isEmpty()) {
            return Collections.emptyList();
        }

        java.util.Map<String, MarketOdds> pendingMap = new java.util.LinkedHashMap<>();

        for (SaveMarketOddsCommand command : commands) {
            if (command == null) {
                continue;
            }

            verifyMatchExists(command.matchId());
            String key = command.matchId() + ":" + command.marketType() + ":" + (command.outcome() != null ? command.outcome().trim().toUpperCase() : "");

            MarketOdds inFlight = pendingMap.get(key);
            if (inFlight != null) {
                inFlight.updateOdds(command.backOdds(), command.layOdds());
            } else {
                Optional<MarketOdds> existingOpt = marketOddsRepositoryPort.findByMatchIdAndMarketTypeAndOutcome(
                        command.matchId(),
                        command.marketType(),
                        command.outcome()
                );

                if (existingOpt.isPresent()) {
                    MarketOdds existing = existingOpt.get();
                    existing.updateOdds(command.backOdds(), command.layOdds());
                    pendingMap.put(key, existing);
                } else {
                    MarketOdds newOdds = MarketOdds.create(
                            command.matchId(),
                            command.marketType(),
                            command.outcome(),
                            command.backOdds(),
                            command.layOdds()
                    );
                    pendingMap.put(key, newOdds);
                }
            }
        }

        return marketOddsRepositoryPort.saveAll(new ArrayList<>(pendingMap.values()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MarketOdds> getOddsForMatch(int matchId) {
        verifyMatchExists(matchId);
        return marketOddsRepositoryPort.findByMatchId(matchId);
    }

    @Override
    @Transactional
    public void deleteOddsForMatch(int matchId) {
        verifyMatchExists(matchId);
        marketOddsRepositoryPort.deleteByMatchId(matchId);
    }

    private void verifyMatchExists(int matchId) {
        if (matchRepositoryPort.findById(matchId).isEmpty()) {
            throw new EntityNotFoundException(
                    String.format("Match with ID %d not found.", matchId)
            );
        }
    }
}
