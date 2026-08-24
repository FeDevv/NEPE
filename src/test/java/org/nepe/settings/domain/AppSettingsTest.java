package org.nepe.settings.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.nepe.shared.exception.DomainValidationException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

@DisplayName("AppSettings Unit Tests")
class AppSettingsTest {

    private static final double EPSILON = 1e-6;

    @Nested
    @DisplayName("Defaults and Key-Value Mapping Tests")
    class DefaultsAndMappingTests {

        @Test
        @DisplayName("defaults() should initialize standard parameters")
        void shouldInitializeDefaults() {
            AppSettings settings = AppSettings.defaults();

            assertThat(settings.getCommissionRate()).isCloseTo(0.05, within(EPSILON));
            assertThat(settings.getDefaultNMatches()).isEqualTo(10);
            assertThat(settings.getSeasonalDecayGamma()).isCloseTo(0.70, within(EPSILON));
            assertThat(settings.getGreenUpProfitTarget()).isCloseTo(0.10, within(EPSILON));
        }

        @Test
        @DisplayName("toMap() and fromMap() should round-trip accurately")
        void shouldRoundTripMapSerialization() {
            AppSettings original = new AppSettings(0.045, 12, 0.75, 0.15);
            Map<String, String> map = original.toMap();

            AppSettings reconstituted = AppSettings.fromMap(map);
            assertThat(reconstituted).isEqualTo(original);
        }

        @Test
        @DisplayName("fromMap() with empty or null map should fallback to default values")
        void shouldFallbackToDefaultsOnMissingKeys() {
            AppSettings fromEmpty = AppSettings.fromMap(Map.of());
            AppSettings fromNull = AppSettings.fromMap(null);

            assertThat(fromEmpty).isEqualTo(AppSettings.defaults());
            assertThat(fromNull).isEqualTo(AppSettings.defaults());
        }

        @Test
        @DisplayName("fromMap() with partial map should use defaults for missing values")
        void shouldHandlePartialMap() {
            Map<String, String> partial = Map.of(
                    AppSettings.KEY_COMMISSION_RATE, "0.02"
            );

            AppSettings settings = AppSettings.fromMap(partial);
            assertThat(settings.getCommissionRate()).isCloseTo(0.02, within(EPSILON));
            assertThat(settings.getDefaultNMatches()).isEqualTo(AppSettings.DEFAULT_N_MATCHES);
            assertThat(settings.getSeasonalDecayGamma()).isCloseTo(AppSettings.DEFAULT_SEASONAL_DECAY_GAMMA, within(EPSILON));
        }
    }

    @Nested
    @DisplayName("Mutation Tests")
    class MutationTests {

        @Test
        @DisplayName("Should mutate individual settings")
        void shouldMutateSettings() {
            AppSettings settings = AppSettings.defaults();

            settings.updateCommissionRate(0.02);
            settings.updateDefaultNMatches(15);
            settings.updateSeasonalDecayGamma(0.80);
            settings.updateGreenUpProfitTarget(0.20);

            assertThat(settings.getCommissionRate()).isCloseTo(0.02, within(EPSILON));
            assertThat(settings.getDefaultNMatches()).isEqualTo(15);
            assertThat(settings.getSeasonalDecayGamma()).isCloseTo(0.80, within(EPSILON));
            assertThat(settings.getGreenUpProfitTarget()).isCloseTo(0.20, within(EPSILON));
        }
    }

    @Nested
    @DisplayName("Invariant Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should throw on commission rate outside [0.0, 1.0)")
        void shouldThrowOnInvalidCommissionRate() {
            assertThatThrownBy(() -> new AppSettings(-0.01, 10, 0.70, 0.10))
                    .isInstanceOf(DomainValidationException.class);
            assertThatThrownBy(() -> new AppSettings(1.00, 10, 0.70, 0.10))
                    .isInstanceOf(DomainValidationException.class);
            assertThatThrownBy(() -> new AppSettings(Double.NaN, 10, 0.70, 0.10))
                    .isInstanceOf(DomainValidationException.class);
        }

        @Test
        @DisplayName("Should throw on sample size N outside [3, 100]")
        void shouldThrowOnInvalidSampleSize() {
            assertThatThrownBy(() -> new AppSettings(0.05, 2, 0.70, 0.10))
                    .isInstanceOf(DomainValidationException.class);
            assertThatThrownBy(() -> new AppSettings(0.05, 101, 0.70, 0.10))
                    .isInstanceOf(DomainValidationException.class);
        }

        @Test
        @DisplayName("Should throw on seasonal decay gamma outside (0.0, 1.0]")
        void shouldThrowOnInvalidGamma() {
            assertThatThrownBy(() -> new AppSettings(0.05, 10, 0.00, 0.10))
                    .isInstanceOf(DomainValidationException.class);
            assertThatThrownBy(() -> new AppSettings(0.05, 10, 1.05, 0.10))
                    .isInstanceOf(DomainValidationException.class);
        }

        @Test
        @DisplayName("Should throw on green-up profit target outside (0.0, 10.0]")
        void shouldThrowOnInvalidProfitTarget() {
            assertThatThrownBy(() -> new AppSettings(0.05, 10, 0.70, 0.00))
                    .isInstanceOf(DomainValidationException.class);
            assertThatThrownBy(() -> new AppSettings(0.05, 10, 0.70, 10.5))
                    .isInstanceOf(DomainValidationException.class);
        }
    }
}
