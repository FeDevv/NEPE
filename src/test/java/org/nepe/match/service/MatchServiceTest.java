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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
                    0, 1,
                    2.15, 0.85
            );

            Match updated = service.updateStatistics(statsCmd);

            assertThat(updated.getStatistics().getHomeScore()).isEqualTo(2);
            assertThat(updated.getStatistics().getAwayScore()).isEqualTo(1);
            assertThat(updated.getStatistics().getHomeShots()).isEqualTo(14);
            assertThat(updated.getStatistics().getAwayShots()).isEqualTo(8);
            assertThat(updated.getStatistics().getHomeShotsOnTarget()).isEqualTo(6);
            assertThat(updated.getStatistics().getAwayShotsOnTarget()).isEqualTo(3);
            assertThat(updated.getStatistics().getHomeRedCards()).isEqualTo(0);
            assertThat(updated.getStatistics().getAwayRedCards()).isEqualTo(1);
            assertThat(updated.getStatistics().getManualHomeXg()).isEqualTo(2.15);
            assertThat(updated.getStatistics().getManualAwayXg()).isEqualTo(0.85);
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

        @Test
        @DisplayName("Should retrieve all match details projection DTOs")
        void shouldRetrieveAllMatchDetails() {
            MatchDetailsDTO dto1 = new MatchDetailsDTO(
                    1, KICKOFF, MatchState.SCHEDULED, false,
                    null, null, null, null, null, null, 0, 0,
                    null, null, 2.10, 3.40, 3.50,
                    false, false, false, false, false, 1.0, 1.0, 1.0, 1.0, 0,
                    COMP_ID, "I1", "Serie A", "Italy", -0.12,
                    SEASON_ID, "2025/2026",
                    HOME_TEAM_ID, "Inter", AWAY_TEAM_ID, "Milan"
            );
            matchDetailsRepository.storage.put(1, dto1);

            List<MatchDetailsDTO> all = service.getAllMatchDetails();

            assertThat(all).hasSize(1);
            assertThat(all.get(0).matchId()).isEqualTo(1);
        }

        @Test
        @DisplayName("getPreMatchEligibleMatches() should return only SCHEDULED and POSTPONED matches")
        void shouldReturnOnlyScheduledAndPostponedMatchesForPreMatchAnalysis() {
            MatchDetailsDTO scheduled = createMatchDetailsDto(1, MatchState.SCHEDULED, COMP_ID, SEASON_ID);
            MatchDetailsDTO postponed = createMatchDetailsDto(2, MatchState.POSTPONED, COMP_ID, SEASON_ID);
            MatchDetailsDTO live = createMatchDetailsDto(3, MatchState.LIVE, COMP_ID, SEASON_ID);
            MatchDetailsDTO finished = createMatchDetailsDto(4, MatchState.FINISHED, COMP_ID, SEASON_ID);
            MatchDetailsDTO cancelled = createMatchDetailsDto(5, MatchState.CANCELLED, COMP_ID, SEASON_ID);
            // Match from another competition (SCHEDULED) - must be ignored
            MatchDetailsDTO otherComp = createMatchDetailsDto(6, MatchState.SCHEDULED, 999, SEASON_ID);

            matchDetailsRepository.storage.put(1, scheduled);
            matchDetailsRepository.storage.put(2, postponed);
            matchDetailsRepository.storage.put(3, live);
            matchDetailsRepository.storage.put(4, finished);
            matchDetailsRepository.storage.put(5, cancelled);
            matchDetailsRepository.storage.put(6, otherComp);

            List<MatchDetailsDTO> eligible = service.getPreMatchEligibleMatches(COMP_ID, SEASON_ID);

            assertThat(eligible).hasSize(2);
            assertThat(eligible).extracting(MatchDetailsDTO::matchId).containsExactlyInAnyOrder(1, 2);
            assertThat(eligible).extracting(MatchDetailsDTO::matchState).containsOnly(MatchState.SCHEDULED, MatchState.POSTPONED);
        }

        @Test
        @DisplayName("getPreMatchEligibleMatches() should return empty list when no eligible matches exist")
        void shouldReturnEmptyListWhenNoEligibleMatchesExist() {
            MatchDetailsDTO finished = createMatchDetailsDto(1, MatchState.FINISHED, COMP_ID, SEASON_ID);
            matchDetailsRepository.storage.put(1, finished);

            List<MatchDetailsDTO> eligible = service.getPreMatchEligibleMatches(COMP_ID, SEASON_ID);

            assertThat(eligible).isEmpty();
        }

        private MatchDetailsDTO createMatchDetailsDto(int matchId, MatchState state, int compId, int seasonId) {
            return new MatchDetailsDTO(
                    matchId, KICKOFF.plusSeconds(matchId * 3600L), state, false,
                    state == MatchState.FINISHED ? 2 : null,
                    state == MatchState.FINISHED ? 1 : null,
                    null, null, null, null, 0, 0,
                    null, null, 2.10, 3.40, 3.50,
                    false, false, false, false, false, 1.0, 1.0, 1.0, 1.0, 0,
                    compId, "I1", "Serie A", "Italy", -0.12,
                    seasonId, "2025/2026",
                    HOME_TEAM_ID, "Inter", AWAY_TEAM_ID, "Milan"
            );
        }
    }

    @Nested
    @DisplayName("Historical Sampling and League xG Tests")
    class HistoricalSamplingTests {

        @Test
        @DisplayName("Should analyze all matches in current season when M >= 10")
        void shouldFetchAllMatchesWhenCurrentSeasonHasAtLeastN() {
            // Seed 12 finished matches in current season
            for (int i = 1; i <= 12; i++) {
                Match m = new Match(
                        null, SEASON_ID, COMP_ID, HOME_TEAM_ID, AWAY_TEAM_ID,
                        KICKOFF.plusSeconds(i * 86400L), MatchState.FINISHED, false,
                        new org.nepe.match.domain.MatchStatistics(2, 1, 12, 8, 5, 3, 0, 0, 1.80, 0.90),
                        MatchModifiers.defaultModifiers(), 2.0, 3.2, 3.5, 90
                );
                matchRepository.save(m);
            }

            List<org.nepe.inference.domain.TeamStrengthCalculator.MatchPerformance> performances =
                    service.getHistoricalTeamPerformances(HOME_TEAM_ID, COMP_ID, SEASON_ID, 10);

            assertThat(performances).hasSize(12);
            assertThat(performances).allMatch(p -> !p.fromPreviousSeason());
            assertThat(performances.get(0).xgScored()).isEqualTo(1.80);
            assertThat(performances.get(0).xgConceded()).isEqualTo(0.90);
        }

        @Test
        @DisplayName("Should supplement with previous season matches when current season has M < 10")
        void shouldFetchPreviousSeasonMatchesWhenCurrentSeasonHasLessThanN() {
            int prevSeasonId = 2;
            Season prevSeason = Season.create("2024/2025");
            prevSeason.assignId(prevSeasonId);
            seasonRepository.save(prevSeason);

            // Seed 3 matches in current season (2025/2026)
            for (int i = 1; i <= 3; i++) {
                Match m = new Match(
                        null, SEASON_ID, COMP_ID, HOME_TEAM_ID, AWAY_TEAM_ID,
                        KICKOFF.plusSeconds(i * 86400L), MatchState.FINISHED, false,
                        new org.nepe.match.domain.MatchStatistics(2, 0, 10, 5, 4, 1, 0, 0, 1.50, 0.50),
                        MatchModifiers.defaultModifiers(), 2.0, 3.2, 3.5, 90
                );
                matchRepository.save(m);
            }

            // Seed 10 matches in previous season (2024/2025)
            for (int i = 1; i <= 10; i++) {
                Match m = new Match(
                        null, prevSeasonId, COMP_ID, HOME_TEAM_ID, AWAY_TEAM_ID,
                        KICKOFF.minusSeconds(i * 86400L), MatchState.FINISHED, false,
                        new org.nepe.match.domain.MatchStatistics(1, 1, 8, 8, 3, 3, 0, 0, 1.10, 1.10),
                        MatchModifiers.defaultModifiers(), 2.0, 3.2, 3.5, 90
                );
                matchRepository.save(m);
            }

            List<org.nepe.inference.domain.TeamStrengthCalculator.MatchPerformance> performances =
                    service.getHistoricalTeamPerformances(HOME_TEAM_ID, COMP_ID, SEASON_ID, 10);

            // 3 from current season + 7 from previous season = 10 total
            assertThat(performances).hasSize(10);
            long currentSeasonCount = performances.stream().filter(p -> !p.fromPreviousSeason()).count();
            long prevSeasonCount = performances.stream().filter(p -> p.fromPreviousSeason()).count();

            assertThat(currentSeasonCount).isEqualTo(3);
            assertThat(prevSeasonCount).isEqualTo(7);
        }

        @Test
        @DisplayName("Should correctly calculate league average xG per team")
        void shouldCalculateLeagueAverageXgPerTeam() {
            Match m1 = new Match(
                    null, SEASON_ID, COMP_ID, HOME_TEAM_ID, AWAY_TEAM_ID,
                    KICKOFF, MatchState.FINISHED, false,
                    new org.nepe.match.domain.MatchStatistics(2, 1, 10, 8, 4, 3, 0, 0, 2.00, 1.00),
                    MatchModifiers.defaultModifiers(), 2.0, 3.2, 3.5, 90
            );
            Match m2 = new Match(
                    null, SEASON_ID, COMP_ID, AWAY_TEAM_ID, HOME_TEAM_ID,
                    KICKOFF.plusSeconds(86400L), MatchState.FINISHED, false,
                    new org.nepe.match.domain.MatchStatistics(1, 1, 6, 6, 2, 2, 0, 0, 1.20, 1.40),
                    MatchModifiers.defaultModifiers(), 2.0, 3.2, 3.5, 90
            );
            matchRepository.save(m1);
            matchRepository.save(m2);

            double leagueAvg = service.getLeagueAverageXgPerTeam(COMP_ID, SEASON_ID);

            // (2.0 + 1.0 + 1.2 + 1.4) / (2 * 2 matches) = 5.6 / 4 = 1.40
            assertThat(leagueAvg).isEqualTo(1.40);
        }

        @Test
        @DisplayName("Should compute league average xG from finished matches only, ignoring scheduled matches")
        void shouldComputeLeagueAverageXgFromFinishedMatchesOnly() {
            // Match 1: Home xG = 2.0, Away xG = 1.0 -> Sum = 3.0
            Match m1 = new Match(
                    null, SEASON_ID, COMP_ID, HOME_TEAM_ID, AWAY_TEAM_ID,
                    KICKOFF, MatchState.FINISHED, false,
                    new org.nepe.match.domain.MatchStatistics(2, 1, 10, 5, 5, 2, 0, 0, 2.0, 1.0),
                    MatchModifiers.defaultModifiers(), 2.0, 3.2, 3.5, 90
            );
            // Match 2: Home xG = 1.2, Away xG = 1.8 -> Sum = 3.0
            Match m2 = new Match(
                    null, SEASON_ID, COMP_ID, HOME_TEAM_ID, AWAY_TEAM_ID,
                    KICKOFF.plusSeconds(86400L), MatchState.FINISHED, false,
                    new org.nepe.match.domain.MatchStatistics(1, 1, 8, 8, 3, 3, 0, 0, 1.2, 1.8),
                    MatchModifiers.defaultModifiers(), 2.0, 3.2, 3.5, 90
            );
            // Match 3: SCHEDULED (must be ignored)
            Match m3 = new Match(
                    null, SEASON_ID, COMP_ID, HOME_TEAM_ID, AWAY_TEAM_ID,
                    KICKOFF.plusSeconds(172800L), MatchState.SCHEDULED, false,
                    org.nepe.match.domain.MatchStatistics.empty(),
                    MatchModifiers.defaultModifiers(), 2.0, 3.2, 3.5, 0
            );

            matchRepository.save(m1);
            matchRepository.save(m2);
            matchRepository.save(m3);

            // Total xG = 3.0 + 3.0 = 6.0 across 2 finished matches -> avg per team per match = 6.0 / (2 * 2) = 1.50
            double avgXg = service.getLeagueAverageXgPerTeam(COMP_ID, SEASON_ID);

            assertThat(avgXg).isEqualTo(1.50);
        }
    }

    @Nested
    @DisplayName("getDynamicHomeAdvantage() Tests")
    class GetDynamicHomeAdvantageTests {

        @Test
        @DisplayName("Should return manual Home Advantage when configured on competition")
        void shouldReturnManualOverrideWhenConfigured() {
            Competition comp = competitionRepository.findById(COMP_ID).orElseThrow();
            comp.updateHomeAdvantage(1.28);
            competitionRepository.save(comp);

            double ha = service.getDynamicHomeAdvantage(COMP_ID, SEASON_ID);

            assertThat(ha).isEqualTo(1.28);
        }

        @Test
        @DisplayName("Should return default 1.20 when no matches and no previous season")
        void shouldReturnDefaultWhenNoMatchesAndNoPriorSeason() {
            double ha = service.getDynamicHomeAdvantage(COMP_ID, SEASON_ID);

            assertThat(ha).isEqualTo(1.20);
        }

        @Test
        @DisplayName("Should return previous season HA when current season has 0 matches (M=0)")
        void shouldReturnPriorSeasonHaWhenNoCurrentMatches() {
            // Setup previous season 2024/2025
            Season prevSeason = Season.create("2024/2025");
            prevSeason.assignId(2);
            seasonRepository.save(prevSeason);

            // Add finished matches to previous season: home xG 20.0, away xG 15.0 -> HA = 20/15 = 1.3333
            Match prevMatch = new Match(
                    null, 2, COMP_ID, HOME_TEAM_ID, AWAY_TEAM_ID,
                    KICKOFF.minusSeconds(86400L * 30), MatchState.FINISHED, false,
                    new org.nepe.match.domain.MatchStatistics(2, 1, 10, 8, 4, 3, 0, 0, 2.0, 1.5),
                    MatchModifiers.defaultModifiers(), 2.0, 3.2, 3.5, 90
            );
            matchRepository.save(prevMatch);

            double ha = service.getDynamicHomeAdvantage(COMP_ID, SEASON_ID);

            assertThat(ha).isCloseTo(2.0 / 1.5, org.assertj.core.data.Offset.offset(0.001));
        }

        @Test
        @DisplayName("Should blend prior and current season via Empirical Bayes Shrinkage with 40 matches")
        void shouldBlendPriorAndCurrentSeasonViaShrinkage() {
            // Previous season: HA = 1.30 (home xG 13.0, away xG 10.0)
            Season prevSeason = Season.create("2024/2025");
            prevSeason.assignId(2);
            seasonRepository.save(prevSeason);

            Match prevMatch = new Match(
                    null, 2, COMP_ID, HOME_TEAM_ID, AWAY_TEAM_ID,
                    KICKOFF.minusSeconds(86400L * 30), MatchState.FINISHED, false,
                    new org.nepe.match.domain.MatchStatistics(1, 1, 10, 8, 4, 3, 0, 0, 1.30, 1.00),
                    MatchModifiers.defaultModifiers(), 2.0, 3.2, 3.5, 90
            );
            matchRepository.save(prevMatch);

            // Current season: 40 finished matches with HA_current = 1.10 (home xG 1.10, away xG 1.00 each)
            for (int i = 0; i < 40; i++) {
                Match m = new Match(
                        null, SEASON_ID, COMP_ID, HOME_TEAM_ID, AWAY_TEAM_ID,
                        KICKOFF.plusSeconds(86400L * i), MatchState.FINISHED, false,
                        new org.nepe.match.domain.MatchStatistics(1, 1, 10, 8, 4, 3, 0, 0, 1.10, 1.00),
                        MatchModifiers.defaultModifiers(), 2.0, 3.2, 3.5, 90
                );
                matchRepository.save(m);
            }

            // w(40) = 40 / (40 + 40) = 0.50
            // HA = 0.50 * 1.30 + 0.50 * 1.10 = 1.20
            double ha = service.getDynamicHomeAdvantage(COMP_ID, SEASON_ID);

            assertThat(ha).isCloseTo(1.20, org.assertj.core.data.Offset.offset(0.001));
        }

        @Test
        @DisplayName("Should clamp Home Advantage within [1.00, 1.60]")
        void shouldClampHomeAdvantage() {
            // Extreme home advantage in previous season (e.g. 2.5) -> must be clamped to 1.60
            Season prevSeason = Season.create("2024/2025");
            prevSeason.assignId(2);
            seasonRepository.save(prevSeason);

            Match prevMatch = new Match(
                    null, 2, COMP_ID, HOME_TEAM_ID, AWAY_TEAM_ID,
                    KICKOFF.minusSeconds(86400L * 30), MatchState.FINISHED, false,
                    new org.nepe.match.domain.MatchStatistics(5, 0, 20, 2, 10, 0, 0, 0, 2.50, 0.50),
                    MatchModifiers.defaultModifiers(), 2.0, 3.2, 3.5, 90
            );
            matchRepository.save(prevMatch);

            double ha = service.getDynamicHomeAdvantage(COMP_ID, SEASON_ID);

            assertThat(ha).isEqualTo(1.60);
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
        private final Map<Integer, Set<Integer>> competitionTeams = new HashMap<>();

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
            competitionTeams.values().forEach(set -> set.remove(id));
        }

        @Override
        public long count() {
            return storage.size();
        }

        @Override
        public List<Team> findByCompetitionId(int competitionId) {
            Set<Integer> teamIds = competitionTeams.getOrDefault(competitionId, Set.of());
            return storage.values().stream().filter(t -> teamIds.contains(t.getId())).toList();
        }

        @Override
        public void associateTeamToCompetition(int competitionId, int teamId) {
            competitionTeams.computeIfAbsent(competitionId, k -> new HashSet<>()).add(teamId);
        }

        @Override
        public void disassociateTeamFromCompetition(int competitionId, int teamId) {
            Set<Integer> teams = competitionTeams.get(competitionId);
            if (teams != null) {
                teams.remove(teamId);
            }
        }

        @Override
        public boolean isTeamAssociatedWithCompetition(int competitionId, int teamId) {
            return competitionTeams.getOrDefault(competitionId, Set.of()).contains(teamId);
        }
    }
}
