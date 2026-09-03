package org.nepe.match.service;

import org.nepe.competition.domain.Competition;
import org.nepe.competition.domain.Season;
import org.nepe.competition.port.out.CompetitionRepositoryPort;
import org.nepe.competition.port.out.SeasonRepositoryPort;
import org.nepe.competition.port.out.TeamRepositoryPort;
import org.nepe.inference.domain.TeamStrengthCalculator;
import org.nepe.inference.domain.XgEstimator;
import org.nepe.match.domain.Match;
import org.nepe.match.domain.MatchState;
import org.nepe.match.domain.MatchStatistics;
import org.nepe.match.port.in.CreateMatchCommand;
import org.nepe.match.port.in.ManageMatchUseCase;
import org.nepe.match.port.in.UpdateMatchCommand;
import org.nepe.match.port.in.UpdateMatchStatisticsCommand;
import org.nepe.match.port.out.MatchDetailsDTO;
import org.nepe.match.port.out.MatchDetailsRepositoryPort;
import org.nepe.match.port.out.MatchRepositoryPort;
import org.nepe.shared.exception.DomainValidationException;
import org.nepe.shared.exception.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Application Service implementing {@link ManageMatchUseCase}.
 * <p>
 * Orchestrates match scheduling, lifecycle transitions (SCHEDULED -> LIVE -> FINISHED / POSTPONED / CANCELLED),
 * manual statistic overrides with overwrite protection, and denormalized dashboard queries.
 */
@Service
public class MatchService implements ManageMatchUseCase {

    private final MatchRepositoryPort matchRepositoryPort;
    private final MatchDetailsRepositoryPort matchDetailsRepositoryPort;
    private final CompetitionRepositoryPort competitionRepositoryPort;
    private final SeasonRepositoryPort seasonRepositoryPort;
    private final TeamRepositoryPort teamRepositoryPort;

    public MatchService(MatchRepositoryPort matchRepositoryPort,
                        MatchDetailsRepositoryPort matchDetailsRepositoryPort,
                        CompetitionRepositoryPort competitionRepositoryPort,
                        SeasonRepositoryPort seasonRepositoryPort,
                        TeamRepositoryPort teamRepositoryPort) {
        this.matchRepositoryPort = Objects.requireNonNull(matchRepositoryPort, "MatchRepositoryPort must not be null");
        this.matchDetailsRepositoryPort = Objects.requireNonNull(matchDetailsRepositoryPort, "MatchDetailsRepositoryPort must not be null");
        this.competitionRepositoryPort = Objects.requireNonNull(competitionRepositoryPort, "CompetitionRepositoryPort must not be null");
        this.seasonRepositoryPort = Objects.requireNonNull(seasonRepositoryPort, "SeasonRepositoryPort must not be null");
        this.teamRepositoryPort = Objects.requireNonNull(teamRepositoryPort, "TeamRepositoryPort must not be null");
    }

    @Override
    @Transactional
    public Match createMatch(CreateMatchCommand command) {
        if (command == null) {
            throw new DomainValidationException("CreateMatchCommand cannot be null.");
        }

        validateEntitiesExist(command.competitionId(), command.seasonId(), command.homeTeamId(), command.awayTeamId());

        if (command.homeTeamId() == command.awayTeamId()) {
            throw new DomainValidationException("Home team and Away team cannot be identical.");
        }

        if (matchRepositoryPort.findByTeamsAndDateTime(command.homeTeamId(), command.awayTeamId(), command.matchDateTime()).isPresent()) {
            throw new DomainValidationException(
                    String.format("A match between home team %d and away team %d at '%s' already exists.",
                            command.homeTeamId(), command.awayTeamId(), command.matchDateTime())
            );
        }

        Match match = Match.createScheduled(
                command.seasonId(),
                command.competitionId(),
                command.homeTeamId(),
                command.awayTeamId(),
                command.matchDateTime(),
                command.oddsHome(),
                command.oddsDraw(),
                command.oddsAway()
        );

        return matchRepositoryPort.save(match);
    }

    @Override
    @Transactional
    public Match updateMatch(UpdateMatchCommand command) {
        if (command == null) {
            throw new DomainValidationException("UpdateMatchCommand cannot be null.");
        }

        Match match = findMatchOrThrow(command.matchId());

        if (command.matchDateTime() != null && !command.matchDateTime().equals(match.getMatchDateTime())) {
            match.reschedule(command.matchDateTime());
        }

        match.updateReferenceOdds(command.oddsHome(), command.oddsDraw(), command.oddsAway());

        if (command.modifiers() != null) {
            match.updateModifiers(command.modifiers());
        }

        match.markAsManuallyEdited();
        return matchRepositoryPort.save(match);
    }

    @Override
    @Transactional
    public Match updateStatistics(UpdateMatchStatisticsCommand command) {
        if (command == null) {
            throw new DomainValidationException("UpdateMatchStatisticsCommand cannot be null.");
        }

        Match match = findMatchOrThrow(command.matchId());

        int homeRedCards = (command.homeRedCards() != null)
                ? command.homeRedCards()
                : match.getStatistics().getHomeRedCards();
        int awayRedCards = (command.awayRedCards() != null)
                ? command.awayRedCards()
                : match.getStatistics().getAwayRedCards();

        MatchStatistics updatedStats = new MatchStatistics(
                command.homeScore(),
                command.awayScore(),
                command.homeShots(),
                command.awayShots(),
                command.homeShotsOnTarget(),
                command.awayShotsOnTarget(),
                homeRedCards,
                awayRedCards,
                command.manualHomeXg(),
                command.manualAwayXg()
        );

        match.updateStatistics(updatedStats);
        match.markAsManuallyEdited();

        return matchRepositoryPort.save(match);
    }

    @Override
    @Transactional
    public Match markAsPostponed(int matchId) {
        Match match = findMatchOrThrow(matchId);
        match.postponeMatch();
        match.markAsManuallyEdited();
        return matchRepositoryPort.save(match);
    }

    @Override
    @Transactional
    public Match markAsCancelled(int matchId) {
        Match match = findMatchOrThrow(matchId);
        match.cancelMatch();
        match.markAsManuallyEdited();
        return matchRepositoryPort.save(match);
    }

    @Override
    @Transactional
    public Match markAsFinished(int matchId) {
        Match match = findMatchOrThrow(matchId);
        match.finishMatch();
        return matchRepositoryPort.save(match);
    }

    @Override
    @Transactional(readOnly = true)
    public Match getMatchById(int matchId) {
        return findMatchOrThrow(matchId);
    }

    @Override
    @Transactional(readOnly = true)
    public MatchDetailsDTO getMatchDetailsById(int matchId) {
        return matchDetailsRepositoryPort.findDetailsById(matchId)
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format("Match details with ID %d not found.", matchId)
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MatchDetailsDTO> getMatchDetailsByCompetitionAndSeason(int competitionId, int seasonId) {
        return matchDetailsRepositoryPort.findDetailsByCompetitionAndSeason(competitionId, seasonId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MatchDetailsDTO> getMatchDetailsByState(int competitionId, int seasonId, MatchState state) {
        if (state == null) {
            return matchDetailsRepositoryPort.findDetailsByCompetitionAndSeason(competitionId, seasonId);
        }
        return matchDetailsRepositoryPort.findDetailsByCompetitionAndSeasonAndState(competitionId, seasonId, state);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MatchDetailsDTO> getPreMatchEligibleMatches(int competitionId, int seasonId) {
        return matchDetailsRepositoryPort.findDetailsByCompetitionAndSeason(competitionId, seasonId)
                .stream()
                .filter(dto -> dto.matchState() != null && dto.matchState().allowsPreMatchAnalysis())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MatchDetailsDTO> getAllMatchDetails() {
        return matchDetailsRepositoryPort.findAllDetails();
    }

    @Override
    @Transactional(readOnly = true)
    public List<org.nepe.inference.domain.TeamStrengthCalculator.MatchPerformance> getHistoricalTeamPerformances(
            int teamId,
            int competitionId,
            int seasonId,
            int minSampleSize) {
        int safeN = Math.max(1, minSampleSize);
        List<Match> currentSeasonMatches = matchRepositoryPort.findFinishedMatchesForTeamInSeason(teamId, competitionId, seasonId);

        List<org.nepe.inference.domain.TeamStrengthCalculator.MatchPerformance> performances = new ArrayList<>();

        if (currentSeasonMatches.size() >= safeN) {
            for (Match m : currentSeasonMatches) {
                performances.add(mapToPerformance(m, teamId, false));
            }
        } else {
            for (Match m : currentSeasonMatches) {
                performances.add(mapToPerformance(m, teamId, false));
            }

            int needed = safeN - currentSeasonMatches.size();
            Optional<org.nepe.competition.domain.Season> currentSeasonOpt = seasonRepositoryPort.findById(seasonId);
            if (currentSeasonOpt.isPresent()) {
                String prevSeasonName = currentSeasonOpt.get().previous().getName();
                Optional<org.nepe.competition.domain.Season> prevSeasonOpt = seasonRepositoryPort.findByName(prevSeasonName);
                if (prevSeasonOpt.isPresent()) {
                    List<Match> prevMatches = matchRepositoryPort.findFinishedMatchesForTeamInSeason(teamId, competitionId, prevSeasonOpt.get().getId());
                    int toTake = Math.min(needed, prevMatches.size());
                    for (int i = 0; i < toTake; i++) {
                        performances.add(mapToPerformance(prevMatches.get(i), teamId, true));
                    }
                }
            }
        }

        return List.copyOf(performances);
    }

    @Override
    @Transactional(readOnly = true)
    public double getLeagueAverageXgPerTeam(int competitionId, int seasonId) {
        List<Match> finishedMatches = matchRepositoryPort.findFinishedMatchesByCompetitionAndSeason(competitionId, seasonId);

        if (finishedMatches.isEmpty()) {
            return org.nepe.inference.domain.TeamStrengthCalculator.DEFAULT_LEAGUE_AVG_XG;
        }

        double totalXg = 0.0;
        for (Match m : finishedMatches) {
            double homeXg = resolveXg(
                    m.getStatistics().getManualHomeXg(),
                    m.getStatistics().getHomeShots(),
                    m.getStatistics().getHomeShotsOnTarget(),
                    m.getStatistics().getHomeScore()
            );
            double awayXg = resolveXg(
                    m.getStatistics().getManualAwayXg(),
                    m.getStatistics().getAwayShots(),
                    m.getStatistics().getAwayShotsOnTarget(),
                    m.getStatistics().getAwayScore()
            );
            totalXg += (homeXg + awayXg);
        }

        double avg = totalXg / (2.0 * finishedMatches.size());
        return Math.max(0.5, avg);
    }

    @Override
    @Transactional(readOnly = true)
    public double getDynamicHomeAdvantage(int competitionId, int seasonId) {
        Competition competition = competitionRepositoryPort.findById(competitionId)
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format("Competition with ID %d not found.", competitionId)
                ));

        if (competition.hasManualHomeAdvantage()) {
            return competition.getHomeAdvantage();
        }

        // 1. Calculate HA_current on finished matches of active season
        List<Match> currentMatches = matchRepositoryPort.findFinishedMatchesByCompetitionAndSeason(competitionId, seasonId);
        int currentCount = currentMatches.size();
        double haCurrent = TeamStrengthCalculator.DEFAULT_HOME_ADVANTAGE;

        if (currentCount > 0) {
            double sumHomeXg = 0.0;
            double sumAwayXg = 0.0;
            for (Match m : currentMatches) {
                sumHomeXg += resolveXg(
                        m.getStatistics().getManualHomeXg(),
                        m.getStatistics().getHomeShots(),
                        m.getStatistics().getHomeShotsOnTarget(),
                        m.getStatistics().getHomeScore()
                );
                sumAwayXg += resolveXg(
                        m.getStatistics().getManualAwayXg(),
                        m.getStatistics().getAwayShots(),
                        m.getStatistics().getAwayShotsOnTarget(),
                        m.getStatistics().getAwayScore()
                );
            }
            if (sumAwayXg > 0.0) {
                haCurrent = sumHomeXg / sumAwayXg;
            }
        }

        // 2. Calculate HA_prior on previous season (if available)
        double haPrior = TeamStrengthCalculator.DEFAULT_HOME_ADVANTAGE;
        Optional<Season> currentSeasonOpt = seasonRepositoryPort.findById(seasonId);
        if (currentSeasonOpt.isPresent()) {
            String prevSeasonName = currentSeasonOpt.get().previous().getName();
            Optional<Season> prevSeasonOpt = seasonRepositoryPort.findByName(prevSeasonName);
            if (prevSeasonOpt.isPresent()) {
                List<Match> prevMatches = matchRepositoryPort.findFinishedMatchesByCompetitionAndSeason(competitionId, prevSeasonOpt.get().getId());
                if (!prevMatches.isEmpty()) {
                    double prevHomeXg = 0.0;
                    double prevAwayXg = 0.0;
                    for (Match m : prevMatches) {
                        prevHomeXg += resolveXg(
                                m.getStatistics().getManualHomeXg(),
                                m.getStatistics().getHomeShots(),
                                m.getStatistics().getHomeShotsOnTarget(),
                                m.getStatistics().getHomeScore()
                        );
                        prevAwayXg += resolveXg(
                                m.getStatistics().getManualAwayXg(),
                                m.getStatistics().getAwayShots(),
                                m.getStatistics().getAwayShotsOnTarget(),
                                m.getStatistics().getAwayScore()
                        );
                    }
                    if (prevAwayXg > 0.0) {
                        haPrior = prevHomeXg / prevAwayXg;
                    }
                }
            }
        }

        // 3. Apply Empirical Bayes Shrinkage: w(M) = M / (M + 40)
        double weight = (double) currentCount / (currentCount + 40.0);
        double haEstimate = ((1.0 - weight) * haPrior) + (weight * haCurrent);

        // 4. Safety Clamping in [1.00, 1.60]
        return Math.max(1.00, Math.min(1.60, haEstimate));
    }

    private TeamStrengthCalculator.MatchPerformance mapToPerformance(Match m, int teamId, boolean fromPreviousSeason) {
        boolean isHome = m.getHomeTeamId().equals(teamId);
        double xgScored;
        double xgConceded;

        if (isHome) {
            xgScored = resolveXg(m.getStatistics().getManualHomeXg(),
                    m.getStatistics().getHomeShots(),
                    m.getStatistics().getHomeShotsOnTarget(),
                    m.getStatistics().getHomeScore());
            xgConceded = resolveXg(m.getStatistics().getManualAwayXg(),
                    m.getStatistics().getAwayShots(),
                    m.getStatistics().getAwayShotsOnTarget(),
                    m.getStatistics().getAwayScore());
        } else {
            xgScored = resolveXg(m.getStatistics().getManualAwayXg(),
                    m.getStatistics().getAwayShots(),
                    m.getStatistics().getAwayShotsOnTarget(),
                    m.getStatistics().getAwayScore());
            xgConceded = resolveXg(m.getStatistics().getManualHomeXg(),
                    m.getStatistics().getHomeShots(),
                    m.getStatistics().getHomeShotsOnTarget(),
                    m.getStatistics().getHomeScore());
        }

        return new TeamStrengthCalculator.MatchPerformance(xgScored, xgConceded, fromPreviousSeason);
    }

    private double resolveXg(Double manualXg, Integer totalShots, Integer shotsOnTarget, Integer goals) {
        return XgEstimator.resolveEffectiveXg(manualXg, totalShots, shotsOnTarget, goals)
                .orElse(TeamStrengthCalculator.DEFAULT_LEAGUE_AVG_XG);
    }

    @Override
    @Transactional
    public void deleteMatch(int matchId) {
        if (matchRepositoryPort.findById(matchId).isEmpty()) {
            throw new EntityNotFoundException(
                    String.format("Match with ID %d not found.", matchId)
            );
        }
        matchRepositoryPort.deleteById(matchId);
    }

    private Match findMatchOrThrow(int matchId) {
        return matchRepositoryPort.findById(matchId)
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format("Match with ID %d not found.", matchId)
                ));
    }

    private void validateEntitiesExist(int competitionId, int seasonId, int homeTeamId, int awayTeamId) {
        if (competitionRepositoryPort.findById(competitionId).isEmpty()) {
            throw new EntityNotFoundException(String.format("Competition with ID %d not found.", competitionId));
        }
        if (seasonRepositoryPort.findById(seasonId).isEmpty()) {
            throw new EntityNotFoundException(String.format("Season with ID %d not found.", seasonId));
        }
        if (teamRepositoryPort.findById(homeTeamId).isEmpty()) {
            throw new EntityNotFoundException(String.format("Home team with ID %d not found.", homeTeamId));
        }
        if (teamRepositoryPort.findById(awayTeamId).isEmpty()) {
            throw new EntityNotFoundException(String.format("Away team with ID %d not found.", awayTeamId));
        }
    }
}
