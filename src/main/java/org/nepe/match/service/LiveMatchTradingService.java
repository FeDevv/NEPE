package org.nepe.match.service;

import org.nepe.match.domain.Match;
import org.nepe.match.domain.MatchEvent;
import org.nepe.match.domain.MatchModifiers;
import org.nepe.match.port.in.LiveMatchTradingUseCase;
import org.nepe.match.port.in.RecordMatchEventCommand;
import org.nepe.match.port.out.MatchEventRepositoryPort;
import org.nepe.match.port.out.MatchRepositoryPort;
import org.nepe.shared.exception.DomainValidationException;
import org.nepe.shared.exception.EntityNotFoundException;
import org.nepe.shared.exception.LiveTradingException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Application Service implementing {@link LiveMatchTradingUseCase}.
 * <p>
 * Coordinates live trading operations, in-game event logging (Goals, Red Cards),
 * event rollbacks (Undo Last Event), game minute progression, and live match finalization.
 */
@Service
public class LiveMatchTradingService implements LiveMatchTradingUseCase {

    private final MatchRepositoryPort matchRepositoryPort;
    private final MatchEventRepositoryPort matchEventRepositoryPort;

    public LiveMatchTradingService(MatchRepositoryPort matchRepositoryPort,
                                   MatchEventRepositoryPort matchEventRepositoryPort) {
        this.matchRepositoryPort = Objects.requireNonNull(matchRepositoryPort, "MatchRepositoryPort must not be null");
        this.matchEventRepositoryPort = Objects.requireNonNull(matchEventRepositoryPort, "MatchEventRepositoryPort must not be null");
    }

    @Override
    @Transactional
    public Match startLiveTrading(int matchId) {
        Match match = findMatchOrThrow(matchId);
        match.startLive();
        return matchRepositoryPort.save(match);
    }

    @Override
    @Transactional
    public Match recordEvent(RecordMatchEventCommand command) {
        if (command == null) {
            throw new DomainValidationException("RecordMatchEventCommand cannot be null.");
        }

        Match match = findMatchOrThrow(command.matchId());

        MatchEvent event = MatchEvent.create(
                command.matchId(),
                command.eventType(),
                command.minute()
        );

        // Apply event to match domain aggregate (updates scores/cards and advances minute if needed)
        match.applyEvent(event);

        matchEventRepositoryPort.save(event);
        return matchRepositoryPort.save(match);
    }

    @Override
    @Transactional
    public Match revertLastEvent(int matchId) {
        Match match = findMatchOrThrow(matchId);

        Optional<MatchEvent> latestEventOpt = matchEventRepositoryPort.findLatestEventByMatchId(matchId);
        if (latestEventOpt.isEmpty()) {
            throw new LiveTradingException(String.format("No recorded events to revert for match ID %d.", matchId));
        }

        MatchEvent latestEvent = latestEventOpt.get();

        // Revert event changes on match aggregate
        match.revertEvent(latestEvent);

        if (latestEvent.getId() != null) {
            matchEventRepositoryPort.deleteById(latestEvent.getId());
        }

        return matchRepositoryPort.save(match);
    }

    @Override
    @Transactional
    public Match updateLiveMinute(int matchId, int currentMinute) {
        Match match = findMatchOrThrow(matchId);
        match.updateCurrentMinute(currentMinute);
        return matchRepositoryPort.save(match);
    }

    @Override
    @Transactional
    public Match updateLiveModifiers(int matchId, MatchModifiers modifiers) {
        Match match = findMatchOrThrow(matchId);
        match.updateModifiers(modifiers);
        return matchRepositoryPort.save(match);
    }

    @Override
    @Transactional
    public Match finishLiveMatch(int matchId) {
        Match match = findMatchOrThrow(matchId);
        match.finishMatch();
        return matchRepositoryPort.save(match);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MatchEvent> getMatchEvents(int matchId) {
        findMatchOrThrow(matchId);
        return matchEventRepositoryPort.findByMatchIdOrderByMinuteAsc(matchId);
    }

    private Match findMatchOrThrow(int matchId) {
        return matchRepositoryPort.findById(matchId)
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format("Match with ID %d not found.", matchId)
                ));
    }
}
