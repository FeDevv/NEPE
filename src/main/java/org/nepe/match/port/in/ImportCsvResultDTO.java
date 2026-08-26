package org.nepe.match.port.in;

import java.util.Collections;
import java.util.List;

/**
 * Immutable DTO reporting the execution metrics and outcome of a CSV dataset import.
 * <p>
 * Transferred to the JavaFX UI to display a summary dialog of the ingestion process.
 *
 * @param totalRowsParsed        total number of rows read from the CSV
 * @param newMatchesInserted     number of new matches created and persisted in the database
 * @param existingMatchesUpdated number of existing matches refreshed with updated scores/statistics
 * @param manualMatchesPreserved number of manually edited matches skipped to protect user edits
 * @param skippedRows            number of invalid or empty rows skipped during parsing
 * @param warnings               list of descriptive warning or non-fatal issue messages
 */
public record ImportCsvResultDTO(
        int totalRowsParsed,
        int newMatchesInserted,
        int existingMatchesUpdated,
        int manualMatchesPreserved,
        int skippedRows,
        List<String> warnings
) {

    public ImportCsvResultDTO {
        warnings = (warnings != null) ? List.copyOf(warnings) : Collections.emptyList();
    }

    /**
     * Formats a human-readable summary of the import outcome.
     */
    public String getSummaryMessage() {
        return String.format(
                "Import completed: %d total rows parsed. (%d inserted, %d updated, %d manual protected, %d skipped).",
                totalRowsParsed, newMatchesInserted, existingMatchesUpdated, manualMatchesPreserved, skippedRows
        );
    }
}
