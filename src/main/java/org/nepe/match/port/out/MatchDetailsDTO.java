package org.nepe.match.port.out;

import org.nepe.match.domain.MatchState;

import java.time.Instant;

/**
 * Immutable denormalized projection DTO mapping the SQL view {@code v_matches_details}.
 * <p>
 * Unifies match data, statistics, modifiers, competition metadata, season name,
 * and official team names for single-query presentation in JavaFX TableViews.
 */
public record MatchDetailsDTO(
        int matchId,
        Instant matchDateTime,
        MatchState matchState,
        boolean isManuallyEdited,
        Integer homeScore,
        Integer awayScore,
        Integer homeShots,
        Integer awayShots,
        Integer homeShotsOnTarget,
        Integer awayShotsOnTarget,
        int homeRedCards,
        int awayRedCards,
        Double manualHomeXg,
        Double manualAwayXg,
        Double oddsHome,
        Double oddsDraw,
        Double oddsAway,
        boolean isNeutralVenue,
        boolean isMustWinHome,
        boolean isMustWinAway,
        boolean isLowUrgencyHome,
        boolean isLowUrgencyAway,
        double modAttHome,
        double modDefHome,
        double modAttAway,
        double modDefAway,
        int currentMinute,
        int competitionId,
        String competitionCode,
        String competitionName,
        String competitionCountry,
        double dixonColesRho,
        int seasonId,
        String seasonName,
        int homeTeamId,
        String homeTeamName,
        int awayTeamId,
        String awayTeamName
) {

    /**
     * Formats the match fixture label (e.g. "Inter vs Milan").
     */
    public String getFixtureLabel() {
        return homeTeamName + " vs " + awayTeamName;
    }

    /**
     * Formats the current scoreline (e.g. "2 - 1" or "-").
     */
    public String getScoreLabel() {
        if (homeScore != null && awayScore != null) {
            return homeScore + " - " + awayScore;
        }
        return "-";
    }

    /**
     * Checks if reference 1X2 market odds are present.
     */
    public boolean hasReferenceOdds() {
        return oddsHome != null && oddsDraw != null && oddsAway != null;
    }
}
