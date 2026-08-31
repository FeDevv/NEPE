package org.nepe.match.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.nepe.competition.domain.Competition;
import org.nepe.competition.domain.Season;
import org.nepe.competition.domain.Team;
import org.nepe.competition.domain.TeamAlias;
import org.nepe.competition.port.in.ManageSeasonUseCase;
import org.nepe.competition.port.in.ManageTeamUseCase;
import org.nepe.competition.port.out.CompetitionRepositoryPort;
import org.nepe.match.domain.Match;
import org.nepe.match.domain.MatchState;
import org.nepe.match.port.in.ImportCsvResultDTO;
import org.nepe.match.port.out.CsvParserPort;
import org.nepe.match.port.out.MatchRepositoryPort;
import org.nepe.match.port.out.RawCsvMatchRow;
import org.nepe.shared.exception.AliasMappingRequiredException;
import org.nepe.shared.exception.DataImportException;

import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ImportCsvMatchesService Unit Tests")
class ImportCsvMatchesServiceTest {

    private FakeCsvParser csvParser;
    private InMemoryMatchRepository matchRepository;
    private InMemoryCompetitionRepository competitionRepository;
    private FakeSeasonUseCase seasonUseCase;
    private FakeTeamUseCase teamUseCase;
    private ImportCsvMatchesService service;

    private static final int COMP_ID = 1;
    private static final int SEASON_ID = 1;
    private static final int INTER_ID = 10;
    private static final int MILAN_ID = 20;

    @BeforeEach
    void setUp() {
        csvParser = new FakeCsvParser();
        matchRepository = new InMemoryMatchRepository();
        competitionRepository = new InMemoryCompetitionRepository();
        seasonUseCase = new FakeSeasonUseCase();
        teamUseCase = new FakeTeamUseCase();

        // Seed reference entities
        Competition comp = Competition.create("I1", "Serie A", "Italy");
        comp.assignId(COMP_ID);
        competitionRepository.save(comp);

        Season season = Season.create("2025/2026");
        season.assignId(SEASON_ID);
        seasonUseCase.save(season);

        Team inter = Team.create("Inter");
        inter.assignId(INTER_ID);
        teamUseCase.saveTeam(inter);

        Team milan = Team.create("Milan");
        milan.assignId(MILAN_ID);
        teamUseCase.saveTeam(milan);

        service = new ImportCsvMatchesService(
                csvParser,
                matchRepository,
                competitionRepository,
                seasonUseCase,
                teamUseCase
        );
    }

    @Nested
    @DisplayName("Constructor and Invariants")
    class ConstructorTests {

        @Test
        @DisplayName("Should throw NullPointerException when dependencies are null")
        void shouldThrowWhenDependenciesNull() {
            assertThatThrownBy(() -> new ImportCsvMatchesService(null, matchRepository, competitionRepository, seasonUseCase, teamUseCase))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new ImportCsvMatchesService(csvParser, null, competitionRepository, seasonUseCase, teamUseCase))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new ImportCsvMatchesService(csvParser, matchRepository, null, seasonUseCase, teamUseCase))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new ImportCsvMatchesService(csvParser, matchRepository, competitionRepository, null, teamUseCase))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new ImportCsvMatchesService(csvParser, matchRepository, competitionRepository, seasonUseCase, null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Should throw DataImportException on null inputs to importCsvContent")
        void shouldThrowOnNullInput() {
            assertThatThrownBy(() -> service.importCsvContent(null, "2025/2026"))
                    .isInstanceOf(DataImportException.class);
            assertThatThrownBy(() -> service.importCsvContent("some,csv", null))
                    .isInstanceOf(DataImportException.class);
        }
    }

    @Nested
    @DisplayName("Ingestion and Upsert Pipeline Tests")
    class IngestionTests {

        @Test
        @DisplayName("Should insert newly encountered finished and scheduled matches")
        void shouldInsertNewMatches() {
            List<RawCsvMatchRow> rows = List.of(
                    // Finished match with full statistics
                    new RawCsvMatchRow("I1", "20/09/2025", "19:45", "Inter", "Milan", 2, 1, 14, 10, 5, 3, 0, 1, 2.10, 3.40, 3.50),
                    // Upcoming scheduled match without score
                    new RawCsvMatchRow("I1", "25/09/2025", "14:00", "Milan", "Inter", null, null, null, null, null, null, 0, 0, 2.40, 3.20, 2.90)
            );
            csvParser.setRows(rows);

            ImportCsvResultDTO result = service.importCsvContent("dummy_csv", "2025/2026");

            assertThat(result.totalRowsParsed()).isEqualTo(2);
            assertThat(result.newMatchesInserted()).isEqualTo(2);
            assertThat(result.existingMatchesUpdated()).isEqualTo(0);
            assertThat(result.manualMatchesPreserved()).isEqualTo(0);
            assertThat(result.skippedRows()).isEqualTo(0);
            assertThat(result.warnings()).isEmpty();

            List<Match> savedMatches = matchRepository.findAll();
            assertThat(savedMatches).hasSize(2);

            Match finishedMatch = savedMatches.stream().filter(m -> m.getState() == MatchState.FINISHED).findFirst().orElseThrow();
            assertThat(finishedMatch.getStatistics().getHomeScore()).isEqualTo(2);
            assertThat(finishedMatch.getStatistics().getAwayScore()).isEqualTo(1);
            assertThat(finishedMatch.getStatistics().getAwayRedCards()).isEqualTo(1);
            assertThat(finishedMatch.getCurrentMinute()).isEqualTo(90);

            Match scheduledMatch = savedMatches.stream().filter(m -> m.getState() == MatchState.SCHEDULED).findFirst().orElseThrow();
            assertThat(scheduledMatch.getCurrentMinute()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should update existing scheduled match when new result arrives in CSV")
        void shouldUpdateExistingMatch() {
            // First import: scheduled match
            RawCsvMatchRow scheduledRow = new RawCsvMatchRow("I1", "20/09/2025", "19:45", "Inter", "Milan", null, null, null, null, null, null, 0, 0, 2.10, 3.40, 3.50);
            csvParser.setRows(List.of(scheduledRow));
            service.importCsvContent("dummy", "2025/2026");

            assertThat(matchRepository.findAll().get(0).getState()).isEqualTo(MatchState.SCHEDULED);

            // Second import: match is now played and finished
            RawCsvMatchRow finishedRow = new RawCsvMatchRow("I1", "20/09/2025", "19:45", "Inter", "Milan", 3, 0, 16, 6, 7, 2, 0, 0, 2.10, 3.40, 3.50);
            csvParser.setRows(List.of(finishedRow));
            ImportCsvResultDTO updateResult = service.importCsvContent("dummy", "2025/2026");

            assertThat(updateResult.newMatchesInserted()).isEqualTo(0);
            assertThat(updateResult.existingMatchesUpdated()).isEqualTo(1);

            Match updated = matchRepository.findAll().get(0);
            assertThat(updated.getState()).isEqualTo(MatchState.FINISHED);
            assertThat(updated.getStatistics().getHomeScore()).isEqualTo(3);
        }

        @Test
        @DisplayName("Should preserve manually edited matches and increment manualMatchesPreserved")
        void shouldProtectManuallyEditedMatches() {
            // Seed a match and flag it as manually edited
            RawCsvMatchRow scheduledRow = new RawCsvMatchRow("I1", "20/09/2025", "19:45", "Inter", "Milan", 1, 0, 10, 5, 4, 2, 0, 0, 2.0, 3.2, 3.8);
            csvParser.setRows(List.of(scheduledRow));
            service.importCsvContent("dummy", "2025/2026");

            Match match = matchRepository.findAll().get(0);
            match.markAsManuallyEdited();
            matchRepository.save(match);

            // Re-import with conflicting score
            RawCsvMatchRow conflictRow = new RawCsvMatchRow("I1", "20/09/2025", "19:45", "Inter", "Milan", 5, 5, 20, 20, 10, 10, 0, 0, 2.0, 3.2, 3.8);
            csvParser.setRows(List.of(conflictRow));

            ImportCsvResultDTO result = service.importCsvContent("dummy", "2025/2026");

            assertThat(result.manualMatchesPreserved()).isEqualTo(1);
            assertThat(result.existingMatchesUpdated()).isEqualTo(0);

            // Verify original score is preserved
            Match preserved = matchRepository.findAll().get(0);
            assertThat(preserved.getStatistics().getHomeScore()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should backfill missing reference odds on manually edited matches while keeping manual score")
        void shouldBackfillMissingOddsOnManuallyEditedMatches() {
            // Initial import without odds
            RawCsvMatchRow initialRow = new RawCsvMatchRow("I1", "20/09/2025", "19:45", "Inter", "Milan", 2, 1, 12, 8, 4, 3, 0, 0, null, null, null);
            csvParser.setRows(List.of(initialRow));
            service.importCsvContent("dummy", "2025/2026");

            Match match = matchRepository.findAll().get(0);
            match.markAsManuallyEdited();
            matchRepository.save(match);

            assertThat(match.getOddsHome()).isNull();
            assertThat(match.isManuallyEdited()).isTrue();

            // Subsequent CSV import provides reference odds
            RawCsvMatchRow secondRow = new RawCsvMatchRow("I1", "20/09/2025", "19:45", "Inter", "Milan", 9, 9, 30, 30, 10, 10, 0, 0, 2.15, 3.40, 3.60);
            csvParser.setRows(List.of(secondRow));
            ImportCsvResultDTO result = service.importCsvContent("dummy", "2025/2026");

            assertThat(result.manualMatchesPreserved()).isEqualTo(1);
            assertThat(result.newMatchesInserted()).isEqualTo(0);

            Match updated = matchRepository.findAll().get(0);
            // Score remains manual override
            assertThat(updated.getStatistics().getHomeScore()).isEqualTo(2);
            assertThat(updated.getStatistics().getAwayScore()).isEqualTo(1);
            // Odds are backfilled
            assertThat(updated.getOddsHome()).isEqualTo(2.15);
            assertThat(updated.getOddsDraw()).isEqualTo(3.40);
            assertThat(updated.getOddsAway()).isEqualTo(3.60);
            assertThat(updated.isManuallyEdited()).isTrue();
        }

        @Test
        @DisplayName("Should preserve manually rescheduled kickoff time on manually edited match during CSV re-import")
        void shouldPreserveManualKickoffTimeOnReimport() {
            // Initial CSV import at 19:45
            RawCsvMatchRow initialRow = new RawCsvMatchRow("I1", "20/09/2025", "19:45", "Inter", "Milan", null, null, null, null, null, null, 0, 0, 2.10, 3.40, 3.50);
            csvParser.setRows(List.of(initialRow));
            service.importCsvContent("dummy", "2025/2026");

            Match match = matchRepository.findAll().get(0);
            // User manually reschedules the match to a custom time (e.g. 15:00 UTC)
            Instant manualReschedule = Instant.parse("2025-09-20T15:00:00Z");
            match.reschedule(manualReschedule);
            matchRepository.save(match);

            assertThat(match.isManuallyEdited()).isTrue();
            assertThat(match.getMatchDateTime()).isEqualTo(manualReschedule);

            // Re-importing CSV feed that still reports 19:45
            RawCsvMatchRow secondImportRow = new RawCsvMatchRow("I1", "20/09/2025", "19:45", "Inter", "Milan", 2, 0, 15, 5, 6, 2, 0, 0, 2.10, 3.40, 3.50);
            csvParser.setRows(List.of(secondImportRow));
            ImportCsvResultDTO result = service.importCsvContent("dummy", "2025/2026");

            assertThat(result.manualMatchesPreserved()).isEqualTo(1);

            Match reloaded = matchRepository.findAll().get(0);
            // Must keep manual schedule of 15:00 UTC, NOT overwrite back to 19:45
            assertThat(reloaded.getMatchDateTime()).isEqualTo(manualReschedule);
        }

        @Test
        @DisplayName("Should update default 12:00 UTC kickoff time when subsequent CSV specifies explicit time")
        void shouldUpdateDefaultKickoffTimeOnLaterCsvWithExplicitTime() {
            // First CSV import without Time column (defaults to 12:00:00 UTC)
            RawCsvMatchRow dateOnlyRow = new RawCsvMatchRow("I1", "20/09/2025", null, "Inter", "Milan", null, null, null, null, null, null, 0, 0, null, null, null);
            csvParser.setRows(List.of(dateOnlyRow));
            service.importCsvContent("dummy", "2025/2026");

            List<Match> initialMatches = matchRepository.findAll();
            assertThat(initialMatches).hasSize(1);
            assertThat(initialMatches.get(0).getMatchDateTime()).isEqualTo(Instant.parse("2025-09-20T12:00:00Z"));

            // Second CSV import has explicit time 20:45 UTC for the same date
            RawCsvMatchRow timeRow = new RawCsvMatchRow("I1", "20/09/2025", "20:45", "Inter", "Milan", null, null, null, null, null, null, 0, 0, 2.10, 3.40, 3.50);
            csvParser.setRows(List.of(timeRow));
            ImportCsvResultDTO result = service.importCsvContent("dummy", "2025/2026");

            assertThat(result.newMatchesInserted()).isEqualTo(0);
            assertThat(result.existingMatchesUpdated()).isEqualTo(1);

            List<Match> updatedMatches = matchRepository.findAll();
            assertThat(updatedMatches).hasSize(1);
            assertThat(updatedMatches.get(0).getMatchDateTime()).isEqualTo(Instant.parse("2025-09-20T20:45:00Z"));
            assertThat(updatedMatches.get(0).getOddsHome()).isEqualTo(2.10);
        }

        @Test
        @DisplayName("Should bubble up AliasMappingRequiredException when encountering unmapped team")
        void shouldThrowAliasMappingRequiredExceptionOnUnknownTeam() {
            RawCsvMatchRow unknownTeamRow = new RawCsvMatchRow("I1", "20/09/2025", "19:45", "Unknown FC", "Milan", null, null, null, null, null, null, 0, 0, null, null, null);
            csvParser.setRows(List.of(unknownTeamRow));

            assertThatThrownBy(() -> service.importCsvContent("dummy", "2025/2026"))
                    .isInstanceOf(AliasMappingRequiredException.class)
                    .satisfies(e -> {
                        AliasMappingRequiredException ex = (AliasMappingRequiredException) e;
                        assertThat(ex.getRawTeamName()).isEqualTo("Unknown FC");
                    });
        }

        @Test
        @DisplayName("Should skip rows with unknown competition codes and add warnings")
        void shouldSkipUnknownCompetition() {
            RawCsvMatchRow unknownCompRow = new RawCsvMatchRow("E0", "20/09/2025", "19:45", "Inter", "Milan", null, null, null, null, null, null, 0, 0, null, null, null);
            csvParser.setRows(List.of(unknownCompRow));

            ImportCsvResultDTO result = service.importCsvContent("dummy", "2025/2026");

            assertThat(result.skippedRows()).isEqualTo(1);
            assertThat(result.warnings()).hasSize(1);
            assertThat(result.warnings().get(0)).contains("Competition code 'E0' is not registered");
        }
    }

    // --- In-Memory Test Doubles ---

    private static class FakeCsvParser implements CsvParserPort {
        private List<RawCsvMatchRow> rows = List.of();

        public void setRows(List<RawCsvMatchRow> rows) {
            this.rows = rows;
        }

        @Override
        public List<RawCsvMatchRow> parseCsv(InputStream inputStream) {
            return rows;
        }

        @Override
        public List<RawCsvMatchRow> parseCsvContent(String csvContent) {
            return rows;
        }
    }

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

    private static class FakeSeasonUseCase implements ManageSeasonUseCase {
        private final Map<Integer, Season> storage = new HashMap<>();
        private int seq = 1;

        public void save(Season season) {
            storage.put(season.getId(), season);
        }

        @Override
        public Season createSeason(String name) {
            Season s = Season.create(name);
            s.assignId(seq++);
            storage.put(s.getId(), s);
            return s;
        }

        @Override
        public Season createSeasonFromYear(int startYear) {
            return createSeason(startYear + "/" + (startYear + 1));
        }

        @Override
        public Season getSeasonById(int id) {
            return storage.get(id);
        }

        @Override
        public Season getSeasonByName(String name) {
            return storage.values().stream().filter(s -> s.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
        }

        @Override
        public Season getOrCreateSeason(String name) {
            Season s = getSeasonByName(name);
            return (s != null) ? s : createSeason(name);
        }

        @Override
        public Season getLatestSeason() {
            return storage.values().stream().max(Season::compareTo).orElse(null);
        }

        @Override
        public List<Season> getAllSeasons() {
            return List.copyOf(storage.values());
        }

        @Override
        public void deleteSeason(int id) {
            storage.remove(id);
        }
    }

    private static class FakeTeamUseCase implements ManageTeamUseCase {
        private final Map<Integer, Team> storage = new HashMap<>();
        private int seq = 1;

        public void saveTeam(Team team) {
            storage.put(team.getId(), team);
        }

        @Override
        public Team createTeam(org.nepe.competition.port.in.CreateTeamCommand command) {
            Team t = Team.create(command.name());
            t.assignId(seq++);
            storage.put(t.getId(), t);
            return t;
        }

        @Override
        public Team renameTeam(org.nepe.competition.port.in.RenameTeamCommand command) {
            Team t = storage.get(command.id());
            if (t != null) {
                t.rename(command.newName());
            }
            return t;
        }

        @Override
        public Team getTeamById(int id) {
            return storage.get(id);
        }

        @Override
        public Team getTeamByName(String name) {
            return storage.values().stream().filter(t -> t.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
        }

        @Override
        public Team resolveTeamByRawName(String rawName) {
            return storage.values().stream()
                    .filter(t -> t.getName().equalsIgnoreCase(rawName.trim()))
                    .findFirst()
                    .orElseThrow(() -> new AliasMappingRequiredException(rawName));
        }

        @Override
        public TeamAlias mapAlias(org.nepe.competition.port.in.MapTeamAliasCommand command) {
            return null;
        }

        @Override
        public List<Team> getAllTeams() {
            return List.copyOf(storage.values());
        }

        @Override
        public List<Team> searchTeams(String query) {
            return List.of();
        }

        @Override
        public List<TeamAlias> getAliasesForTeam(int teamId) {
            return List.of();
        }

        @Override
        public List<TeamAlias> getAllAliases() {
            return List.of();
        }

        @Override
        public void deleteTeam(int id) {
            storage.remove(id);
        }

        @Override
        public void deleteAlias(int aliasId) {
        }
    }
}
