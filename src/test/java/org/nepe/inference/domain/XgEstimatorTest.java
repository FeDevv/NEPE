package org.nepe.inference.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.nepe.shared.exception.DomainValidationException;

import java.util.OptionalDouble;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

@DisplayName("XgEstimator Unit Tests")
class XgEstimatorTest {

    private static final double EPSILON = 1e-6;

    @Nested
    @DisplayName("Heuristic Calculation Tests")
    class HeuristicCalculationTests {

        @ParameterizedTest(name = "TotalShots: {0}, ShotsOnTarget: {1} -> Expected xG: {2}")
        @CsvSource({
                "10, 4, 1.50", // (4 * 0.30) + (6 * 0.05) = 1.20 + 0.30 = 1.50
                "0,  0, 0.00", // (0 * 0.30) + (0 * 0.05) = 0.00
                "5,  0, 0.25", // (0 * 0.30) + (5 * 0.05) = 0.25
                "5,  5, 1.50", // (5 * 0.30) + (0 * 0.05) = 1.50
                "20, 8, 3.00"  // (8 * 0.30) + (12 * 0.05) = 2.40 + 0.60 = 3.00
        })
        void shouldCalculateCorrectHeuristicXg(int totalShots, int shotsOnTarget, double expectedXg) {
            double actualXg = XgEstimator.estimate(totalShots, shotsOnTarget);
            assertThat(actualXg).isCloseTo(expectedXg, within(EPSILON));
        }

        @Test
        @DisplayName("Should throw exception when total shots is negative")
        void shouldThrowWhenTotalShotsNegative() {
            assertThatThrownBy(() -> XgEstimator.estimate(-1, 0))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("Total shots cannot be negative");
        }

        @Test
        @DisplayName("Should throw exception when shots on target is negative")
        void shouldThrowWhenShotsOnTargetNegative() {
            assertThatThrownBy(() -> XgEstimator.estimate(5, -1))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("Shots on target cannot be negative");
        }

        @Test
        @DisplayName("Should throw exception when shots on target exceeds total shots")
        void shouldThrowWhenShotsOnTargetExceedsTotalShots() {
            assertThatThrownBy(() -> XgEstimator.estimate(5, 6))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("cannot exceed total shots");
        }
    }

    @Nested
    @DisplayName("Effective xG Resolution Hierarchy Tests")
    class ResolutionHierarchyTests {

        @Test
        @DisplayName("Priority 1: Manual override should take precedence over shots and actual goals")
        void shouldPrioritizeManualOverride() {
            OptionalDouble result = XgEstimator.resolveEffectiveXg(2.45, 10, 4, 3);

            assertThat(result).isPresent();
            assertThat(result.getAsDouble()).isCloseTo(2.45, within(EPSILON));
        }

        @Test
        @DisplayName("Priority 2: Shot heuristic should take precedence when manual override is null")
        void shouldPrioritizeShotHeuristicOverFallbackGoals() {
            OptionalDouble result = XgEstimator.resolveEffectiveXg(null, 10, 4, 3);

            assertThat(result).isPresent();
            assertThat(result.getAsDouble()).isCloseTo(1.50, within(EPSILON));
        }

        @Test
        @DisplayName("Priority 3: Actual goals should be used when manual override and shot stats are absent")
        void shouldFallbackToActualGoals() {
            OptionalDouble result = XgEstimator.resolveEffectiveXg(null, null, null, 2);

            assertThat(result).isPresent();
            assertThat(result.getAsDouble()).isCloseTo(2.00, within(EPSILON));
        }

        @Test
        @DisplayName("Priority 4: Should return empty when no data is provided")
        void shouldReturnEmptyWhenNoDataAvailable() {
            OptionalDouble result = XgEstimator.resolveEffectiveXg(null, null, null, null);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should throw exception when manual xG is negative")
        void shouldThrowWhenManualXgNegative() {
            assertThatThrownBy(() -> XgEstimator.resolveEffectiveXg(-0.5, 10, 4, 1))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("Manual xG must be a non-negative");
        }

        @Test
        @DisplayName("Should throw exception when manual xG is NaN or Infinite")
        void shouldThrowWhenManualXgNotFinite() {
            assertThatThrownBy(() -> XgEstimator.resolveEffectiveXg(Double.NaN, null, null, null))
                    .isInstanceOf(DomainValidationException.class);

            assertThatThrownBy(() -> XgEstimator.resolveEffectiveXg(Double.POSITIVE_INFINITY, null, null, null))
                    .isInstanceOf(DomainValidationException.class);
        }

        @Test
        @DisplayName("Should throw exception when fallback goals is negative")
        void shouldThrowWhenFallbackGoalsNegative() {
            assertThatThrownBy(() -> XgEstimator.resolveEffectiveXg(null, null, null, -1))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("Fallback goals cannot be negative");
        }
    }
}
