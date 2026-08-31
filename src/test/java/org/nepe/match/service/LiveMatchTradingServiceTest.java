package org.nepe.match.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.nepe.match.domain.Match;
import org.nepe.match.domain.MatchEvent;
import org.nepe.match.domain.MatchEventType;
import org.nepe.match.domain.MatchModifiers;
import org.nepe.match.domain.MatchState;
import org.nepe.match.port.in.RecordMatchEventCommand;
import org.nepe.match.port.out.MatchEventRepositoryPort;
import org.nepe.match.port.out.MatchRepositoryPort;
import org.nepe.shared.exception.DomainValidationException;
import org.nepe.shared.exception.EntityNotFoundException;
import org.nepe.shared.exception.LiveTradingException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("LiveMatchTradingService Unit Tests")
class LiveMatchTradingServiceTest {

    private InMemoryMatchRepository matchRepository;
    private InMemoryMatchEventRepository eventRepository;
    private LiveMatchTradingService service;

    private static final int MATCH_ID = 10;

    @BeforeEach
    void setUp() {
        matchRepository = new InMemoryMatchRepository();
        eventRepository = new InMemoryMatchEventRepository();
        service = new LiveMatchTradingService(matchRepository, eventRepository);

        Match match = Match.createScheduled(1, 1, 100, 200, Instant.now(), 2.0, 3.2, 3.8);
        match.assignId(MATCH_ID);
        matchRepository.save(match);
    }

    @Nested
    @DisplayName("Constructor and Invariants")
    class ConstructorTests {

        @Test
        @DisplayName("Should throw NullPointerException when dependencies are null")
        void shouldThrowWhenDependenciesNull() {
            assertThatThrownBy(() -> new LiveMatchTradingService(null, eventRepository))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new LiveMatchTradingService(matchRepository, null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("Lifecycle Transitions Tests")
    class LifecycleTests {

        @Test
        @DisplayName("startLiveTrading() should transition match state from SCHEDULED to LIVE")
        void shouldStartLiveTrading() {
            Match liveMatch = service.startLiveTrading(MATCH_ID);

            assertThat(liveMatch.getState()).isEqualTo(MatchState.LIVE);
            assertThat(matchRepository.findById(MATCH_ID).orElseThrow().getState()).isEqualTo(MatchState.LIVE);
        }

        @Test
        @DisplayName("startLiveTrading() should throw LiveTradingException on terminal match")
        void shouldThrowWhenStartingTerminalMatch() {
            Match match = matchRepository.findById(MATCH_ID).orElseThrow();
            match.finishMatch();
            matchRepository.save(match);

            assertThatThrownBy(() -> service.startLiveTrading(MATCH_ID))
                    .isInstanceOf(LiveTradingException.class)
                    .hasMessageContaining("Cannot start live tracking");
        }

        @Test
        @DisplayName("finishLiveMatch() should transition state to FINISHED and clamp minute to 90")
        void shouldFinishLiveMatch() {
            service.startLiveTrading(MATCH_ID);

            Match finished = service.finishLiveMatch(MATCH_ID);

            assertThat(finished.getState()).isEqualTo(MatchState.FINISHED);
            assertThat(finished.getCurrentMinute()).isEqualTo(90);
        }
    }

    @Nested
    @DisplayName("Event Recording and Rollback Tests")
    class EventTests {

        @Test
        @DisplayName("recordEvent() should apply Goal and advance match minute")
        void shouldRecordGoalEvent() {
            service.startLiveTrading(MATCH_ID);

            RecordMatchEventCommand goalHome = new RecordMatchEventCommand(MATCH_ID, MatchEventType.GOAL_HOME, 23);
            Match updated = service.recordEvent(goalHome);

            assertThat(updated.getStatistics().getHomeScore()).isEqualTo(1);
            assertThat(updated.getCurrentMinute()).isEqualTo(23);
            assertThat(eventRepository.findByMatchIdOrderByMinuteAsc(MATCH_ID)).hasSize(1);
        }

        @Test
        @DisplayName("recordEvent() should apply Red Card to opponent and persist event")
        void shouldRecordRedCardEvent() {
            service.startLiveTrading(MATCH_ID);

            RecordMatchEventCommand redCardAway = new RecordMatchEventCommand(MATCH_ID, MatchEventType.RED_CARD_AWAY, 44);
            Match updated = service.recordEvent(redCardAway);

            assertThat(updated.getStatistics().getAwayRedCards()).isEqualTo(1);
            assertThat(updated.getCurrentMinute()).isEqualTo(44);
        }

        @Test
        @DisplayName("revertLastEvent() should rollback score/cards and delete last recorded event")
        void shouldRevertLastEvent() {
            service.startLiveTrading(MATCH_ID);
            service.recordEvent(new RecordMatchEventCommand(MATCH_ID, MatchEventType.GOAL_HOME, 15));
            service.recordEvent(new RecordMatchEventCommand(MATCH_ID, MatchEventType.GOAL_AWAY, 30));

            Match afterRevert = service.revertLastEvent(MATCH_ID);

            assertThat(afterRevert.getStatistics().getAwayScore()).isEqualTo(0);
            assertThat(afterRevert.getStatistics().getHomeScore()).isEqualTo(1);
            assertThat(eventRepository.findByMatchIdOrderByMinuteAsc(MATCH_ID)).hasSize(1);
        }

        @Test
        @DisplayName("revertLastEvent() should throw LiveTradingException when no events exist")
        void shouldThrowWhenNoEventsToRevert() {
            service.startLiveTrading(MATCH_ID);

            assertThatThrownBy(() -> service.revertLastEvent(MATCH_ID))
                    .isInstanceOf(LiveTradingException.class)
                    .hasMessageContaining("No recorded events to revert");
        }

        @Test
        @DisplayName("recordEvent() should throw DomainValidationException on null command")
        void shouldThrowOnNullRecordCommand() {
            assertThatThrownBy(() -> service.recordEvent(null))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("RecordMatchEventCommand cannot be null");
        }
    }

    @Nested
    @DisplayName("Minute and Modifier Progression Tests")
    class ProgressionTests {

        @Test
        @DisplayName("updateLiveMinute() should update minute when match is LIVE")
        void shouldUpdateLiveMinute() {
            service.startLiveTrading(MATCH_ID);

            Match updated = service.updateLiveMinute(MATCH_ID, 55);

            assertThat(updated.getCurrentMinute()).isEqualTo(55);
        }

        @Test
        @DisplayName("updateLiveMinute() should throw LiveTradingException when match is not LIVE")
        void shouldThrowWhenUpdatingMinuteOnScheduledMatch() {
            assertThatThrownBy(() -> service.updateLiveMinute(MATCH_ID, 55))
                    .isInstanceOf(LiveTradingException.class)
                    .hasMessageContaining("Cannot update game minute when match is not LIVE");
        }

        @Test
        @DisplayName("updateLiveModifiers() should update tactical modifiers during live trading")
        void shouldUpdateLiveModifiers() {
            service.startLiveTrading(MATCH_ID);

            MatchModifiers newMods = new MatchModifiers(false, true, false, false, false, 1.2, 0.8, 1.0, 1.0);
            Match updated = service.updateLiveModifiers(MATCH_ID, newMods);

            assertThat(updated.getModifiers().isMustWinHome()).isTrue();
        }

        @Test
        @DisplayName("getMatchEvents() should return events ordered by minute")
        void shouldGetEventsOrdered() {
            service.startLiveTrading(MATCH_ID);
            service.recordEvent(new RecordMatchEventCommand(MATCH_ID, MatchEventType.GOAL_HOME, 75));
            service.recordEvent(new RecordMatchEventCommand(MATCH_ID, MatchEventType.GOAL_AWAY, 20));

            List<MatchEvent> events = service.getMatchEvents(MATCH_ID);

            assertThat(events).hasSize(2);
            assertThat(events.get(0).getMinute()).isEqualTo(20);
            assertThat(events.get(1).getMinute()).isEqualTo(75);
        }
    }

    // --- In-Memory Test Doubles ---

    private static class InMemoryMatchRepository implements MatchRepositoryPort {
        private final Map<Integer, Match> storage = new HashMap<>();
        private int seq = 1;

        @Override
        public Match save(Match match) {
            if (match.getId() == null) {
                match.assignId(seq++);
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
                    .filter(m -> m.getHomeTeamId() == homeTeamId && m.getAwayTeamId() == awayTeamId && m.getMatchDateTime().equals(matchDateTime))
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
            return List.of();
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
            return List.of();
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

    private static class InMemoryMatchEventRepository implements MatchEventRepositoryPort {
        private final Map<Integer, MatchEvent> storage = new HashMap<>();
        private int seq = 1;

        @Override
        public MatchEvent save(MatchEvent event) {
            if (event.getId() == null) {
                event.assignId(seq++);
            }
            storage.put(event.getId(), event);
            return event;
        }

        @Override
        public Optional<MatchEvent> findById(int id) {
            return Optional.ofNullable(storage.get(id));
        }

        @Override
        public List<MatchEvent> findByMatchIdOrderByMinuteAsc(int matchId) {
            List<MatchEvent> list = new ArrayList<>(storage.values().stream()
                    .filter(e -> e.getMatchId().equals(matchId))
                    .toList());
            list.sort(Comparator.naturalOrder());
            return List.copyOf(list);
        }

        @Override
        public Optional<MatchEvent> findLatestEventByMatchId(int matchId) {
            return storage.values().stream()
                    .filter(e -> e.getMatchId().equals(matchId))
                    .max(Comparator.naturalOrder());
        }

        @Override
        public void deleteById(int id) {
            storage.remove(id);
        }

        @Override
        public void deleteByMatchId(int matchId) {
            storage.values().removeIf(e -> e.getMatchId().equals(matchId));
        }

        @Override
        public long count() {
            return storage.size();
        }
    }
}
