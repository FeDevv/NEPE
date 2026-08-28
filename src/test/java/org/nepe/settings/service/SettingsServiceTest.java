package org.nepe.settings.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.nepe.settings.domain.AppSettings;
import org.nepe.settings.port.in.UpdateSettingsCommand;
import org.nepe.settings.port.out.SettingsRepositoryPort;
import org.nepe.shared.exception.DomainValidationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SettingsService Unit Tests")
class SettingsServiceTest {

    private InMemorySettingsRepository repository;
    private SettingsService service;

    @BeforeEach
    void setUp() {
        repository = new InMemorySettingsRepository();
        service = new SettingsService(repository);
    }

    @Nested
    @DisplayName("Constructor and Invariant Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should throw NullPointerException when repository port is null")
        void shouldThrowWhenRepositoryPortIsNull() {
            assertThatThrownBy(() -> new SettingsService(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("SettingsRepositoryPort must not be null");
        }
    }

    @Nested
    @DisplayName("getSettings() Tests")
    class GetSettingsTests {

        @Test
        @DisplayName("Should return stored settings from repository")
        void shouldReturnStoredSettings() {
            AppSettings custom = new AppSettings(0.02, 15, 0.80, 0.15);
            repository.saveSettings(custom);

            AppSettings result = service.getSettings();

            assertThat(result).isEqualTo(custom);
        }

        @Test
        @DisplayName("Should return default settings when repository contains null")
        void shouldReturnDefaultSettingsWhenRepositoryReturnsNull() {
            repository.setStoredSettings(null);

            AppSettings result = service.getSettings();

            assertThat(result).isEqualTo(AppSettings.defaults());
        }
    }

    @Nested
    @DisplayName("updateSettings() Tests")
    class UpdateSettingsTests {

        @Test
        @DisplayName("Should validate, update and persist new settings")
        void shouldUpdateAndPersistSettings() {
            UpdateSettingsCommand command = new UpdateSettingsCommand(0.03, 20, 0.65, 0.25);

            AppSettings updated = service.updateSettings(command);

            assertThat(updated.getCommissionRate()).isEqualTo(0.03);
            assertThat(updated.getDefaultNMatches()).isEqualTo(20);
            assertThat(updated.getSeasonalDecayGamma()).isEqualTo(0.65);
            assertThat(updated.getGreenUpProfitTarget()).isEqualTo(0.25);
            assertThat(repository.loadSettings()).isEqualTo(updated);
        }

        @Test
        @DisplayName("Should throw DomainValidationException when command is null")
        void shouldThrowWhenCommandIsNull() {
            assertThatThrownBy(() -> service.updateSettings(null))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("UpdateSettingsCommand cannot be null");
        }

        @Test
        @DisplayName("Should propagate DomainValidationException when command parameters violate domain rules")
        void shouldThrowWhenCommandParametersAreInvalid() {
            UpdateSettingsCommand invalidCommand = new UpdateSettingsCommand(1.50, 20, 0.65, 0.25);

            assertThatThrownBy(() -> service.updateSettings(invalidCommand))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("Commission rate must be in the range [0.0, 1.0)");
        }
    }

    @Nested
    @DisplayName("resetToDefaults() Tests")
    class ResetToDefaultsTests {

        @Test
        @DisplayName("Should reset settings to system defaults and persist")
        void shouldResetSettingsToDefaults() {
            repository.saveSettings(new AppSettings(0.02, 15, 0.80, 0.15));

            AppSettings result = service.resetToDefaults();

            assertThat(result).isEqualTo(AppSettings.defaults());
            assertThat(repository.loadSettings()).isEqualTo(AppSettings.defaults());
        }
    }

    /**
     * In-memory test double (Fake) implementing {@link SettingsRepositoryPort}.
     */
    private static class InMemorySettingsRepository implements SettingsRepositoryPort {
        private AppSettings storedSettings = AppSettings.defaults();

        @Override
        public AppSettings loadSettings() {
            return storedSettings;
        }

        @Override
        public AppSettings saveSettings(AppSettings settings) {
            this.storedSettings = settings;
            return this.storedSettings;
        }

        public void setStoredSettings(AppSettings storedSettings) {
            this.storedSettings = storedSettings;
        }
    }
}
