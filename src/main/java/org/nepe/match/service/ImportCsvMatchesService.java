package org.nepe.match.service;

import org.nepe.competition.domain.Competition;
import org.nepe.competition.domain.Season;
import org.nepe.competition.domain.Team;
import org.nepe.competition.port.in.ManageSeasonUseCase;
import org.nepe.competition.port.in.ManageTeamUseCase;
import org.nepe.competition.port.out.CompetitionRepositoryPort;
import org.nepe.match.domain.Match;
import org.nepe.match.domain.MatchModifiers;
import org.nepe.match.domain.MatchState;
import org.nepe.match.domain.MatchStatistics;
import org.nepe.match.port.in.ImportCsvMatchesUseCase;
import org.nepe.match.port.in.ImportCsvResultDTO;
import org.nepe.match.port.out.CsvParserPort;
import org.nepe.match.port.out.MatchRepositoryPort;
import org.nepe.match.port.out.RawCsvMatchRow;
import org.nepe.shared.exception.DataImportException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Application Service implementing {@link ImportCsvMatchesUseCase}.
 * <p>
 * Orchestrates the full ingestion and upserting pipeline for Football-Data CSV datasets:
 * 1. Stream parsing via {@link CsvParserPort}.
 * 2. Competition and Season verification/auto-provisioning.
 * 3. Team resolution and alias discovery (throwing {@link org.nepe.shared.exception.AliasMappingRequiredException}).
 * 4. Match upserting with date/time unification and manual edit protection.
 */
@Service
public class ImportCsvMatchesService implements ImportCsvMatchesUseCase {

    private static final DateTimeFormatter[] DATE_FORMATTERS = {
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yy"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ofPattern("d/M/yy")
    };

    private static final DateTimeFormatter[] TIME_FORMATTERS = {
            DateTimeFormatter.ofPattern("HH:mm:ss"),
            DateTimeFormatter.ofPattern("HH:mm"),
            DateTimeFormatter.ofPattern("H:mm")
    };

    private static final LocalTime DEFAULT_MATCH_TIME = LocalTime.of(12, 0);

    private final CsvParserPort csvParserPort;
    private final MatchRepositoryPort matchRepositoryPort;
    private final CompetitionRepositoryPort competitionRepositoryPort;
    private final ManageSeasonUseCase manageSeasonUseCase;
    private final ManageTeamUseCase manageTeamUseCase;

    public ImportCsvMatchesService(CsvParserPort csvParserPort,
                                  MatchRepositoryPort matchRepositoryPort,
                                  CompetitionRepositoryPort competitionRepositoryPort,
                                  ManageSeasonUseCase manageSeasonUseCase,
                                  ManageTeamUseCase manageTeamUseCase) {
        this.csvParserPort = Objects.requireNonNull(csvParserPort, "CsvParserPort must not be null");
        this.matchRepositoryPort = Objects.requireNonNull(matchRepositoryPort, "MatchRepositoryPort must not be null");
        this.competitionRepositoryPort = Objects.requireNonNull(competitionRepositoryPort, "CompetitionRepositoryPort must not be null");
        this.manageSeasonUseCase = Objects.requireNonNull(manageSeasonUseCase, "ManageSeasonUseCase must not be null");
        this.manageTeamUseCase = Objects.requireNonNull(manageTeamUseCase, "ManageTeamUseCase must not be null");
    }

    @Override
    @Transactional
    public ImportCsvResultDTO importCsv(InputStream csvInputStream, String defaultSeasonName) {
        if (csvInputStream == null) {
            throw new DataImportException("CSV InputStream cannot be null.");
        }
        if (defaultSeasonName == null || defaultSeasonName.isBlank()) {
            throw new DataImportException("Default season name cannot be null or blank.");
        }

        List<RawCsvMatchRow> rows = csvParserPort.parseCsv(csvInputStream);
        return processRows(rows, defaultSeasonName);
    }

    @Override
    @Transactional
    public ImportCsvResultDTO importCsvFile(Path csvFilePath, String defaultSeasonName) {
        if (csvFilePath == null) {
            throw new DataImportException("CSV file path cannot be null.");
        }
        if (!Files.exists(csvFilePath) || !Files.isRegularFile(csvFilePath)) {
            throw new DataImportException(String.format("CSV file at '%s' does not exist or is not a readable file.", csvFilePath));
        }

        try (InputStream stream = Files.newInputStream(csvFilePath)) {
            return importCsv(stream, defaultSeasonName);
        } catch (IOException e) {
            throw new DataImportException("Error opening CSV file: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public ImportCsvResultDTO importCsvContent(String csvContent, String defaultSeasonName) {
        if (csvContent == null || csvContent.isBlank()) {
            throw new DataImportException("CSV content cannot be null or blank.");
        }

        ByteArrayInputStream stream = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8));
        return importCsv(stream, defaultSeasonName);
    }

    // --- Core Dataset Processing Pipeline ---

    private ImportCsvResultDTO processRows(List<RawCsvMatchRow> rows, String defaultSeasonName) {
        Season season = manageSeasonUseCase.getOrCreateSeason(defaultSeasonName);

        int totalRowsParsed = rows.size();
        int newMatchesInserted = 0;
        int existingMatchesUpdated = 0;
        int manualMatchesPreserved = 0;
        int skippedRows = 0;
        List<String> warnings = new ArrayList<>();

        Map<String, Competition> competitionCache = new HashMap<>();

        for (int i = 0; i < rows.size(); i++) {
            RawCsvMatchRow row = rows.get(i);

            // 1. Resolve Competition
            Competition comp = competitionCache.computeIfAbsent(row.div(), divCode ->
                    competitionRepositoryPort.findByCode(divCode).orElse(null)
            );

            if (comp == null) {
                warnings.add(String.format("Row %d skipped: Competition code '%s' is not registered.", i + 1, row.div()));
                skippedRows++;
                continue;
            }

            // 2. Parse Kickoff Timestamp
            Instant matchDateTime = parseKickoffInstant(row.dateStr(), row.timeStr());
            if (matchDateTime == null) {
                warnings.add(String.format("Row %d skipped: Unable to parse date/time '%s %s'.", i + 1, row.dateStr(), row.timeStr()));
                skippedRows++;
                continue;
            }

            // 3. Resolve Teams via 3-Tier Alias Resolution (Throws AliasMappingRequiredException if unknown)
            Team homeTeam = manageTeamUseCase.resolveTeamByRawName(row.homeTeamRaw());
            Team awayTeam = manageTeamUseCase.resolveTeamByRawName(row.awayTeamRaw());

            // 4. Upsert Matching against Database
            Optional<Match> existingMatchOpt = matchRepositoryPort.findByTeamsAndDateTime(
                    homeTeam.getId(),
                    awayTeam.getId(),
                    matchDateTime
            );

            boolean isFinished = (row.fthg() != null && row.ftag() != null);

            if (existingMatchOpt.isPresent()) {
                Match existing = existingMatchOpt.get();

                // Protect manually edited matches from automated CSV overwrites
                if (existing.isManuallyEdited()) {
                    manualMatchesPreserved++;
                } else {
                    if (isFinished) {
                        MatchStatistics stats = new MatchStatistics(
                                row.fthg(),
                                row.ftag(),
                                row.hs(),
                                row.as(),
                                row.hst(),
                                row.ast(),
                                row.hr(),
                                row.ar(),
                                null,
                                null
                        );
                        existing.updateStatistics(stats);
                        existing.finishMatch();
                    }

                    if (row.oddsHome() != null || row.oddsDraw() != null || row.oddsAway() != null) {
                        existing.updateReferenceOdds(row.oddsHome(), row.oddsDraw(), row.oddsAway());
                    }

                    matchRepositoryPort.save(existing);
                    existingMatchesUpdated++;
                }
            } else {
                MatchState state = isFinished ? MatchState.FINISHED : MatchState.SCHEDULED;
                MatchStatistics stats = isFinished ?
                        new MatchStatistics(row.fthg(), row.ftag(), row.hs(), row.as(), row.hst(), row.ast(), row.hr(), row.ar(), null, null) :
                        MatchStatistics.empty();
                int minute = isFinished ? 90 : 0;

                Match newMatch = new Match(
                        null,
                        season.getId(),
                        comp.getId(),
                        homeTeam.getId(),
                        awayTeam.getId(),
                        matchDateTime,
                        state,
                        false,
                        stats,
                        MatchModifiers.defaultModifiers(),
                        row.oddsHome(),
                        row.oddsDraw(),
                        row.oddsAway(),
                        minute
                );

                matchRepositoryPort.save(newMatch);
                newMatchesInserted++;
            }
        }

        return new ImportCsvResultDTO(
                totalRowsParsed,
                newMatchesInserted,
                existingMatchesUpdated,
                manualMatchesPreserved,
                skippedRows,
                warnings
        );
    }

    private static Instant parseKickoffInstant(String dateStr, String timeStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }

        LocalDate date = null;
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                date = LocalDate.parse(dateStr.trim(), formatter);
                break;
            } catch (DateTimeParseException ignored) {
            }
        }

        if (date == null) {
            return null;
        }

        LocalTime time = DEFAULT_MATCH_TIME;
        if (timeStr != null && !timeStr.isBlank()) {
            for (DateTimeFormatter formatter : TIME_FORMATTERS) {
                try {
                    time = LocalTime.parse(timeStr.trim(), formatter);
                    break;
                } catch (DateTimeParseException ignored) {
                }
            }
        }

        return date.atTime(time).toInstant(ZoneOffset.UTC);
    }
}
