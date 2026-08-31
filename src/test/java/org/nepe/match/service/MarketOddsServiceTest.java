package org.nepe.match.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.nepe.match.domain.MarketOdds;
import org.nepe.match.domain.MarketType;
import org.nepe.match.domain.Match;
import org.nepe.match.domain.MatchState;
import org.nepe.match.port.in.SaveMarketOddsCommand;
import org.nepe.match.port.out.MarketOddsRepositoryPort;
import org.nepe.match.port.out.MatchRepositoryPort;
import org.nepe.shared.exception.DomainValidationException;
import org.nepe.shared.exception.EntityNotFoundException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("MarketOddsService Unit Tests")
class MarketOddsServiceTest {

    private InMemoryMarketOddsRepository oddsRepository;
    private InMemoryMatchRepository matchRepository;
    private MarketOddsService service;

    private static final int MATCH_ID = 100;

    @BeforeEach
    void setUp() {
        oddsRepository = new InMemoryMarketOddsRepository();
        matchRepository = new InMemoryMatchRepository();
        service = new MarketOddsService(oddsRepository, matchRepository);

        // Seed a valid match
        Match match = Match.createScheduled(1, 1, 10, 20, Instant.now(), null, null, null);
        match.assignId(MATCH_ID);
        matchRepository.save(match);
    }

    @Nested
    @DisplayName("Constructor and Invariants")
    class ConstructorTests {

        @Test
        @DisplayName("Should throw NullPointerException when dependencies are null")
        void shouldThrowWhenDependenciesNull() {
            assertThatThrownBy(() -> new MarketOddsService(null, matchRepository))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("MarketOddsRepositoryPort must not be null");

            assertThatThrownBy(() -> new MarketOddsService(oddsRepository, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("MatchRepositoryPort must not be null");
        }
    }

    @Nested
    @DisplayName("saveOdds() Tests")
    class SaveOddsTests {

        @Test
        @DisplayName("Should create and persist new market odds")
        void shouldSaveNewOdds() {
            SaveMarketOddsCommand command = new SaveMarketOddsCommand(
                    MATCH_ID,
                    MarketType.MATCH_ODDS,
                    "1",
                    2.10,
                    2.14
            );

            MarketOdds saved = service.saveOdds(command);

            assertThat(saved.getId()).isPositive();
            assertThat(saved.getMatchId()).isEqualTo(MATCH_ID);
            assertThat(saved.getMarketType()).isEqualTo(MarketType.MATCH_ODDS);
            assertThat(saved.getOutcome()).isEqualTo("1");
            assertThat(saved.getBackOdds()).isEqualTo(2.10);
            assertThat(saved.getLayOdds()).isEqualTo(2.14);
        }

        @Test
        @DisplayName("Should update existing odds when re-saving the same market outcome")
        void shouldUpdateExistingOdds() {
            service.saveOdds(new SaveMarketOddsCommand(MATCH_ID, MarketType.MATCH_ODDS, "1", 2.10, 2.14));

            SaveMarketOddsCommand updateCmd = new SaveMarketOddsCommand(
                    MATCH_ID,
                    MarketType.MATCH_ODDS,
                    "1",
                    2.20,
                    2.24
            );

            MarketOdds updated = service.saveOdds(updateCmd);

            assertThat(updated.getBackOdds()).isEqualTo(2.20);
            assertThat(updated.getLayOdds()).isEqualTo(2.24);
            assertThat(oddsRepository.findByMatchId(MATCH_ID)).hasSize(1);
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when saving odds for non-existent match")
        void shouldThrowWhenMatchNotFound() {
            SaveMarketOddsCommand command = new SaveMarketOddsCommand(
                    999,
                    MarketType.MATCH_ODDS,
                    "1",
                    2.10,
                    2.14
            );

            assertThatThrownBy(() -> service.saveOdds(command))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Match with ID 999 not found");
        }

        @Test
        @DisplayName("Should throw DomainValidationException when command is null")
        void shouldThrowWhenCommandIsNull() {
            assertThatThrownBy(() -> service.saveOdds(null))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("SaveMarketOddsCommand cannot be null");
        }
    }

    @Nested
    @DisplayName("saveBatchOdds() Tests")
    class BatchSaveTests {

        @Test
        @DisplayName("Should save a collection of market odds in batch")
        void shouldSaveBatchOdds() {
            List<SaveMarketOddsCommand> commands = List.of(
                    new SaveMarketOddsCommand(MATCH_ID, MarketType.MATCH_ODDS, "1", 2.10, 2.14),
                    new SaveMarketOddsCommand(MATCH_ID, MarketType.MATCH_ODDS, "X", 3.40, 3.50),
                    new SaveMarketOddsCommand(MATCH_ID, MarketType.MATCH_ODDS, "2", 3.60, 3.75)
            );

            List<MarketOdds> savedList = service.saveBatchOdds(commands);

            assertThat(savedList).hasSize(3);
            assertThat(oddsRepository.findByMatchId(MATCH_ID)).hasSize(3);
        }

        @Test
        @DisplayName("Should handle duplicate commands in the same batch by updating in-flight record")
        void shouldHandleDuplicateCommandsInSameBatch() {
            List<SaveMarketOddsCommand> commandsWithDuplicates = List.of(
                    new SaveMarketOddsCommand(MATCH_ID, MarketType.MATCH_ODDS, "1", 2.10, 2.14),
                    new SaveMarketOddsCommand(MATCH_ID, MarketType.MATCH_ODDS, "1", 2.20, 2.24) // Duplicate / updated quote in same batch
            );

            List<MarketOdds> savedList = service.saveBatchOdds(commandsWithDuplicates);

            assertThat(savedList).hasSize(1);
            assertThat(savedList.get(0).getBackOdds()).isEqualTo(2.20);
            assertThat(savedList.get(0).getLayOdds()).isEqualTo(2.24);
        }

        @Test
        @DisplayName("Should return empty list on empty or null batch command")
        void shouldHandleEmptyBatch() {
            assertThat(service.saveBatchOdds(List.of())).isEmpty();
            assertThat(service.saveBatchOdds(null)).isEmpty();
        }
    }

    @Nested
    @DisplayName("Query and Deletion Tests")
    class QueryAndDeleteTests {

        @Test
        @DisplayName("getOddsForMatch() should return all odds for match")
        void shouldGetOddsForMatch() {
            service.saveOdds(new SaveMarketOddsCommand(MATCH_ID, MarketType.MATCH_ODDS, "1", 2.10, 2.14));
            service.saveOdds(new SaveMarketOddsCommand(MATCH_ID, MarketType.BTTS, "YES", 1.80, 1.85));

            List<MarketOdds> odds = service.getOddsForMatch(MATCH_ID);

            assertThat(odds).hasSize(2);
        }

        @Test
        @DisplayName("getOddsForMatch() should throw EntityNotFoundException when match not found")
        void shouldThrowWhenQueryingNonExistentMatch() {
            assertThatThrownBy(() -> service.getOddsForMatch(999))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Match with ID 999 not found");
        }

        @Test
        @DisplayName("deleteOddsForMatch() should delete all odds for match")
        void shouldDeleteOddsForMatch() {
            service.saveOdds(new SaveMarketOddsCommand(MATCH_ID, MarketType.MATCH_ODDS, "1", 2.10, 2.14));

            service.deleteOddsForMatch(MATCH_ID);

            assertThat(oddsRepository.findByMatchId(MATCH_ID)).isEmpty();
        }
    }

    /**
     * In-memory test double for {@link MarketOddsRepositoryPort}.
     */
    private static class InMemoryMarketOddsRepository implements MarketOddsRepositoryPort {
        private final Map<Integer, MarketOdds> storage = new HashMap<>();
        private int idSequence = 1;

        @Override
        public MarketOdds save(MarketOdds marketOdds) {
            if (marketOdds.getId() == null) {
                marketOdds.assignId(idSequence++);
            }
            storage.put(marketOdds.getId(), marketOdds);
            return marketOdds;
        }

        @Override
        public List<MarketOdds> saveAll(List<MarketOdds> oddsList) {
            List<MarketOdds> saved = new ArrayList<>();
            for (MarketOdds odds : oddsList) {
                saved.add(save(odds));
            }
            return List.copyOf(saved);
        }

        @Override
        public Optional<MarketOdds> findById(int id) {
            return Optional.ofNullable(storage.get(id));
        }

        @Override
        public List<MarketOdds> findByMatchId(int matchId) {
            return storage.values().stream()
                    .filter(o -> o.getMatchId().equals(matchId))
                    .toList();
        }

        @Override
        public Optional<MarketOdds> findByMatchIdAndMarketTypeAndOutcome(int matchId, MarketType marketType, String outcome) {
            return storage.values().stream()
                    .filter(o -> o.getMatchId().equals(matchId)
                            && o.getMarketType() == marketType
                            && o.getOutcome().equalsIgnoreCase(outcome.trim()))
                    .findFirst();
        }

        @Override
        public void deleteById(int id) {
            storage.remove(id);
        }

        @Override
        public void deleteByMatchId(int matchId) {
            storage.values().removeIf(o -> o.getMatchId().equals(matchId));
        }

        @Override
        public long count() {
            return storage.size();
        }
    }

    /**
     * In-memory test double for {@link MatchRepositoryPort}.
     */
    private static class InMemoryMatchRepository implements MatchRepositoryPort {
        private final Map<Integer, Match> storage = new HashMap<>();
        private int idSequence = 1;

        @Override
        public Match save(Match match) {
            if (match.getId() == null) {
                match.assignId(idSequence++);
            }
            storage.put(match.getId(), match);
            return match;
        }

        @Override
        public Optional<Match> findById(int id) {
            return Optional.ofNullable(storage.get(id));
        }

        @Override
        public Optional<Match> findByTeamsAndDateTime(int homeTeamId, int awayTeamId, Instant matchDateTime) {
            return storage.values().stream()
                    .filter(m -> m.getHomeTeamId() == homeTeamId
                            && m.getAwayTeamId() == awayTeamId
                            && m.getMatchDateTime().equals(matchDateTime))
                    .findFirst();
        }

        @Override
        public Optional<Match> findByTeamsAndDateRange(int homeTeamId, int awayTeamId, Instant startOfDay, Instant endOfDay) {
            return storage.values().stream()
                    .filter(m -> m.getHomeTeamId() == homeTeamId && m.getAwayTeamId() == awayTeamId
                            && !m.getMatchDateTime().isBefore(startOfDay) && !m.getMatchDateTime().isAfter(endOfDay))
                    .findFirst();
        }

        @Override
        public List<Match> findByCompetitionAndSeason(int competitionId, int seasonId) {
            return storage.values().stream()
                    .filter(m -> m.getCompetitionId() == competitionId && m.getSeasonId() == seasonId)
                    .toList();
        }

        @Override
        public List<Match> findFinishedMatchesByCompetitionAndSeason(int competitionId, int seasonId) {
            return storage.values().stream()
                    .filter(m -> m.getCompetitionId() == competitionId && m.getSeasonId() == seasonId && m.getState() == MatchState.FINISHED)
                    .toList();
        }

        @Override
        public List<Match> findFinishedMatchesForTeamInSeason(int teamId, int competitionId, int seasonId) {
            return storage.values().stream()
                    .filter(m -> m.getCompetitionId() == competitionId && m.getSeasonId() == seasonId && m.getState() == MatchState.FINISHED
                            && (m.getHomeTeamId() == teamId || m.getAwayTeamId() == teamId))
                    .toList();
        }

        @Override
        public List<Match> findRecentMatchesForTeam(int teamId, int competitionId, int limit) {
            return storage.values().stream()
                    .filter(m -> (m.getHomeTeamId() == teamId || m.getAwayTeamId() == teamId)
                            && m.getCompetitionId() == competitionId
                            && m.getState() == MatchState.FINISHED)
                    .limit(limit)
                    .toList();
        }

        @Override
        public List<Match> findAll() {
            return List.copyOf(storage.values());
        }

        @Override
        public void deleteById(int id) {
            storage.remove(id);
        }

        @Override
        public long count() {
            return storage.size();
        }
    }
}
