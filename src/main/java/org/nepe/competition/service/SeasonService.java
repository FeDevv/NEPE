package org.nepe.competition.service;

import org.nepe.competition.domain.Season;
import org.nepe.competition.port.in.ManageSeasonUseCase;
import org.nepe.competition.port.out.SeasonRepositoryPort;
import org.nepe.shared.exception.DomainValidationException;
import org.nepe.shared.exception.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * Application Service implementing the {@link ManageSeasonUseCase} Inbound Port.
 * <p>
 * Coordinates seasonal partitioning, chronological sequence retrieval, and idempotent season registration
 * during CSV ingestion and UI selection workflows.
 */
@Service
public class SeasonService implements ManageSeasonUseCase {

    private final SeasonRepositoryPort seasonRepositoryPort;

    public SeasonService(SeasonRepositoryPort seasonRepositoryPort) {
        this.seasonRepositoryPort = Objects.requireNonNull(
                seasonRepositoryPort,
                "SeasonRepositoryPort must not be null"
        );
    }

    @Override
    @Transactional
    public Season getOrCreateSeason(String seasonName) {
        if (seasonName == null || seasonName.isBlank()) {
            throw new DomainValidationException("Season name cannot be null or blank.");
        }

        String normalizedName = seasonName.trim();
        return seasonRepositoryPort.findByName(normalizedName)
                .orElseGet(() -> {
                    Season newSeason = Season.create(normalizedName);
                    return seasonRepositoryPort.save(newSeason);
                });
    }

    @Override
    @Transactional
    public Season createSeason(String name) {
        if (name == null || name.isBlank()) {
            throw new DomainValidationException("Season name cannot be null or blank.");
        }

        String normalizedName = name.trim();
        if (seasonRepositoryPort.existsByName(normalizedName)) {
            throw new DomainValidationException(
                    String.format("Season '%s' already exists.", normalizedName)
            );
        }

        Season season = Season.create(normalizedName);
        return seasonRepositoryPort.save(season);
    }

    @Override
    @Transactional
    public Season createSeasonFromYear(int startYear) {
        Season season = Season.of(startYear);
        if (seasonRepositoryPort.existsByName(season.getName())) {
            throw new DomainValidationException(
                    String.format("Season '%s' already exists.", season.getName())
            );
        }

        return seasonRepositoryPort.save(season);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Season> getAllSeasons() {
        return seasonRepositoryPort.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Season getLatestSeason() {
        return seasonRepositoryPort.findLatest()
                .orElseThrow(() -> new EntityNotFoundException(
                        "No seasons currently registered in the database."
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public Season getSeasonById(int id) {
        return seasonRepositoryPort.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format("Season with ID %d not found.", id)
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public Season getSeasonByName(String name) {
        if (name == null || name.isBlank()) {
            throw new DomainValidationException("Season name cannot be null or blank.");
        }

        return seasonRepositoryPort.findByName(name.trim())
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format("Season with name '%s' not found.", name.trim())
                ));
    }

    @Override
    @Transactional
    public void deleteSeason(int id) {
        if (seasonRepositoryPort.findById(id).isEmpty()) {
            throw new EntityNotFoundException(
                    String.format("Season with ID %d not found.", id)
            );
        }
        seasonRepositoryPort.deleteById(id);
    }
}
