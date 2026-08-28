package org.nepe.settings.service;

import org.nepe.settings.domain.AppSettings;
import org.nepe.settings.port.in.ManageSettingsUseCase;
import org.nepe.settings.port.in.UpdateSettingsCommand;
import org.nepe.settings.port.out.SettingsRepositoryPort;
import org.nepe.shared.exception.DomainValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * Application Service implementing the {@link ManageSettingsUseCase} Inbound Port.
 * <p>
 * Orchestrates the lifecycle and configuration of application settings, coordinating
 * domain validations in {@link AppSettings} and persistence via {@link SettingsRepositoryPort}.
 */
@Service
public class SettingsService implements ManageSettingsUseCase {

    private final SettingsRepositoryPort settingsRepositoryPort;

    public SettingsService(SettingsRepositoryPort settingsRepositoryPort) {
        this.settingsRepositoryPort = Objects.requireNonNull(
                settingsRepositoryPort,
                "SettingsRepositoryPort must not be null"
        );
    }

    @Override
    @Transactional(readOnly = true)
    public AppSettings getSettings() {
        AppSettings settings = settingsRepositoryPort.loadSettings();
        return (settings != null) ? settings : AppSettings.defaults();
    }

    @Override
    @Transactional
    public AppSettings updateSettings(UpdateSettingsCommand command) {
        if (command == null) {
            throw new DomainValidationException("UpdateSettingsCommand cannot be null.");
        }

        // Instantiate domain entity which strictly validates all mathematical & business invariants
        AppSettings updatedSettings = new AppSettings(
                command.commissionRate(),
                command.defaultNMatches(),
                command.seasonalDecayGamma(),
                command.greenUpProfitTarget()
        );

        return settingsRepositoryPort.saveSettings(updatedSettings);
    }

    @Override
    @Transactional
    public AppSettings resetToDefaults() {
        AppSettings defaultSettings = AppSettings.defaults();
        return settingsRepositoryPort.saveSettings(defaultSettings);
    }
}
