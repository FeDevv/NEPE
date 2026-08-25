package org.nepe.match.port.out;

/**
 * Immutable DTO representing a single raw row parsed from a Football-Data CSV file.
 * <p>
 * Transferred from the CSV Parser Outbound Adapter to the Import Use Case before domain entity
 * resolution (resolving raw team names to team IDs via aliases, parsing UTC date/times, and season lookup).
 *
 * @param div         raw division/league code (e.g. "I1", "E0")
 * @param dateStr     raw match date string (e.g. "15/03/2026", "15/03/26")
 * @param timeStr     raw match time string (e.g. "19:45", nullable)
 * @param homeTeamRaw raw name of the home team as written in the CSV
 * @param awayTeamRaw raw name of the away team as written in the CSV
 * @param fthg        full time home goals (nullable if match is upcoming/scheduled)
 * @param ftag        full time away goals (nullable if match is upcoming/scheduled)
 * @param hs          home total shots (nullable)
 * @param as          away total shots (nullable)
 * @param hst         home shots on target (nullable)
 * @param ast         away shots on target (nullable)
 * @param hr          home red cards count
 * @param ar          away red cards count
 * @param oddsHome    reference pre-match home win odds (e.g. from AvgH / B365H, nullable)
 * @param oddsDraw    reference pre-match draw odds (e.g. from AvgD / B365D, nullable)
 * @param oddsAway    reference pre-match away win odds (e.g. from AvgA / B365A, nullable)
 */
public record RawCsvMatchRow(
        String div,
        String dateStr,
        String timeStr,
        String homeTeamRaw,
        String awayTeamRaw,
        Integer fthg,
        Integer ftag,
        Integer hs,
        Integer as,
        Integer hst,
        Integer ast,
        int hr,
        int ar,
        Double oddsHome,
        Double oddsDraw,
        Double oddsAway
) {
}
