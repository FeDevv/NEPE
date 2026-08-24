package org.nepe.settings.port.in;

import org.nepe.settings.domain.AppSettings;

/**
 * Inbound Port (Driving Port / Use Case) defining the business contract for managing application settings.
 * <p>
 * Invoked by Inbound Adapters (e.g. JavaFX UI Controllers) to read, update, or reset global settings.
 */
public interface ManageSettingsUseCase {

    /**
     * Retrieves the current active application settings.
     *
     * @return the current {@link AppSettings}
     */
    AppSettings getSettings();

    /**
     * Updates the application settings based on the supplied command data and persists them.
     *
     * @param command the validated {@link UpdateSettingsCommand} payload (must not be null)
     * @return the updated and persisted {@link AppSettings}
     */
    AppSettings updateSettings(UpdateSettingsCommand command);

    /**
     * Resets application settings to their system defaults and persists the changes.
     *
     * @return the newly restored default {@link AppSettings}
     */
    AppSettings resetToDefaults();
}
