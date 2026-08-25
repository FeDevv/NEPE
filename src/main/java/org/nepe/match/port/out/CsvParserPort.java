package org.nepe.match.port.out;

import java.io.InputStream;
import java.util.List;

/**
 * Outbound Port (Driven Port / SPI) defining the contract for parsing raw Football-Data CSV files.
 * <p>
 * Decouples file streaming and CSV text parsing from domain ingestion workflows.
 */
public interface CsvParserPort {

    /**
     * Parses an input stream of CSV data into a collection of raw match rows.
     *
     * @param inputStream the stream containing raw CSV data (must not be null)
     * @return list of parsed {@link RawCsvMatchRow} objects
     * @throws org.nepe.shared.exception.DataImportException if the stream contains fatal formatting or header errors
     */
    List<RawCsvMatchRow> parseCsv(InputStream inputStream);

    /**
     * Parses a string containing raw CSV text into a collection of raw match rows.
     * Useful for direct text parsing and automated testing.
     *
     * @param csvContent raw CSV string content (must not be null)
     * @return list of parsed {@link RawCsvMatchRow} objects
     * @throws org.nepe.shared.exception.DataImportException if the content contains fatal formatting or header errors
     */
    List<RawCsvMatchRow> parseCsvContent(String csvContent);
}
