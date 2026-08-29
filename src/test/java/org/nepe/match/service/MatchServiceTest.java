package org.nepe.match.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.nepe.competition.domain.Competition;
import org.nepe.competition.domain.Season;
import org.nepe.competition.domain.Team;
import org.nepe.competition.port.out.CompetitionRepositoryPort;
import org.nepe.competition.port.out.SeasonRepositoryPort;
import org.nepe.competition.port.out.TeamRepositoryPort;
import org.nepe.match.domain.Match;
import org.nepe.match.domain.MatchModifiers;
import org.nepe.match.domain.MatchState;
import org.nepe.match.port.in.CreateMatchCommand;
import org.nepe.match.port.in.UpdateMatchCommand;
import org.nepe.match.port.in.UpdateMatchStatisticsCommand;
import org.nepe.match.port.out.MatchDetailsDTO;
import org.nepe.match.port.out.MatchDetailsRepositoryPort;
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

@DisplayName("MatchService Unit Tests")
class MatchServiceTest {

    private InMemoryMatchRepository matchRepository;
    private InMemoryMatchDetailsRepository matchDetailsRepository;
    private InMemoryCompetitionRepository competitionRepository;
    private InMemorySeasonRepository seasonRepository;
    private InMemoryTeamRepository teamRepository;
    private MatchService service;

    private static final int COMP_ID = 1;
    private static final int SEASON_ID = 1;
    private static final int HOME_TEAM_ID = 10;
    private static final int AWAY_TEAM_ID = 20;
    private static final Instant KICKOFF = Instant.parse("2026-09-01T18:00:00Z");

    @BeforeEach
    void setUp() {
        matchRepository = new InMemoryMatchRepository();
        matchDetailsRepository = new InMemoryMatchDetailsRepository();
        competitionRepository = new InMemoryCompetitionRepository();
        seasonRepository = new InMemorySeasonRepository();
        teamRepository = new InMemoryTeamRepository();

        // Seed parent entities
        Competition comp = Competition.create("I1", "Serie A", "Italy");
        comp.assignId(COMP_ID);
        competitionRepository.save(comp);

        Season season = Season.create("2025/2026");
        season.assignId(SEASON_ID);
        seasonRepository.save(season);

        Team home = Team.create("Inter");
        home.assignId(HOME_TEAM_ID);
        teamRepository.save(home);

        Team away = Team.create("Milan");
        away.assignId(AWAY_TEAM_ID);
        teamRepository.save(away);

        service = new MatchService(
                matchRepository,
                matchDetailsRepository,
                competitionRepository,
                seasonRepository,
                teamRepository
        );
    }

    @Nested
    @DisplayName("Constructor and Invariants")
    class ConstructorTests {

        @Test
        @DisplayName("Should throw NullPointerException when any dependency is null")
        void shouldThrowWhenAnyDependencyNull() {
            assertThatThrownBy(() -> new MatchService(null, matchDetailsRepository, competitionRepository, seasonRepository, teamRepository))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new MatchService(matchRepository, null, competitionRepository, seasonRepository, teamRepository))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new MatchService(matchRepository, matchDetailsRepository, null, seasonRepository, teamRepository))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new MatchService(matchRepository, matchDetailsRepository, competitionRepository, null, teamRepository))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new MatchService(matchRepository, matchDetailsRepository, competitionRepository, seasonRepository, null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("createMatch() Tests")
    class CreateMatchTests {

        @Test
        @DisplayName("Should create and schedule a new match successfully")
        void shouldCreateMatch() {
            CreateMatchCommand command = new CreateMatchCommand(
                    COMP_ID,
                    SEASON_ID,
                    HOME_TEAM_ID,
                    AWAY_TEAM_ID,
                    KICKOFF,
                    2.10,
                    3.40,
                    3.50
            );

            Match match = service.createMatch(command);

            assertThat(match.getId()).isPositive();
            assertThat(match.getState()).isEqualTo(MatchState.SCHEDULED);
            assertThat(match.getHomeTeamId()).isEqualTo(HOME_TEAM_ID);
            assertThat(match.getAwayTeamId()).isEqualTo(AWAY_TEAM_ID);
            assertThat(match.getMatchDateTime()).isEqualTo(KICKOFF);
            assertThat(match.getOddsHome()).isEqualTo(2.10);
            assertThat(match.isManuallyEdited()).isFalse();
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException if foreign keys are invalid")
        void shouldThrowWhenParentEntityNotFound() {
            CreateMatchCommand invalidComp = new CreateMatchCommand(999, SEASON_ID, HOME_TEAM_ID, AWAY_TEAM_ID, KICKOFF);
            assertThatThrownBy(() -> service.createMatch(invalidComp)).isInstanceOf(EntityNotFoundException.class);

            CreateMatchCommand invalidSeason = new CreateMatchCommand(COMP_ID, 999, HOME_TEAM_ID, AWAY_TEAM_ID, KICKOFF);
            assertThatThrownBy(() -> service.createMatch(invalidSeason)).isInstanceOf(EntityNotFoundException.class);

            CreateMatchCommand invalidHome = new CreateMatchCommand(COMP_ID, SEASON_ID, 999, AWAY_TEAM_ID, KICKOFF);
            assertThatThrownBy(() -> service.createMatch(invalidHome)).isInstanceOf(EntityNotFoundException.class);

            CreateMatchCommand invalidAway = new CreateMatchCommand(COMP_ID, SEASON_ID, HOME_TEAM_ID, 999, KICKOFF);
            assertThatThrownBy(() -> service.createMatch(invalidAway)).isInstanceOf(EntityNotFoundException.class);
        }

        @Test
        @DisplayName("Should throw DomainValidationException if Home and Away teams are identical")
        void shouldThrowWhenSameTeams() {
            CreateMatchCommand sameTeams = new CreateMatchCommand(COMP_ID, SEASON_ID, HOME_TEAM_ID, HOME_TEAM_ID, KICKOFF);

            assertThatThrownBy(() -> service.createMatch(sameTeams))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("cannot be identical");
        }

        @Test
        @DisplayName("Should throw DomainValidationException if identical fixture already exists")
        void shouldThrowOnDuplicateFixture() {
            CreateMatchCommand command = new CreateMatchCommand(COMP_ID, SEASON_ID, HOME_TEAM_ID, AWAY_TEAM_ID, KICKOFF);
            service.createMatch(command);

            assertThatThrownBy(() -> service.createMatch(command))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("already exists");
        }
    }

    @Nested
    @DisplayName("updateMatch() & updateStatistics() Tests")
    class UpdateTests {

        @Test
        @DisplayName("updateMatch() should update kickoff, odds, modifiers and set isManuallyEdited to true")
        void shouldUpdateMatch() {
            Match match = service.createMatch(new CreateMatchCommand(COMP_ID, SEASON_ID, HOME_TEAM_ID, AWAY_TEAM_ID, KICKOFF));

            Instant newKickoff = Instant.parse("2026-09-02T20:45:00Z");
            MatchModifiers mods = new MatchModifiers(true, false, false, false, false, 1.1, 0.9, 1.0, 1.0);
            UpdateMatchCommand updateCmd = new UpdateMatchCommand(match.getId(), newKickoff, 2.25, 3.30, 3.40, mods);

            Match updated = service.updateMatch(updateCmd);

            assertThat(updated.getMatchDateTime()).isEqualTo(newKickoff);
            assertThat(updated.getOddsHome()).isEqualTo(2.25);
            assertThat(updated.getModifiers().isNeutralVenue()).isTrue();
            assertThat(updated.isManuallyEdited()).isTrue();
        }

        @Test
        @DisplayName("updateStatistics() should update scores, shots, xG and set isManuallyEdited to true")
        void shouldUpdateStatistics() {
            Match match = service.createMatch(new CreateMatchCommand(COMP_ID, SEASON_ID, HOME_TEAM_ID, AWAY_TEAM_ID, KICKOFF));

            UpdateMatchStatisticsCommand statsCmd = new UpdateMatchStatisticsCommand(
                    match.getId(),
                    2, 1,
                    14, 8,
                    6, 3,
                    2.15, 0.85
            );

            Match updated = service.updateStatistics(statsCmd);

            assertThat(updated.getStatistics().getHomeScore()).isEqualTo(2);
            assertThat(updated.getStatistics().getAwayScore()).isEqualTo(1);
            assertThat(updated.getStatistics().getHomeShots()).isEqualTo(14);
            assertThat(updated.getStatistics().getManualHomeXg()).isEqualTo(2.15);
            assertThat(updated.isManuallyEdited()).isTrue();
        }
    }

    @Nested
    @DisplayName("State Transitions Tests")
    class StateTransitionTests {

        @Test
        @DisplayName("markAsPostponed() should set state to POSTPONED")
        void shouldPostpone() {
            Match match = service.createMatch(new CreateMatchCommand(COMP_ID, SEASON_ID, HOME_TEAM_ID, AWAY_TEAM_ID, KICKOFF));

            Match postponed = service.markAsPostponed(match.getId());

            assertThat(postponed.getState()).isEqualTo(MatchState.POSTPONED);
        }

        @Test
        @DisplayName("markAsCancelled() should set state to CANCELLED")
        void shouldCancel() {
            Match match = service.createMatch(new CreateMatchCommand(COMP_ID, SEASON_ID, HOME_TEAM_ID, AWAY_TEAM_ID, KICKOFF));

            Match cancelled = service.markAsCancelled(match.getId());

            assertThat(cancelled.getState()).isEqualTo(MatchState.CANCELLED);
        }

        @Test
        @DisplayName("markAsFinished() should set state to FINISHED and minute to 90")
        void shouldFinish() {
            Match match = service.createMatch(new CreateMatchCommand(COMP_ID, SEASON_ID, HOME_TEAM_ID, AWAY_TEAM_ID, KICKOFF));

            Match finished = service.markAsFinished(match.getId());

            assertThat(finished.getState()).isEqualTo(MatchState.FINISHED);
            assertThat(finished.getCurrentMinute()).isEqualTo(90);
        }
    }

    @Nested
    @DisplayName("Query and Deletion Tests")
    class QueryAndDeletionTests {

        @Test
        @DisplayName("deleteMatch() should remove match")
        void shouldDeleteMatch() {
            Match match = service.createMatch(new CreateMatchCommand(COMP_ID, SEASON_ID, HOME_TEAM_ID, AWAY_TEAM_ID, KICKOFF));

            service.deleteMatch(match.getId());

            assertThat(matchRepository.findById(match.getId())).isEmpty();
        }

        @Test
        @DisplayName("deleteMatch() should throw EntityNotFoundException on non-existent match")
        void shouldThrowOnDeleteNonExistent() {
            assertThatThrownBy(() -> service.deleteMatch(999))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    // --- In-Memory Test Doubles ---

    private static class InMemoryMatchRepository implements MatchRepositoryPort {
        private final Map<Integer, Match> storage = new HashMap<>();
        private int idSeq = 1;

        @Override
        public Match save(Match match) {
            if (match.getId() == null) {
                match.assignId(idSeq++);
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
        public List<Match> findByCompetitionAndSeason(int competitionId, int seasonId) {
            return storage.values().stream()
                    .filter(m -> m.getCompetitionId() == competitionId && m.getSeasonId() == seasonId)
                    .toList();
        }

        @Override
        public List<Match> findRecentMatchesForTeam(int teamId, int competitionId, int limit) {
            return storage.values().stream()
                    .filter(m -> (m.getHomeTeamId() == teamId || m.getAwayTeamId() == teamId) && m.getCompetitionId() == competitionId)
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

    private static class InMemoryMatchDetailsRepository implements MatchDetailsRepositoryPort {
        private final Map<Integer, MatchDetailsDTO> storage = new HashMap<>();

        @Override
        public Optional<MatchDetailsDTO> findDetailsById(int matchId) {
            return Optional.ofNullable(storage.get(matchId));
        }

        @Override
        public List<MatchDetailsDTO> findDetailsByCompetitionAndSeason(int competitionId, int seasonId) {
            return storage.values().stream()
                    .filter(d -> d.competitionId() == competitionId && d.seasonId() == seasonId)
                    .toList();
        }

        @Override
        public List<MatchDetailsDTO> findDetailsByCompetitionAndSeasonAndState(int competitionId, int seasonId, MatchState state) {
            return storage.values().stream()
                    .filter(d -> d.competitionId() == competitionId && d.seasonId() == seasonId && d.matchState() == state)
                    .toList();
        }

        @Override
        public List<MatchDetailsDTO> findAllDetails() {
            return List.copyOf(storage.values());
        }
    }

    private static class InMemoryCompetitionRepository implements CompetitionRepositoryPort {
        private final Map<Integer, Competition> storage = new HashMap<>();

        @Override
        public Competition save(Competition competition) {
            storage.put(competition.getId(), competition);
            return competition;
        }

        @Override
        public Optional<Competition> findById(int id) {
            return Optional.ofNullable(storage.get(id));
        }

        @Override
        public Optional<Competition> findByCode(String code) {
            return storage.values().stream().filter(c -> c.getCode().equalsIgnoreCase(code)).findFirst();
        }

        @Override
        public List<Competition> findAll() {
            return List.copyOf(storage.values());
        }

        @Override
        public boolean existsByCode(String code) {
            return storage.values().stream().anyMatch(c -> c.getCode().equalsIgnoreCase(code));
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

    private static class InMemorySeasonRepository implements SeasonRepositoryPort {
        private final Map<Integer, Season> storage = new HashMap<>();

        @Override
        public Season save(Season season) {
            storage.put(season.getId(), season);
            return season;
        }

        @Override
        public Optional<Season> findById(int id) {
            return Optional.ofNullable(storage.get(id));
        }

        @Override
        public Optional<Season> findByName(String name) {
            return storage.values().stream().filter(s -> s.getName().equalsIgnoreCase(name)).findFirst();
        }

        @Override
        public Optional<Season> findLatest() {
            return storage.values().stream().max(Season::compareTo);
        }

        @Override
        public List<Season> findAll() {
            return List.copyOf(storage.values());
        }

        @Override
        public boolean existsByName(String name) {
            return storage.values().stream().anyMatch(s -> s.getName().equalsIgnoreCase(name));
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

    private static class InMemoryTeamRepository implements TeamRepositoryPort {
        private final Map<Integer, Team> storage = new HashMap<>();

        @Override
        public Team save(Team team) {
            storage.put(team.getId(), team);
            return team;
        }

        @Override
        public Optional<Team> findById(int id) {
            return Optional.ofNullable(storage.get(id));
        }

        @Override
        public Optional<Team> findByName(String name) {
            return storage.values().stream().filter(t -> t.getName().equalsIgnoreCase(name)).findFirst();
        }

        @Override
        public List<Team> findAll() {
            return List.copyOf(storage.values());
        }

        @Override
        public List<Team> searchByName(String query) {
            return storage.values().stream().filter(t -> t.getName().toLowerCase().contains(query.toLowerCase())).toList();
        }

        @Override
        public boolean existsByName(String name) {
            return storage.values().stream().anyMatch(t -> t.getName().equalsIgnoreCase(name));
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
