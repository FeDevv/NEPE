package org.nepe.competition.service;

import org.nepe.competition.domain.Competition;
import org.nepe.competition.port.in.CreateCompetitionCommand;
import org.nepe.competition.port.in.ManageCompetitionUseCase;
import org.nepe.competition.port.in.UpdateCompetitionCommand;
import org.nepe.competition.port.out.CompetitionRepositoryPort;
import org.nepe.shared.exception.DomainValidationException;
import org.nepe.shared.exception.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * Application Service implementing the {@link ManageCompetitionUseCase} Inbound Port.
 * <p>
 * Coordinates CRUD operations and parameter calibration for football leagues / competitions,
 * enforcing business uniqueness constraints and delegating domain validations to {@link Competition}.
 */
@Service
public class CompetitionService implements ManageCompetitionUseCase {

    private final CompetitionRepositoryPort competitionRepositoryPort;

    public CompetitionService(CompetitionRepositoryPort competitionRepositoryPort) {
        this.competitionRepositoryPort = Objects.requireNonNull(
                competitionRepositoryPort,
                "CompetitionRepositoryPort must not be null"
        );
    }

    @Override
    @Transactional
    public Competition createCompetition(CreateCompetitionCommand command) {
        if (command == null) {
            throw new DomainValidationException("CreateCompetitionCommand cannot be null.");
        }

        if (competitionRepositoryPort.existsByCode(command.code())) {
            throw new DomainValidationException(
                    String.format("A competition with code '%s' already exists.", command.code())
            );
        }

        double rho = (command.dixonColesRho() != null)
                ? command.dixonColesRho()
                : Competition.DEFAULT_DIXON_COLES_RHO;

        Competition competition = Competition.create(
                command.code(),
                command.name(),
                command.country(),
                rho
        );

        return competitionRepositoryPort.save(competition);
    }

    @Override
    @Transactional
    public Competition updateCompetition(UpdateCompetitionCommand command) {
        if (command == null) {
            throw new DomainValidationException("UpdateCompetitionCommand cannot be null.");
        }

        Competition competition = competitionRepositoryPort.findById(command.id())
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format("Competition with ID %d not found.", command.id())
                ));

        competition.updateDetails(command.name(), command.country());
        competition.updateDixonColesRho(command.dixonColesRho());

        return competitionRepositoryPort.save(competition);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Competition> getAllCompetitions() {
        return competitionRepositoryPort.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Competition getCompetitionById(int id) {
        return competitionRepositoryPort.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format("Competition with ID %d not found.", id)
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public Competition getCompetitionByCode(String code) {
        if (code == null || code.isBlank()) {
            throw new DomainValidationException("Competition code cannot be null or blank.");
        }

        return competitionRepositoryPort.findByCode(code.trim().toUpperCase())
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format("Competition with code '%s' not found.", code)
                ));
    }

    @Override
    @Transactional
    public void deleteCompetition(int id) {
        if (competitionRepositoryPort.findById(id).isEmpty()) {
            throw new EntityNotFoundException(
                    String.format("Competition with ID %d not found.", id)
            );
        }
        competitionRepositoryPort.deleteById(id);
    }
}
