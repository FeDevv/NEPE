package org.nepe.match.domain;

import org.nepe.shared.exception.DomainValidationException;
import org.nepe.shared.exception.LiveTradingException;

import java.time.Instant;
import java.util.Objects;
import java.util.OptionalDouble;

/**
 * Aggregate Root representing a football match in NEPE.
 * <p>
 * Unifies scheduling, relational team/competition context, live minute tracking,
 * statistical aggregation, tactical modifiers, and Betting Exchange reference odds.
 */
public class Match {

    public static final int MIN_MINUTE = 0;
    public static final int MAX_MINUTE = 130;

    private Integer id;
    private Integer seasonId;
    private Integer competitionId;
    private Integer homeTeamId;
    private Integer awayTeamId;
    private Instant matchDateTime;
    private MatchState state;
    private boolean manuallyEdited;

    // Sub-domain models (Value Objects)
    private MatchStatistics statistics;
    private MatchModifiers modifiers;

    // Reference 1X2 Pre-Match Odds (from CSV / Market)
    private Double oddsHome;
    private Double oddsDraw;
    private Double oddsAway;

    // Live Tracking
    private int currentMinute;

    /**
     * Factory method for creating an unpersisted, scheduled Match.
     */
    public static Match createScheduled(Integer seasonId,
                                         Integer competitionId,
                                         Integer homeTeamId,
                                         Integer awayTeamId,
                                         Instant matchDateTime,
                                         Double oddsHome,
                                         Double oddsDraw,
                                         Double oddsAway) {
        return new Match(
                null,
                seasonId,
                competitionId,
                homeTeamId,
                awayTeamId,
                matchDateTime,
                MatchState.SCHEDULED,
                false,
                MatchStatistics.empty(),
                MatchModifiers.defaultModifiers(),
                oddsHome,
                oddsDraw,
                oddsAway,
                0
        );
    }

    /**
     * Full constructor for domain reconstruction and validated creation.
     */
    public Match(Integer id,
                 Integer seasonId,
                 Integer competitionId,
                 Integer homeTeamId,
                 Integer awayTeamId,
                 Instant matchDateTime,
                 MatchState state,
                 boolean manuallyEdited,
                 MatchStatistics statistics,
                 MatchModifiers modifiers,
                 Double oddsHome,
                 Double oddsDraw,
                 Double oddsAway,
                 int currentMinute) {
        validateIds(seasonId, competitionId, homeTeamId, awayTeamId);
        validateMatchDateTime(matchDateTime);
        validateState(state);
        validateMinute(currentMinute);
        validateOdds(oddsHome, oddsDraw, oddsAway);

        this.id = id;
        this.seasonId = seasonId;
        this.competitionId = competitionId;
        this.homeTeamId = homeTeamId;
        this.awayTeamId = awayTeamId;
        this.matchDateTime = matchDateTime;
        this.state = state;
        this.manuallyEdited = manuallyEdited;
        this.statistics = (statistics != null) ? statistics : MatchStatistics.empty();
        this.modifiers = (modifiers != null) ? modifiers : MatchModifiers.defaultModifiers();
        this.oddsHome = oddsHome;
        this.oddsDraw = oddsDraw;
        this.oddsAway = oddsAway;
        this.currentMinute = currentMinute;
    }

    // --- State Transitions & Lifecycle Management ---

    /**
     * Starts the live match tracking session.
     */
    public void startLive() {
        if (state.isTerminal()) {
            throw new LiveTradingException(String.format("Cannot start live tracking for match in terminal state: %s", state));
        }
        this.state = MatchState.LIVE;
    }

    /**
     * Completes and finalizes the match.
     */
    public void finishMatch() {
        if (this.state == MatchState.CANCELLED) {
            throw new LiveTradingException("Cannot finalize a cancelled match.");
        }
        this.state = MatchState.FINISHED;
        if (this.currentMinute < 90) {
            this.currentMinute = 90;
        }
    }

    public void postponeMatch() {
        if (state.isTerminal()) {
            throw new LiveTradingException("Cannot postpone a completed or cancelled match.");
        }
        this.state = MatchState.POSTPONED;
        markAsManuallyEdited();
    }

    public void cancelMatch() {
        this.state = MatchState.CANCELLED;
        markAsManuallyEdited();
    }

    public void reschedule(Instant newDateTime) {
        validateMatchDateTime(newDateTime);
        this.matchDateTime = newDateTime;
        markAsManuallyEdited();
    }

    /**
     * Updates kickoff date/time from external feed without setting the manual edit flag.
     */
    public void updateKickoffFromFeed(Instant newDateTime) {
        validateMatchDateTime(newDateTime);
        this.matchDateTime = newDateTime;
    }

    // --- Live Event Applications ---

    /**
     * Applies a recorded event to the match's statistics and advances the minute if necessary.
     */
    public void applyEvent(MatchEvent event) {
        if (event == null) {
            throw new DomainValidationException("Event to apply cannot be null.");
        }
        if (!state.isLive() && !state.isScheduled()) {
            throw new LiveTradingException(String.format("Cannot apply events to match in state: %s", state));
        }

        if (event.isGoal()) {
            if (event.isHomeTeamEvent()) {
                statistics.incrementHomeScore();
            } else {
                statistics.incrementAwayScore();
            }
        } else if (event.isRedCard()) {
            if (event.isHomeTeamEvent()) {
                statistics.incrementHomeRedCards();
            } else {
                statistics.incrementAwayRedCards();
            }
        }

        if (event.getMinute() > this.currentMinute) {
            this.currentMinute = event.getMinute();
        }
    }

    /**
     * Reverts a previously applied event.
     */
    public void revertEvent(MatchEvent event) {
        if (event == null) {
            throw new DomainValidationException("Event to revert cannot be null.");
        }

        if (event.isGoal()) {
            if (event.isHomeTeamEvent()) {
                statistics.decrementHomeScore();
            } else {
                statistics.decrementAwayScore();
            }
        } else if (event.isRedCard()) {
            if (event.isHomeTeamEvent()) {
                statistics.decrementHomeRedCards();
            } else {
                statistics.decrementAwayRedCards();
            }
        }
    }

    public void updateCurrentMinute(int minute) {
        validateMinute(minute);
        if (!state.isLive()) {
            throw new LiveTradingException("Cannot update game minute when match is not LIVE.");
        }
        this.currentMinute = minute;
    }

    // --- Manual Edits & Overwrite Protection ---

    public void markAsManuallyEdited() {
        this.manuallyEdited = true;
    }

    public void updateModifiers(MatchModifiers newModifiers) {
        if (newModifiers == null) {
            throw new DomainValidationException("Modifiers cannot be null.");
        }
        this.modifiers = newModifiers;
        markAsManuallyEdited();
    }

    public void updateStatistics(MatchStatistics newStatistics) {
        if (newStatistics == null) {
            throw new DomainValidationException("Statistics cannot be null.");
        }
        this.statistics = newStatistics;
        markAsManuallyEdited();
    }

    /**
     * Updates statistics from external data feed without setting the manual edit flag.
     */
    public void updateStatisticsFromFeed(MatchStatistics newStatistics) {
        if (newStatistics == null) {
            throw new DomainValidationException("Statistics cannot be null.");
        }
        this.statistics = newStatistics;
    }

    public void updateReferenceOdds(Double home, Double draw, Double away) {
        validateOdds(home, draw, away);
        this.oddsHome = home;
        this.oddsDraw = draw;
        this.oddsAway = away;
    }

    public void assignId(Integer id) {
        if (id == null || id <= 0) {
            throw new DomainValidationException("Match ID must be a positive integer.");
        }
        if (this.id != null && !this.id.equals(id)) {
            throw new DomainValidationException("Cannot reassign an existing Match ID.");
        }
        this.id = id;
    }

    // --- Invariant Validations ---

    private static void validateIds(Integer seasonId, Integer compId, Integer homeId, Integer awayId) {
        if (seasonId == null || seasonId <= 0) {
            throw new DomainValidationException("Match seasonId must be a positive integer.");
        }
        if (compId == null || compId <= 0) {
            throw new DomainValidationException("Match competitionId must be a positive integer.");
        }
        if (homeId == null || homeId <= 0) {
            throw new DomainValidationException("Match homeTeamId must be a positive integer.");
        }
        if (awayId == null || awayId <= 0) {
            throw new DomainValidationException("Match awayTeamId must be a positive integer.");
        }
        if (homeId.equals(awayId)) {
            throw new DomainValidationException("Home team and Away team cannot be the same team (ID: " + homeId + ").");
        }
    }

    private static void validateMatchDateTime(Instant dateTime) {
        if (dateTime == null) {
            throw new DomainValidationException("Match date/time cannot be null.");
        }
    }

    private static void validateState(MatchState state) {
        if (state == null) {
            throw new DomainValidationException("Match state cannot be null.");
        }
    }

    private static void validateMinute(int minute) {
        if (minute < MIN_MINUTE || minute > MAX_MINUTE) {
            throw new DomainValidationException(
                    String.format("Current minute must be between %d and %d (received: %d).", MIN_MINUTE, MAX_MINUTE, minute)
            );
        }
    }

    private static void validateOdds(Double home, Double draw, Double away) {
        validateSingleOdd("Home", home);
        validateSingleOdd("Draw", draw);
        validateSingleOdd("Away", away);
    }

    private static void validateSingleOdd(String label, Double odd) {
        if (odd != null) {
            if (Double.isNaN(odd) || Double.isInfinite(odd)) {
                throw new DomainValidationException(label + " reference odd must be a valid finite number.");
            }
            if (odd < 1.01 || odd > 1000.0) {
                throw new DomainValidationException(label + " reference odd must be between 1.01 and 1000.0.");
            }
        }
    }

    // --- Domain Queries ---

    public boolean isScheduled() {
        return state.isScheduled();
    }

    public boolean isLive() {
        return state.isLive();
    }

    public boolean isFinished() {
        return state.isFinished();
    }

    public boolean isTerminal() {
        return state.isTerminal();
    }

    public boolean hasReferenceOdds() {
        return oddsHome != null && oddsDraw != null && oddsAway != null;
    }

    public OptionalDouble getEffectiveHomeXg() {
        return statistics.getEffectiveHomeXg();
    }

    public OptionalDouble getEffectiveAwayXg() {
        return statistics.getEffectiveAwayXg();
    }

    // --- Getters ---

    public Integer getId() {
        return id;
    }

    public Integer getSeasonId() {
        return seasonId;
    }

    public Integer getCompetitionId() {
        return competitionId;
    }

    public Integer getHomeTeamId() {
        return homeTeamId;
    }

    public Integer getAwayTeamId() {
        return awayTeamId;
    }

    public Instant getMatchDateTime() {
        return matchDateTime;
    }

    public MatchState getState() {
        return state;
    }

    public boolean isManuallyEdited() {
        return manuallyEdited;
    }

    public MatchStatistics getStatistics() {
        return statistics;
    }

    public MatchModifiers getModifiers() {
        return modifiers;
    }

    public Double getOddsHome() {
        return oddsHome;
    }

    public Double getOddsDraw() {
        return oddsDraw;
    }

    public Double getOddsAway() {
        return oddsAway;
    }

    public int getCurrentMinute() {
        return currentMinute;
    }

    // --- Identity & Equality based on unique business key: (homeTeamId, awayTeamId, matchDateTime) ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Match match = (Match) o;
        if (id != null && match.id != null) {
            return Objects.equals(id, match.id);
        }
        return Objects.equals(homeTeamId, match.homeTeamId) &&
                Objects.equals(awayTeamId, match.awayTeamId) &&
                Objects.equals(matchDateTime, match.matchDateTime);
    }

    @Override
    public int hashCode() {
        if (id != null) {
            return Objects.hash(id);
        }
        return Objects.hash(homeTeamId, awayTeamId, matchDateTime);
    }

    @Override
    public String toString() {
        return "Match{" +
                "id=" + id +
                ", seasonId=" + seasonId +
                ", competitionId=" + competitionId +
                ", homeTeamId=" + homeTeamId +
                ", awayTeamId=" + awayTeamId +
                ", matchDateTime=" + matchDateTime +
                ", state=" + state +
                ", manuallyEdited=" + manuallyEdited +
                ", currentMinute=" + currentMinute +
                '}';
    }
}
