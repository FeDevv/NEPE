package org.nepe.settings.port.out;

import org.nepe.settings.domain.AppSettings;

/**
 * Outbound Port (Driven Port / SPI) for persisting and retrieving application settings.
 * <p>
 * Decouples the domain and application use cases from the underlying storage mechanism
 * (e.g., MariaDB key-value table, properties file, or in-memory store).
 */
public interface SettingsRepositoryPort {

    /**
     * Loads the active application settings.
     * <p>
     * If no settings have been persisted yet, implementations should return the standard
     * default settings as provided by {@link AppSettings#defaults()}.
     *
     * @return the current active {@link AppSettings}
     */
    AppSettings loadSettings();

    /**
     * Persists the provided application settings.
     *
     * @param settings the {@link AppSettings} instance to store (must not be null)
     * @return the persisted {@link AppSettings} instance
     */
    AppSettings saveSettings(AppSettings settings);
}
