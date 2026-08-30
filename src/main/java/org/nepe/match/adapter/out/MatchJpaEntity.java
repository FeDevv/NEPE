package org.nepe.match.adapter.out;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;

/**
 * JPA Entity mapping for the central {@code matches} database table.
 * <p>
 * Flat relational representation of the {@link org.nepe.match.domain.Match} aggregate root,
 * including scheduling, scorelines, shots, modifiers, and Betting Exchange reference odds.
 */
@Entity
@Table(name = "matches")
public class MatchJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "season_id", nullable = false)
    private Integer seasonId;

    @Column(name = "competition_id", nullable = false)
    private Integer competitionId;

    @Column(name = "home_team_id", nullable = false)
    private Integer homeTeamId;

    @Column(name = "away_team_id", nullable = false)
    private Integer awayTeamId;

    @Column(name = "match_date_time", nullable = false)
    private Instant matchDateTime;

    @Column(name = "state", nullable = false, length = 20)
    private String state;

    @Column(name = "is_manually_edited", nullable = false)
    private boolean isManuallyEdited;

    // --- Scoreline and Game Statistics ---

    @Column(name = "home_score")
    private Integer homeScore;

    @Column(name = "away_score")
    private Integer awayScore;

    @Column(name = "home_shots")
    private Integer homeShots;

    @Column(name = "away_shots")
    private Integer awayShots;

    @Column(name = "home_shots_on_target")
    private Integer homeShotsOnTarget;

    @Column(name = "away_shots_on_target")
    private Integer awayShotsOnTarget;

    @Column(name = "home_red_cards", nullable = false)
    private int homeRedCards;

    @Column(name = "away_red_cards", nullable = false)
    private int awayRedCards;

    // --- Expected Goals (xG) Manual Override ---

    @Column(name = "manual_home_xg")
    private Double manualHomeXg;

    @Column(name = "manual_away_xg")
    private Double manualAwayXg;

    // --- Reference Pre-Match Odds (1X2) ---

    @Column(name = "odds_home")
    private Double oddsHome;

    @Column(name = "odds_draw")
    private Double oddsDraw;

    @Column(name = "odds_away")
    private Double oddsAway;

    // --- Pre-Match Tactical Context & Modifiers ---

    @Column(name = "is_neutral_venue", nullable = false)
    private boolean isNeutralVenue;

    @Column(name = "must_win_home", nullable = false)
    private boolean mustWinHome;

    @Column(name = "must_win_away", nullable = false)
    private boolean mustWinAway;

    @Column(name = "low_urgency_home", nullable = false)
    private boolean lowUrgencyHome;

    @Column(name = "low_urgency_away", nullable = false)
    private boolean lowUrgencyAway;

    @Column(name = "mod_att_home", nullable = false)
    private double modAttHome;

    @Column(name = "mod_def_home", nullable = false)
    private double modDefHome;

    @Column(name = "mod_att_away", nullable = false)
    private double modAttAway;

    @Column(name = "mod_def_away", nullable = false)
    private double modDefAway;

    // --- Live Game Tracking ---

    @Column(name = "current_minute", nullable = false)
    private int currentMinute;

    /**
     * Default no-args constructor required by JPA / Hibernate.
     */
    protected MatchJpaEntity() {
    }

    /**
     * Full constructor for entity creation and domain mapping.
     */
    public MatchJpaEntity(Integer id,
                          Integer seasonId,
                          Integer competitionId,
                          Integer homeTeamId,
                          Integer awayTeamId,
                          Instant matchDateTime,
                          String state,
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
                          boolean mustWinHome,
                          boolean mustWinAway,
                          boolean lowUrgencyHome,
                          boolean lowUrgencyAway,
                          double modAttHome,
                          double modDefHome,
                          double modAttAway,
                          double modDefAway,
                          int currentMinute) {
        this.id = id;
        this.seasonId = seasonId;
        this.competitionId = competitionId;
        this.homeTeamId = homeTeamId;
        this.awayTeamId = awayTeamId;
        this.matchDateTime = matchDateTime;
        this.state = state;
        this.isManuallyEdited = isManuallyEdited;
        this.homeScore = homeScore;
        this.awayScore = awayScore;
        this.homeShots = homeShots;
        this.awayShots = awayShots;
        this.homeShotsOnTarget = homeShotsOnTarget;
        this.awayShotsOnTarget = awayShotsOnTarget;
        this.homeRedCards = homeRedCards;
        this.awayRedCards = awayRedCards;
        this.manualHomeXg = manualHomeXg;
        this.manualAwayXg = manualAwayXg;
        this.oddsHome = oddsHome;
        this.oddsDraw = oddsDraw;
        this.oddsAway = oddsAway;
        this.isNeutralVenue = isNeutralVenue;
        this.mustWinHome = mustWinHome;
        this.mustWinAway = mustWinAway;
        this.lowUrgencyHome = lowUrgencyHome;
        this.lowUrgencyAway = lowUrgencyAway;
        this.modAttHome = modAttHome;
        this.modDefHome = modDefHome;
        this.modAttAway = modAttAway;
        this.modDefAway = modDefAway;
        this.currentMinute = currentMinute;
    }

    // --- Getters and Setters ---

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getSeasonId() {
        return seasonId;
    }

    public void setSeasonId(Integer seasonId) {
        this.seasonId = seasonId;
    }

    public Integer getCompetitionId() {
        return competitionId;
    }

    public void setCompetitionId(Integer competitionId) {
        this.competitionId = competitionId;
    }

    public Integer getHomeTeamId() {
        return homeTeamId;
    }

    public void setHomeTeamId(Integer homeTeamId) {
        this.homeTeamId = homeTeamId;
    }

    public Integer getAwayTeamId() {
        return awayTeamId;
    }

    public void setAwayTeamId(Integer awayTeamId) {
        this.awayTeamId = awayTeamId;
    }

    public Instant getMatchDateTime() {
        return matchDateTime;
    }

    public void setMatchDateTime(Instant matchDateTime) {
        this.matchDateTime = matchDateTime;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public boolean isManuallyEdited() {
        return isManuallyEdited;
    }

    public void setManuallyEdited(boolean manuallyEdited) {
        isManuallyEdited = manuallyEdited;
    }

    public Integer getHomeScore() {
        return homeScore;
    }

    public void setHomeScore(Integer homeScore) {
        this.homeScore = homeScore;
    }

    public Integer getAwayScore() {
        return awayScore;
    }

    public void setAwayScore(Integer awayScore) {
        this.awayScore = awayScore;
    }

    public Integer getHomeShots() {
        return homeShots;
    }

    public void setHomeShots(Integer homeShots) {
        this.homeShots = homeShots;
    }

    public Integer getAwayShots() {
        return awayShots;
    }

    public void setAwayShots(Integer awayShots) {
        this.awayShots = awayShots;
    }

    public Integer getHomeShotsOnTarget() {
        return homeShotsOnTarget;
    }

    public void setHomeShotsOnTarget(Integer homeShotsOnTarget) {
        this.homeShotsOnTarget = homeShotsOnTarget;
    }

    public Integer getAwayShotsOnTarget() {
        return awayShotsOnTarget;
    }

    public void setAwayShotsOnTarget(Integer awayShotsOnTarget) {
        this.awayShotsOnTarget = awayShotsOnTarget;
    }

    public int getHomeRedCards() {
        return homeRedCards;
    }

    public void setHomeRedCards(int homeRedCards) {
        this.homeRedCards = homeRedCards;
    }

    public int getAwayRedCards() {
        return awayRedCards;
    }

    public void setAwayRedCards(int awayRedCards) {
        this.awayRedCards = awayRedCards;
    }

    public Double getManualHomeXg() {
        return manualHomeXg;
    }

    public void setManualHomeXg(Double manualHomeXg) {
        this.manualHomeXg = manualHomeXg;
    }

    public Double getManualAwayXg() {
        return manualAwayXg;
    }

    public void setManualAwayXg(Double manualAwayXg) {
        this.manualAwayXg = manualAwayXg;
    }

    public Double getOddsHome() {
        return oddsHome;
    }

    public void setOddsHome(Double oddsHome) {
        this.oddsHome = oddsHome;
    }

    public Double getOddsDraw() {
        return oddsDraw;
    }

    public void setOddsDraw(Double oddsDraw) {
        this.oddsDraw = oddsDraw;
    }

    public Double getOddsAway() {
        return oddsAway;
    }

    public void setOddsAway(Double oddsAway) {
        this.oddsAway = oddsAway;
    }

    public boolean isNeutralVenue() {
        return isNeutralVenue;
    }

    public void setNeutralVenue(boolean neutralVenue) {
        isNeutralVenue = neutralVenue;
    }

    public boolean isMustWinHome() {
        return mustWinHome;
    }

    public void setMustWinHome(boolean mustWinHome) {
        this.mustWinHome = mustWinHome;
    }

    public boolean isMustWinAway() {
        return mustWinAway;
    }

    public void setMustWinAway(boolean mustWinAway) {
        this.mustWinAway = mustWinAway;
    }

    public boolean isLowUrgencyHome() {
        return lowUrgencyHome;
    }

    public void setLowUrgencyHome(boolean lowUrgencyHome) {
        this.lowUrgencyHome = lowUrgencyHome;
    }

    public boolean isLowUrgencyAway() {
        return lowUrgencyAway;
    }

    public void setLowUrgencyAway(boolean lowUrgencyAway) {
        this.lowUrgencyAway = lowUrgencyAway;
    }

    public double getModAttHome() {
        return modAttHome;
    }

    public void setModAttHome(double modAttHome) {
        this.modAttHome = modAttHome;
    }

    public double getModDefHome() {
        return modDefHome;
    }

    public void setModDefHome(double modDefHome) {
        this.modDefHome = modDefHome;
    }

    public double getModAttAway() {
        return modAttAway;
    }

    public void setModAttAway(double modAttAway) {
        this.modAttAway = modAttAway;
    }

    public double getModDefAway() {
        return modDefAway;
    }

    public void setModDefAway(double modDefAway) {
        this.modDefAway = modDefAway;
    }

    public int getCurrentMinute() {
        return currentMinute;
    }

    public void setCurrentMinute(int currentMinute) {
        this.currentMinute = currentMinute;
    }

    // --- Identity, Equality and Diagnostics ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MatchJpaEntity that = (MatchJpaEntity) o;
        if (id != null && that.id != null) {
            return Objects.equals(id, that.id);
        }
        return Objects.equals(homeTeamId, that.homeTeamId) &&
                Objects.equals(awayTeamId, that.awayTeamId) &&
                Objects.equals(matchDateTime, that.matchDateTime);
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
        return "MatchJpaEntity{" +
                "id=" + id +
                ", seasonId=" + seasonId +
                ", competitionId=" + competitionId +
                ", homeTeamId=" + homeTeamId +
                ", awayTeamId=" + awayTeamId +
                ", matchDateTime=" + matchDateTime +
                ", state='" + state + '\'' +
                ", currentMinute=" + currentMinute +
                '}';
    }
}
