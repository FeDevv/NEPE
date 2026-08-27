package org.nepe.match.adapter.out;

import org.nepe.match.port.out.CsvParserPort;
import org.nepe.match.port.out.RawCsvMatchRow;
import org.nepe.shared.exception.DataImportException;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Outbound Adapter implementing {@link CsvParserPort}.
 * <p>
 * Parses raw Football-Data CSV files into strongly-typed {@link RawCsvMatchRow} objects.
 * Employs dynamic header matching to support varied column orderings across historical seasons,
 * multi-tier odds fallback resolution (Avg -> BbAv -> B365 -> Max), and robust empty-cell handling.
 */
@Component
public class CsvParserAdapter implements CsvParserPort {

    // Mandatory Header Names
    private static final String HEADER_DIV = "DIV";
    private static final String HEADER_DATE = "DATE";
    private static final String[] HEADERS_HOME_TEAM = {"HOMETEAM", "HOME"};
    private static final String[] HEADERS_AWAY_TEAM = {"AWAYTEAM", "AWAY"};

    // Optional Statistics Headers
    private static final String HEADER_TIME = "TIME";
    private static final String[] HEADERS_FTHG = {"FTHG", "HG"};
    private static final String[] HEADERS_FTAG = {"FTAG", "AG"};
    private static final String HEADER_HS = "HS";
    private static final String HEADER_AS = "AS";
    private static final String HEADER_HST = "HST";
    private static final String HEADER_AST = "AST";
    private static final String HEADER_HR = "HR";
    private static final String HEADER_AR = "AR";

    // 1X2 Odds Fallback Priorities
    private static final String[] HEADERS_ODDS_HOME = {"AVGH", "BBAVH", "B365H", "MAXH", "PSH", "PH", "1XBH"};
    private static final String[] HEADERS_ODDS_DRAW = {"AVGD", "BBAVD", "B365D", "MAXD", "PSD", "PD", "1XBD"};
    private static final String[] HEADERS_ODDS_AWAY = {"AVGA", "BBAVA", "B365A", "MAXA", "PSA", "PA", "1XBA"};

    @Override
    public List<RawCsvMatchRow> parseCsv(InputStream inputStream) {
        if (inputStream == null) {
            throw new DataImportException("CSV InputStream cannot be null.");
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            return processReader(reader);
        } catch (IOException e) {
            throw new DataImportException("I/O error occurred while reading CSV stream: " + e.getMessage(), e);
        }
    }

    @Override
    public List<RawCsvMatchRow> parseCsvContent(String csvContent) {
        if (csvContent == null || csvContent.isBlank()) {
            throw new DataImportException("CSV content cannot be null or blank.");
        }

        try (BufferedReader reader = new BufferedReader(new StringReader(csvContent))) {
            return processReader(reader);
        } catch (IOException e) {
            throw new DataImportException("Error parsing CSV content string: " + e.getMessage(), e);
        }
    }

    // --- Core Line-by-Line Processor ---

    private List<RawCsvMatchRow> processReader(BufferedReader reader) throws IOException {
        String headerLine = reader.readLine();
        if (headerLine == null || headerLine.isBlank()) {
            throw new DataImportException("CSV file is empty or does not contain a header row.");
        }

        Map<String, Integer> headerIndexMap = parseHeaders(headerLine);
        validateMandatoryHeaders(headerIndexMap);

        List<RawCsvMatchRow> rows = new ArrayList<>();
        String line;
        int lineNumber = 1;

        while ((line = reader.readLine()) != null) {
            lineNumber++;
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue; // Skip trailing blank lines
            }

            List<String> tokens = parseCsvLine(trimmed);
            RawCsvMatchRow row = parseRow(tokens, headerIndexMap, lineNumber);
            if (row != null) {
                rows.add(row);
            }
        }

        if (rows.isEmpty()) {
            throw new DataImportException("CSV file contains headers but no valid match data rows.");
        }

        return List.copyOf(rows);
    }

    private Map<String, Integer> parseHeaders(String headerLine) {
        List<String> tokens = parseCsvLine(headerLine);
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < tokens.size(); i++) {
            String header = tokens.get(i).trim().toUpperCase();
            if (!header.isEmpty()) {
                map.put(header, i);
            }
        }
        return map;
    }

    private void validateMandatoryHeaders(Map<String, Integer> headers) {
        if (!headers.containsKey(HEADER_DIV)) {
            throw new DataImportException("CSV is missing mandatory league division header: 'Div'");
        }
        if (!headers.containsKey(HEADER_DATE)) {
            throw new DataImportException("CSV is missing mandatory date header: 'Date'");
        }
        if (resolveIndex(headers, HEADERS_HOME_TEAM) == null) {
            throw new DataImportException("CSV is missing mandatory home team header: 'HomeTeam' or 'Home'");
        }
        if (resolveIndex(headers, HEADERS_AWAY_TEAM) == null) {
            throw new DataImportException("CSV is missing mandatory away team header: 'AwayTeam' or 'Away'");
        }
    }

    private RawCsvMatchRow parseRow(List<String> tokens, Map<String, Integer> headers, int lineNumber) {
        String div = getRequiredToken(tokens, headers.get(HEADER_DIV), lineNumber, "Div");
        String dateStr = getRequiredToken(tokens, headers.get(HEADER_DATE), lineNumber, "Date");
        String homeTeamRaw = getRequiredToken(tokens, resolveIndex(headers, HEADERS_HOME_TEAM), lineNumber, "HomeTeam");
        String awayTeamRaw = getRequiredToken(tokens, resolveIndex(headers, HEADERS_AWAY_TEAM), lineNumber, "AwayTeam");

        // If core identifiers are blank, skip row
        if (div.isBlank() || dateStr.isBlank() || homeTeamRaw.isBlank() || awayTeamRaw.isBlank()) {
            return null;
        }

        String timeStr = getOptionalToken(tokens, headers.get(HEADER_TIME));
        Integer fthg = parseInteger(getOptionalToken(tokens, resolveIndex(headers, HEADERS_FTHG)));
        Integer ftag = parseInteger(getOptionalToken(tokens, resolveIndex(headers, HEADERS_FTAG)));
        Integer hs = parseInteger(getOptionalToken(tokens, headers.get(HEADER_HS)));
        Integer as = parseInteger(getOptionalToken(tokens, headers.get(HEADER_AS)));
        Integer hst = parseInteger(getOptionalToken(tokens, headers.get(HEADER_HST)));
        Integer ast = parseInteger(getOptionalToken(tokens, headers.get(HEADER_AST)));

        int hr = parseIntegerOrDefault(getOptionalToken(tokens, headers.get(HEADER_HR)), 0);
        int ar = parseIntegerOrDefault(getOptionalToken(tokens, headers.get(HEADER_AR)), 0);

        Double oddsHome = resolveFirstAvailableOdds(tokens, headers, HEADERS_ODDS_HOME);
        Double oddsDraw = resolveFirstAvailableOdds(tokens, headers, HEADERS_ODDS_DRAW);
        Double oddsAway = resolveFirstAvailableOdds(tokens, headers, HEADERS_ODDS_AWAY);

        return new RawCsvMatchRow(
                div.trim(),
                dateStr.trim(),
                (timeStr != null && !timeStr.isBlank()) ? timeStr.trim() : null,
                homeTeamRaw.trim(),
                awayTeamRaw.trim(),
                fthg,
                ftag,
                hs,
                as,
                hst,
                ast,
                hr,
                ar,
                oddsHome,
                oddsDraw,
                oddsAway
        );
    }

    // --- Helper Utilities for Tokens and Numeric Conversions ---

    private static Integer resolveIndex(Map<String, Integer> headers, String... candidateNames) {
        for (String candidate : candidateNames) {
            Integer idx = headers.get(candidate.toUpperCase());
            if (idx != null) {
                return idx;
            }
        }
        return null;
    }

    private static Double resolveFirstAvailableOdds(List<String> tokens, Map<String, Integer> headers, String... candidates) {
        for (String candidate : candidates) {
            Integer idx = headers.get(candidate);
            if (idx != null) {
                Double val = parseDouble(getOptionalToken(tokens, idx));
                if (val != null && val > 1.0) {
                    return val;
                }
            }
        }
        return null;
    }

    private static String getRequiredToken(List<String> tokens, Integer index, int lineNumber, String fieldName) {
        if (index == null || index >= tokens.size()) {
            throw new DataImportException(
                    String.format("Row %d is truncated or missing required column '%s'.", lineNumber, fieldName)
            );
        }
        return tokens.get(index);
    }

    private static String getOptionalToken(List<String> tokens, Integer index) {
        if (index == null || index >= tokens.size()) {
            return null;
        }
        return tokens.get(index);
    }

    private static Integer parseInteger(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static int parseIntegerOrDefault(String value, int defaultValue) {
        Integer val = parseInteger(value);
        return (val != null) ? val : defaultValue;
    }

    private static Double parseDouble(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Robust CSV line parser supporting quoted cells and arbitrary comma distributions.
     */
    private static List<String> parseCsvLine(String line) {
        List<String> tokens = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                tokens.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        tokens.add(sb.toString());
        return tokens;
    }
}
