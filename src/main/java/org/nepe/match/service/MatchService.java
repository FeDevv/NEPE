package org.nepe.match.service;

import org.nepe.competition.port.out.CompetitionRepositoryPort;
import org.nepe.competition.port.out.SeasonRepositoryPort;
import org.nepe.competition.port.out.TeamRepositoryPort;
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

import java.util.List;
import java.util.Objects;

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

        int currentHomeRedCards = match.getStatistics().getHomeRedCards();
        int currentAwayRedCards = match.getStatistics().getAwayRedCards();

        MatchStatistics updatedStats = new MatchStatistics(
                command.homeScore(),
                command.awayScore(),
                command.homeShots(),
                command.awayShots(),
                command.homeShotsOnTarget(),
                command.awayShotsOnTarget(),
                currentHomeRedCards,
                currentAwayRedCards,
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
        return matchRepositoryPort.save(match);
    }

    @Override
    @Transactional
    public Match markAsCancelled(int matchId) {
        Match match = findMatchOrThrow(matchId);
        match.cancelMatch();
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
