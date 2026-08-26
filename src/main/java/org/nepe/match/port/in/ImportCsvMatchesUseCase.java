package org.nepe.match.port.in;

import java.io.InputStream;
import java.nio.file.Path;

/**
 * Inbound Port (Driving Port / Use Case) defining operations for importing and upserting match datasets
 * from Football-Data CSV sources.
 * <p>
 * Coordinates CSV parsing, competition validation, team alias resolution (raising {@link org.nepe.shared.exception.AliasMappingRequiredException}
 * if an unknown team is encountered), seasonal assignment, and match upserting with overwrite protection.
 */
public interface ImportCsvMatchesUseCase {

    /**
     * Ingests matches from a CSV input stream.
     *
     * @param csvInputStream    the stream containing raw CSV data (must not be null)
     * @param defaultSeasonName fallback season formatted name (e.g., "2025/2026") if not inferred
     * @return the {@link ImportCsvResultDTO} metrics summary
     * @throws org.nepe.shared.exception.AliasMappingRequiredException if a team name is not mapped
     * @throws org.nepe.shared.exception.DataImportException           if fatal parsing or validation errors occur
     */
    ImportCsvResultDTO importCsv(InputStream csvInputStream, String defaultSeasonName);

    /**
     * Ingests matches directly from a local CSV file path.
     *
     * @param csvFilePath       local filesystem path to the CSV file (must not be null)
     * @param defaultSeasonName fallback season formatted name (e.g., "2025/2026")
     * @return the {@link ImportCsvResultDTO} metrics summary
     * @throws org.nepe.shared.exception.AliasMappingRequiredException if a team name is not mapped
     * @throws org.nepe.shared.exception.DataImportException           if fatal parsing or validation errors occur
     */
    ImportCsvResultDTO importCsvFile(Path csvFilePath, String defaultSeasonName);

    /**
     * Ingests matches from a raw CSV text string.
     * Useful for automated integration testing.
     *
     * @param csvContent        raw CSV content string (must not be null)
     * @param defaultSeasonName fallback season formatted name (e.g., "2025/2026")
     * @return the {@link ImportCsvResultDTO} metrics summary
     * @throws org.nepe.shared.exception.AliasMappingRequiredException if a team name is not mapped
     * @throws org.nepe.shared.exception.DataImportException           if fatal parsing or validation errors occur
     */
    ImportCsvResultDTO importCsvContent(String csvContent, String defaultSeasonName);
}
